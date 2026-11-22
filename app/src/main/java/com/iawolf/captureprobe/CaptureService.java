package com.iawolf.captureprobe;
import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.content.pm.PackageManager;
import android.Manifest;
import android.graphics.PixelFormat;
import android.graphics.Bitmap;
import android.hardware.display.*;
import android.media.*;
import android.media.projection.*;
import android.os.*;
import android.util.DisplayMetrics;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
public class CaptureService extends Service {
 public static volatile long frames=0,changes=0,darkFrames=0,callbacks=0,received=0;
 public static volatile int width=0,height=0;
 public static volatile long nullImages=0,staleImages=0,resizes=0,discarded=0;
 public static volatile String lastEvent="Sin eventos";
 // Vista previa solo en memoria; nunca se guarda ni se transmite.
 public static volatile Bitmap previewFrame=null;
 public static volatile long previewAt=0;
 private static final Object historyLock=new Object();
 private static final ArrayDeque<Bitmap> history=new ArrayDeque<>();
 private static final int HISTORY_LIMIT=10;
 public static Bitmap[] getRecentFrames(){
  synchronized(historyLock){return history.toArray(new Bitmap[0]);}
 }
 private static void addRecentFrame(Bitmap image){
  synchronized(historyLock){
   if(history.size()==HISTORY_LIMIT)history.removeFirst();
   history.addLast(image);
  }
 }
 public static void clearRecentFrames(){synchronized(historyLock){history.clear();}}

 public static volatile boolean active=false;
 public static volatile String status="detenido",error="";
 public static volatile String audioStatus="sin iniciar",lastHeard="",lastReply="";
 public static volatile String wakeStatus="sin evaluar";
 public static volatile String voskPartial="sin resultados parciales";
 public static volatile long voskPartialUpdates=0;
 public static volatile String ttsStatus="sin iniciar",ttsVoice="sin seleccionar";
 public static volatile int speechErrors=0,ttsStarted=0,ttsCompleted=0,ttsFailed=0;
 // Prueba verificable de simultaneidad: recepciones de imagen durante una escucha.
 public static volatile long listenAttempts=0,recognizedPhrases=0,framesDuringLastListen=0;
 public static volatile long voiceRestarts=0,sessionStartedAt=0;
 // Tiempo medido desde que la voz termina hasta que Android avisa que vuelve a escuchar.
 public static volatile long lastAudioRecoveryMs=-1;
 public static volatile boolean simultaneousAvObserved=false;
 // Pixel telemetry only. Not HUD identification, HP, skill state or tactical vision.
 public static volatile String hudZoneTelemetry="sin datos";
 private VoskVoiceBridge voiceBridge;
 private final Handler main=new Handler(Looper.getMainLooper());
 private HandlerThread worker;private Handler images;
 private MediaProjection projection;private VirtualDisplay display;private ImageReader reader;
 private long hash=0,lastAt=0;private boolean seen=false,stopping=false;
 private final String CHANNEL="control-probe";
 private volatile long latestFrameAt=0;
 private final Runnable monitor=new Runnable(){@Override public void run(){
  if(!active||stopping)return;
  long elapsed=SystemClock.elapsedRealtime()-latestFrameAt;
  if(elapsed>=15000){
   status="sin cuadros recientes";
   error="Sin imagenes nuevas "+(elapsed/1000)+" s. callbacks="+callbacks+", recibidas="+received+", nulas="+nullImages+", antiguas="+staleImages+", resizes="+resizes;
   saveSnapshot();
  }
  main.postDelayed(this,5000);
 }};
 private void saveSnapshot(){getSharedPreferences("capture_diagnostics",MODE_PRIVATE).edit()
  .putLong("frames",frames).putLong("changes",changes).putLong("dark",darkFrames)
  .putLong("callbacks",callbacks).putLong("received",received)
  .putLong("nullImages",nullImages).putLong("staleImages",staleImages)
  .putLong("resizes",resizes).putLong("discarded",discarded)
  .putLong("listenAttempts",listenAttempts)
  .putLong("recognizedPhrases",recognizedPhrases)
  .putLong("voiceRestarts",voiceRestarts)
  .putLong("lastAudioRecoveryMs",lastAudioRecoveryMs)
  .putLong("framesDuringLastListen",framesDuringLastListen)
  .putBoolean("simultaneousAvObserved",simultaneousAvObserved)
  .putString("hudZoneTelemetry",hudZoneTelemetry)
  .putString("lastEvent",lastEvent)
  .putInt("width",width).putInt("height",height)
  .putString("status",status).putString("error",error)
  .putString("audioStatus",audioStatus).putString("ttsStatus",ttsStatus).putString("ttsVoice",ttsVoice)
  .putInt("speechErrors",speechErrors).putInt("ttsStarted",ttsStarted).putInt("ttsCompleted",ttsCompleted).putInt("ttsFailed",ttsFailed)
  .putLong("updated",System.currentTimeMillis()).apply();}

