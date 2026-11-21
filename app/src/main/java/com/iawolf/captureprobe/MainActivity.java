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
 private Button chooseVoiceButton;
 private final Runnable refresh=new Runnable(){public void run(){
  label.setText("Estado: "+CaptureService.status+"\nMicrófono/voz: "+CaptureService.audioStatus+
   "\nVoz TTS: "+CaptureService.ttsStatus+" · Motor: "+CaptureService.ttsVoice+
   "\nTTS iniciados/finalizados/fallidos: "+CaptureService.ttsStarted+"/"+CaptureService.ttsCompleted+"/"+CaptureService.ttsFailed+
   "\nErrores de escucha: "+CaptureService.speechErrors+
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
  TextView title=new TextView(this);title.setText("CONTROL v26: selector de voces mejorado. Voz latinoamericana PROVISIONAL. El botón de voz prueba SOLO el altavoz. Para probar el micrófono iniciá sesión larga y decí CONTROL, hola. El motor predeterminado puede requerir internet. La app aún no interpreta el juego.");content.addView(title);
  preview=new ImageView(this);preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
  content.addView(preview,new LinearLayout.LayoutParams(-1,(int)(220*getResources().getDisplayMetrics().density)));
  TextView historyLabel=new TextView(this);historyLabel.setText("Últimos 10 fotogramas (deslizá para ver los anteriores)");content.addView(historyLabel);
  HorizontalScrollView hs=new HorizontalScrollView(this);
  historyView=new LinearLayout(this);historyView.setOrientation(LinearLayout.HORIZONTAL);
  hs.addView(historyView);content.addView(hs);
  chooseVoiceButton=new Button(this);chooseVoiceButton.setText("Voces instaladas en tu dispositivo");content.addView(chooseVoiceButton);
  chooseVoiceButton.setOnClickListener(v->showVoiceChooser());
  Button engineButton=new Button(this);engineButton.setText("Cambiar motor de voz (Samsung / Google)");content.addView(engineButton);
  engineButton.setOnClickListener(v->openVoiceEngineSettings());
  Button downloadVoices=new Button(this);downloadVoices.setText("Voces para descargar");content.addView(downloadVoices);
  downloadVoices.setOnClickListener(v->showDownloadOptions());
  voiceButton=new Button(this);voiceButton.setText("Probar ALTAVOZ / voz sintetizada (no micrófono)");content.addView(voiceButton);
  voiceButton.setEnabled(false);
  voiceButton.setOnClickListener(v->speakTest());
  voice=new TextToSpeech(this,status->runOnUiThread(()->{
   if(status==TextToSpeech.SUCCESS && voice!=null){
    voiceReady=selectPreferredVoice();
    voice.setSpeechRate(1.06f);voice.setPitch(0.88f);
   }
   voiceButton.setEnabled(voiceReady);
   // No mostrar aviso cada vez que se abre la app. STT y captura son independientes.
   if(!voiceReady)voiceButton.setText("Sin voz seleccionada (prueba desactivada)");
   if(!getSharedPreferences("control_voice_settings",MODE_PRIVATE).getBoolean("voice_setup_seen",false)){
    getSharedPreferences("control_voice_settings",MODE_PRIVATE).edit().putBoolean("voice_setup_seen",true).apply();
    showVoiceChooser();
   }
  }));
  Button brainTest=new Button(this);brainTest.setText("Probar cerebro por texto (sin micrófono)");content.addView(brainTest);
  brainTest.setOnClickListener(v->startActivity(new Intent(this,BrainTextProbeActivity.class)));
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
 private java.util.ArrayList<android.speech.tts.Voice> compatibleVoices(){
  java.util.ArrayList<android.speech.tts.Voice> result=new java.util.ArrayList<>();
  if(voice==null || voice.getVoices()==null)return result;
  for(android.speech.tts.Voice item:voice.getVoices()){
   Locale loc=item.getLocale();
   if(loc==null)continue;
   result.add(item);
  }
  java.util.Collections.sort(result,(a,b)->{
   int ca=rankLocale(a.getLocale()),cb=rankLocale(b.getLocale());
   if(ca!=cb)return cb-ca;
   return b.getQuality()-a.getQuality();
  });
  return result;
 }
 private int rankLocale(Locale l){
  String country=l.getCountry();
  if(!"es".equalsIgnoreCase(l.getLanguage()))return 0;
  return "AR".equalsIgnoreCase(country)?4:"MX".equalsIgnoreCase(country)?3:
    "US".equalsIgnoreCase(country)?2:1;
 }
 private boolean selectPreferredVoice(){
  java.util.ArrayList<android.speech.tts.Voice> options=compatibleVoices();
  String saved=getSharedPreferences("control_voice_settings",MODE_PRIVATE).getString("voice_name","");
  android.speech.tts.Voice chosen=null;
  for(android.speech.tts.Voice candidate:options)if(candidate.getName().equals(saved)){chosen=candidate;break;}
  if(chosen==null && !options.isEmpty())chosen=options.get(0);
  return chosen!=null && voice.setVoice(chosen)==TextToSpeech.SUCCESS;
 }
 private void showVoiceChooser(){
  if(voice==null){Toast.makeText(this,"Motor de voz todavía no inicializado",Toast.LENGTH_SHORT).show();return;}
  java.util.ArrayList<android.speech.tts.Voice> options=compatibleVoices();
  if(options.isEmpty()){
   new android.app.AlertDialog.Builder(this)
    .setTitle("Sin voces locales disponibles")
    .setMessage("El motor activo no informó voces. Probá cambiar el motor entre Samsung y Google en Ajustes y volver a CONTROL. La escucha y captura siguen disponibles.")
    .setPositiveButton("Voces para descargar", (d,w)->showDownloadOptions())
    .setNegativeButton("Seguir sin voz",null).show();
   return;
  }
  String[] names=new String[options.size()];
  for(int i=0;i<options.size();i++){
   android.speech.tts.Voice v=options.get(i);
   names[i]=v.getLocale().toLanguageTag()+" · "+v.getName()+
      (v.isNetworkConnectionRequired()?" (requiere internet)":" (sin conexión)");
  }
  new android.app.AlertDialog.Builder(this)
   .setTitle("Voces del motor activo (locales y de red)")
   .setSingleChoiceItems(names,-1,(dialog,which)->{
    android.speech.tts.Voice candidate=options.get(which);
    if(voice.setVoice(candidate)==TextToSpeech.SUCCESS){
     getSharedPreferences("control_voice_settings",MODE_PRIVATE).edit().putString("voice_name",candidate.getName()).apply();
     voiceReady=true;voiceButton.setEnabled(true);
     voiceButton.setText("Probar altavoz / voz elegida");
     voice.speak("Wolf, soy Control. Esta es mi voz de prueba.",TextToSpeech.QUEUE_FLUSH,null,"voice_preview");
    }else Toast.makeText(this,"No se pudo seleccionar esta voz",Toast.LENGTH_SHORT).show();
    dialog.dismiss();
   })
   .setNeutralButton("Voces para descargar",(d,w)->showDownloadOptions())
   .setNegativeButton("Cancelar",null).show();
 }
 private void openVoiceEngineSettings(){
  try{startActivity(new Intent("com.android.settings.TTS_SETTINGS"));}
  catch(Exception ex){
   try{startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));}
   catch(Exception ignored){Toast.makeText(this,"Abrí Ajustes > Administración general > Salida de texto a voz",Toast.LENGTH_LONG).show();}
  }
 }
 private void showDownloadOptions(){
  new android.app.AlertDialog.Builder(this)
   .setTitle("Voces para descargar")
   .setMessage("Podés abrir el instalador del motor de voz Android para buscar y descargar voces. El catálogo depende del motor Samsung o Google, y puede no incluir español argentino. La descarga ocurre fuera de CONTROL. La descarga de modelos propios dentro de CONTROL todavía no está implementada.")
   .setPositiveButton("Abrir catálogo de Android",(dialog,which)->openVoiceInstaller())
   .setNegativeButton("Cancelar",null).show();
 }
 private void openVoiceInstaller(){
  try{startActivity(new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA));}
  catch(Exception ex){
   try{startActivity(new Intent("com.android.settings.TTS_SETTINGS"));}
   catch(Exception ignored){Toast.makeText(this,"Buscá Ajustes de texto a voz en Android",Toast.LENGTH_LONG).show();}
  }
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