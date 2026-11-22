import com.iawolf.captureprobe.ModelFingerprint;
import java.nio.file.Files;
import java.nio.file.Path;
public final class ModelFingerprintTest {
 public static void main(String[] args) throws Exception {
  Path f = Files.createTempFile("control-model-check", ".litertlm");
  try {
   Files.writeString(f, "abc");
   String got = ModelFingerprint.sha256(f.toFile());
   if (!got.equals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")) throw new AssertionError(got);
   Files.writeString(f, "abcd");
   if (ModelFingerprint.sha256(f.toFile()).equals(got)) throw new AssertionError("Fingerprint stale");
   System.out.println("MODEL_FINGERPRINT_OK");
  } finally { Files.deleteIfExists(f); }
 }
}
