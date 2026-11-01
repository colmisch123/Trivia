package util;

import javazoom.jl.player.Player;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Lightweight MP3 audio player wrapper backed by the JLayer library.
 *
 * <p>Plays audio asynchronously on a dedicated daemon thread to prevent freezing the Swing GUI.
 * Supports thread-safe start, stop, and completion callback execution.</p>
 */
public class AudioPlayer {
    private static Player currentPlayer;
    private static Thread playerThread;
    private static volatile boolean isPlaying = false;

    /**
     * Plays the specified MP3 audio file asynchronously.
     *
     * @param audioPath Path or filename to the MP3 asset.
     * @param onComplete Optional callback executed when playback completes or is stopped.
     */
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
                // Expected when playback is stopped manually
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

    /**
     * Stops any currently playing audio clip immediately and releases resources.
     */
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

    /**
     * Returns whether an audio clip is currently actively playing.
     */
    public static boolean isPlaying() {
        return isPlaying;
    }
}
