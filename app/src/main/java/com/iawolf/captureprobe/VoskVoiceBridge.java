package com.iawolf.captureprobe;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.audiofx.NoiseSuppressor;
import android.media.audiofx.AcousticEchoCanceler;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.StorageService;
import java.util.Locale;
import java.util.Set;

/**
 * Offline Vosk input + local Android TTS output. This bridge REQUIRES a bundled
 * Spanish model under app/src/main/assets/model-es (provided at build time).
 * Never silently falls back to Android's possibly network-based STT.
 */
public final class VoskVoiceBridge implements RecognitionListener {
 private static final long RECYCLE_MS=15L*60L*1000L;
 private final Context context;
 private final Handler main=new Handler(Looper.getMainLooper());
 private SpeechService speech;
 private NoiseSuppressor noiseSuppressor;
 private AcousticEchoCanceler echoCanceler;
 private Model model;
 private TextToSpeech tts;
 private PiperVoiceOutput piper;
 private boolean piperDisabledThisSession=false;
 private boolean running=false, speaking=false, prepared=false, ttsReady=false;
 private boolean unexplainedIntentNotified=false;
 private long lastTtsFinished=-1L, listenFrameBaseline=0L;
 private final Runnable recycle=()->{
  if(!running) return;
  CaptureService.voiceRestarts++;
  haltListening();
  CaptureService.audioStatus="reciclando Vosk (captura continúa)";
  main.postDelayed(()->{if(running) beginListening();}, 1500L);
  main.postDelayed(this::recycleNow, RECYCLE_MS);
 };
 private void recycleNow(){recycle.run();}
 public VoskVoiceBridge(Context context){this.context=context.getApplicationContext();}

 public void start(){
  main.post(()->{
   if(running)return;
   running=true;unexplainedIntentNotified=false;piperDisabledThisSession=false;
   CaptureService.audioStatus="cargando modelo español Vosk sin conexión";
   initTts();
   try {
    StorageService.unpack(context,"model-es","model-es", unpacked -> main.post(()->{
      if(!running){unpacked.close();return;}
      model=unpacked;prepared=true;beginListening();
      main.postDelayed(recycle,RECYCLE_MS);
    }), failure -> main.post(()->{
      CaptureService.speechErrors++;
      CaptureService.audioStatus="Vosk: falta el modelo español empaquetado o no pudo extraerse";
      CaptureService.error="Modelo Vosk: "+failure.getClass().getSimpleName();
    }));
   }catch(Exception e){
    CaptureService.speechErrors++;
    CaptureService.audioStatus="No pudo cargar Vosk: "+e.getClass().getSimpleName();
   }
  });
 }

 private void beginListening(){
  if(!running||!prepared||speaking||model==null||speech!=null)return;
  try{
   speech=new SpeechService(new Recognizer(model,16000.0f),16000.0f);
   applyAudioFiltersIfSupported();
   listenFrameBaseline=CaptureService.received;
   CaptureService.listenAttempts++;
   speech.startListening(this);
   CaptureService.audioStatus="escuchando (Vosk local)";
   if(lastTtsFinished>=0){
    CaptureService.lastAudioRecoveryMs=SystemClock.elapsedRealtime()-lastTtsFinished;
    lastTtsFinished=-1L;
   }
  }catch(Exception e){
   CaptureService.speechErrors++;
   CaptureService.audioStatus="Error al iniciar micrófono Vosk: "+e.getClass().getSimpleName();
   haltListening();
  }
 }

 /**
  * Vosk's latest SpeechService exposes the AudioRecord session ID.
  * Older binaries may not. Detect capability at runtime; never block STT.
  * AEC/NS reduce leakage but cannot isolate the user's identity.
  */
 private void applyAudioFiltersIfSupported(){
  if(speech==null)return;
  try {
   Object raw=speech.getClass().getMethod("getAudioSessionId").invoke(speech);
   int session=((Number)raw).intValue();
   if(session<=0)return;
   if(NoiseSuppressor.isAvailable()){
    noiseSuppressor=NoiseSuppressor.create(session);
    if(noiseSuppressor!=null)noiseSuppressor.setEnabled(true);
   }
   if(AcousticEchoCanceler.isAvailable()){
    echoCanceler=AcousticEchoCanceler.create(session);
    if(echoCanceler!=null)echoCanceler.setEnabled(true);
   }
  } catch(Exception unavailable){
   CaptureService.audioStatus="Vosk activo; filtros DSP no disponibles en esta versión";
  }
 }
 private void releaseAudioFilters(){
  if(noiseSuppressor!=null){noiseSuppressor.release();noiseSuppressor=null;}
  if(echoCanceler!=null){echoCanceler.release();echoCanceler=null;}
 }
 private void haltListening(){
  SpeechService old=speech;speech=null;
  releaseAudioFilters();
  if(old!=null){
   try{old.stop();}catch(Exception ignored){}
   try{old.shutdown();}catch(Exception ignored){}
  }
 }

