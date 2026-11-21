package com.iawolf.captureprobe;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.media.projection.MediaProjectionConfig;
import android.os.*;
import android.widget.*;
public class MainActivity extends Activity {
 private TextView label; private final Handler h=new Handler(Looper.getMainLooper());
 private long before=-1;
 private final Runnable refresh=new Runnable(){public void run(){
  label.setText("Estado: "+CaptureService.status+"\nCallbacks: "+CaptureService.callbacks+\n   "\nImágenes recibidas: "+CaptureService.received+"\nImágenes analizadas: "+CaptureService.frames+
   "\nCambios: "+CaptureService.changes+"\nOscuros: "+CaptureService.darkFrames+
   "\nTamaño: "+CaptureService.width+" x "+CaptureService.height+
   "\nCuadros desde salida: "+(before<0?"sin medición":Math.max(0,CaptureService.frames-before))+
   "\nError: "+CaptureService.error);
  android.content.SharedPreferences snap=getSharedPreferences("capture_diagnostics",MODE_PRIVATE);
  label.append("\nÚltimo registro guardado: callbacks "+snap.getLong("callbacks",0)+\n    " · recibidas "+snap.getLong("received",0)+\n    " · analizadas "+snap.getLong("frames",0)+
    " · cambios "+snap.getLong("changes",0)+
    " · estado "+snap.getString("status","sin registro")+
    " · error "+snap.getString("error",""));
  h.postDelayed(this,1000);
 }};
 @Override public void onCreate(Bundle b){super.onCreate(b);LinearLayout l=new LinearLayout(this);
  l.setOrientation(1);l.setPadding(25,25,25,25);
  label=new TextView(this);l.addView(label);
  Button start=new Button(this);start.setText("Autorizar captura");l.addView(start);
  Button stop=new Button(this);stop.setText("Detener");l.addView(stop);
  start.setOnClickListener(v->requestCapture());stop.setOnClickListener(v->startService(new Intent(this,CaptureService.class).setAction("STOP")));
  setContentView(l);
 }
 private void requestCapture(){
  if(CaptureService.active){Toast.makeText(this,"Detené la sesión anterior",Toast.LENGTH_SHORT).show();return;}
  MediaProjectionManager m=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
  Intent request=android.os.Build.VERSION.SDK_INT>=34
   ?m.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
   :m.createScreenCaptureIntent();
  startActivityForResult(request,77);
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