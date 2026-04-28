import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;

class SoundManager implements AutoCloseable {
    private static final int INSTRUMENT = 80;
    private static final int VELOCITY = 100;
    private static final int BGM_INSTRUMENT = 88;
    private static final int BGM_VELOCITY = 52;
    private static final int[] PLAYABLE_CHANNELS = {0, 1, 2, 3, 4, 5, 6};
    private static final int BACKGROUND_CHANNEL_INDEX = 7;
    private static final int[] BGM_NOTES = {48, 55, 60, 55, 50, 57, 62, 57, 52, 59, 64, 59, 43, 50, 55, 50};
    private static final int[] BGM_DURATIONS_MS = {
        420, 210, 210, 420,
        420, 210, 210, 420,
        420, 210, 210, 420,
        420, 210, 210, 630
    };

    private final ExecutorService soundExecutor = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "tetris-sound");
        thread.setDaemon(true);
        return thread;
    });
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "tetris-bgm");
        thread.setDaemon(true);
        return thread;
    });
    private final BlockingQueue<MidiChannel> channelPool = new LinkedBlockingQueue<>();
    private final Object midiLock = new Object();
    private final AtomicBoolean backgroundPlaying = new AtomicBoolean(false);

    private Synthesizer synthesizer;
    private MidiChannel backgroundChannel;

    SoundManager() {
        try {
            synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();

            MidiChannel[] channels = synthesizer.getChannels();
            for (int channelIndex : PLAYABLE_CHANNELS) {
                if (channelIndex < channels.length && channels[channelIndex] != null) {
                    MidiChannel channel = channels[channelIndex];
                    channel.programChange(INSTRUMENT);
                    channelPool.offer(channel);
                }
            }
            if (BACKGROUND_CHANNEL_INDEX < channels.length && channels[BACKGROUND_CHANNEL_INDEX] != null) {
                backgroundChannel = channels[BACKGROUND_CHANNEL_INDEX];
                backgroundChannel.programChange(BGM_INSTRUMENT);
            }

            if (channelPool.isEmpty()) {
                System.err.println("No MIDI channels are available for Tetris sound.");
            }
        } catch (MidiUnavailableException e) {
            synthesizer = null;
            System.err.println("MIDI synthesizer is unavailable. Tetris sound is disabled.");
            e.printStackTrace();
        }
    }

    void playMove() {
        playTone(60, 100);
    }

    void playRotate() {
        playTone(67, 100);
    }

    void playHardDrop() {
        playTone(36, 150);
    }

    void playClearLine() {
        playSequence(
            new int[] {60, 64, 67, 72},
            new int[] {100, 100, 100, 300}
        );
    }

    void playGameOver() {
        playSequence(
            new int[] {60, 59, 58, 57},
            new int[] {300, 300, 300, 800}
        );
    }

    void startBackgroundMusic() {
        if (synthesizer == null || backgroundChannel == null || !backgroundPlaying.compareAndSet(false, true)) {
            return;
        }

        backgroundExecutor.submit(() -> {
            while (backgroundPlaying.get() && !Thread.currentThread().isInterrupted()) {
                for (int i = 0; i < BGM_NOTES.length && backgroundPlaying.get(); i++) {
                    playBackgroundTone(BGM_NOTES[i], BGM_DURATIONS_MS[i]);
                }
            }
            stopBackgroundNotes();
        });
    }

    void stopBackgroundMusic() {
        backgroundPlaying.set(false);
        stopBackgroundNotes();
    }

    void playTone(int note, int durationMs) {
        playSequence(new int[] {note}, new int[] {durationMs});
    }

    void playSequence(int[] notes, int[] durationsMs) {
        if (notes.length != durationsMs.length) {
            throw new IllegalArgumentException("notes and durations must have the same length");
        }
        if (synthesizer == null || channelPool.isEmpty()) {
            return;
        }

        soundExecutor.submit(() -> {
            MidiChannel channel = borrowChannel();
            if (channel == null) {
                return;
            }

            try {
                for (int i = 0; i < notes.length; i++) {
                    playBlocking(channel, notes[i], durationsMs[i]);
                }
            } finally {
                releaseChannel(channel);
            }
        });
    }

    private void playBlocking(MidiChannel channel, int note, int durationMs) {
        synchronized (midiLock) {
            channel.noteOn(note, VELOCITY);
        }

        try {
            Thread.sleep(durationMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            synchronized (midiLock) {
                channel.noteOff(note);
            }
        }
    }

    private void playBackgroundTone(int note, int durationMs) {
        synchronized (midiLock) {
            backgroundChannel.noteOn(note, BGM_VELOCITY);
        }

        try {
            Thread.sleep(durationMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            synchronized (midiLock) {
                backgroundChannel.noteOff(note);
            }
        }
    }

    private void stopBackgroundNotes() {
        if (backgroundChannel == null) {
            return;
        }
        synchronized (midiLock) {
            backgroundChannel.allNotesOff();
        }
    }

    private MidiChannel borrowChannel() {
        return channelPool.poll();
    }

    private void releaseChannel(MidiChannel channel) {
        if (channel != null) {
            channelPool.offer(channel);
        }
    }

    @Override
    public void close() {
        stopBackgroundMusic();
        soundExecutor.shutdownNow();
        backgroundExecutor.shutdownNow();

        synchronized (midiLock) {
            while (!channelPool.isEmpty()) {
                MidiChannel channel = channelPool.poll();
                if (channel != null) {
                    channel.allNotesOff();
                }
            }

            if (backgroundChannel != null) {
                backgroundChannel.allNotesOff();
            }

            if (synthesizer != null && synthesizer.isOpen()) {
                synthesizer.close();
            }
        }
    }
}
