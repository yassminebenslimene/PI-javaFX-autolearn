package tn.esprit.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;

/**
 * Service de synthèse vocale utilisant Windows SAPI via PowerShell
 */
public class TextToSpeechService {
    
    private Process currentProcess;
    private boolean isPlaying = false;
    
    /**
     * Lit un texte avec la synthèse vocale
     * @param text Le texte à lire
     * @param rate La vitesse de lecture (-10 à +10, 0 = normal)
     * @param onComplete Callback appelé quand la lecture est terminée
     * @param onError Callback appelé en cas d'erreur
     */
    public void speak(String text, int rate, Runnable onComplete, java.util.function.Consumer<String> onError) {
        // Arrêter la lecture en cours
        stop();
        
        new Thread(() -> {
            try {
                isPlaying = true;
                
                // Nettoyer le texte
                String cleanText = text
                    .replace("\"", "''")
                    .replace("`", "'")
                    .replace("\n", " ")
                    .replace("\r", "")
                    .replaceAll("\\s+", " ")
                    .trim();
                
                if (cleanText.length() > 2000) {
                    cleanText = cleanText.substring(0, 2000);
                }
                
                System.out.println("[TTS] Texte à lire: " + cleanText.substring(0, Math.min(100, cleanText.length())) + "...");
                System.out.println("[TTS] Vitesse: " + rate);
                
                // Créer le script PowerShell
                String psScript = 
                    "Add-Type -AssemblyName System.Speech\n" +
                    "$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer\n" +
                    "$synth.Rate = " + rate + "\n" +
                    "$synth.Volume = 100\n" +
                    "\n" +
                    "# Lister les voix disponibles\n" +
                    "Write-Host 'Voix disponibles:'\n" +
                    "$synth.GetInstalledVoices() | ForEach-Object {\n" +
                    "    Write-Host \"  - $($_.VoiceInfo.Name) [$($_.VoiceInfo.Culture.Name)]\"\n" +
                    "}\n" +
                    "\n" +
                    "# Essayer de sélectionner une voix française\n" +
                    "$frenchVoice = $synth.GetInstalledVoices() | Where-Object { $_.VoiceInfo.Culture.Name -like 'fr*' } | Select-Object -First 1\n" +
                    "if ($frenchVoice) {\n" +
                    "    Write-Host \"Utilisation de la voix: $($frenchVoice.VoiceInfo.Name)\"\n" +
                    "    $synth.SelectVoice($frenchVoice.VoiceInfo.Name)\n" +
                    "} else {\n" +
                    "    Write-Host 'Aucune voix française trouvée, utilisation de la voix par défaut'\n" +
                    "}\n" +
                    "\n" +
                    "$text = @\"\n" + cleanText + "\n\"@\n" +
                    "\n" +
                    "Write-Host 'Démarrage de la lecture...'\n" +
                    "$synth.Speak($text)\n" +
                    "Write-Host 'Lecture terminée'\n";
                
                // Créer un fichier temporaire
                File tempScript = File.createTempFile("tts_", ".ps1");
                tempScript.deleteOnExit();
                
                FileWriter writer = new FileWriter(tempScript);
                writer.write(psScript);
                writer.close();
                
                System.out.println("[TTS] Script créé: " + tempScript.getAbsolutePath());
                
                // Exécuter le script
                ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe",
                    "-ExecutionPolicy", "Bypass",
                    "-File", tempScript.getAbsolutePath()
                );
                pb.redirectErrorStream(true);
                
                currentProcess = pb.start();
                
                // Lire la sortie
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(currentProcess.getInputStream(), "UTF-8")
                );
                
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[TTS Output] " + line);
                }
                
                int exitCode = currentProcess.waitFor();
                System.out.println("[TTS] Code de sortie: " + exitCode);
                
                isPlaying = false;
                tempScript.delete();
                
                if (exitCode == 0 && onComplete != null) {
                    onComplete.run();
                } else if (exitCode != 0 && onError != null) {
                    onError.accept("Code de sortie: " + exitCode);
                }
                
            } catch (Exception ex) {
                System.err.println("[TTS] Erreur: " + ex.getMessage());
                ex.printStackTrace();
                isPlaying = false;
                if (onError != null) {
                    onError.accept(ex.getMessage());
                }
            }
        }).start();
    }
    
    /**
     * Arrête la lecture en cours
     */
    public void stop() {
        if (currentProcess != null && currentProcess.isAlive()) {
            System.out.println("[TTS] Arrêt de la lecture");
            currentProcess.destroyForcibly();
            try {
                currentProcess.waitFor();
            } catch (Exception e) {
                // Ignorer
            }
            isPlaying = false;
        }
    }
    
    /**
     * Vérifie si une lecture est en cours
     */
    public boolean isPlaying() {
        return isPlaying && currentProcess != null && currentProcess.isAlive();
    }
    
    /**
     * Teste si la synthèse vocale fonctionne
     */
    public void test(Runnable onSuccess, java.util.function.Consumer<String> onError) {
        speak("Bonjour, ceci est un test de synthèse vocale.", 0, onSuccess, onError);
    }
}
