package com.iawolf.captureprobe;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import java.util.Set;
import android.speech.tts.UtteranceProgressListener;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Prueba de larga duración: escucha local por turnos + reinicio periódico del
 * motor STT. No es un sistema de escucha continua garantizada ni un LLM.
 *
 * Usa el motor de reconocimiento predeterminado de Android. El proveedor
 * instalado puede procesar audio mediante red; no se promete STT offline.
 */
public final class VoiceBridge {
 private static final long RECYCLE_MS=20L*60L*1000L;
 private static final long PAUSE_MS=4000L;
 private final Context context;
 private final Handler main=new Handler(Looper.getMainLooper());
 private SpeechRecognizer recognizer;
 private TextToSpeech tts;
 private boolean running=false,recycling=false,speaking=false,ttsReady=false;
 private int failures=0;
 private long listeningReceivedAt=0;
 private long voiceFinishedAt=0;

 private final Runnable retry=()->listen();
 private final Runnable periodicRecycle=()->recycleRecognizer();
 private final Runnable resumeAfterRecycle=()->{
  if(!running)return;
  recycling=false;
  try{
   createRecognizer();
   CaptureService.audioStatus="motor predeterminado reiniciado; retomando escucha";
   schedule(350);
   main.postDelayed(periodicRecycle,RECYCLE_MS);
  }catch(Exception e){
   CaptureService.audioStatus="falló reinicio de voz: "+e.getClass().getSimpleName();
   // Evitar un bucle rápido si el motor local se quedó sin recursos.
   recycling=true;
   main.postDelayed(()->{
    if(running){recycling=false;recycleRecognizer();}
   },60000L);
  }
 };

 private final RecognitionListener listener=new RecognitionListener(){
  @Override public void onReadyForSpeech(Bundle b){
   if(voiceFinishedAt>0){
    CaptureService.lastAudioRecoveryMs=Math.max(0,SystemClock.elapsedRealtime()-voiceFinishedAt);
    voiceFinishedAt=0;
   }
   CaptureService.audioStatus="escuchando con reconocedor predeterminado";
  }
  @Override public void onBeginningOfSpeech(){CaptureService.audioStatus="voz detectada";}
  @Override public void onRmsChanged(float rms){}
  @Override public void onBufferReceived(byte[] b){}
  @Override public void onEndOfSpeech(){CaptureService.audioStatus="procesando frase";}
  @Override public void onError(int code){
   if(!running || recycling)return;
   finishListening(false);
   CaptureService.audioStatus="error de reconocimiento "+code;
   if(code==SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS){
    CaptureService.audioStatus="falta permiso de micrófono";
    stop();return;
   }
   if(code==SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
      code==SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE){
    CaptureService.audioStatus="español no disponible en motor predeterminado (error "+code+")";
    stop();return;
   }
   if(code==SpeechRecognizer.ERROR_NO_MATCH || code==SpeechRecognizer.ERROR_SPEECH_TIMEOUT){
    failures=0;schedule(1500);return;
   }
   failures++;
   if(code==SpeechRecognizer.ERROR_TOO_MANY_REQUESTS) {
    CaptureService.audioStatus="límite del reconocedor; descanso de 60 segundos";
    schedule(60000);return;
   }
   if(code==SpeechRecognizer.ERROR_RECOGNIZER_BUSY){
    CaptureService.audioStatus="motor ocupado; reintento de 10 segundos";
    schedule(10000);return;
   }
   if(failures>=10){
    failures=0;
    CaptureService.audioStatus="demasiados errores; recuperando motor en 60 segundos";
    main.removeCallbacks(periodicRecycle);
    main.postDelayed(periodicRecycle,60000);
    return;
   }
   schedule(Math.min(15000L,1500L*failures));
  }
  @Override public void onResults(Bundle bundle){
   if(!running || recycling)return;
   failures=0;
   ArrayList<String> matches=bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
   if(matches==null || matches.isEmpty()){finishListening(false);schedule(650);return;}
   String heard=matches.get(0);
   finishListening(true);
   CaptureService.recognizedPhrases++;
   CaptureService.lastHeard=heard;
   CaptureService.audioStatus="frase reconocida";
   String reply=ControlBrain.replyTo(heard,CaptureService.active,CaptureService.frames,CaptureService.changes);
   if(reply==null){CaptureService.audioStatus="frase sin activación CONTROL: "+heard;schedule(650);return;}
   CaptureService.lastReply=reply;
   say(reply);
  }
  @Override public void onPartialResults(Bundle b){}
  @Override public void onEvent(int type,Bundle b){}
 };

 public VoiceBridge(Context ctx){context=ctx.getApplicationContext();}

