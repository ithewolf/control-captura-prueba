package com.iawolf.captureprobe;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;
import com.k2fsa.sherpa.onnx.GeneratedAudio;
import com.k2fsa.sherpa.onnx.OfflineTts;
import com.k2fsa.sherpa.onnx.OfflineTtsConfig;
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig;
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Lab4-only, opt-in Piper engine. Never blocks mic or Android main thread. */
final class PiperVoiceOutput {
 interface Done {void completed(boolean ok, String reason);}
 private static final String DIR="piper/vits-piper-es_MX-ald-medium/";
 private final Context context;
 private final Handler main=new Handler(Looper.getMainLooper());
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private volatile boolean closed=false;
 private volatile AudioTrack activeTrack;
 private OfflineTts engine;
 PiperVoiceOutput(Context context){this.context=context.getApplicationContext();}
 void speak(String words, Done callback){
  worker.execute(()->{
   boolean ok=false;String reason="Sin audio";
   try{
    if(closed)throw new IllegalStateException("Piper detenido");
    if(engine==null){
     OfflineTtsVitsModelConfig vits=new OfflineTtsVitsModelConfig();
     vits.setModel(DIR+"es_MX-ald-medium.onnx");
     vits.setTokens(DIR+"tokens.txt");
     vits.setDataDir(DIR+"espeak-ng-data");
     OfflineTtsModelConfig model=new OfflineTtsModelConfig();
     model.setVits(vits);
     model.setNumThreads(1);model.setProvider("cpu");
     OfflineTtsConfig cfg=new OfflineTtsConfig();
     cfg.setModel(model);
     engine=new OfflineTts(context.getAssets(),cfg);
    }
    GeneratedAudio audio=engine.generate(words,0,1.0f);
    float[] samples=audio.getSamples();int rate=audio.getSampleRate();
    if(samples==null||samples.length<100||rate<8000)
     throw new IllegalStateException("Piper devolvió audio vacío");
    int min=AudioTrack.getMinBufferSize(rate,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);
    if(min<=0)throw new IllegalStateException("Formato de audio no soportado");
    int buffer=Math.max(min,rate/2);
    AudioTrack track=new AudioTrack.Builder()
      .setAudioAttributes(new AudioAttributes.Builder()
         .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
         .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
      .setAudioFormat(new AudioFormat.Builder()
         .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
         .setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
      .setTransferMode(AudioTrack.MODE_STREAM).setBufferSizeInBytes(buffer).build();
    activeTrack=track;
    try{
     if(track.getState()!=AudioTrack.STATE_INITIALIZED)throw new IllegalStateException("AudioTrack no iniciado");
     track.play();
     short[] block=new short[2048];
     int pos=0;
     while(pos<samples.length&&!closed){
      int count=Math.min(block.length,samples.length-pos);
      for(int i=0;i<count;i++){
       float v=Math.max(-1f,Math.min(1f,samples[pos+i]));
       block[i]=(short)(v*32767f);
      }
      int written=track.write(block,0,count,AudioTrack.WRITE_BLOCKING);
      if(written<=0)throw new IllegalStateException("AudioTrack error "+written);
      pos+=written;
     }
     if(closed)throw new IllegalStateException("Piper interrumpido");
     // Wait for queued PCM playback to finish before resuming microphone.
     int queued=track.getPlaybackHeadPosition();
     long deadline=android.os.SystemClock.elapsedRealtime()+Math.min(2000,1000L*buffer/rate+600);
     while(queued<samples.length && android.os.SystemClock.elapsedRealtime()<deadline&&!closed){
      android.os.SystemClock.sleep(25);
      queued=track.getPlaybackHeadPosition();
     }
     ok=!closed;
     reason=ok?"audio Piper reproducido":"audio Piper interrumpido";
    }finally{
     try{track.stop();}catch(Exception ignored){}
     track.release();activeTrack=null;
    }
   }catch(Throwable problem){
    reason=problem.getClass().getSimpleName()+": "+String.valueOf(problem.getMessage());
   }
   final boolean success=ok;final String status=reason;
   main.post(()->{if(!closed)callback.completed(success,status);});
  });
 }
 void close(){
  closed=true;
  AudioTrack track=activeTrack;
  if(track!=null){try{track.pause();track.flush();}catch(Exception ignored){}}
  worker.shutdownNow();
  // Avoid freeing the native pointer concurrently while inference is running.
  // The whole process owns the remaining memory if shutdown happens mid-generation.
 }
}
