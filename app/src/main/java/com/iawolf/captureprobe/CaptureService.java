package com.iawolf.captureprobe;
import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.hardware.display.*;
import android.media.*;
import android.media.projection.*;
import android.os.*;
import android.util.DisplayMetrics;
import java.nio.ByteBuffer;
public class CaptureService extends Service {
 public static volatile long frames=0,changes=0,darkFrames=0;
 public static volatile int width=0,height=0;
 public static volatile boolean active=false;
 public static volatile String status="detenido",error="";
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
   error="No llegaron cuadros nuevos en "+(elapsed/1000)+" segundos";
   saveSnapshot();
  }
  main.postDelayed(this,5000);
 }};
 private void saveSnapshot(){getSharedPreferences("capture_diagnostics",MODE_PRIVATE).edit()
  .putLong("frames",frames).putLong("changes",changes).putLong("dark",darkFrames)
  .putInt("width",width).putInt("height",height)
  .putString("status",status).putString("error",error)
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
   .setContentTitle("CONTROL captura de pantalla")
   .setContentText("Cuadros "+frames+" · Cambios "+changes+" · "+status)
   .setOngoing(true).addAction(new Notification.Action.Builder(null,"Detener",stop).build()).build();
 }
 @Override public int onStartCommand(Intent intent,int flags,int startId){
  if(intent==null||!"START".equals(intent.getAction())){stopNow();return START_NOT_STICKY;}
  if(active)return START_NOT_STICKY;
  active=true;stopping=false;frames=changes=darkFrames=0;status="iniciando";error="";seen=false;lastAt=0;
  try{
   startForeground(1,notice(),ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
   Intent grant=android.os.Build.VERSION.SDK_INT>=33?
    intent.getParcelableExtra("projectionData",Intent.class):
    intent.getParcelableExtra("projectionData");
   if(grant==null)throw new IllegalStateException("Sin permiso de captura");
   MediaProjectionManager manager=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
   projection=manager.getMediaProjection(intent.getIntExtra("resultCode",0),grant);
   if(projection==null)throw new IllegalStateException("MediaProjection no disponible");
   worker=new HandlerThread("frames");worker.start();images=new Handler(worker.getLooper());
   projection.registerCallback(new MediaProjection.Callback(){
    @Override public void onStop(){main.post(()->stopNow());}
    @Override public void onCapturedContentResize(int w,int h){
     if(android.os.Build.VERSION.SDK_INT>=34&&w>0&&h>0)main.post(()->resize(w,h));
    }
   },main);
   DisplayMetrics metrics=getResources().getDisplayMetrics();
   createReader(metrics.widthPixels,metrics.heightPixels);
   display=projection.createVirtualDisplay("CONTROL diagnosis",width,height,metrics.densityDpi,
    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader.getSurface(),null,images);
   status="capturando";latestFrameAt=SystemClock.elapsedRealtime();
   main.postDelayed(monitor,15000);
   main.postDelayed(()->{if(active)stopNow();},600000);
   main.postDelayed(()->{if(active&&frames==0){error="No llegaron cuadros en 12 segundos";status="sin cuadros";}},12000);
  }catch(Exception e){error=e.toString();status="error";saveSnapshot();stopNow();}
  return START_NOT_STICKY;
 }
 private void createReader(int w,int h){
  double scale=Math.min(1.0,640.0/Math.max(w,h));
  width=Math.max(2,((int)(w*scale)/2)*2);height=Math.max(2,((int)(h*scale)/2)*2);
  reader=ImageReader.newInstance(width,height,PixelFormat.RGBA_8888,2);
  reader.setOnImageAvailableListener(src->frame(src),images);
 }
 private void resize(int w,int h){
  if(!active||display==null||stopping)return;
  try{
   display.setSurface(null);ImageReader old=reader;
   createReader(w,h);display.resize(width,height,getResources().getDisplayMetrics().densityDpi);
   display.setSurface(reader.getSurface());images.post(old::close);
  }catch(Exception e){error=e.toString();status="error";stopNow();}
 }
 private void frame(ImageReader source){
  Image img=null;
  try{
   img=source.acquireLatestImage();
   if(img==null||!active||stopping||source!=reader)return;
   long now=SystemClock.elapsedRealtime();if(now-lastAt<900)return;lastAt=now;
   Image.Plane p=img.getPlanes()[0];ByteBuffer b=p.getBuffer();
   FrameSampler.Result data=FrameSampler.sample(b,img.getWidth(),img.getHeight(),p.getRowStride(),p.getPixelStride());
   if(seen&&hash!=data.fingerprint)changes++;
   hash=data.fingerprint;seen=true;frames++;latestFrameAt=now;if(data.dark)darkFrames++;
   if(status.equals("sin cuadros recientes")){status="capturando";error="";}
   if(frames==1){status="capturando";error="";}
   if(frames==1||frames%5==0)saveSnapshot();
   if(frames%5==0)main.post(()->{
    if(active)((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(1,notice());
   });
  }catch(Exception e){error=e.toString();status="error";main.post(()->stopNow());}
  finally{if(img!=null)img.close();}
 }
 private void stopNow(){
  if(stopping)return;stopping=true;active=false;
  main.removeCallbacks(monitor);
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