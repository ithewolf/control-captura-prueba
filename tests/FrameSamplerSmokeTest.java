package com.iawolf.captureprobe;

import java.nio.ByteBuffer;

public final class FrameSamplerSmokeTest {
  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  public static void main(String[] args) {
    int w = 32, h = 24, row = w * 4;
    ByteBuffer pixels = ByteBuffer.allocate(row * h);
    FrameSampler.Result empty = FrameSampler.sample(pixels, w, h, row, 4);
    check(empty.dark, "La imagen negra debe marcarse oscura");
    FrameSampler.Result same = FrameSampler.sample(pixels, w, h, row, 4);
    check(empty.fingerprint == same.fingerprint, "Una imagen idéntica debe conservar su huella");

    for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
      int i = y * row + x * 4;
      pixels.put(i, (byte)120);
      pixels.put(i + 1, (byte)80);
      pixels.put(i + 2, (byte)40);
      pixels.put(i + 3, (byte)255);
    }
    FrameSampler.Result changed = FrameSampler.sample(pixels, w, h, row, 4);
    check(!changed.dark, "La imagen iluminada no debe marcarse oscura");
    check(changed.fingerprint != empty.fingerprint, "Un cambio importante debe modificar la huella");
    boolean invalid = false;
    try { FrameSampler.sample(pixels, w, h, row - 1, 4); }
    catch (IllegalArgumentException expected) { invalid = true; }
    check(invalid, "Debe rechazar geometrías inválidas");
    System.out.println("FrameSampler: 5 comprobaciones correctas");
  }
}
