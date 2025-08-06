package nw;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URL;

public class YippeeDisplay extends JFrame {
    private static final int GIF_DURATION = 1740; // Optional if you want to auto-close after playback

    public YippeeDisplay() {
        // basic window setup
        setUndecorated(true);
        setAlwaysOnTop(true);
        setTitle("yippee");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // load gif
        URL imageUrl = getClass().getClassLoader().getResource("yippee.gif");
        if (imageUrl == null) {
            System.err.println("GIF file not found in resources!");
            return;
        }

        ImageIcon gifIcon = new ImageIcon(imageUrl);
        JLabel gifLabel = new JLabel(gifIcon);
        add(gifLabel, BorderLayout.CENTER);

        // load audio
        URL audioUrl = getClass().getClassLoader().getResource("yippee.wav");
        Clip clip;
        try {
            clip = AudioSystem.getClip();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
            clip = null;
        }
        if (audioUrl == null || clip == null) {
            System.err.println("Audio file not found in resources!");
        } else {
            try (AudioInputStream audioIn = AudioSystem.getAudioInputStream(audioUrl)) {
                // prime audio for being played
                clip.open(audioIn);
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
                e.printStackTrace();
            }
        }

        pack(); // resize to fit the gif
        setLocationRelativeTo(null); // center the frame
        setVisible(true);
        // play audio
        if (clip != null)
            clip.start();
        
        // wait until the gif ends, then close
        Timer timer = new Timer(GIF_DURATION, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(YippeeDisplay::new);
    }
}
