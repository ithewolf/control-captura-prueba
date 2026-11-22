import com.iawolf.captureprobe.GameFrameSelectionPolicy;

public final class GameFrameSelectionPolicyTest {
 private static int checks;
 private static void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 public static void main(String[] args){
  GameFrameSelectionPolicy p=new GameFrameSelectionPolicy();
  check(!p.shouldCapture(1000,1440,3120),"portrait diagnostics are not gameplay samples");
  check(!p.shouldCapture(1000,0,720),"invalid geometry");
  check(p.shouldCapture(1000,3120,1440),"first landscape frame");
  check(!p.shouldCapture(1001,3120,1440),"reject bursts");
  check(!p.shouldCapture(5499,3120,1440),"respect interval");
  check(p.shouldCapture(5500,3120,1440),"second sample");
  check(!p.shouldCapture(5400,3120,1440),"reject decreasing timestamps");
  check(p.shouldCapture(10000,3120,1440),"third sample");
  check(p.finished()&&p.count()==3,"bounded to three");
  check(!p.shouldCapture(20000,3120,1440),"sampling stops automatically");
  p.reset();
  check(!p.finished()&&p.count()==0,"reset clears state");
  check(p.shouldCapture(30000,1920,1080),"new session");
  System.out.println("GameFrameSelectionPolicyTest: "+checks+" checks OK");
 }
}
