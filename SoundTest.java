import java.awt.Font;
import java.awt.GridLayout;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class SoundTest extends JFrame {
    private Synthesizer synth;
    private MidiChannel channel;

    public SoundTest() {
        setTitle("Tetris 音效測試器 (MIDI合成)");
        setSize(350, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(6, 1, 10, 10));
        ((javax.swing.JPanel) getContentPane()).setBorder(
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        );

        try {
            synth = MidiSystem.getSynthesizer();
            synth.open();
            channel = synth.getChannels()[0];
            channel.programChange(80);
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
        }

        addButton("移動 (Move)", () -> playTone(60, 100));
        addButton("旋轉 (Rotate)", () -> playTone(67, 100));
        addButton("快速落下/鎖定 (Hard Drop)", () -> playTone(36, 150));
        addButton("消除橫排 (Clear Line)", () -> {
            playTone(60, 100);
            playTone(64, 100);
            playTone(67, 100);
            playTone(72, 300);
        });
        addButton("遊戲結束 (Game Over)", () -> {
            playTone(60, 300);
            playTone(59, 300);
            playTone(58, 300);
            playTone(57, 800);
        });

        setVisible(true);
    }

    private void addButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setFont(new Font("微軟正黑體", Font.BOLD, 16));
        button.setFocusable(false);
        button.addActionListener(event -> new Thread(action).start());
        add(button);
    }

    private void playTone(int note, int durationMs) {
        if (channel == null) {
            return;
        }

        channel.noteOn(note, 100);
        try {
            Thread.sleep(durationMs);
        } catch (InterruptedException ignored) {
        }
        channel.noteOff(note);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SoundTest::new);
    }
}