 @Override public IBinder onBind(Intent i){return null;}
 @Override public void onCreate(){super.onCreate();
  ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(
   new NotificationChannel(CHANNEL,"Captura CONTROL",NotificationManager.IMPORTANCE_LOW));
 }
 private Notification notice(){
  PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,CaptureService.class).setAction("STOP"),
   PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  return new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_menu_view)
   .setContentTitle("CONTROL captura audiovisual")
   .setContentText("Pantalla y voz de prueba · "+status+" · "+frames+" cuadros")
   .setOngoing(true).addAction(new Notification.Action.Builder(null,"Detener",stop).build()).build();
 }
 @Override public int onStartCommand(Intent intent,int flags,int startId){
  if(intent!=null&&"CHANGE_VOICE".equals(intent.getAction())){
   if(active&&voiceBridge!=null)voiceBridge.refreshSelectedVoice();
   return START_NOT_STICKY;
  }
  if(intent!=null&&"TEST_REPLY".equals(intent.getAction())){
   if(active&&voiceBridge!=null)voiceBridge.diagnosticReply();
   else wakeStatus="No hay sesión de pantalla y voz activa para la prueba";
   return START_NOT_STICKY;
  }
  if(intent==null||!"START".equals(intent.getAction())){stopNow();return START_NOT_STICKY;}
  if(active)return START_NOT_STICKY;
  active=true;stopping=false;frames=changes=darkFrames=callbacks=received=0;
  nullImages=staleImages=resizes=discarded=0;lastEvent="Inicio";status="iniciando";error="";seen=false;lastAt=0;
  previewFrame=null;previewAt=0;clearRecentFrames();
  audioStatus="iniciando";lastHeard="";lastReply="";wakeStatus="sin evaluar";voskPartial="sin resultados parciales";voskPartialUpdates=0;ttsStatus="iniciando";ttsVoice="sin seleccionar";speechErrors=ttsStarted=ttsCompleted=ttsFailed=0;
  listenAttempts=recognizedPhrases=framesDuringLastListen=voiceRestarts=0;
  sessionStartedAt=SystemClock.elapsedRealtime();AvContext.reset(sessionStartedAt);lastAudioRecoveryMs=-1;
  simultaneousAvObserved=false;hudZoneTelemetry="sin datos";
  try{
   boolean captureOnly=intent.getBooleanExtra("captureOnly",false);
   boolean audioAllowed=!captureOnly&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;
   int fgType=ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION;
   if(audioAllowed)fgType|=ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
   startForeground(1,notice(),fgType);
   Intent grant=android.os.Build.VERSION.SDK_INT>=33?
    intent.getParcelableExtra("projectionData",Intent.class):
    intent.getParcelableExtra("projectionData");
   if(grant==null)throw new IllegalStateException("Sin permiso de captura");
   MediaProjectionManager manager=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
   projection=manager.getMediaProjection(intent.getIntExtra("resultCode",0),grant);
   if(projection==null)throw new IllegalStateException("MediaProjection no disponible");
   worker=new HandlerThread("frames");worker.start();images=new Handler(worker.getLooper());
   projection.registerCallback(new MediaProjection.Callback(){
    @Override public void onStop(){lastEvent="MediaProjection.onStop";saveSnapshot();main.post(()->stopNow());}
    @Override public void onCapturedContentResize(int w,int h){
     if(android.os.Build.VERSION.SDK_INT>=34&&w>0&&h>0)main.post(()->resize(w,h));
    }
   },main);
   DisplayMetrics metrics=getResources().getDisplayMetrics();
   int screenW=metrics.widthPixels,screenH=metrics.heightPixels;
   if(Build.VERSION.SDK_INT>=30){
    android.view.WindowManager wm=(android.view.WindowManager)getSystemService(WINDOW_SERVICE);
    android.graphics.Rect bounds=wm.getMaximumWindowMetrics().getBounds();
    screenW=bounds.width();screenH=bounds.height();
   }
   createReader(screenW,screenH);
   display=projection.createVirtualDisplay("CONTROL diagnosis",width,height,metrics.densityDpi,
    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader.getSurface(),null,images);
   status="capturando";latestFrameAt=SystemClock.elapsedRealtime();
   if(audioAllowed){voiceBridge=new VoskVoiceBridge(this);voiceBridge.start();}
   else audioStatus=captureOnly?"diagnóstico solo pantalla (micrófono desactivado)":"permiso de micrófono pendiente";
   main.postDelayed(monitor,15000);
   main.postDelayed(()->{if(active&&frames==0){error="No llegaron cuadros en 12 segundos";status="sin cuadros";}},12000);
  }catch(Exception e){error=e.toString();status="error";saveSnapshot();stopNow();}
  return START_NOT_STICKY;
 }
 private void createReader(int w,int h){
  width=w;height=h;
  reader=ImageReader.newInstance(width,height,PixelFormat.RGBA_8888,3);
  reader.setOnImageAvailableListener(src->frame(src),images);
 }
 private void resize(int w,int h){
  if(!active||display==null||stopping||w<2||h<2)return;
  // Android can repeatedly report the same captured-content size after setSurface().
  // Never recreate the ImageReader for a no-op resize.
  if(w==width&&h==height)return;
  try{
   resizes++;lastEvent="resize real "+w+"x"+h;saveSnapshot();
   display.setSurface(null);ImageReader old=reader;
   createReader(w,h);display.resize(width,height,getResources().getDisplayMetrics().densityDpi);
   display.setSurface(reader.getSurface());images.post(old::close);
  }catch(Exception e){error=e.toString();status="error";stopNow();}
 }
 private static Bitmap makePreview(Image img,Image.Plane p){
  int sourceW=img.getWidth(),sourceH=img.getHeight();
  int maxSide=Math.max(sourceW,sourceH);
  float scale=Math.min(1f,320f/maxSide);
  int outW=Math.max(1,Math.round(sourceW*scale));
  int outH=Math.max(1,Math.round(sourceH*scale));
  ByteBuffer buffer=p.getBuffer();
  int stride=p.getRowStride(),pixel=p.getPixelStride();
  if(pixel<4)throw new IllegalArgumentException("Pixel stride inesperado");
  int[] argb=new int[outW*outH];
  for(int y=0;y<outH;y++){
   int srcY=Math.min(sourceH-1,(int)((long)y*sourceH/outH));
   for(int x=0;x<outW;x++){
    int srcX=Math.min(sourceW-1,(int)((long)x*sourceW/outW));
    int pos=srcY*stride+srcX*pixel;
    int r=buffer.get(pos)&255,g=buffer.get(pos+1)&255,b=buffer.get(pos+2)&255;
    argb[y*outW+x]=0xff000000|(r<<16)|(g<<8)|b;
   }
  }
  return Bitmap.createBitmap(argb,outW,outH,Bitmap.Config.ARGB_8888);
 }
 private void frame(ImageReader source){
  Image img=null;
  try{
   callbacks++;
   img=source.acquireLatestImage();
   if(img==null){nullImages++;lastEvent="callback sin imagen";saveSnapshot();return;}
   if(!active||stopping){discarded++;lastEvent="sesion inactiva";saveSnapshot();return;}
   if(source!=reader){staleImages++;lastEvent="superficie anterior";saveSnapshot();return;}
   received++;lastEvent="imagen adquirida";
   long now=SystemClock.elapsedRealtime();latestFrameAt=now;
   if(received==1||received%5==0)saveSnapshot();
   if(now-lastAt<900){discarded++;return;}lastAt=now;
   Image.Plane p=img.getPlanes()[0];ByteBuffer b=p.getBuffer();
   FrameSampler.Result data=FrameSampler.sample(b,img.getWidth(),img.getHeight(),p.getRowStride(),p.getPixelStride());
   // Fixed-size sampled regions: no bitmap allocation or off-device traffic.
   HudRegionSampler.Zones zones=HudRegionSampler.sample(b,img.getWidth(),img.getHeight(),p.getRowStride(),p.getPixelStride());
   hudZoneTelemetry="mapa(media="+zones.mapMean+",contraste="+zones.mapContrast+") "+
       "controles(media="+zones.controlsMean+",contraste="+zones.controlsContrast+") "+
       "centro(media="+zones.centerMean+")";
   // Un fotograma visual cada 3 segundos; no altera la tasa de captura.
   if(now-previewAt>=3000){
    try{Bitmap next=makePreview(img,p);addRecentFrame(next);previewFrame=next;previewAt=now;}
    catch(RuntimeException previewError){lastEvent="vista previa no disponible";}
   }
   if(seen&&hash!=data.fingerprint)changes++;
   hash=data.fingerprint;seen=true;frames++;if(data.dark)darkFrames++;
   AvContext.recordFrame(sessionStartedAt,now,frames,changes,data.dark);
   if(status.equals("sin cuadros recientes")){status="capturando";error="";}
   if(frames==1){status="capturando";error="";}
   if(frames==1||frames%5==0)saveSnapshot();
   if(frames%5==0)main.post(()->{
    if(active)((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(1,notice());
   });
  }catch(Exception e){error=e.toString();lastEvent="Excepcion lectura";status="error";saveSnapshot();main.post(()->stopNow());}
  finally{if(img!=null)img.close();}
 }
 private void stopNow(){
  if(stopping)return;stopping=true;active=false;
  if(voiceBridge!=null){voiceBridge.stop();voiceBridge=null;}
  audioStatus="detenido";
  main.removeCallbacks(monitor);
  previewFrame=null;previewAt=0;clearRecentFrames();
  if(display!=null){display.release();display=null;}
  if(reader!=null){reader.close();reader=null;}
  if(projection!=null){MediaProjection p=projection;projection=null;p.stop();}
  if(worker!=null){worker.quitSafely();worker=null;}
  if(!status.equals("error"))status="detenido";
  saveSnapshot();
  stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();
 }
 @Override public void onDestroy(){stopNow();super.onDestroy();}
}