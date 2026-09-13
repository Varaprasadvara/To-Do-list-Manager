import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class SoundNotifier {

    private static final String ADD_SOUND = "add.wav";
    private static final String DELETE_SOUND = "delete.wav";
    private static final String COMPLETE_SOUND = "complete.wav";

    private boolean soundEnabled = true;
    private boolean voiceEnabled = true;

    public void notifyTaskAdded() {
        playSoundFile(ADD_SOUND);
        speak("Task Added Successfully");
    }

    public void notifyTaskDeleted() {
        playSoundFile(DELETE_SOUND);
        speak("Task Deleted");
    }

    public void notifyTaskCompleted() {
        playSoundFile(COMPLETE_SOUND);
        speak("Task Completed");
    }

    public void notifyTaskReminder(String taskTitle) {
        beep();
        speak("Reminder. Your task " + taskTitle + " is due now.");
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public void setSoundEnabled(boolean soundEnabled) {
        this.soundEnabled = soundEnabled;
    }

    public boolean isVoiceEnabled() {
        return voiceEnabled;
    }

    public void setVoiceEnabled(boolean voiceEnabled) {
        this.voiceEnabled = voiceEnabled;
    }

    private void playSoundFile(String fileName) {
        if (!soundEnabled) {
            return;
        }
        File file = new File(fileName);
        if (!file.exists()) {
            beep();
            return;
        }
        try (AudioInputStream audioIn = AudioSystem.getAudioInputStream(file)) {
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
            clip.addLineListener(event -> {
                if (event.getType() == javax.sound.sampled.LineEvent.Type.STOP) {
                    clip.close();
                }
            });
        } catch (UnsupportedAudioFileException | IOException
                | LineUnavailableException e) {
            System.err.println("Could not play sound '" + fileName + "': "
                    + e.getMessage());
            beep();
        }
    }

    private void beep() {
        if (!soundEnabled) {
            return;
        }
        try {
            java.awt.Toolkit.getDefaultToolkit().beep();
        } catch (Exception e) {
            System.err.println("Beep unavailable: " + e.getMessage());
        }
    }

    private void speak(String text) {
        if (!voiceEnabled) {
            return;
        }
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        Thread voiceThread = new Thread(() -> {
            if (!speakWithFreeTts(text)) {
                speakWithSystemVoice(text);
            }
        }, "voice-notification");
        voiceThread.setDaemon(true);
        voiceThread.start();
    }

    /**
     * Uses FreeTTS when it has been added to the application's classpath.
     *
     * @return true when FreeTTS spoke the message successfully
     */
    private boolean speakWithFreeTts(String text) {
        try {
            Class<?> voiceManagerClass = Class.forName(
                    "com.sun.speech.freetts.VoiceManager");
            java.lang.reflect.Method getInstance =
                    voiceManagerClass.getMethod("getInstance");
            Object voiceManager = getInstance.invoke(null);

            Class<?> voiceClass = Class.forName("com.sun.speech.freetts.Voice");
            java.lang.reflect.Method getVoice =
                    voiceManagerClass.getMethod("getVoice", String.class);
            Object voice = getVoice.invoke(voiceManager, "kevin16");

            if (voice == null) {
                voice = getVoice.invoke(voiceManager, "kevin");
            }
            if (voice == null) {
                return false;
            }

            java.lang.reflect.Method allocate = voiceClass.getMethod("allocate");
            allocate.invoke(voice);
            java.lang.reflect.Method speak = voiceClass.getMethod("speak",
                    String.class);
            speak.invoke(voice, text);
            java.lang.reflect.Method deallocate = voiceClass.getMethod("deallocate");
            deallocate.invoke(voice);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (Exception e) {
            System.err.println("Voice announcement failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Uses Windows' built-in speech synthesizer when FreeTTS is unavailable.
     */
    private void speakWithSystemVoice(String text) {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            printAnnouncement(text);
            return;
        }

        String escapedText = text.replace("'", "''");
        String command = "Add-Type -AssemblyName System.Speech; "
                + "$voice = New-Object System.Speech.Synthesis.SpeechSynthesizer; "
                + "$voice.Speak('" + escapedText + "'); $voice.Dispose()";
        try {
            Process process = new ProcessBuilder("powershell", "-NoProfile",
                    "-NonInteractive", "-Command", command)
                    .redirectErrorStream(true)
                    .start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                printAnnouncement(text);
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            System.err.println("System voice announcement failed: " + e.getMessage());
            printAnnouncement(text);
        }
    }

    private void printAnnouncement(String text) {
        System.out.println("[Voice] " + text);
    }
}