 @Override public void onPartialResult(String json){
  if(!running||speaking)return;
  try{
   String partial=new JSONObject(json).optString("partial","").trim();
   if(partial.isEmpty())return;
   // Only keep the latest small text in RAM. No microphone recordings or uploads.
   CaptureService.voskPartial=partial.length()>120?partial.substring(0,120):partial;
   CaptureService.voskPartialUpdates++;
  }catch(Exception ignored){}
 }
 @Override public void onResult(String json){acceptResult(json);}
 @Override public void onFinalResult(String json){/* end-of-stream only; avoid duplicate reply */}
 @Override public void onError(Exception e){
  main.post(()->{
   if(!running||speaking)return;
   CaptureService.speechErrors++;
   CaptureService.audioStatus="Vosk error: "+e.getClass().getSimpleName();
   haltListening();
   main.postDelayed(()->{if(running)beginListening();},4000L);
  });
 }
 @Override public void onTimeout(){
  main.post(()->{if(running&&!speaking){haltListening();beginListening();}});
 }

 private void acceptResult(String json){
  main.post(()->{
   if(!running||speaking)return;
   String heard="";
   try{heard=new JSONObject(json).optString("text","").trim();}catch(Exception ignored){}
   if(heard.isEmpty())return;
   CaptureService.recognizedPhrases++;
   CaptureService.lastHeard=heard;
   boolean wake=WakeGate.isActivated(heard);
   boolean freeDiagnostic=context.getSharedPreferences("control_voice_diag",Context.MODE_PRIVATE)
      .getBoolean("free_listen",false);
   // A temporary opt-in test, not the normal voice assistant. Restrict to
   // nontrivial phrases; background TV/game speech can still trigger this mode.
   String[] words=heard.trim().split("\\s+");
   boolean freeAccept=freeDiagnostic && words.length>=2 && words.length<=20;
   CaptureService.wakeStatus=wake?"Comando CONTROL reconocido":
      (freeAccept?"Prueba sin CONTROL: texto aceptado para intérprete básico":
       "Texto transcrito sin comando reconocible");
   long during=Math.max(0L,CaptureService.received-listenFrameBaseline);
   CaptureService.framesDuringLastListen=during;
   if(during>0)CaptureService.simultaneousAvObserved=true;
   AvContext.Snapshot visual=AvContext.snapshot(CaptureService.sessionStartedAt,SystemClock.elapsedRealtime());
   String mode=context.getSharedPreferences("control_mode",Context.MODE_PRIVATE).getString("mode","coach");
   String reply=(wake||freeAccept)?
     LocalIntent.reply(heard,mode,visual.fresh,visual.dark,CaptureService.active):null;
   if(reply!=null){CaptureService.lastReply=reply;say(reply);}
   else if(wake||freeAccept){
    CaptureService.wakeStatus="Vosk escuchó correctamente; falta intérprete conversacional para esta pregunta";
    CaptureService.lastReply="Sin respuesta conversacional: falta integrar modelo local";
    if(!unexplainedIntentNotified){
     unexplainedIntentNotified=true;
     say("Te escucho, Wolf, pero todavía no puedo conversar libremente. Estoy usando respuestas básicas mientras preparamos mi cerebro local.");
    }
   } else CaptureService.wakeStatus="Texto transcrito, pero no pasó el filtro de activación";
  });
 }

