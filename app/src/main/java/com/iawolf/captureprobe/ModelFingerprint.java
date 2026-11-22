package com.iawolf.captureprobe;
import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
public final class ModelFingerprint {
  private ModelFingerprint() {}
  public static String sha256(File file) throws Exception {
    if (file == null || !file.isFile()) throw new IllegalArgumentException("Modelo no encontrado");
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    try (FileInputStream stream = new FileInputStream(file)) {
      byte[] buffer = new byte[65536]; int count;
      while ((count = stream.read(buffer)) != -1) if (count > 0) md.update(buffer, 0, count);
    }
    StringBuilder out = new StringBuilder(64);
    for (byte b : md.digest()) out.append(String.format(java.util.Locale.ROOT, "%02x", b & 0xff));
    return out.toString();
  }
}
