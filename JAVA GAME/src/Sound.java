import java.io.File;
import java.io.IOException;
import javax.sound.sampled.*;

public class Sound {
    private Clip clip;

    public Sound(String filePath) {
        try {
            // Open an audio input stream.
            File soundFile = new File(filePath);
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
            
            // Get a sound clip resource.
            clip = AudioSystem.getClip();
            
            // Open audio clip and load samples from the audio input stream.
            clip.open(audioIn);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public void play() {
        if (clip == null) return;
        clip.setFramePosition(0); // Rewind to the beginning
        clip.start();
    }

    public void loop() {
        if (clip == null) return;
        clip.loop(Clip.LOOP_CONTINUOUSLY); // Loop forever
        clip.start();
    }

    public void stop() {
        if (clip == null) return;
        clip.stop();
    }
}
