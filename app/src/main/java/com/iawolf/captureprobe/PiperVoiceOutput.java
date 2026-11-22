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
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
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
     vits.setDataDir(preparePhonemizer());
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
     long played=track.getPlaybackHeadPosition() & 0xffffffffL;
     // AudioTrack.write() only queues PCM; never stop a long reply after 2 seconds.
     long drainMillis=PiperPlaybackBudget.drainMillis(samples.length,played,rate);
     long deadline=android.os.SystemClock.elapsedRealtime()+drainMillis;
     while(played<samples.length && android.os.SystemClock.elapsedRealtime()<deadline&&!closed){
      android.os.SystemClock.sleep(25);
      played=track.getPlaybackHeadPosition() & 0xffffffffL;
     }
     if(!closed && played<samples.length)
      throw new IllegalStateException("Audio incompleto: "+played+"/"+samples.length);
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
 // Piper/eSpeak requires real filesystem paths on Android for pronunciation tables.
 // Copy only the bundled phonemizer on the dedicated worker; never on the UI thread.
 private String preparePhonemizer() throws Exception {
  String assetDir=DIR+"espeak-ng-data";
  File dest=new File(context.getFilesDir(),"piper/espeak-ng-data");
  copyAssetTree(assetDir,dest);
  return dest.getAbsolutePath();
 }
 private void copyAssetTree(String assetDir,File dest) throws Exception {
  String[] children=context.getAssets().list(assetDir);
  if(children==null||children.length==0){
   if(dest.isFile()&&dest.length()>0)return;
   File parent=dest.getParentFile();
   if(parent==null||(!parent.isDirectory()&&!parent.mkdirs()))
     throw new IllegalStateException("Piper: no se puede crear carpeta de voz");
   try(InputStream in=context.getAssets().open(assetDir);
       FileOutputStream out=new FileOutputStream(dest)){
    byte[] bytes=new byte[16384];int n;
    while((n=in.read(bytes))!=-1)out.write(bytes,0,n);
   }
   return;
  }
  if(!dest.isDirectory()&&!dest.mkdirs())
   throw new IllegalStateException("Piper: no se puede crear carpeta de pronunciación");
  for(String child:children)copyAssetTree(assetDir+"/"+child,new File(dest,child));
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
