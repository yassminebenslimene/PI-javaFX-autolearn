package tn.esprit.controllers.evenement.front;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Utilitaire pour les sons de confirmation et autres effets sonores.
 */
public class SoundUtil {
    private static final float SR = 44100f;

    public static void playSound(String type) {
        switch (type) {
            case "confirmation":
                playConfirmation();
                break;
            case "success":
                playSuccess();
                break;
            case "error":
                playError();
                break;
        }
    }

    private static void playConfirmation() {
        playAsync(() -> {
            try {
                AudioFormat fmt = new AudioFormat(SR, 16, 1, true, false);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                for (double[] n : new double[][]{{523,80},{659,80},{784,80},{1047,150}})
                    baos.write(genTone((int)n[0], (int)n[1], 0.4f));
                playBytes(baos.toByteArray(), fmt);
            } catch (Exception ignored) {}
        });
    }

    private static void playSuccess() {
        playAsync(() -> {
            try {
                AudioFormat fmt = new AudioFormat(SR, 16, 1, true, false);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                double[][] notes = {{523,80},{659,80},{784,80},{1047,80},{1319,80},{1568,80},{2093,300}};
                for (double[] n : notes) baos.write(genTone((int)n[0], (int)n[1], 0.45f));
                playBytes(baos.toByteArray(), fmt);
            } catch (Exception ignored) {}
        });
    }

    private static void playError() {
        playAsync(() -> {
            try {
                AudioFormat fmt = new AudioFormat(SR, 16, 1, true, false);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                baos.write(genTone(200, 100, 0.3f));
                baos.write(genTone(150, 100, 0.3f));
                playBytes(baos.toByteArray(), fmt);
            } catch (Exception ignored) {}
        });
    }

    private static void playAsync(Runnable r) {
        Thread t = new Thread(r, "sound-util");
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
