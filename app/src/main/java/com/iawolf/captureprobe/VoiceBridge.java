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
import android.speech.tts.UtteranceProgressListener;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Diagnóstico audiovisual sin servidores: reconocimiento exclusivamente on-device.
 * No graba archivos ni transmite voz. Si Android no tiene motor offline, se informa.
 * El motor TTS es provisional y puede elegir una voz femenina (se ajustará después).
 */
public final class VoiceBridge {
 private final Context context;
 private final Handler main=new Handler(Looper.getMainLooper());
 private final Runnable retry=new Runnable(){@Override public void run(){listen();}};
 private SpeechRecognizer recognizer;
 private TextToSpeech tts;
 private boolean running=false, speaking=false, ttsReady=false;
 private int failures=0;
 private long listeningReceivedAt=0;
 private long sessionStartedAt=0;
 // SpeechRecognizer es una prueba puntual, no escucha permanente.
 private static final long TEST_WINDOW_MS=180000L;
 private void finishListening(){
  long during=Math.max(0,CaptureService.received-listeningReceivedAt);
  CaptureService.framesDuringLastListen=during;
  if(during>0 && CaptureService.active)CaptureService.simultaneousAvObserved=true;
 }
 private final RecognitionListener listener=new RecognitionListener(){
  @Override public void onReadyForSpeech(Bundle b){CaptureService.audioStatus="escuchando (local)";}
  @Override public void onBeginningOfSpeech(){CaptureService.audioStatus="voz detectada";}
  @Override public void onRmsChanged(float rms){}
  @Override public void onBufferReceived(byte[] b){}
  @Override public void onEndOfSpeech(){CaptureService.audioStatus="procesando voz local";}
  @Override public void onError(int error){
   if(!running)return;
   finishListening();
   CaptureService.audioStatus="reconocimiento local: código "+error;
   if(error==SpeechRecognizer.ERROR_SPEECH_TIMEOUT || error==SpeechRecognizer.ERROR_NO_MATCH){
    schedule(1500);return;
   }
   if(error==SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
      error==SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE){
    CaptureService.audioStatus="el motor local no tiene español compatible";
    stop();return;
   }
   if(error==SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ||
      error==SpeechRecognizer.ERROR_CLIENT){
    // Un error cliente ocasional puede recuperarse, sin hacer un bucle agresivo.
    if(error==SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS){stop();return;}
   }
   failures++;
   if(failures>10){CaptureService.audioStatus="reconocimiento pausado tras errores";return;}
   schedule(Math.min(8000L,1200L*failures));
  }
  @Override public void onResults(Bundle results){
   if(!running)return;
   finishListening();
   failures=0;
   ArrayList<String> matches=results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
   if(matches==null || matches.isEmpty()){schedule(500);return;}
   String heard=matches.get(0);
   CaptureService.recognizedPhrases++;
   CaptureService.lastHeard=heard;
   CaptureService.audioStatus="texto recibido (local)";
   String reply=ControlBrain.replyTo(heard,CaptureService.active,CaptureService.frames,CaptureService.changes);
   if(reply==null){schedule(650);return;}
   CaptureService.lastReply=reply;
   say(reply);
  }
  @Override public void onPartialResults(Bundle results){}
  @Override public void onEvent(int eventType,Bundle params){}
 };
 public VoiceBridge(Context context){this.context=context.getApplicationContext();}
 public void start(){
  main.post(()->{
   if(running)return;
   if(Build.VERSION.SDK_INT<31 || !SpeechRecognizer.isOnDeviceRecognitionAvailable(context)){
    CaptureService.audioStatus="sin reconocimiento local instalado en Android";
    return;
   }
   running=true;failures=0;
   sessionStartedAt=SystemClock.elapsedRealtime();
   try{
    recognizer=SpeechRecognizer.createOnDeviceSpeechRecognizer(context);
    recognizer.setRecognitionListener(listener);
    tts=new TextToSpeech(context,code->main.post(()->{
     if(!running || tts==null)return;
     if(code==TextToSpeech.SUCCESS){
      int l=tts.setLanguage(new Locale("es","AR"));
      if(l==TextToSpeech.LANG_MISSING_DATA || l==TextToSpeech.LANG_NOT_SUPPORTED)
       l=tts.setLanguage(new Locale("es","ES"));
      ttsReady=l!=TextToSpeech.LANG_MISSING_DATA && l!=TextToSpeech.LANG_NOT_SUPPORTED;
      if(ttsReady){
       tts.setSpeechRate(1.0f);
       tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){
        @Override public void onStart(String id){}
        @Override public void onDone(String id){main.post(()->{speaking=false;schedule(650);});}
        @Override public void onError(String id){main.post(()->{speaking=false;schedule(650);});}
       });
      }
     }
    }));
    schedule(300);
   }catch(Exception e){
    CaptureService.audioStatus="error inicialización voz: "+e.getClass().getSimpleName();
    stop();
   }
  });
 }
 private void listen(){
  if(!running || recognizer==null || speaking)return;
  if(SystemClock.elapsedRealtime()-sessionStartedAt>=TEST_WINDOW_MS){
   CaptureService.audioStatus="prueba local de escucha terminada (3 minutos)";
   stop();return;
  }
  try{
   Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
   i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
   i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"es-AR");
   i.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true);
   i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false);
   listeningReceivedAt=CaptureService.received;
   recognizer.startListening(i);
   CaptureService.listenAttempts++;
   CaptureService.audioStatus="esperando voz (local)";
  }catch(Exception e){
   CaptureService.audioStatus="error al escuchar: "+e.getClass().getSimpleName();
   failures++;
   if(failures<=10)schedule(2000);
  }
 }
 private void say(String reply){
  main.removeCallbacks(retry);
  if(!running)return;
  if(!ttsReady || tts==null){
   CaptureService.audioStatus="texto reconocido, voz TTS no disponible";
   schedule(1000);
   return;
  }
  speaking=true;CaptureService.audioStatus="respondiendo por voz";
  int code=tts.speak(reply,TextToSpeech.QUEUE_FLUSH,null,"control_av_reply");
  if(code==TextToSpeech.ERROR){speaking=false;schedule(1000);}
 }
 private void schedule(long delay){
  if(!running || speaking)return;
  main.removeCallbacks(retry);
  main.postDelayed(retry,delay);
 }
 public void stop(){
  main.post(()->{
   running=false;speaking=false;
   main.removeCallbacks(retry);
   if(recognizer!=null){
    try{recognizer.cancel();recognizer.destroy();}catch(Exception ignored){}
    recognizer=null;
   }
   if(tts!=null){tts.stop();tts.shutdown();tts=null;}
  });
 }
}
