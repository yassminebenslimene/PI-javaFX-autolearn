package tn.esprit.controllers.evenement.front;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Générateur de sons pour les jeux et interactions.
 * Utilise javax.sound.sampled pour générer des sons synthétiques réalistes.
 */
public class SoundGenerator {
    private static final float SR = 44100f;

    // ── CANDY CRUSH SOUNDS ──
    public static void playCandySwap() {
        playAsync(() -> {
            try {
                AudioFormat fmt = new AudioFormat(SR, 16, 1, true, false);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                // Swap sound: quick ascending beep
                baos.write(genTone(600, 80, 0.3f));
                baos.write(genTone(800, 80, 0.3f));
                playBytes(baos.toByteArray(), fmt);
            } catch (Exception ignored) {}
        });
    }

    public static void playCandyMatch() {
        playAsync(() -> {
            try {
                AudioFormat fmt = new AudioFormat(SR, 16, 1, true, false);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                // Match sound: ascending melody
                baos.write(genTone(523, 100, 0.4f));
                baos.write(genTone(659, 100, 0.4f));
                baos.write(genTone(784, 150, 0.5f));
                playBytes(baos.toByteArray(), fmt);
            } catch (Exception ignored) {}
        });
    }

    public static void playCandyExplosion() {
        playAsync(() -> {
            try {
                AudioFormat fmt = new AudioFormat(SR, 16, 1, true, false);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                // Explosion: descending noise burst
                baos.write(genNoise(200, 0.6f));
                baos.write(genTone(400, 100, 0.3f));
                playBytes(baos.toByteArray(), fmt);
            } catch (Exception ignored) {}
        });
    }

    public static void playCandyVictory() {
        playAsync(() -> {
            try {
                AudioFormat fmt = new AudioFormat(SR, 16, 1, true, false);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                // Victory: ascending triumphant melody
                baos.write(genTone(523, 150, 0.4f));
                baos.write(genTone(659, 150, 0.4f));
                baos.write(genTone(784, 150, 0.4f));
                baos.write(genTone(1047, 300, 0.5f));
                playBytes(baos.toByteArray(), fmt);
            } catch (Exception ignored) {}
        });
    }

    // ── COFFEE MACHINE SOUNDS ── (sons doux et agréables)
    public static void playCoffeeGrind() {
        playAsync(() -> {
            try {
                AudioFormat fmt = new AudioFormat(SR, 16, 1, true, false);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                // Son doux de mouture : tons graves harmonieux
                baos.write(genTone(180, 200, 0.2f));
                baos.write(genTone(220, 200, 0.2f));
                baos.write(genTone(180, 200, 0.15f));
                playBytes(baos.toByteArray(), fmt);
            } catch (Exception ignored) {}
        });
    }

    public static void playCoffeeSteam() {
        playAsync(() -> {
            try {
                AudioFormat fmt = new AudioFormat(SR, 16, 1, true, false);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                // Son de vapeur : mélodie douce montante
                baos.write(genTone(440, 150, 0.2f));
                baos.write(genTone(494, 150, 0.2f));
                baos.write(genTone(523, 200, 0.2f));
                playBytes(baos.toByteArray(), fmt);
            } catch (Exception ignored) {}
        });
    }

    public static void playCoffeeDing() {
        playAsync(() -> {
            try {
                AudioFormat fmt = new AudioFormat(SR, 16, 1, true, false);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                // Ding final : son de cloche agréable
                baos.write(genTone(1047, 120, 0.35f));
                baos.write(genTone(1319, 120, 0.35f));
                baos.write(genTone(1568, 250, 0.4f));
                playBytes(baos.toByteArray(), fmt);
            } catch (Exception ignored) {}
        });
    }

    // ── MEMORY GAME SOUNDS ──
    public static void playMemoryFlip() {
        playAsync(() -> {
            try {
                AudioFormat fmt = new AudioFormat(SR, 16, 1, true, false);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                // Flip sound: quick ascending beep
                baos.write(genTone(800, 100, 0.3f));
                playBytes(baos.toByteArray(), fmt);
            } catch (Exception ignored) {}
        });
    }

    public static void playMemoryMatch() {
        playAsync(() -> {
            try {
                AudioFormat fmt = new AudioFormat(SR, 16, 1, true, false);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                // Match sound: happy ascending melody
                baos.write(genTone(659, 100, 0.4f));
                baos.write(genTone(784, 100, 0.4f));
                baos.write(genTone(1047, 200, 0.5f));
                playBytes(baos.toByteArray(), fmt);
            } catch (Exception ignored) {}
        });
    }

    public static void playMemoryVictory() {
        playAsync(() -> {
            try {
                AudioFormat fmt = new AudioFormat(SR, 16, 1, true, false);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                // Victory: triumphant melody
                baos.write(genTone(523, 150, 0.4f));
                baos.write(genTone(659, 150, 0.4f));
                baos.write(genTone(784, 150, 0.4f));
                baos.write(genTone(1047, 300, 0.5f));
                playBytes(baos.toByteArray(), fmt);
            } catch (Exception ignored) {}
        });
    }

    // ── LEGACY SOUNDS (kept for compatibility) ──
    public static void playSelection() {
        playCandySwap();
    }

    public static void playRevelation() {
        playCandyMatch();
    }

    private static void playAsync(Runnable r) {
        Thread t = new Thread(r, "sound-gen");
        t.setDaemon(true);
        t.start();
    }

    private static byte[] genTone(int freq, int ms, float vol) {
        int n = (int)(SR * ms / 1000.0);
        byte[] buf = new byte[n * 2];
        int fi = Math.max(1, n/8), fo = Math.max(1, n/5);
        for (int i = 0; i < n; i++) {
            double t = i / SR;
            double w = (Math.sin(2*Math.PI*freq*t) + 0.3*Math.sin(4*Math.PI*freq*t)) / 1.3;
            double env = i < fi ? (double)i/fi : i > n-fo ? (double)(n-i)/fo : 1.0;
            short s = (short)(w * env * vol * Short.MAX_VALUE);
            buf[i*2]=(byte)(s&0xFF);
            buf[i*2+1]=(byte)((s>>8)&0xFF);
        }
        return buf;
    }

    private static byte[] genNoise(int ms, float vol) {
        int n = (int)(SR * ms / 1000.0);
        byte[] buf = new byte[n * 2];
        java.util.Random rand = new java.util.Random();
        int fi = Math.max(1, n/8), fo = Math.max(1, n/5);
        for (int i = 0; i < n; i++) {
            double env = i < fi ? (double)i/fi : i > n-fo ? (double)(n-i)/fo : 1.0;
            short s = (short)(rand.nextGaussian() * env * vol * Short.MAX_VALUE);
            buf[i*2]=(byte)(s&0xFF);
            buf[i*2+1]=(byte)((s>>8)&0xFF);
        }
        return buf;
    }

    private static void playBytes(byte[] data, AudioFormat fmt) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        AudioInputStream ais = new AudioInputStream(bais, fmt, data.length / fmt.getFrameSize());
        DataLine.Info info = new DataLine.Info(Clip.class, fmt);
        if (!AudioSystem.isLineSupported(info)) return;
        Clip clip = (Clip) AudioSystem.getLine(info);
        clip.open(ais);
        clip.start();
        Thread.sleep(clip.getMicrosecondLength() / 1000 + 50);
        clip.close();
    }
}
