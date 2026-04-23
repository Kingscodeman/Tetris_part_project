import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;

class SoundManager implements AutoCloseable {
    private static final int INSTRUMENT = 80;
    private static final int VELOCITY = 100;
    private static final int[] PLAYABLE_CHANNELS = {0, 1, 2, 3, 4, 5, 6, 7};

    private final ExecutorService soundExecutor = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "tetris-sound");
        thread.setDaemon(true);
        return thread;
    });
    private final BlockingQueue<MidiChannel> channelPool = new LinkedBlockingQueue<>();
    private final Object midiLock = new Object();

    private Synthesizer synthesizer;

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
        soundExecutor.shutdownNow();

        synchronized (midiLock) {
            while (!channelPool.isEmpty()) {
                MidiChannel channel = channelPool.poll();
                if (channel != null) {
                    channel.allNotesOff();
                }
            }

            if (synthesizer != null && synthesizer.isOpen()) {
                synthesizer.close();
            }
        }
    }
}
