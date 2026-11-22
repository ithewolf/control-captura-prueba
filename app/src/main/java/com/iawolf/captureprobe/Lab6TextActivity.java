package com.iawolf.captureprobe;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StatFs;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Lab6 text-only model probe. Kept separate from the active Vosk/video service. */
public final class Lab6TextActivity extends Activity {
 private static final int PICK=206;
 private static final long LIMIT=3L*1024*1024*1024;
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private final AtomicBoolean busy=new AtomicBoolean(false);
 private volatile boolean destroyed;
 private LiteRtTextModel model;
 private ConversationCore brain;
 private TextView status,output;
 private EditText input;
 private Button importButton,askButton;

 @Override public void onCreate(Bundle saved){
  super.onCreate(saved);
  LinearLayout l=new LinearLayout(this);l.setOrientation(1);l.setPadding(24,24,24,24);
  TextView title=new TextView(this);
  title.setText("LAB 6 - CONTROL IA local\nPrueba solo texto: sin microfono ni captura. Sin respuestas preparadas.");
  l.addView(title);
  status=new TextView(this);status.setText("Falta cargar modelo .litertlm desde el telefono. No hay descargas automaticas.");l.addView(status);
  importButton=new Button(this);importButton.setText("Importar modelo .litertlm");l.addView(importButton);
  input=new EditText(this);input.setHint("Escribile una pregunta nueva a CONTROL");input.setMinLines(2);l.addView(input);
  askButton=new Button(this);askButton.setText("Generar respuesta con IA local");askButton.setEnabled(false);l.addView(askButton);
  ScrollView s=new ScrollView(this);output=new TextView(this);output.setText("Conversacion de prueba:\n");s.addView(output);
  l.addView(s,new LinearLayout.LayoutParams(-1,0,1f));setContentView(l);
  importButton.setOnClickListener(v->{
   if(busy.get())return;
   Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);
   i.setType("*/*");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
   startActivityForResult(i,PICK);
  });
  askButton.setOnClickListener(v->ask());
 }

 @Override protected void onActivityResult(int req,int res,Intent data){
  super.onActivityResult(req,res,data);
  if(req!=PICK||res!=RESULT_OK||data==null||data.getData()==null||!busy.compareAndSet(false,true))return;
  Uri chosen=data.getData();
  state("Importando modelo al almacenamiento privado; puede ocupar cientos de MB...");
  worker.execute(()->loadModel(chosen));
 }
 private void loadModel(Uri uri){
  File dir=new File(getFilesDir(),"control-lab6"),tmp=new File(dir,"model.part"),
       dest=new File(dir,"model.litertlm");
  try{
   String name=null;
   try(Cursor c=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){
    if(c!=null&&c.moveToFirst())name=c.getString(0);
   }
   if(name==null||!name.toLowerCase(Locale.ROOT).endsWith(".litertlm"))
    throw new IllegalArgumentException("Selecciona un archivo .litertlm");
   if(!dir.isDirectory()&&!dir.mkdirs())throw new IllegalStateException("No pude crear carpeta");
   long free=new StatFs(dir.getAbsolutePath()).getAvailableBytes();
   if(free<700L*1024*1024)throw new IllegalStateException("No hay espacio libre suficiente");
   long length=0;
   try(InputStream in=getContentResolver().openInputStream(uri);FileOutputStream out=new FileOutputStream(tmp)){
    if(in==null)throw new IllegalStateException("No se pudo abrir el archivo");
    byte[] buf=new byte[262144];int n;
    while((n=in.read(buf))!=-1){
     if(n==0)continue;length+=n;
     if(length>LIMIT||length>free-250L*1024*1024)
      throw new IllegalStateException("Modelo demasiado grande para el espacio disponible");
     out.write(buf,0,n);
    }
    out.getFD().sync();
   }
   if(length<1024*1024)throw new IllegalStateException("Modelo incompleto");
   if(model!=null){model.close();model=null;brain=null;}
   if(dest.exists()&&!dest.delete())throw new IllegalStateException("No pude reemplazar el modelo");
   if(!tmp.renameTo(dest))throw new IllegalStateException("No se pudo guardar el modelo");
   String fingerprint=ModelFingerprint.sha256(dest);
   state("Importado ("+(length/(1024*1024))+" MB), SHA-256: "+fingerprint.substring(0,16)+"... Inicializando LiteRT-LM local en CPU...");
   model=new LiteRtTextModel(dest,getCacheDir());
   brain=new ConversationCore(model);
   state("Modelo LOCAL cargado. Escribi una pregunta que no este programada.");
  }catch(Throwable e){state("Modelo NO listo: "+e.getClass().getSimpleName()+": "+e.getMessage());}
  finally{if(tmp.exists())tmp.delete();busy.set(false);refresh();}
 }
 private void ask(){
  if(brain==null||destroyed||busy.get())return;
  String question=input.getText().toString().trim();
  if(question.isEmpty()||!busy.compareAndSet(false,true))return;
  state("Generando con el modelo local, sin red...");
  worker.execute(()->{
   String mode=getSharedPreferences("control_mode",MODE_PRIVATE).getString("mode","coach");
   long started=android.os.SystemClock.elapsedRealtime();
   ConversationCore.Answer answer=brain.respond(mode,question,new ConversationCore.VisionStatus(false,false,false));
   long duration=android.os.SystemClock.elapsedRealtime()-started;
   if(!destroyed)runOnUiThread(()->{
    output.append("\nWolf: "+question+"\n"+(answer.ready?"CONTROL: "+answer.text:"ERROR: "+answer.detail)+"\n");
    input.setText("");
    status.setText((answer.ready?"Respuesta generada":"Sin respuesta")+" / "+duration+" ms / "+mode);
   });
   busy.set(false);refresh();
  });
 }
 private void state(String text){if(!destroyed)runOnUiThread(()->{status.setText(text);askButton.setEnabled(false);importButton.setEnabled(false);});}
 private void refresh(){if(!destroyed)runOnUiThread(()->{askButton.setEnabled(!busy.get()&&brain!=null);importButton.setEnabled(!busy.get());});}
 @Override protected void onDestroy(){
  destroyed=true;
  worker.execute(()->{if(model!=null)model.close();});worker.shutdown();
  super.onDestroy();
 }
}
