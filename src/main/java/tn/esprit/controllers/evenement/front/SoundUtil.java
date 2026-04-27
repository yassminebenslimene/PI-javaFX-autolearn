package tn.esprit.controllers.evenement.front;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilitaire sons synthétiques via javax.sound.sampled (JDK standard).
 * Zéro dépendance externe, zéro fichier audio requis.
 * Tous les sons sont générés programmatiquement.
 */
public class SoundUtil {

    private SoundUtil() {}

    /**
     * Joue un son selon le type demandé.
     * @param type "selection", "revelation", "confirmation", ou tout autre (bip simple)
     */
    public static void playSound(String type) {
        new Thread(() -> {
            try {
                AudioFormat format = new AudioFormat(44100f, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                if (!AudioSystem.isLineSupported(info)) return;

                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(format);
                line.start();

                byte[] data = switch (type) {
                    case "selection"     -> generateTone(880, 200, 44100, 0.4);
                    case "revelation"    -> generateMelody(44100);
                    case "confirmation"  -> generateTone(660, 400, 44100, 0.5);
                    default              -> generateTone(440, 150, 44100, 0.3);
                };

                line.write(data, 0, data.length);
                line.drain();
                line.close();
            } catch (Exception ignored) {
                // Silencieux — le son est optionnel
            }
        }, "sound-player").start();
    }

    static byte[] generateTone(double freq, int durationMs, int sampleRate, double volume) {
        int samples = (int) (sampleRate * durationMs / 1000.0);
        byte[] data = new byte[samples * 2];
        int fadeLen = Math.max(1, samples / 10);
        for (int i = 0; i < samples; i++) {
            double angle = 2.0 * Math.PI * i * freq / sampleRate;
            double env = 1.0;
            if (i < fadeLen) env = (double) i / fadeLen;
            else if (i > samples - fadeLen) env = (double) (samples - i) / fadeLen;
            short val = (short) (Math.sin(angle) * env * volume * Short.MAX_VALUE);
            data[i * 2]     = (byte) (val & 0xFF);
            data[i * 2 + 1] = (byte) ((val >> 8) & 0xFF);
        }
        return data;
    }

    private static byte[] generateMelody(int sampleRate) {
        // do-mi-sol-do (style jeu)
        double[] freqs = {523.25, 659.25, 783.99, 1046.50};
        List<byte[]> notes = new ArrayList<>();
        int total = 0;
        for (double f : freqs) {
            byte[] note = generateTone(f, 150, sampleRate, 0.45);
            notes.add(note);
            total += note.length;
        }
        byte[] melody = new byte[total];
        int pos = 0;
        for (byte[] note : notes) {
            System.arraycopy(note, 0, melody, pos, note.length);
            pos += note.length;
        }
        return melody;
    }
}
