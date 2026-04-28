import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Random;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

class TetrisPanel extends JPanel {
    private static final int COLS = 10;
    private static final int ROWS = 20;
    private static final int CELL_SIZE = 30;
    private static final int SIDE_PANEL_WIDTH = 150;
    private static final int BOARD_X = 20;
    private static final int BOARD_Y = 20;
    private static final int NORMAL_DROP_DELAY = 500;
    private static final int FAST_DROP_DELAY = 60;

    private final Color[][] board = new Color[ROWS][COLS];
    private final Random random = new Random();
    private final SoundManager soundManager = new SoundManager();
    private final Timer timer;

    private Tetromino currentPiece;
    private Tetromino nextPiece;
    private boolean gameOver;
    private int score;
    private int linesCleared;

    TetrisPanel() {
        setPreferredSize(new Dimension(490, 640));
        setBackground(new Color(18, 18, 24));
        setFocusable(true);
        setDoubleBuffered(true);

        setupKeyBindings();

        timer = new Timer(NORMAL_DROP_DELAY, e -> gameStep());
        startGame();
    }

    private void startGame() {
        soundManager.stopBackgroundMusic();
        clearBoard();
        score = 0;
        linesCleared = 0;
        gameOver = false;
        currentPiece = randomPiece();
        nextPiece = randomPiece();
        centerCurrentPiece();
        timer.setDelay(NORMAL_DROP_DELAY);
        timer.start();
        soundManager.startBackgroundMusic();
        requestFocusInWindow();
        repaint();
    }

