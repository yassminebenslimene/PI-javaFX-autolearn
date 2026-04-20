package tn.esprit.tools;

import javax.sound.sampled.*;

/**
 * Sons instantanés via Clip pré-chargé — zéro latence au clic.
 */
public class SoundPlayer {

    private static boolean enabled = true;
    public static void setEnabled(boolean e) { enabled = e; }

    // Sons pré-générés et pré-chargés en mémoire au démarrage
    private static Clip clipClick;
    private static Clip clipStart;
    private static Clip clipFinish;

    static {
        // Pré-charger tous les sons au démarrage de l'app
        clipClick  = buildClip(880, 60,  0.25f);
        clipStart  = buildClip(784, 200, 0.40f);
        clipFinish = buildClip(1047, 300, 0.55f);
    }

    public static void playClick()  { play(clipClick); }
    public static void playStart()  { play(clipStart); }
    public static void playFinish() { play(clipFinish); }

    /** Joue un Clip pré-chargé — non bloquant, instantané */
    private static void play(Clip clip) {
        if (!enabled || clip == null) return;
        // Rembobiner et jouer immédiatement
        clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }

    /** Génère et pré-charge un Clip en mémoire */
    private static Clip buildClip(int freqHz, int durationMs, float volume) {
        try {
            float sampleRate = 44100f;
            int samples = (int)(sampleRate * durationMs / 1000);
            byte[] buf = new byte[samples * 2];

            for (int i = 0; i < samples; i++) {
                double angle = 2.0 * Math.PI * i * freqHz / sampleRate;
                double fadeIn  = sampleRate * 0.005;
                double fadeOut = sampleRate * 0.015;
                double env = Math.min(1.0, Math.min(i / fadeIn, (samples - i) / fadeOut));
                short val = (short)(Math.sin(angle) * env * volume * Short.MAX_VALUE);
                buf[i * 2]     = (byte)(val & 0xFF);
                buf[i * 2 + 1] = (byte)((val >> 8) & 0xFF);
            }

            AudioFormat fmt = new AudioFormat(sampleRate, 16, 1, true, false);
            Clip clip = AudioSystem.getClip();
            clip.open(fmt, buf, 0, buf.length);
            return clip;
        } catch (Exception e) {
            return null;
        }
    }
}
