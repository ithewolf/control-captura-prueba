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
   "\nPiper (prueba): "+CaptureService.piperStatus+" · Tiempo total: "+(CaptureService.piperLastMs<0?"sin medición":CaptureService.piperLastMs+" ms")+
   "\nTTS iniciados/finalizados/fallidos: "+CaptureService.ttsStarted+"/"+CaptureService.ttsCompleted+"/"+CaptureService.ttsFailed+
   "\nPermiso de micrófono: "+(checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED?"concedido":"pendiente/denegado")+
   "\nErrores de escucha: "+CaptureService.speechErrors+
   "\nModo seleccionado: "+getSharedPreferences("control_mode",MODE_PRIVATE).getString("mode","coach")+" (diagnóstico, no entrenador completo)"+
   "\nVosk parcial (diagnóstico): "+CaptureService.voskPartial+
   "\nActualizaciones parciales: "+CaptureService.voskPartialUpdates+
   "\nÚltimo texto escuchado: "+CaptureService.lastHeard+
   "\nPrueba sin decir CONTROL: "+(getSharedPreferences("control_voice_diag",MODE_PRIVATE).getBoolean("free_listen",false)?"ACTIVA":"desactivada")+
   "\nActivación de voz: "+CaptureService.wakeStatus+
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
    " · error "+snap.getString("error","")+
    " · simultáneos "+(snap.getBoolean("simultaneousAvObserved",false)?"sí":"no")+
    " · Piper "+snap.getString("piperStatus","sin probar"));
  // The historical thumbnails are only refreshed when a NEW preview arrives.
  // Never create ten new ImageViews every second while Wild Rift is running.
  Bitmap bitmap=CaptureService.previewFrame;
  long timestamp=CaptureService.previewAt;
  if(timestamp!=lastShownAt){
   Bitmap[] recent=CaptureService.getRecentFrames();
   historyView.removeAllViews();
   for(int i=recent.length-1;i>=0;i--){
    ImageView thumb=new ImageView(MainActivity.this);
    thumb.setImageBitmap(recent[i]);thumb.setScaleType(ImageView.ScaleType.FIT_CENTER);
    historyView.addView(thumb,new LinearLayout.LayoutParams((int)(185*getResources().getDisplayMetrics().density),(int)(115*getResources().getDisplayMetrics().density)));
   }
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
  TextView title=new TextView(this);title.setText("CONTROL: APK experimental de diagnóstico. Piper Ald es voz masculina PROVISIONAL, no la voz original de CONTROL. Para probar la voz Piper, seleccioná Piper, iniciá Pantalla + micrófono y pulsá Diagnóstico: hacer responder al CONTROL de la sesión. Para probar escucha, decí CONTROL, hola. La app todavía no interpreta el juego.");content.addView(title);
  // Lab6 stays text-only and separate from the active Vosk / MediaProjection session.
  Button lab6=new Button(this);lab6.setText("LAB 6: probar IA LOCAL por texto (modelo LiteRT)");
  content.addView(lab6);lab6.setOnClickListener(v->startActivity(new Intent(this,Lab6TextActivity.class)));
  preview=new ImageView(this);preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
  content.addView(preview,new LinearLayout.LayoutParams(-1,(int)(220*getResources().getDisplayMetrics().density)));
  TextView historyLabel=new TextView(this);historyLabel.setText("Últimos 10 fotogramas (deslizá para ver los anteriores)");content.addView(historyLabel);
  HorizontalScrollView hs=new HorizontalScrollView(this);
  historyView=new LinearLayout(this);historyView.setOrientation(LinearLayout.HORIZONTAL);
  hs.addView(historyView);content.addView(hs);
  chooseVoiceButton=new Button(this);chooseVoiceButton.setText("Elegir voz de CONTROL");content.addView(chooseVoiceButton);
  chooseVoiceButton.setOnClickListener(v->showVoiceSourceChooser());
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
  Button testSessionVoice=new Button(this);
  testSessionVoice.setText("Diagnóstico: hacer responder al CONTROL de la sesión");
  content.addView(testSessionVoice);
  testSessionVoice.setOnClickListener(v->{
   if(!CaptureService.active){
    Toast.makeText(this,"Primero iniciá Pantalla + micrófono",Toast.LENGTH_LONG).show();
    return;
   }
   startService(new Intent(this,CaptureService.class).setAction("TEST_REPLY"));
  });
  Button diagnosticFree=new Button(this);
  diagnosticFree.setText("Prueba de micrófono SIN decir CONTROL: activar/desactivar");
  content.addView(diagnosticFree);
  diagnosticFree.setOnClickListener(v->{
    boolean active=getSharedPreferences("control_voice_diag",MODE_PRIVATE)
       .getBoolean("free_listen",false);
    boolean next=!active;
    getSharedPreferences("control_voice_diag",MODE_PRIVATE).edit()
       .putBoolean("free_listen",next).apply();
    new android.app.AlertDialog.Builder(this)
      .setTitle(next?"Prueba sin palabra de activación ACTIVA":"Prueba desactivada")
      .setMessage(next?"Solo diagnóstico: durante una sesión activa CONTROL procesará frases reconocidas de 2 a 20 palabras aunque no digas CONTROL. SOLO responde preguntas incluidas en su intérprete básico; no habilita conversación libre. Podría activarse con sonidos del ambiente: probá con el juego cerrado. Se desactiva al detener la sesión.":"CONTROL vuelve a exigir su palabra de activación.")
      .setPositiveButton("Entendido",null).show();
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
  final String[] modes={"Modo Coach (diagnóstico)","Modo Compañero (diagnóstico)"};
  String current=getSharedPreferences("control_mode",MODE_PRIVATE).getString("mode","coach");
  int selected="companero".equals(current)?1:0;
  new android.app.AlertDialog.Builder(this)
   .setTitle("Elegí el modo de CONTROL")
   // Do NOT combine setMessage with setSingleChoiceItems: message hides list in Android.
   .setSingleChoiceItems(modes,selected,(dialog,index)->{
     selectedMode=index==0?"coach":"companero";
     getSharedPreferences("control_mode",MODE_PRIVATE).edit().putString("mode",selectedMode).apply();
     Toast.makeText(this,"Modo "+(index==0?"Coach":"Compañero")+" seleccionado",Toast.LENGTH_LONG).show();
     dialog.dismiss();
   })
   .setNegativeButton("Cancelar",null).show();
 }
 private void showVoiceSourceChooser(){
  String[] sources={"Voces del dispositivo — elegir una instalada",
                    "Piper Ald — voz masculina de PRUEBA (español mexicano)"};
  new android.app.AlertDialog.Builder(this)
   .setTitle("Voz de CONTROL")
   .setItems(sources,(dialog,which)->{
    if(which==0)showVoiceChooser();
    else showPiperVoiceInfo();
   })
   .setNegativeButton("Cancelar",null).show();
 }
 private void showPiperVoiceInfo(){
  new android.app.AlertDialog.Builder(this)
   .setTitle("Piper: laboratorio Android")
   .setMessage("Esta es la voz MASCULINA provisional Ald (español mexicano), no la voz argentina definitiva. La prueba consume más memoria y puede tardar en empezar a hablar. Solo usala en sesiones cortas de diagnóstico, no jugando Wild Rift. Si falla, CONTROL intentará responder con una voz del dispositivo. ¿Activar Piper para las respuestas de CONTROL?")
   .setPositiveButton("Activar Piper (prueba)",(dialog,which)->{
    getSharedPreferences("control_voice_settings",MODE_PRIVATE).edit()
       .putString("voice_source","piper").apply();
    if(CaptureService.active)startService(new Intent(this,CaptureService.class).setAction("CHANGE_VOICE"));
    Toast.makeText(this,"Piper experimental seleccionado para la próxima respuesta",Toast.LENGTH_LONG).show();
   })
   .setNegativeButton("Cancelar",null).show();
 }
 private java.util.ArrayList<android.speech.tts.Voice> compatibleVoices(){
  java.util.ArrayList<android.speech.tts.Voice> result=new java.util.ArrayList<>();
  if(voice==null || voice.getVoices()==null)return result;
  for(android.speech.tts.Voice item:voice.getVoices()){
   Locale loc=item.getLocale();
   if(loc==null || item.isNetworkConnectionRequired() || !"es".equalsIgnoreCase(loc.getLanguage()))continue;
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
  return "AR".equalsIgnoreCase(country)?4:"UY".equalsIgnoreCase(country)?3:"MX".equalsIgnoreCase(country)?2:
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
    .setTitle("Sin voces disponibles en el dispositivo")
    .setMessage("El motor de Android no informó voces instaladas sin conexión. Revisá las voces disponibles en Ajustes de síntesis de voz del dispositivo.")
    .setPositiveButton("Entendido",null).show();
   return;
  }
  String[] names=new String[options.size()];
  for(int i=0;i<options.size();i++){
   android.speech.tts.Voice v=options.get(i);
   names[i]=("AR".equalsIgnoreCase(v.getLocale().getCountry())?"🇦🇷 Argentina · ":"")+v.getLocale().toLanguageTag()+" · "+v.getName()+
      (v.isNetworkConnectionRequired()?" (requiere internet)":" (sin conexión)");
  }
  new android.app.AlertDialog.Builder(this)
.setTitle("Voces del dispositivo (sin conexión)")
   .setSingleChoiceItems(names,-1,(dialog,which)->{
    android.speech.tts.Voice candidate=options.get(which);
    if(voice.setVoice(candidate)==TextToSpeech.SUCCESS){
     getSharedPreferences("control_voice_settings",MODE_PRIVATE).edit()
      .putString("voice_source","device").putString("voice_name",candidate.getName()).apply();
     if(CaptureService.active)
       startService(new Intent(this,CaptureService.class).setAction("CHANGE_VOICE"));
     voiceReady=true;voiceButton.setEnabled(true);
     voiceButton.setText("Probar altavoz / voz elegida");
     if(CaptureService.active){
      Toast.makeText(this,"Voz elegida para CONTROL. Escuchala cuando termines la sesión, para evitar que Vosk capte la muestra.",Toast.LENGTH_LONG).show();
     }else voice.speak("Wolf, soy Control. Esta es mi voz de prueba.",TextToSpeech.QUEUE_FLUSH,null,"voice_preview");
    }else Toast.makeText(this,"No se pudo seleccionar esta voz",Toast.LENGTH_SHORT).show();
    dialog.dismiss();
   })
   .setNegativeButton("Cancelar",null).show();
 }
 private void speakTest(){
  if(!voiceReady||voice==null){Toast.makeText(this,"Voz aún no disponible",Toast.LENGTH_SHORT).show();return;}
  if(CaptureService.active){
   Toast.makeText(this,"Terminá primero la sesión de micrófono para probar la voz sin eco.",Toast.LENGTH_LONG).show();
   return;
  }
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