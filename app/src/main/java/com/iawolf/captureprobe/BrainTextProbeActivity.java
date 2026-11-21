package com.iawolf.captureprobe;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Laboratorio de conversación por texto: no solicita micrófono ni captura.
 * Sirve para demostrar y depurar el cerebro antes de integrar voz y visión.
 */
public final class BrainTextProbeActivity extends Activity {
  private final TacticalSession session=new TacticalSession();
  private TextView log;
  private EditText input;
  private void ask() {
    String text=input.getText().toString().trim();
    if(text.isEmpty())return;
    TacticalBrain.Decision d=session.respond(text,false,0,SystemClock.elapsedRealtime());
    log.append("\nVOS: "+text+"\nCONTROL: "+d.answer+
      "\n[Intento: "+d.intent+" | Contexto insuficiente: "+d.needsMoreContext+
      " | Motivo: "+d.rationale+"]\n");
    input.setText("");
  }
  @Override public void onCreate(Bundle b) {
    super.onCreate(b);
    LinearLayout layout=new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    int pad=(int)(16*getResources().getDisplayMetrics().density);
    layout.setPadding(pad,pad,pad,pad);
    TextView info=new TextView(this);
    info.setText("CONTROL — laboratorio del cerebro por texto. Sin micrófono, internet ni visión. Los hechos solo provienen de lo que escribís.");
    layout.addView(info);
    input=new EditText(this);input.setSingleLine(false);input.setMinLines(2);
    input.setHint("Ej.: Me están gankeando y estoy solo");
    layout.addView(input);
    Button ask=new Button(this);ask.setText("Preguntar a CONTROL");ask.setOnClickListener(v->ask());layout.addView(ask);
    Button reset=new Button(this);reset.setText("Nueva situación / borrar contexto");
    reset.setOnClickListener(v->{session.reset();log.setText("Contexto reiniciado.\n");});
    layout.addView(reset);
    ScrollView scroll=new ScrollView(this);
    log=new TextView(this);log.setText("Escribí una situación o pregunta.\n");
    scroll.addView(log);layout.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    setContentView(layout);
  }
}
