import javax.swing.JFrame;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Font;
import java.awt.image.BufferStrategy;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Random;

public class game extends Canvas implements Runnable, KeyListener {

    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;
    public boolean running = false;
    public Thread thread;

    // Game State
    public enum State {
        MENU, PLAYING, GAMEOVER
    }

    public State state = State.MENU;
    public int score = 0;
    public int health = 3;
    public double renderHealth = 3.0;
    public int invulnerabilityTimer = 0;
    public float hue = 0.5f; // Start with Cyan-ish

    public vector playerPos = new vector(0, 0, 0);
    public vector playerVel = new vector(0, 0, 0);
    public camera cam = new camera(0, 0, -100);

    public ArrayList<obstacle> obstacles = new ArrayList<>();
    public ArrayList<particle> particles = new ArrayList<>();
    public Random rand = new Random();

    public boolean leftPressed = false;
    public boolean rightPressed = false;
    public boolean wPressed = false;
    public boolean sPressed = false;
    public boolean aPressed = false;
    public boolean dPressed = false;
    public boolean spacePressed = false;

    public static void main(String[] args) {
        game g = new game();
        JFrame frame = new JFrame("RETRO RUNNER");
        frame.add(g);
        frame.pack();
        frame.setSize(WIDTH, HEIGHT);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        g.start();
    }

    public synchronized void start() {
        if (running)
            return;
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    public void run() {
        this.addKeyListener(this);
        this.requestFocus();

        long lastTime = System.nanoTime();
        double amountOfTicks = 60.0;
        double ns = 1000000000 / amountOfTicks;
        double delta = 0;

        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            lastTime = now;
            while (delta >= 1) {
                update();
                delta--;
            }
            render();
        }
    }

    public void update() {
        // Handle Game Restart
        if (spacePressed) {
            if (state == State.MENU || state == State.GAMEOVER) {
                state = State.PLAYING;
                playerPos = new vector(0, 0, 0);
                obstacles.clear();
                particles.clear();
                score = 0;
                health = 3;
                renderHealth = 3.0;
                invulnerabilityTimer = 0;
                hue = 0.5f;
                // Reset camera
                cam.pos = new vector(0, 0, -200);
                cam.roll = 0;
            }
        }

        if (state == State.PLAYING) {
            if (invulnerabilityTimer > 0)
                invulnerabilityTimer--;

            // Smooth Health Bar
            renderHealth += (health - renderHealth) * 0.1;

            // Color Cycling
            hue += 0.0005f; // Slow cycle
            if (hue > 1.0f)
                hue = 0.0f;

            // Difficulty Scaling
            // Base speed 10, increases by 1 every 500 score
            double currentSpeed = 10 + (score / 500.0);
            // Base spawn rate 1 in 20, decreases (gets harder) every 1000 score, min 1 in 5
            int spawnRate = Math.max(5, 20 - (score / 1000));

            // Plane-like movement
            double xySpeed = 4.0;
            if (leftPressed || aPressed)
                playerPos.x -= xySpeed;
            if (rightPressed || dPressed)
                playerPos.x += xySpeed;
            if (wPressed)
                playerPos.y -= xySpeed;
            if (sPressed)
                playerPos.y += xySpeed;

            // Tunnel Boundary Check
            double radius = 250;
            double dist = Math.sqrt(playerPos.x * playerPos.x + playerPos.y * playerPos.y);
            if (dist > radius) {
                playerPos.x = (playerPos.x / dist) * radius;
                playerPos.y = (playerPos.y / dist) * radius;
            }

            // Move forward
            playerPos.z += currentSpeed;
            score++;

            // Obstacle Spawning
            if (rand.nextInt(spawnRate) == 0) { // Spawn chance
                spawnObstacle();
            }

            // Update Obstacles & Collision
            for (int i = 0; i < obstacles.size(); i++) {
                obstacle o = obstacles.get(i);
                if (o.pos.z < playerPos.z - 500) {
                    obstacles.remove(i);
                    i--;
                    continue;
                }

                // Simple AABB Collision
                // Player is rendered at playerPos.z + 200
                double playerVisualZ = playerPos.z + 200;

                // Tightened Hitbox
                if (Math.abs(o.pos.z - playerVisualZ) < 50 &&
                        Math.abs(o.pos.x - playerPos.x) < 35 &&
                        Math.abs(o.pos.y - playerPos.y) < 35) {

                    if (o.type == 2) { // Health Pack
                        if (health < 3)
                            health++;
                        spawnExplosion(o.pos, Color.GREEN, 20); // Happy explosion
                        obstacles.remove(i);
                        i--;
                    } else { // Obstacle
                        if (invulnerabilityTimer == 0) {
                            health--;
                            invulnerabilityTimer = 60; // 1 second invulnerability
                            spawnExplosion(o.pos, o.color, 20); // Explosion!

                            if (health <= 0) {
                                state = State.GAMEOVER;
                                spawnExplosion(new vector(playerPos.x, playerPos.y, playerPos.z + 200), Color.MAGENTA,
                                        50); // Player explosion
                            }
                        }
                    }
                }
            }

            // Update Particles
            for (int i = 0; i < particles.size(); i++) {
                particle p = particles.get(i);
                p.update();
                if (p.life <= 0) {
                    particles.remove(i);
                    i--;
                }
            }
        }

        // Cinematic Camera (Always active for smooth feel)
        double lerpFactor = 0.1;
        cam.pos.x += (playerPos.x * 0.5 - cam.pos.x) * lerpFactor;
        cam.pos.y += (playerPos.y * 0.5 - cam.pos.y) * lerpFactor;
        cam.pos.z = playerPos.z - 200;

        double targetRoll = playerPos.x * 0.002;
        cam.roll += (targetRoll - cam.roll) * 0.05;
    }