 private void initTts(){
  tts=new TextToSpeech(context,code->main.post(()->{
   if(!running||tts==null)return;
   if(code!=TextToSpeech.SUCCESS){CaptureService.ttsStatus="TTS no disponible";return;}
   if(!applyPreferredVoice())return;
   tts.setSpeechRate(1.06f);tts.setPitch(0.88f);
   tts.setAudioAttributes(new AudioAttributes.Builder()
     .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
     .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
   tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){
    @Override public void onStart(String id){CaptureService.ttsStarted++;CaptureService.ttsStatus="hablando";}
    @Override public void onDone(String id){main.post(()->finishSpeech(true));}
    @Override public void onError(String id){main.post(()->finishSpeech(false));}
   });
  }));
 }

 /** Applies the same saved device voice to actual session responses, not only previews. */
 private boolean applyPreferredVoice(){
  if(tts==null)return false;
  Set<Voice> voices=tts.getVoices();Voice best=null;int rank=-1;
  String saved=context.getSharedPreferences("control_voice_settings",Context.MODE_PRIVATE)
      .getString("voice_name","");
  if(voices!=null)for(Voice v:voices){
   if(v.isNetworkConnectionRequired())continue;
   Locale loc=v.getLocale();if(loc==null)continue;
   if(v.getName().equals(saved)){best=v;break;}
   if(!"es".equalsIgnoreCase(loc.getLanguage()))continue;
   int score="AR".equalsIgnoreCase(loc.getCountry())?30:
      "UY".equalsIgnoreCase(loc.getCountry())?25:
      "MX".equalsIgnoreCase(loc.getCountry())?20:10;
   if(score>rank){rank=score;best=v;}
  }
  if(best==null||tts.setVoice(best)!=TextToSpeech.SUCCESS){
   ttsReady=false;
   CaptureService.ttsStatus="sin voz local compatible disponible";
   return false;
  }
  ttsReady=true;
  CaptureService.ttsVoice=best.getName()+" ("+best.getLocale().toLanguageTag()+")";
  CaptureService.ttsStatus="TTS local preparado";
  return true;
 }
 public void refreshSelectedVoice(){
  main.post(()->{
   if(!running)return;
   if(speaking){
    CaptureService.ttsStatus="Cambio de voz pendiente hasta que termine de hablar";
    main.postDelayed(this::refreshSelectedVoice,1000L);
    return;
   }
   applyPreferredVoice();
  });
 }

 /** Diagnostic through the same TTS and microphone pause/restart path as live replies. */
 public void diagnosticReply(){
  main.post(()->{
   if(!running){CaptureService.wakeStatus="Diagnóstico: sesión de voz inactiva";return;}
   String mode=context.getSharedPreferences("control_mode",Context.MODE_PRIVATE)
       .getString("mode","coach");
   String reply="companero".equals(mode)
       ?"Wolf, estoy en modo compañero de diagnóstico. Te escucho cuando Vosk reconoce CONTROL."
       :"Wolf, estoy en modo coach de diagnóstico. La voz funciona, pero todavía no doy consejos tácticos.";
   CaptureService.lastReply=reply;
   CaptureService.wakeStatus="Prueba manual de voz; sin reconocimiento de micrófono";
   say(reply);
  });
 }
 private void say(String words){
  boolean usePiper=!piperDisabledThisSession && "piper".equals(context.getSharedPreferences(
     "control_voice_settings",Context.MODE_PRIVATE).getString("voice_source","device"));
  if(!usePiper && (!ttsReady||tts==null)){
   CaptureService.ttsStatus="respuesta disponible solo en texto";return;
  }
  speaking=true;
  haltListening(); // Never transcribe CONTROL's own speech.
  if(usePiper){
   CaptureService.ttsStarted++;
   CaptureService.ttsStatus="Piper: generando voz sin conexión (laboratorio)";
   CaptureService.ttsVoice="Piper Ald (es-MX, provisional)";
   if(piper==null)piper=new PiperVoiceOutput(context);
   final long piperStartedAt=SystemClock.elapsedRealtime();
   piper.speak(words,(ok,reason)->{
    CaptureService.piperLastMs=SystemClock.elapsedRealtime()-piperStartedAt;
    CaptureService.piperStatus=(ok?"Piper OK: ":"Piper ERROR: ")+reason;
    if(!running)return;
    if(ok){
     CaptureService.ttsStatus="Piper: "+reason;
     finishSpeech(true);
    }else{
     piperDisabledThisSession=true;
     CaptureService.ttsStatus="Piper falló: "+reason+"; usando voz del dispositivo";
     if(ttsReady&&tts!=null){
      if(tts.speak(words,TextToSpeech.QUEUE_FLUSH,null,"control_reply")!=TextToSpeech.SUCCESS)
       finishSpeech(false);
     }else finishSpeech(false);
    }
   });
   return;
  }
  if(tts.speak(words,TextToSpeech.QUEUE_FLUSH,null,"control_reply")!=TextToSpeech.SUCCESS)
   finishSpeech(false);
 }
 private void finishSpeech(boolean ok){
  if(!running)return;
  if(ok){CaptureService.ttsCompleted++;CaptureService.ttsStatus="respuesta completada";}
  else {CaptureService.ttsFailed++;CaptureService.ttsStatus="falló TTS";}
  speaking=false;lastTtsFinished=SystemClock.elapsedRealtime();
  main.postDelayed(()->{if(running)beginListening();},900L);
 }

 public void stop(){
  // Never leave no-wake diagnostics enabled after an active voice session ends.
  context.getSharedPreferences("control_voice_diag",Context.MODE_PRIVATE)
    .edit().putBoolean("free_listen",false).apply();
  main.post(()->{
   running=false;speaking=false;prepared=false;
   main.removeCallbacksAndMessages(null);
   haltListening();
   if(piper!=null){piper.close();piper=null;}
   if(tts!=null){tts.stop();tts.shutdown();tts=null;}
   if(model!=null){model.close();model=null;}
  });
 }
}
