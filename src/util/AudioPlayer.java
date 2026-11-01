package util;

import javazoom.jl.player.Player;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class AudioPlayer {
    private static Player currentPlayer;
    private static Thread playerThread;
    private static volatile boolean isPlaying = false;

    public static synchronized void play(String audioPath, Runnable onComplete) {
        stop();

        File file = MediaResolver.resolveMediaFile(audioPath);
        if (file == null || !file.exists()) {
            System.err.println("AudioPlayer: Could not find audio file: " + audioPath);
            return;
        }

        isPlaying = true;
        playerThread = new Thread(() -> {
            try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
                synchronized (AudioPlayer.class) {
                    currentPlayer = new Player(is);
                }
                currentPlayer.play();
            } catch (Exception e) {
                // Expected when playback is stopped
            } finally {
                synchronized (AudioPlayer.class) {
                    isPlaying = false;
                    currentPlayer = null;
                }
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        }, "AudioPlayer-Thread");

        playerThread.setDaemon(true);
        playerThread.start();
    }

    public static synchronized void stop() {
        if (currentPlayer != null) {
            try {
                currentPlayer.close();
            } catch (Exception ignored) {
            }
            currentPlayer = null;
        }
        if (playerThread != null && playerThread.isAlive()) {
            playerThread.interrupt();
            playerThread = null;
        }
        isPlaying = false;
    }

    public static synchronized boolean isPlaying() {
        return isPlaying;
    }
}