    public void spawnObstacle() {
        double angle = rand.nextDouble() * Math.PI * 2;
        double r = rand.nextDouble() * 200; // Within tunnel
        double x = Math.cos(angle) * r;
        double y = Math.sin(angle) * r;
        double z = playerPos.z + 2000; // Spawn far ahead

        int type = 0;
        Color c = Color.RED;

        if (rand.nextInt(20) == 0) { // 5% chance for Health Pack
            type = 2;
            c = Color.GREEN;
        } else {
            type = rand.nextInt(2);
            c = (type == 0) ? Color.RED : Color.ORANGE;
        }

        obstacles.add(new obstacle(x, y, z, 40, type, c));
    }

    public void spawnExplosion(vector pos, Color c, int count) {
        for (int i = 0; i < count; i++) {
            double vx = (rand.nextDouble() - 0.5) * 10;
            double vy = (rand.nextDouble() - 0.5) * 10;
            double vz = (rand.nextDouble() - 0.5) * 10 + 10; // Move forward with player
            int life = 30 + rand.nextInt(30);
            particles.add(new particle(pos.x, pos.y, pos.z, vx, vy, vz, life, c));
        }
    }

    public void render() {
        BufferStrategy bs = this.getBufferStrategy();
        if (bs == null) {
            this.createBufferStrategy(3);
            return;
        }

        Graphics g = bs.getDrawGraphics();

        // Clear screen
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw Tunnel
        Color tunnelColor = Color.getHSBColor(hue, 1.0f, 1.0f);
        g.setColor(tunnelColor);
        int segments = 12;
        double radius = 300;
        int startZ = ((int) playerPos.z / 100) * 100;

        for (int z = startZ; z < startZ + 2000; z += 100) {
            for (int i = 0; i < segments; i++) {
                double angle1 = (Math.PI * 2 * i) / segments;
                double angle2 = (Math.PI * 2 * (i + 1)) / segments;
                vector p1 = new vector(Math.cos(angle1) * radius, Math.sin(angle1) * radius, z);
                vector p2 = new vector(Math.cos(angle2) * radius, Math.sin(angle2) * radius, z);
                renderer.drawLine(g, p1, p2, cam, tunnelColor);
            }
        }
        for (int i = 0; i < segments; i++) {
            double angle = (Math.PI * 2 * i) / segments;
            vector p1 = new vector(Math.cos(angle) * radius, Math.sin(angle) * radius, startZ);
            vector p2 = new vector(Math.cos(angle) * radius, Math.sin(angle) * radius, startZ + 2000);
            renderer.drawLine(g, p1, p2, cam, tunnelColor);
        }

        // Draw Obstacles
        for (obstacle o : obstacles) {
            if (o.type == 0 || o.type == 2) // Cube or Health Pack
                renderer.drawCube(g, o.pos, o.size, cam, o.color);
            else
                renderer.drawCone(g, o.pos, o.size / 2, o.size, cam, o.color);
        }

        // Draw Particles
        for (particle p : particles) {
            // Simple line rendering for particles
            vector p1 = p.pos;
            vector p2 = new vector(p.pos.x - p.vel.x, p.pos.y - p.vel.y, p.pos.z - p.vel.z);
            renderer.drawLine(g, p1, p2, cam, p.color);
        }

        // Draw Player
        if (state != State.GAMEOVER) {
            // Flash if invulnerable
            if (invulnerabilityTimer == 0 || (invulnerabilityTimer / 5) % 2 == 0) {
                renderer.drawCone(g, new vector(playerPos.x, playerPos.y, playerPos.z + 200), 20, 60, cam,
                        Color.MAGENTA);
            }
        }

        // UI & VHS Effects
        g.setColor(new Color(0, 0, 0, 50));
        for (int i = 0; i < HEIGHT; i += 4)
            g.drawLine(0, i, WIDTH, i);

        // UI Text
        g.setFont(new Font("Courier New", Font.BOLD, 20));
        g.setColor(Color.WHITE);
        g.drawString("SCORE: " + score, 20, 30);

        // Smooth Health Bar
        if (state == State.PLAYING) {
            int barWidth = 200;
            int barHeight = 20;
            int barX = WIDTH - barWidth - 20;
            int barY = 20;

            // Background
            g.setColor(Color.DARK_GRAY);
            g.fillRect(barX, barY, barWidth, barHeight);

            // Foreground (Smooth)
            int fillWidth = (int) ((renderHealth / 3.0) * barWidth);
            if (fillWidth < 0)
                fillWidth = 0;

            g.setColor(new Color(255, 0, 100)); // Retro Pink/Red
            g.fillRect(barX, barY, fillWidth, barHeight);

            // Border
            g.setColor(Color.WHITE);
            g.drawRect(barX, barY, barWidth, barHeight);
            g.drawString("HP", barX - 30, barY + 15);
        }

        if (state == State.MENU) {
            g.setFont(new Font("Courier New", Font.BOLD, 40));
            g.drawString("RETRO RUNNER", WIDTH / 2 - 140, HEIGHT / 2 - 40);
            g.setFont(new Font("Courier New", Font.PLAIN, 20));
            g.drawString("PRESS SPACE TO START", WIDTH / 2 - 110, HEIGHT / 2 + 20);
            g.drawString("WASD / ARROWS TO FLY", WIDTH / 2 - 110, HEIGHT / 2 + 50);
        } else if (state == State.GAMEOVER) {
            g.setFont(new Font("Courier New", Font.BOLD, 40));
            g.setColor(Color.RED);
            g.drawString("GAME OVER", WIDTH / 2 - 110, HEIGHT / 2 - 40);
            g.setFont(new Font("Courier New", Font.PLAIN, 20));
            g.setColor(Color.WHITE);
            g.drawString("FINAL SCORE: " + score, WIDTH / 2 - 80, HEIGHT / 2 + 10);
            g.drawString("PRESS SPACE TO RESTART", WIDTH / 2 - 120, HEIGHT / 2 + 50);
        }

        g.dispose();
        bs.show();
    }

    // Input Handling
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT)
            leftPressed = true;
        if (key == KeyEvent.VK_RIGHT)
            rightPressed = true;
        if (key == KeyEvent.VK_W)
            wPressed = true;
        if (key == KeyEvent.VK_S)
            sPressed = true;
        if (key == KeyEvent.VK_A)
            aPressed = true;
        if (key == KeyEvent.VK_D)
            dPressed = true;
        if (key == KeyEvent.VK_SPACE)
            spacePressed = true;
    }

    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT)
            leftPressed = false;
        if (key == KeyEvent.VK_RIGHT)
            rightPressed = false;
        if (key == KeyEvent.VK_W)
            wPressed = false;
        if (key == KeyEvent.VK_S)
            sPressed = false;
        if (key == KeyEvent.VK_A)
            aPressed = false;
        if (key == KeyEvent.VK_D)
            dPressed = false;
        if (key == KeyEvent.VK_SPACE)
            spacePressed = false;
    }

    public void keyTyped(KeyEvent e) {
    }
}
