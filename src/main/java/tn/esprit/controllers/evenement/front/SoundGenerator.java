package tn.esprit.controllers.evenement.front;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Générateur de sons synthétiques via javax.sound.sampled (JDK standard).
 * Zéro dépendance externe, zéro fichier externe.
 * Tous les sons sont générés programmatiquement en mémoire.
 */
public class SoundGenerator {

    private static final float SAMPLE_RATE = 44100f;
    private static final int BITS = 16;
    private static final int CHANNELS = 1;

    /**
     * Joue un son de sélection (bip court montant — style jeu).
     */
    public static void playSelection() {
        playAsync(() -> playTone(new double[]{880, 1100}, new int[]{120, 120}, 0.4f));
    }

    /**
     * Joue un son de révélation (mélodie joyeuse — style surprise/cadeau).
     */
    public static void playRevelation() {
        playAsync(() -> playTone(
                new double[]{523, 659, 784, 1047},
                new int[]{120, 120, 120, 300},
                0.5f));
    }

    /**
     * Joue un son de confirmation (bip de succès).
     */
    public static void playConfirmation() {
        playAsync(() -> playTone(new double[]{660, 880}, new int[]{150, 250}, 0.45f));
    }

    // ── Implémentation interne ───────────────────────────────────

    private static void playAsync(Runnable r) {
        Thread t = new Thread(r, "sound-player");
        t.setDaemon(true);
        t.start();
    }

    private static void playTone(double[] frequencies, int[] durationsMs, float volume) {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, BITS, CHANNELS, true, false);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            for (int i = 0; i < frequencies.length; i++) {
                byte[] samples = generateSamples(frequencies[i], durationsMs[i], volume);
                baos.write(samples);
            }

            byte[] audioData = baos.toByteArray();
            ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
            AudioInputStream ais = new AudioInputStream(bais, format,
                    audioData.length / format.getFrameSize());

            DataLine.Info info = new DataLine.Info(Clip.class, format);
            if (!AudioSystem.isLineSupported(info)) return;

            Clip clip = (Clip) AudioSystem.getLine(info);
            clip.open(ais);
            clip.start();

            // Attendre la fin de la lecture
            Thread.sleep(clip.getMicrosecondLength() / 1000 + 50);
            clip.close();

        } catch (Exception e) {
            // Silencieux — le son est optionnel
        }
    }

    private static byte[] generateSamples(double frequency, int durationMs, float volume) {
        int numSamples = (int) (SAMPLE_RATE * durationMs / 1000.0);
        byte[] buffer = new byte[numSamples * 2]; // 16 bits = 2 bytes par sample

        for (int i = 0; i < numSamples; i++) {
            // Onde sinusoïdale avec envelope (fade in/out pour éviter les clics)
            double t = i / SAMPLE_RATE;
            double wave = Math.sin(2 * Math.PI * frequency * t);

            // Envelope: fade in 10ms, fade out 20ms
            double fadeInSamples = SAMPLE_RATE * 0.01;
            double fadeOutSamples = SAMPLE_RATE * 0.02;
            double envelope = 1.0;
            if (i < fadeInSamples) envelope = i / fadeInSamples;
            else if (i > numSamples - fadeOutSamples) envelope = (numSamples - i) / fadeOutSamples;

            short sample = (short) (wave * envelope * volume * Short.MAX_VALUE);
            buffer[i * 2] = (byte) (sample & 0xFF);
            buffer[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        return buffer;
    }
}
