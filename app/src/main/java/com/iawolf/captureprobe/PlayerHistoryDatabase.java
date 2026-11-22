package com.iawolf.captureprobe;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.Locale;

/**
 * Local, player-confirmed champion/role history. No game telemetry is invented.
 * This data is opt-in and never uploaded. Context summaries are bounded.
 */
public final class PlayerHistoryDatabase extends SQLiteOpenHelper {
  private static final String NAME="control_player_history.db";
  private static final int VERSION=1;
  public PlayerHistoryDatabase(Context context){super(context,NAME,null,VERSION);}
  @Override public void onCreate(SQLiteDatabase db){
    db.execSQL("CREATE TABLE player_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, champion TEXT NOT NULL, role TEXT NOT NULL, recorded_ms INTEGER NOT NULL)");
    db.execSQL("CREATE INDEX idx_player_recent ON player_sessions(recorded_ms DESC)");
  }
  @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){
    throw new IllegalStateException("Database migration not yet defined");
  }
  private static boolean validChampion(String value){
    return value!=null && value.length()>=2 && value.length()<=40 && value.matches("[\\p{L}\\p{M}0-9 .'-]+");
  }
  private static String validRole(String value){
    if(value==null) return null;
    switch(value.trim().toUpperCase(Locale.ROOT)){
      case "TOP": case "BARON": return "BARON";
      case "JUNGLE": case "JUNGLA": return "JUNGLA";
      case "MID": case "MEDIO": return "MID";
      case "ADC": case "DRAGON": return "ADC";
      case "SUPPORT": case "SOPORTE": return "SOPORTE";
      default: return null;
    }
  }
  /** Returns false for malformed player input instead of storing invented data. */
  public synchronized boolean recordManual(String champion,String role,long timestampMs){
    if(timestampMs<=0 || champion==null) return false;
    String clean=champion.trim();
    String normalized=validRole(role);
    if(!validChampion(clean)||normalized==null) return false;
    SQLiteDatabase db=getWritableDatabase();
    ContentValues cv=new ContentValues();
    cv.put("champion",clean);cv.put("role",normalized);cv.put("recorded_ms",timestampMs);
    db.beginTransaction();
    try{
      if(db.insert("player_sessions",null,cv)<0) return false;
      // Retain up to 5000 records; storage is independent of the model context.
      db.execSQL("DELETE FROM player_sessions WHERE id NOT IN (SELECT id FROM player_sessions ORDER BY recorded_ms DESC,id DESC LIMIT 5000)");
      db.setTransactionSuccessful();
      return true;
    }finally{db.endTransaction();}
  }
  /** Latest manual entries only: not an estimate of win rate or skill. */
  public synchronized String recentPrompt(int maxEntries) {
    int limit=Math.max(1,Math.min(12,maxEntries));
    StringBuilder out=new StringBuilder();
    try(Cursor c=getReadableDatabase().query("player_sessions",new String[]{"champion","role","recorded_ms"},null,null,null,null,"recorded_ms DESC,id DESC",String.valueOf(limit))){
      while(c.moveToNext()){
        if(out.length()>0) out.append("; ");
        out.append(c.getString(0)).append(" (").append(c.getString(1)).append(")");
      }
    }
    return out.length()==0?"":"Historial manual de campeones y roles recientes: "+out.toString();
  }
  public synchronized void clearHistory(){getWritableDatabase().delete("player_sessions",null,null);}
}