    private void clearBoard() {
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                board[i][j] = null;
            }
        }
    }

    private void spawnNewPiece() {
        currentPiece = nextPiece;
        nextPiece = randomPiece();
        centerCurrentPiece();
        currentPiece.rotation = 0;

        if (!isValidPosition(currentPiece, currentPiece.x, currentPiece.y, currentPiece.rotation)) {
            endGame();
        }
        repaint();
    }

    private void centerCurrentPiece() {
        currentPiece.x = 3;
        currentPiece.y = 0;
    }

    private Tetromino randomPiece() {
        TetrominoType[] types = TetrominoType.values();
        return new Tetromino(types[random.nextInt(types.length)]);
    }

    private void setupKeyBindings() {
        InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        bindKey(inputMap, actionMap, "LEFT", "moveLeft", () -> {
            if (moveCurrentPiece(-1, 0)) {
                soundManager.playMove();
            }
        });
        bindKey(inputMap, actionMap, "RIGHT", "moveRight", () -> {
            if (moveCurrentPiece(1, 0)) {
                soundManager.playMove();
            }
        });
        bindKey(inputMap, actionMap, "UP", "rotate", () -> {
            if (rotateCurrentPiece()) {
                soundManager.playRotate();
            }
        });
        bindKey(inputMap, actionMap, "DOWN", "softDropPress", () -> timer.setDelay(FAST_DROP_DELAY));
        bindKey(inputMap, actionMap, "released DOWN", "softDropRelease", () -> timer.setDelay(NORMAL_DROP_DELAY));
        bindKey(inputMap, actionMap, "SPACE", "hardDrop", this::hardDrop);
        bindKey(inputMap, actionMap, "R", "restart", () -> {
            if (gameOver) {
                startGame();
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (gameOver && event.getKeyCode() == KeyEvent.VK_ENTER) {
                    startGame();
                }
            }
        });
    }

    private void bindKey(
        InputMap inputMap,
        ActionMap actionMap,
        String keyStroke,
        String actionKey,
        Runnable action
    ) {
        inputMap.put(KeyStroke.getKeyStroke(keyStroke), actionKey);
        actionMap.put(actionKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!gameOver || "restart".equals(actionKey)) {
                    action.run();
                }
            }
        });
    }

    private void gameStep() {
        if (gameOver) {
            return;
        }

        if (!moveCurrentPiece(0, 1)) {
            lockCurrentPiece();
            clearCompletedLines();
            spawnNewPiece();
        }
        repaint();
    }

    private boolean moveCurrentPiece(int dx, int dy) {
        if (gameOver || currentPiece == null) {
            return false;
        }

        int newX = currentPiece.x + dx;
        int newY = currentPiece.y + dy;

        if (isValidPosition(currentPiece, newX, newY, currentPiece.rotation)) {
            currentPiece.x = newX;
            currentPiece.y = newY;
            repaint();
            return true;
        }
        return false;
    }

    private boolean rotateCurrentPiece() {
        if (gameOver || currentPiece == null) {
            return false;
        }

        int newRotation = (currentPiece.rotation + 1) % currentPiece.type.shapes.length;
        int[] kicks = {0, -1, 1, -2, 2};
        for (int kick : kicks) {
            if (isValidPosition(currentPiece, currentPiece.x + kick, currentPiece.y, newRotation)) {
                currentPiece.x += kick;
                currentPiece.rotation = newRotation;
                repaint();
                return true;
            }
        }
        return false;
    }

    private void hardDrop() {
        if (gameOver || currentPiece == null) {
            return;
        }

        boolean moved = false;
        while (moveCurrentPiece(0, 1)) {
            moved = true;
        }
        if (moved) {
            soundManager.playHardDrop();
        }
        gameStep();
    }

    private boolean isValidPosition(Tetromino piece, int x, int y, int rotation) {
        for (int[] offset : piece.type.shapes[rotation]) {
            int px = x + offset[0];
            int py = y + offset[1];

            if (px < 0 || px >= COLS || py >= ROWS) {
                return false;
            }
            if (py >= 0 && board[py][px] != null) {
                return false;
            }
        }
        return true;
    }

    private void lockCurrentPiece() {
        for (int[] offset : currentPiece.type.shapes[currentPiece.rotation]) {
            int px = currentPiece.x + offset[0];
            int py = currentPiece.y + offset[1];
            if (py < 0) {
                endGame();
                return;
            }
            board[py][px] = currentPiece.type.color;
        }
    }

    private void clearCompletedLines() {
        int linesCount = 0;
        for (int i = ROWS - 1; i >= 0; i--) {
            boolean full = true;
            for (int j = 0; j < COLS; j++) {
                if (board[i][j] == null) {
                    full = false;
                    break;
                }
            }

            if (full) {
                linesCount++;
                for (int k = i; k > 0; k--) {
                    System.arraycopy(board[k - 1], 0, board[k], 0, COLS);
                }
                for (int j = 0; j < COLS; j++) {
                    board[0][j] = null;
                }
                i++;
            }
        }

        if (linesCount > 0) {
            linesCleared += linesCount;
            score += switch (linesCount) {
                case 1 -> 100;
                case 2 -> 300;
                case 3 -> 500;
                case 4 -> 800;
                default -> linesCount * 200;
            };
            soundManager.playClearLine();
        }
    }

    private void endGame() {
        if (gameOver) {
            return;
        }
        gameOver = true;
        timer.stop();
        soundManager.stopBackgroundMusic();
        soundManager.playGameOver();
    }

    @Override
    public void removeNotify() {
        timer.stop();
        soundManager.close();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2d = (Graphics2D) graphics.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawBoard(g2d);
        drawPlacedBlocks(g2d);
        drawGhostPiece(g2d);
        drawCurrentPiece(g2d);
        drawSidePanel(g2d);

        if (gameOver) {
            drawGameOver(g2d);
        }

        g2d.dispose();
    }

    private void drawBoard(Graphics2D g2d) {
        int boardWidth = COLS * CELL_SIZE;
        int boardHeight = ROWS * CELL_SIZE;

        g2d.setColor(new Color(30, 33, 45));
        g2d.fillRoundRect(BOARD_X - 4, BOARD_Y - 4, boardWidth + 8, boardHeight + 8, 12, 12);
        g2d.setColor(new Color(70, 74, 90));
        g2d.drawRoundRect(BOARD_X - 4, BOARD_Y - 4, boardWidth + 8, boardHeight + 8, 12, 12);

        g2d.setColor(new Color(23, 26, 36));
        g2d.fillRect(BOARD_X, BOARD_Y, boardWidth, boardHeight);

        g2d.setColor(new Color(40, 44, 58));
        for (int i = 0; i <= COLS; i++) {
            int x = BOARD_X + i * CELL_SIZE;
            g2d.drawLine(x, BOARD_Y, x, BOARD_Y + boardHeight);
        }
        for (int i = 0; i <= ROWS; i++) {
            int y = BOARD_Y + i * CELL_SIZE;
            g2d.drawLine(BOARD_X, y, BOARD_X + boardWidth, y);
        }
    }

    private void drawPlacedBlocks(Graphics2D g2d) {
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                if (board[i][j] != null) {
                    drawCell(g2d, j, i, board[i][j]);
                }
            }
        }
    }

    private void drawCurrentPiece(Graphics2D g2d) {
        if (currentPiece == null) {
            return;
        }

        for (int[] offset : currentPiece.type.shapes[currentPiece.rotation]) {
            int px = currentPiece.x + offset[0];
            int py = currentPiece.y + offset[1];
            if (py >= 0) {
                drawCell(g2d, px, py, currentPiece.type.color);
            }
        }
    }

    private void drawGhostPiece(Graphics2D g2d) {
        if (currentPiece == null || gameOver) {
            return;
        }

        int ghostY = currentPiece.y;
        while (isValidPosition(currentPiece, currentPiece.x, ghostY + 1, currentPiece.rotation)) {
            ghostY++;
        }

        for (int[] offset : currentPiece.type.shapes[currentPiece.rotation]) {
            int px = currentPiece.x + offset[0];
            int py = ghostY + offset[1];
            if (py >= 0) {
                drawCell(g2d, px, py, currentPiece.type.color, true);
            }
        }
    }

    private void drawSidePanel(Graphics2D g2d) {
        int panelX = BOARD_X + (COLS * CELL_SIZE) + 24;
        int panelY = 44;

        g2d.setColor(new Color(220, 225, 236));
        g2d.setFont(new Font("SansSerif", Font.BOLD, 22));
        g2d.drawString("Tetris", panelX, panelY);

        g2d.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g2d.drawString("Score: " + score, panelX, panelY + 40);
        g2d.drawString("Lines: " + linesCleared, panelX, panelY + 70);

        g2d.drawString("Controls", panelX, panelY + 130);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g2d.drawString("Left / Right : Move", panelX, panelY + 160);
        g2d.drawString("Up : Rotate", panelX, panelY + 185);
        g2d.drawString("Down : Soft Drop", panelX, panelY + 210);
        g2d.drawString("Space : Hard Drop", panelX, panelY + 235);
        g2d.drawString("R / Enter : Restart", panelX, panelY + 260);

        g2d.setFont(new Font("SansSerif", Font.BOLD, 16));
        g2d.drawString("Next", panelX, panelY + 320);
        drawNextPiece(g2d, panelX, panelY + 340);
    }

    private void drawNextPiece(Graphics2D g2d, int x, int y) {
        g2d.setColor(new Color(30, 33, 45));
        g2d.fillRoundRect(x, y, SIDE_PANEL_WIDTH - 50, 100, 12, 12);
        g2d.setColor(new Color(70, 74, 90));
        g2d.drawRoundRect(x, y, SIDE_PANEL_WIDTH - 50, 100, 12, 12);

        if (nextPiece == null) {
            return;
        }

        for (int[] offset : nextPiece.type.shapes[0]) {
            int blockX = x + 18 + offset[0] * 20;
            int blockY = y + 18 + offset[1] * 20;
            Color color = nextPiece.type.color;

            g2d.setColor(color);
            g2d.fillRoundRect(blockX, blockY, 18, 18, 6, 6);
            g2d.setColor(color.brighter());
            g2d.drawRoundRect(blockX, blockY, 18, 18, 6, 6);
        }
    }

    private void drawGameOver(Graphics2D g2d) {
        int boardWidth = COLS * CELL_SIZE;
        int boardHeight = ROWS * CELL_SIZE;

        g2d.setColor(new Color(0, 0, 0, 170));
        g2d.fillRect(BOARD_X, BOARD_Y, boardWidth, boardHeight);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 32));
        String title = "Game Over";
        FontMetrics titleMetrics = g2d.getFontMetrics();
        int titleX = BOARD_X + (boardWidth - titleMetrics.stringWidth(title)) / 2;
        int titleY = BOARD_Y + boardHeight / 2 - 10;
        g2d.drawString(title, titleX, titleY);

        g2d.setFont(new Font("SansSerif", Font.PLAIN, 16));
        String subtitle = "Press R or Enter to restart";
        FontMetrics subtitleMetrics = g2d.getFontMetrics();
        int subtitleX = BOARD_X + (boardWidth - subtitleMetrics.stringWidth(subtitle)) / 2;
        g2d.drawString(subtitle, subtitleX, titleY + 35);
    }

    private void drawCell(Graphics2D g2d, int x, int y, Color color, boolean ghost) {
        int px = BOARD_X + x * CELL_SIZE;
        int py = BOARD_Y + y * CELL_SIZE;

        Color fillColor = ghost
            ? new Color(color.getRed(), color.getGreen(), color.getBlue(), 70)
            : color;

        g2d.setColor(fillColor);
        g2d.fillRoundRect(px + 1, py + 1, CELL_SIZE - 2, CELL_SIZE - 2, 8, 8);

        if (ghost) {
            g2d.setColor(fillColor.darker());
            g2d.drawRoundRect(px + 3, py + 3, CELL_SIZE - 6, CELL_SIZE - 6, 8, 8);
            return;
        }

        g2d.setColor(color.brighter());
        g2d.drawLine(px + 4, py + 4, px + CELL_SIZE - 6, py + 4);
        g2d.drawLine(px + 4, py + 4, px + 4, py + CELL_SIZE - 6);

        g2d.setColor(color.darker().darker());
        g2d.drawLine(px + 4, py + CELL_SIZE - 5, px + CELL_SIZE - 5, py + CELL_SIZE - 5);
        g2d.drawLine(px + CELL_SIZE - 5, py + 4, px + CELL_SIZE - 5, py + CELL_SIZE - 5);
    }

    private void drawCell(Graphics2D g2d, int x, int y, Color color) {
        drawCell(g2d, x, y, color, false);
    }
}