 public void start(){
  main.post(()->{
   if(running)return;
   if(!SpeechRecognizer.isRecognitionAvailable(context)){
    CaptureService.audioStatus="sin servicio de reconocimiento de Android";
    return;
   }
   running=true;recycling=false;speaking=false;failures=0;
   try{
    createRecognizer();
    initTts();
    main.postDelayed(periodicRecycle,RECYCLE_MS);
    schedule(400);
   }catch(Exception e){
    CaptureService.audioStatus="no se pudo iniciar STT: "+e.getClass().getSimpleName();
    stop();
   }
  });
 }
 private void createRecognizer(){
  if(recognizer!=null)throw new IllegalStateException("reconocedor duplicado");
  recognizer=SpeechRecognizer.createSpeechRecognizer(context);
  recognizer.setRecognitionListener(listener);
 }
 private void initTts(){
  tts=new TextToSpeech(context,code->main.post(()->{
   if(!running || tts==null)return;
   if(code!=TextToSpeech.SUCCESS){
    CaptureService.audioStatus="TTS provisional no disponible";return;
   }
   int result=tts.setLanguage(new Locale("es","AR"));
   // No usar español de España como alternativa automática.
   ttsReady=result!=TextToSpeech.LANG_MISSING_DATA && result!=TextToSpeech.LANG_NOT_SUPPORTED;
   if(!ttsReady){CaptureService.audioStatus="no hay voz argentina instalada";return;}
   if(!chooseArgentineVoice()){
    ttsReady=false;
    CaptureService.audioStatus="no hay voz argentina es-AR disponible: respuesta solo escrita";
    return;
   }
   tts.setSpeechRate(1.06f);
   tts.setPitch(0.88f);
   tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){
    @Override public void onStart(String id){}
    @Override public void onDone(String id){main.post(()->{
     CaptureService.audioStatus="respuesta por voz finalizada";
     voiceFinishedAt=SystemClock.elapsedRealtime();
     speaking=false;
     schedule(900);
    });}
    @Override public void onError(String id){main.post(()->{
     CaptureService.audioStatus="error de síntesis de voz argentina";
     speaking=false;
     voiceFinishedAt=0;
     schedule(900);
    });}
   });
  }));
 }

 // Android does not expose a gender field for TTS voices. Prefer a Spanish
 // embedded voice of higher reported quality; verify perceived gender on device.
 private boolean chooseArgentineVoice(){
  try{
   Set<Voice> voices=tts.getVoices();
   if(voices==null)return false;
   Voice best=null;int score=Integer.MIN_VALUE;
   for(Voice v:voices){
    Locale l=v.getLocale();
    if(l==null || !"es".equalsIgnoreCase(l.getLanguage()) || !"AR".equalsIgnoreCase(l.getCountry()))continue;
    int n=v.getQuality()*10+(v.isNetworkConnectionRequired()?-1:2);
    // Android TTS no informa género; elegimos calidad, nunca acento es-ES.
    if(best==null || n>score){best=v;score=n;}
   }
   return best!=null && tts.setVoice(best)==TextToSpeech.SUCCESS;
  }catch(Exception ignored){return false;}
 }
 private void finishListening(boolean recognized){
  long during=Math.max(0,CaptureService.received-listeningReceivedAt);
  CaptureService.framesDuringLastListen=during;
  if(recognized && during>0 && CaptureService.active)
   CaptureService.simultaneousAvObserved=true;
 }
 private void listen(){
  if(!running || recycling || recognizer==null || speaking)return;
  try{
   Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
   i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
   i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"es-AR");
   i.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,false);
   i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false);
   listeningReceivedAt=CaptureService.received;
   recognizer.startListening(i);
   CaptureService.listenAttempts++;
   CaptureService.audioStatus="esperando frase (motor predeterminado)";
  }catch(Exception e){
   CaptureService.audioStatus="excepción STT: "+e.getClass().getSimpleName();
   failures++;
   if(failures<10)schedule(10000);
   else {failures=0;main.removeCallbacks(periodicRecycle);main.postDelayed(periodicRecycle,60000);}
  }
 }
 private void say(String words){
  main.removeCallbacks(retry);
  if(!running)return;
  if(tts==null || !ttsReady){
   CaptureService.audioStatus="respuesta escrita; voz argentina es-AR no disponible";
   schedule(750);return;
  }
  speaking=true;
  CaptureService.audioStatus="enviando respuesta a voz argentina";
  if(tts.speak(words,TextToSpeech.QUEUE_FLUSH,null,"control_reply")==TextToSpeech.ERROR){
   CaptureService.audioStatus="falló reproducción de voz";
   speaking=false;voiceFinishedAt=0;schedule(900);
  }
 }
 private void schedule(long delay){
  if(!running || recycling || speaking)return;
  main.removeCallbacks(retry);
  main.postDelayed(retry,delay);
 }
 private void recycleRecognizer(){
  if(!running)return;
  recycling=true;
  CaptureService.audioStatus="descanso de voz (4 segundos); captura continúa";
  CaptureService.voiceRestarts++;
  main.removeCallbacks(retry);
  main.removeCallbacks(periodicRecycle);
  if(recognizer!=null){
   try{recognizer.cancel();recognizer.destroy();}catch(Exception ignored){}
   recognizer=null;
  }
  main.postDelayed(resumeAfterRecycle,PAUSE_MS);
 }
 public void stop(){
  main.post(()->{
   running=false;recycling=false;speaking=false;
   main.removeCallbacks(retry);
   main.removeCallbacks(periodicRecycle);
   main.removeCallbacks(resumeAfterRecycle);
   if(recognizer!=null){
    try{recognizer.cancel();recognizer.destroy();}catch(Exception ignored){}
    recognizer=null;
   }
   if(tts!=null){tts.stop();tts.shutdown();tts=null;}
   ttsReady=false;voiceFinishedAt=0;
  });
 }
}
