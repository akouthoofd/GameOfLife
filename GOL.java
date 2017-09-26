import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Game Of Life
 */
public class GOL extends Canvas {
    private static final int GRID_SIZE = 250;
    private static final int FRAME_SIZE = 2500;
    private static final String GAME_TITLE = "Game Of Life";
    private static final long ONE_SECOND_NANO = 1000000000;
    private static final long GENERATION_SPEED = 30;
    private static final int DEAD_CELL = 0x3498db;
    private static final int LIVING_CELL = 0xffffff;
    
    // The world to be displayed
    private BufferedImage image;
    // Each pixel in the buffered image
    private int[] pixels;
    // A boolean mask of @pixels
    private boolean[] pixelMask;
    // Pause game variable
    private boolean paused;

    /**
     * Start of the program
     * @param args
     *      Unused but required parameters
     */
    public static void main(String[] args) {
       SwingUtilities.invokeLater(GOL::new);     
    }

    /**
     * Constructor.
     * Sets up container for content
     */
    private GOL() {
        // Setting up main frame
        JFrame frame = new JFrame(GAME_TITLE);
        // Exit on close
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        // Adding canvas to frame
        frame.add(this);
        // Set preferred size of screen
        frame.setPreferredSize(new Dimension(FRAME_SIZE, FRAME_SIZE));
        // Packing...
        frame.pack();
        // Display!
        frame.setVisible(true);
        // Kick off game off EDT
        new Thread(this::run).start();
        // Add action listener
        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                double projection = (double)FRAME_SIZE / (double)GRID_SIZE;
                int x = (int) (e.getX() / projection);
                int y = (int) (e.getY() / projection);
                pixelMask[x + (GRID_SIZE * y)] = !pixelMask[x + (GRID_SIZE * y)];
            }
        });
        // Pause game and move step
        addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                if (e.getKeyCode() == KeyEvent.VK_SPACE)
                    paused = !paused;
                else if (e.getKeyCode() == KeyEvent.VK_RIGHT)
                    update();
            }
        });
    }

    /**
     * Initial game setup and then run the game loop forever
     */
    private void run() {
        image = new BufferedImage(GRID_SIZE, GRID_SIZE, BufferedImage.TYPE_INT_RGB);
        pixels = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
        pixelMask = new boolean[pixels.length];
        paused = false;
        // Randomize initial world
        for (int i = 0; i < pixels.length; i++)
        {
            boolean live = ThreadLocalRandom.current().nextInt(100) > 80;
            pixels[i] = live ? LIVING_CELL: DEAD_CELL;
            pixelMask[i] = live;
        }

        double frameCut = ONE_SECOND_NANO / GENERATION_SPEED;

        long currentTime = System.nanoTime();
        long resetTimer = 0;
        long frameTimer = currentTime;
        long previousTime;
        long deltaTime;
        double unprocessedTime = 0.0;

        int generation = 1;

        while (true) {
            previousTime = currentTime;
            currentTime = System.nanoTime();
            deltaTime = currentTime - previousTime;

            unprocessedTime += deltaTime;

            // Update @GENERATION_SPEED times per second
            if (unprocessedTime > frameCut && !paused) {
                unprocessedTime = 0L;
                update();
                generation++;
            }

            // Display current generation every second
            if (currentTime - frameTimer > ONE_SECOND_NANO) {
                resetTimer += currentTime - frameTimer;
                frameTimer = currentTime;
                if (resetTimer > ONE_SECOND_NANO * 30) {
                    resetTimer = 0;
                    // Randomize initial world
                    for (int i = 0; i < pixels.length; i++)
                    {
                        boolean live = ThreadLocalRandom.current().nextInt(100) > 80;
                        pixels[i] = live ? LIVING_CELL: DEAD_CELL;
                        pixelMask[i] = live;
                    }
                    generation = 1;
                }
                System.out.printf("Generation: %d\n", generation);
            }

            render();
        }
    }

    /**
     * Updates game state
     */
    private void update() {
        boolean[] bufferedMask = Arrays.copyOf(pixelMask, pixelMask.length);
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int y = 0; y < GRID_SIZE; y++) {
                int result = 0;

                int left = x - 1;
                int right = x + 1;
                int top = y - 1;
                int bottom = y + 1;

                if (x != 0) // left
                    result += bufferedMask[left + (GRID_SIZE * y)] ? 1 : 0;
                if (x != GRID_SIZE - 1) // right
                    result += bufferedMask[right + (GRID_SIZE * y)] ? 1 : 0;
                if (y != 0) // top
                    result += bufferedMask[top + (GRID_SIZE * x)] ? 1 : 0;
                if (y != GRID_SIZE - 1) // bottom
                    result += bufferedMask[bottom + (GRID_SIZE * x)] ? 1 : 0;
                if (x != 0 && y != 0) // top left
                    result += bufferedMask[left + (GRID_SIZE * top)] ? 1 : 0;
                if (x != GRID_SIZE - 1 && y != 0) // top right
                    result += bufferedMask[right + (GRID_SIZE * top)] ? 1 : 0;
                if (x != 0 && y != GRID_SIZE - 1) // bottom left
                    result += bufferedMask[left + (GRID_SIZE * bottom)] ? 1 : 0;
                if (x != GRID_SIZE - 1 && y != GRID_SIZE - 1) // bottom right
                    result += bufferedMask[right + (GRID_SIZE * bottom)] ? 1 : 0;

                if (bufferedMask[x + (GRID_SIZE * y)]) // cell is alive
                    pixelMask[x + (GRID_SIZE * y)] = !(result < 2 || result > 3);
                else if (result == 3)
                    pixelMask[x + (GRID_SIZE * y)] = true;
            }
        }
    }

    /**
     * Displays the current game state to screen
     */
    private void render() {
        BufferStrategy bs = getBufferStrategy();
        if (bs == null) {
            // Three means if current render (using 1 & 2) is computed, look ahead and compute next render (stored in 3)
            createBufferStrategy(3);
            return;
        }

        Graphics g = bs.getDrawGraphics();
        
        for (int i = 0; i < pixels.length; i++)
            pixels[i] = pixelMask[i] ? LIVING_CELL: DEAD_CELL;

        g.drawImage(image, 0, 0, FRAME_SIZE, FRAME_SIZE, this);
        g.dispose();
        bs.show();
    }
}
