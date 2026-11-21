package com.iawolf.captureprobe;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.media.projection.MediaProjectionConfig;
import android.os.*;
import android.graphics.Bitmap;
import android.speech.tts.TextToSpeech;
import java.util.Locale;
import android.widget.*;
public class MainActivity extends Activity {
 private TextView label; private ImageView preview; private LinearLayout historyView; private final Handler h=new Handler(Looper.getMainLooper());
 private long lastShownAt=-1;
 private long before=-1;
 private TextToSpeech voice;
 private boolean voiceReady=false;
 private Button voiceButton;
 private final Runnable refresh=new Runnable(){public void run(){
  label.setText("Estado: "+CaptureService.status+"\nMicrófono/voz: "+CaptureService.audioStatus+
   "\nÚltimo texto escuchado: "+CaptureService.lastHeard+
   "\nRespuesta local: "+CaptureService.lastReply+
   "\nIntentos de escucha: "+CaptureService.listenAttempts+
   " · Frases reconocidas: "+CaptureService.recognizedPhrases+
   "\nImágenes recibidas durante última escucha: "+CaptureService.framesDuringLastListen+
   "\nAudio + imagen simultáneos: "+(CaptureService.simultaneousAvObserved?"observados":"sin confirmar")+
   "\nReinicios del motor de voz: "+CaptureService.voiceRestarts+
   "\nVuelta a escuchar después de hablar (ms): "+(CaptureService.lastAudioRecoveryMs<0?"aún sin medir":CaptureService.lastAudioRecoveryMs)+
   "\nSesión activa (min): "+(CaptureService.active?((SystemClock.elapsedRealtime()-CaptureService.sessionStartedAt)/60000):0)+
   "\nCallbacks: "+CaptureService.callbacks+
   "\nImágenes recibidas: "+CaptureService.received+"\nImágenes analizadas: "+CaptureService.frames+
   "\nNulas: "+CaptureService.nullImages+" · Antiguas: "+CaptureService.staleImages+
   "\nResize: "+CaptureService.resizes+" · Descartadas: "+CaptureService.discarded+
   "\nÚltimo evento: "+CaptureService.lastEvent+
   "\nCambios: "+CaptureService.changes+"\nOscuros: "+CaptureService.darkFrames+
   "\nTamaño: "+CaptureService.width+" x "+CaptureService.height+
   "\nCuadros desde salida: "+(before<0?"sin medición":Math.max(0,CaptureService.frames-before))+
   "\nError: "+CaptureService.error);
  android.content.SharedPreferences snap=getSharedPreferences("capture_diagnostics",MODE_PRIVATE);
  label.append("\nÚltimo registro guardado: callbacks "+snap.getLong("callbacks",0)+
    " · recibidas "+snap.getLong("received",0)+
    " · analizadas "+snap.getLong("frames",0)+
    " · nulas "+snap.getLong("nullImages",0)+
    " · antiguas "+snap.getLong("staleImages",0)+
    " · resize "+snap.getLong("resizes",0)+
    " · evento "+snap.getString("lastEvent","sin eventos")+
    " · cambios "+snap.getLong("changes",0)+
    " · estado "+snap.getString("status","sin registro")+
    " · error "+snap.getString("error",""));
  Bitmap[] recent=CaptureService.getRecentFrames();
  historyView.removeAllViews();
  for(int i=recent.length-1;i>=0;i--){
   ImageView thumb=new ImageView(MainActivity.this);
   thumb.setImageBitmap(recent[i]);thumb.setScaleType(ImageView.ScaleType.FIT_CENTER);
   historyView.addView(thumb,new LinearLayout.LayoutParams((int)(185*getResources().getDisplayMetrics().density),(int)(115*getResources().getDisplayMetrics().density)));
  }
  Bitmap bitmap=CaptureService.previewFrame;
  long timestamp=CaptureService.previewAt;
  if(timestamp!=lastShownAt){
   preview.setImageBitmap(bitmap);
   lastShownAt=timestamp;
  }
  h.postDelayed(this,1000);
 }};
 @Override public void onCreate(Bundle b){super.onCreate(b);LinearLayout l=new LinearLayout(this);
  l.setOrientation(1);l.setPadding(25,25,25,25);
  ScrollView scroll=new ScrollView(this);
  LinearLayout content=new LinearLayout(this);content.setOrientation(1);
  scroll.addView(content);
  label=new TextView(this);content.addView(label);
  TextView title=new TextView(this);title.setText("Prueba de audio + video: no interpreta aún el juego. La escucha se reinicia cada 20 minutos con una pausa de 4 segundos; la sesión termina cuando la detenés o Android interrumpe la captura.");content.addView(title);
  preview=new ImageView(this);preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
  content.addView(preview,new LinearLayout.LayoutParams(-1,(int)(220*getResources().getDisplayMetrics().density)));
  TextView historyLabel=new TextView(this);historyLabel.setText("Últimos 10 fotogramas (deslizá para ver los anteriores)");content.addView(historyLabel);
  HorizontalScrollView hs=new HorizontalScrollView(this);
  historyView=new LinearLayout(this);historyView.setOrientation(LinearLayout.HORIZONTAL);
  hs.addView(historyView);content.addView(hs);
  voiceButton=new Button(this);voiceButton.setText("Probar voz de Android (independiente)");content.addView(voiceButton);
  voiceButton.setEnabled(false);
  voiceButton.setOnClickListener(v->speakTest());
  voice=new TextToSpeech(this,status->runOnUiThread(()->{
   if(status==TextToSpeech.SUCCESS && voice!=null){
    int result=voice.setLanguage(new Locale("es","AR"));
    if(result==TextToSpeech.LANG_MISSING_DATA || result==TextToSpeech.LANG_NOT_SUPPORTED)
     result=voice.setLanguage(new Locale("es","ES"));
    voiceReady=(result!=TextToSpeech.LANG_MISSING_DATA && result!=TextToSpeech.LANG_NOT_SUPPORTED);
    voice.setSpeechRate(1.0f);voice.setPitch(0.85f);
   }
   voiceButton.setEnabled(voiceReady);
   if(!voiceReady)Toast.makeText(this,"Falta una voz española instalada en Android",Toast.LENGTH_LONG).show();
  }));
  Button start=new Button(this);start.setText("Iniciar sesión larga: pantalla + micrófono");content.addView(start);
  Button stop=new Button(this);stop.setText("Detener");content.addView(stop);
  start.setOnClickListener(v->new android.app.AlertDialog.Builder(this)
   .setTitle("Uso de micrófono")
   .setMessage("CONTROL intentará escuchar durante la sesión. Android puede dejar sin micrófono al chat de voz de Wild Rift, Discord u otra app. La captura de pantalla requiere tu autorización. Podés detener CONTROL cuando quieras desde su notificación.")
   .setPositiveButton("Iniciar", (dialog,which)->requestCapture())
   .setNegativeButton("Cancelar",null).show());stop.setOnClickListener(v->startService(new Intent(this,CaptureService.class).setAction("STOP")));
  l.addView(scroll);
  setContentView(l);
 }
 private void speakTest(){
  if(!voiceReady||voice==null){Toast.makeText(this,"Voz aún no disponible",Toast.LENGTH_SHORT).show();return;}
  voice.speak("Wolf, soy CONTROL. Mi voz está funcionando. La captura sigue siendo un sistema independiente.",TextToSpeech.QUEUE_FLUSH,null,"control_test");
 }
 @Override protected void onDestroy(){
  h.removeCallbacks(refresh);
  if(voice!=null){voice.stop();voice.shutdown();voice=null;}
  super.onDestroy();
 }
 private void requestCapture(){
  if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
   requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},78);return;
  }
  if(CaptureService.active){Toast.makeText(this,"Detené la sesión anterior",Toast.LENGTH_SHORT).show();return;}
  MediaProjectionManager m=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
  Intent request=android.os.Build.VERSION.SDK_INT>=34
   ?m.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
   :m.createScreenCaptureIntent();
  startActivityForResult(request,77);
 }
 @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results){
  super.onRequestPermissionsResult(code,permissions,results);
  if(code==78){
   if(results.length>0 && results[0]==PackageManager.PERMISSION_GRANTED)requestCapture();
   else Toast.makeText(this,"Sin micrófono no se podrá probar la conversación.",Toast.LENGTH_LONG).show();
  }
 }
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);
  if(request==77 && result==RESULT_OK && data!=null){
    Intent i=new Intent(this,CaptureService.class).setAction("START");
    i.putExtra("resultCode",result);i.putExtra("projectionData",data);
    startForegroundService(i);
  }
 }
 @Override protected void onResume(){super.onResume();h.post(refresh);}
 @Override protected void onPause(){before=CaptureService.frames;h.removeCallbacks(refresh);super.onPause();}
}