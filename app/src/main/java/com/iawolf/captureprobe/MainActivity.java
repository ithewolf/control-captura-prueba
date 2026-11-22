package com.iawolf.captureprobe;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.provider.Settings;
import android.net.Uri;
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
 private String selectedMode="";
 private boolean welcomeSpoken=false;
 private boolean openingPermissionCheckDone=false;
 private static final int OPENING_PERMISSIONS=80;
 private boolean projectionCaptureOnly=false;
 private final Runnable refresh=new Runnable(){public void run(){
  label.setText("Estado: "+CaptureService.status+"\nMicrófono/voz: "+CaptureService.audioStatus+
   "\nVoz TTS: "+CaptureService.ttsStatus+" · Motor: "+CaptureService.ttsVoice+
   "\nTTS iniciados/finalizados/fallidos: "+CaptureService.ttsStarted+"/"+CaptureService.ttsCompleted+"/"+CaptureService.ttsFailed+
   "\nPermiso de micrófono: "+(checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED?"concedido":"pendiente/denegado")+
   "\nErrores de escucha: "+CaptureService.speechErrors+
   "\nModo seleccionado: "+getSharedPreferences("control_mode",MODE_PRIVATE).getString("mode","coach")+" (diagnóstico, no entrenador completo)"+
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
   "\nLecturas visuales experimentales (NO son estado táctico): "+CaptureService.hudZoneTelemetry+
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
    " · regiones "+snap.getString("hudZoneTelemetry","sin datos")+
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
  TextView title=new TextView(this);title.setText("CONTROL: compilación experimental pendiente. Voz latinoamericana PROVISIONAL. El botón de voz prueba SOLO el altavoz. Para probar el micrófono iniciá sesión larga y decí CONTROL, hola. El motor predeterminado puede requerir internet. La app aún no interpreta el juego.");content.addView(title);
  preview=new ImageView(this);preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
  content.addView(preview,new LinearLayout.LayoutParams(-1,(int)(220*getResources().getDisplayMetrics().density)));
  TextView historyLabel=new TextView(this);historyLabel.setText("Últimos 10 fotogramas (deslizá para ver los anteriores)");content.addView(historyLabel);
  HorizontalScrollView hs=new HorizontalScrollView(this);
  historyView=new LinearLayout(this);historyView.setOrientation(LinearLayout.HORIZONTAL);
  hs.addView(historyView);content.addView(hs);
  chooseVoiceButton=new Button(this);chooseVoiceButton.setText("Voces instaladas en tu dispositivo");content.addView(chooseVoiceButton);
  chooseVoiceButton.setOnClickListener(v->showVoiceChooser());
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
   // Avoid unsolicited TTS during microphone diagnostics; it contaminates Vosk.
   // No mostrar aviso cada vez que se abre la app. STT y captura son independientes.
   if(!voiceReady)voiceButton.setText("Sin voz seleccionada (prueba desactivada)");
   // Voice chooser remains available on demand; do not stack dialogs at startup.
  }));
  Button chooseMode=new Button(this);chooseMode.setText("Elegir modo de CONTROL");content.addView(chooseMode);
  chooseMode.setOnClickListener(v->showModeChooser());
  Button micPermission=new Button(this);
  micPermission.setText("Comprobar / solicitar permiso del micrófono");
  content.addView(micPermission);
  micPermission.setOnClickListener(v->{
   if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED){
    new android.app.AlertDialog.Builder(this).setMessage("Permiso de micrófono concedido. Esto NO demuestra que Vosk pueda escuchar: revisá el estado durante una sesión.").setPositiveButton("Entendido",null).show();
   }else requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},79);
  });
  Button start=new Button(this);start.setText("Iniciar sesión larga: pantalla + micrófono");content.addView(start);
  Button captureOnly=new Button(this);
  captureOnly.setText("Diagnóstico: capturar solo pantalla (sin micrófono)");
  content.addView(captureOnly);
  captureOnly.setOnClickListener(v->new android.app.AlertDialog.Builder(this)
   .setTitle("Prueba de captura independiente")
   .setMessage("Esta prueba solicitará únicamente autorización de pantalla y no iniciará Vosk, aunque el micrófono tenga permiso.")
   .setPositiveButton("Continuar",(d,w)->beginProjectionConsent(true))
   .setNegativeButton("Cancelar",null).show());
  Button stop=new Button(this);stop.setText("Detener");content.addView(stop);
  start.setOnClickListener(v->new android.app.AlertDialog.Builder(this)
   .setTitle("Uso de micrófono")
   .setMessage("CONTROL intentará escuchar durante la sesión. Android puede dejar sin micrófono al chat de voz de Wild Rift, Discord u otra app. La captura de pantalla requiere tu autorización. Podés detener CONTROL cuando quieras desde su notificación.")
   .setPositiveButton("Iniciar", (dialog,which)->requestCapture())
   .setNegativeButton("Cancelar",null).show());stop.setOnClickListener(v->startService(new Intent(this,CaptureService.class).setAction("STOP")));
  l.addView(scroll);
  setContentView(l);
  // Mode chooser is now on demand to avoid overlapping the permission dialog.
 }
 private void speakWelcomeIfReady(){
  if(welcomeSpoken || !voiceReady || voice==null) return;
  welcomeSpoken=true;
  voice.speak("Hola, Wolf. ¿Cómo estás? ¿Qué tenés pensado hacer hoy? ¿Querés que te acompañe en modo coach o en modo compañero?",TextToSpeech.QUEUE_FLUSH,null,"welcome_mode");
 }
 private void showModeChooser(){
  final String[] modes={"🧠 Modo Coach — entrenar con consejos concretos","😈 Modo Compañero — jugar, charlar y hacer cargadas"};
  new android.app.AlertDialog.Builder(this)
   .setTitle("Hola, ¿cómo estás? ¿Qué tenés pensado hacer hoy?")
   .setMessage("¿Querés que te acompañe en modo coach o en modo compañero? Elegís en cada inicio.")
   .setItems(modes,(dialog,index)->{
    selectedMode=index==0?"coach":"companero";
    getSharedPreferences("control_mode",MODE_PRIVATE).edit().putString("mode",selectedMode).apply();
    new android.app.AlertDialog.Builder(this)
     .setTitle(index==0?"Modo Coach: audio recomendado":"Modo Compañero: audio recomendado")
     .setMessage(index==0
      ? "Te recomiendo usar auriculares para escuchar mejor las indicaciones y evitar que el audio del juego entre al micrófono. Si querés entrenar con mejor reconocimiento de voz, también podés bajar la música y los diálogos de campeones desde los ajustes de Wild Rift. Conservá efectos, avisos y señales del combate. Todo es opcional y CONTROL no cambiará el juego."
      : "Para jugar y charlar podés conservar música, diálogos y efectos. Para que entienda mejor tus órdenes, usar auriculares ayuda a que los sonidos del juego no entren al micrófono. Si querés, también podés bajar los diálogos de campeones. No es obligatorio.")
     .setPositiveButton("Entendido",null)
     .setNegativeButton("Dejar audio como está",null).show();
   })
   .setNegativeButton("Decidir después",null).show();
 }
 private java.util.ArrayList<android.speech.tts.Voice> compatibleVoices(){
  java.util.ArrayList<android.speech.tts.Voice> result=new java.util.ArrayList<>();
  if(voice==null || voice.getVoices()==null)return result;
  for(android.speech.tts.Voice item:voice.getVoices()){
   Locale loc=item.getLocale();
   if(loc==null || item.isNetworkConnectionRequired())continue;
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
  return "AR".equalsIgnoreCase(country)?3:"MX".equalsIgnoreCase(country)?2:
    "US".equalsIgnoreCase(country)?1:0;
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
    .setMessage("No encontramos voces locales enumeradas por el motor actual. Podés abrir las opciones para descargar voces. La escucha y captura siguen disponibles.")
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
   .setTitle("Voces instaladas en tu dispositivo")
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
 private void showDownloadOptions(){
  new android.app.AlertDialog.Builder(this)
   .setTitle("Voces para descargar")
   .setMessage("Podés abrir el instalador del motor de voz Android para buscar y descargar voces. La descarga ocurre fuera de CONTROL y depende del catálogo del proveedor. La descarga de modelos propios dentro de CONTROL todavía no está implementada.")
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
   new android.app.AlertDialog.Builder(this)
    .setTitle("Permiso de micrófono pendiente")
    .setMessage("Para probar imagen y voz, permití el micrófono. Si Android no lo concede, podés probar la captura de pantalla por separado.")
    .setPositiveButton("Solicitar micrófono",(d,w)->requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},78))
    .setNeutralButton("Solo pantalla",(d,w)->beginProjectionConsent(true))
    .setNegativeButton("Cancelar",null).show();
   return;
  }
  beginProjectionConsent(false);
 }
 private void beginProjectionConsent(boolean captureOnly){
  projectionCaptureOnly=captureOnly;
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
   else new android.app.AlertDialog.Builder(this).setTitle("Micrófono no autorizado")
    .setMessage("Podemos comprobar la captura igualmente. La escucha seguirá sin funcionar hasta conceder el permiso.")
    .setPositiveButton("Probar pantalla",(d,w)->beginProjectionConsent(true))
    .setNegativeButton("Cancelar",null).show();
  }else if(code==79){
   boolean granted=results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED;
   Toast.makeText(this,granted?"Permiso de micrófono concedido":"Micrófono denegado; captura independiente disponible",Toast.LENGTH_LONG).show();
  }else if(code==OPENING_PERMISSIONS){
   boolean mic=checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;
   boolean notices=Build.VERSION.SDK_INT<33||
      checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;
   if(!mic||!notices)
    new android.app.AlertDialog.Builder(this).setTitle("Permisos pendientes")
     .setMessage("Permisos: micrófono "+(mic?"habilitado":"no autorizado")+
      ", notificaciones "+(notices?"habilitadas":"no autorizadas")+
      ". Si Android bloqueó la solicitud, podés habilitarlos en Ajustes. CONTROL permite diagnóstico solo pantalla.")
     .setPositiveButton("Abrir ajustes",(d,w)->openAppPermissionSettings())
     .setNegativeButton("Continuar",null).show();
  }
 }
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);
  if(request==77 && result==RESULT_OK && data!=null){
    Intent i=new Intent(this,CaptureService.class).setAction("START");
    i.putExtra("resultCode",result);i.putExtra("projectionData",data);
    i.putExtra("captureOnly",projectionCaptureOnly);
    startForegroundService(i);
  }
 }
 private void reviewPermissionsOnOpen(){
  // Run once per Activity creation, not after every system dialog or returning
  // from MediaProjection; otherwise Android can prompt in a loop.
  if(openingPermissionCheckDone)return;
  openingPermissionCheckDone=true;
  java.util.ArrayList<String> missing=new java.util.ArrayList<>();
  if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)
   missing.add(Manifest.permission.RECORD_AUDIO);
  if(Build.VERSION.SDK_INT>=33 &&
     checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
   missing.add(Manifest.permission.POST_NOTIFICATIONS);
  if(missing.isEmpty())return;
  boolean attemptedBefore=getSharedPreferences("control_permissions",MODE_PRIVATE)
   .getBoolean("opening_prompt_attempted",false);
  if(attemptedBefore){
   boolean blocked=false;
   for(String permission:missing)
    if(!shouldShowRequestPermissionRationale(permission)){blocked=true;break;}
   if(blocked){
    new android.app.AlertDialog.Builder(this)
     .setTitle("CONTROL necesita revisar permisos")
     .setMessage("Faltan permisos para la voz o las notificaciones. Android puede bloquear solicitudes repetidas. Revisalos en Ajustes de la aplicación. Igual podés probar solo la pantalla sin micrófono.")
     .setPositiveButton("Abrir ajustes",(d,w)->openAppPermissionSettings())
     .setNegativeButton("Ahora no",null).show();
    return;
   }
  }
  new android.app.AlertDialog.Builder(this)
   .setTitle("Permisos de CONTROL")
   .setMessage("Cada vez que abras CONTROL revisaremos micrófono y notificaciones. Solo solicitaremos los permisos ausentes. Para capturar pantalla Android pedirá otra autorización al iniciar cada sesión.")
   .setPositiveButton("Solicitar",(d,w)->{
    getSharedPreferences("control_permissions",MODE_PRIVATE).edit()
     .putBoolean("opening_prompt_attempted",true).apply();
    requestPermissions(missing.toArray(new String[0]),OPENING_PERMISSIONS);
   })
   .setNegativeButton("Ahora no",null).show();
 }
 private void openAppPermissionSettings(){
  Intent settings=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
    Uri.fromParts("package",getPackageName(),null));
  startActivity(settings);
 }
 @Override protected void onResume(){super.onResume();h.post(refresh);reviewPermissionsOnOpen();}
 @Override protected void onPause(){before=CaptureService.frames;h.removeCallbacks(refresh);super.onPause();}
}