import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import javax.swing.JFrame;

public class game extends Canvas implements Runnable, KeyListener, MouseListener {

    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;
    public boolean running = false;
    public Thread thread;

    public enum State {
        MENU, OPTIONS, CREDITS, COUNTDOWN, PLAYING, PAUSED, GAMEOVER
    }

    public State state = State.MENU;
    public boolean gameInProgress = false;
    
    public int score = 0;
    public long gameStartTime = 0;
    public int health = 3;
    public double renderHealth = 3.0;
    public int invulnerabilityTimer = 0;
    public int healthDropTimer = 0;
    public float hue = 0.5f;
    private int animationFrame = 0;

    public ArrayList<projectile> projectiles = new ArrayList<>();
    public int shootCooldown = 0;
    public boolean canShoot = true;
    public int bombAmmo = 5; 
    public int bombCooldown = 0; // Prevent accidental rapid firing of bombs

    public int countdownTimer = 0;
    public int countdownValue = 0;

    public vector playerPos = new vector(0, 0, 0);
    public camera cam = new camera(0, 0, -100);

    public ArrayList<obstacle> obstacles = new ArrayList<>();
    public ArrayList<particle> particles = new ArrayList<>();
    public Random rand = new Random();

    // 20 Neon Suited Colors
    public final Color[] neonColors = {
        new Color(255, 0, 255), new Color(0, 255, 255), new Color(0, 255, 0), new Color(255, 255, 0),
        new Color(255, 0, 127), new Color(127, 0, 255), new Color(0, 127, 255), new Color(127, 255, 0),
        new Color(255, 127, 0), new Color(255, 0, 0), new Color(0, 0, 255), new Color(0, 255, 127),
        new Color(255, 20, 147), new Color(0, 250, 154), new Color(30, 144, 255), new Color(255, 215, 0),
        new Color(138, 43, 226), new Color(255, 69, 0), new Color(50, 205, 50), new Color(75, 0, 130)
    };
    
    public Color playerColor = neonColors[0];
    public int selectedColorIndex = 0;
    
    public int menuSelection = 0;
    public ArrayList<String> menuItems = new ArrayList<>();
    public ArrayList<HighScore> highScores = new ArrayList<>();

    public int pauseSelection = 0;
    public final String[] pauseItems = { "RESUME", "EXIT" };

    public boolean leftPressed, rightPressed, wPressed, sPressed, aPressed, dPressed;
    public boolean spacePressed, enterPressed, shootPressed, bombPressed, mouseLeftClicked, mouseRightClicked;

    public game() {
        updateMenuItems();
    }
    
    public void updateMenuItems() {
        menuItems.clear();
        if (gameInProgress) {
            menuItems.add("CONTINUE PLAYING");
            menuItems.add("NEW GAME");
        } else {
            menuItems.add("START GAME");
        }
        menuItems.add("PLAYER COLOR");
        menuItems.add("HIGHEST SCORE");
        menuItems.add("CREDITS");
    }

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
        if (running) return;
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    public void run() {
        this.addKeyListener(this);
        this.addMouseListener(this);
        this.requestFocus();
        
        highScores.add(new HighScore(3000, 15000));
        highScores.add(new HighScore(1500, 8000));
        Collections.sort(highScores);

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
        if (state != State.PLAYING) animationFrame = (animationFrame + 1) % 60;
        boolean actionTriggered = enterPressed || spacePressed || mouseLeftClicked;

        // --- MENU BACKGROUND LOGIC ---
        if (state == State.MENU || state == State.PAUSED || state == State.GAMEOVER) {
             playerPos.z += 10; 
             hue += 0.0005f; if (hue > 1.0f) hue = 0.0f;
             cam.pos.z = playerPos.z - 200;
        }

        if (state == State.PAUSED) {
            if (wPressed || sPressed) {
                if (wPressed) pauseSelection = (pauseSelection - 1 + pauseItems.length) % pauseItems.length;
                if (sPressed) pauseSelection = (pauseSelection + 1) % pauseItems.length;
                try { Thread.sleep(150); } catch (InterruptedException e) {}
            }
            if (actionTriggered) {
                if (pauseSelection == 0) state = State.PLAYING;
                else { state = State.MENU; gameInProgress = true; updateMenuItems(); }
                resetInputs();
            }
            return;
        }

        if (state == State.MENU) {
            if (wPressed || sPressed) {
                if (wPressed) menuSelection = (menuSelection - 1 + menuItems.size()) % menuItems.size();
                if (sPressed) menuSelection = (menuSelection + 1) % menuItems.size();
                try { Thread.sleep(150); } catch (InterruptedException e) {}
            }
            int colorOptionIndex = gameInProgress ? 2 : 1; 
            if ((aPressed || dPressed) && menuSelection == colorOptionIndex) {
                 if (aPressed) selectedColorIndex = (selectedColorIndex - 1 + neonColors.length) % neonColors.length;
                 if (dPressed) selectedColorIndex = (selectedColorIndex + 1) % neonColors.length;
                 playerColor = neonColors[selectedColorIndex];
                 try { Thread.sleep(150); } catch (InterruptedException e) {}
            }
            if (actionTriggered) {
                String selected = menuItems.get(menuSelection);
                if (selected.equals("CONTINUE PLAYING")) state = State.PLAYING;
                else if (selected.equals("START GAME") || selected.equals("NEW GAME")) { state = State.COUNTDOWN; initCountdown(); }
                else if (selected.equals("HIGHEST SCORE")) state = State.OPTIONS;
                else if (selected.equals("CREDITS")) state = State.CREDITS;
                resetInputs();
            }
        } 
        else if (state == State.GAMEOVER) {
            if (actionTriggered) { state = State.MENU; gameInProgress = false; updateMenuItems(); resetInputs(); }
        }
        else if (state == State.OPTIONS || state == State.CREDITS) {
            if (actionTriggered) { state = State.MENU; resetInputs(); }
        } 
        else if (state == State.COUNTDOWN) {
            countdownTimer--;
            if (countdownTimer % 60 == 0) countdownValue = countdownTimer / 60;
            if (countdownTimer <= 0) {
                state = State.PLAYING;
                gameStartTime = System.currentTimeMillis();
            }
        }

        if (state == State.PLAYING) {
            if (spacePressed) { state = State.PAUSED; resetInputs(); return; }

            if (invulnerabilityTimer > 0) invulnerabilityTimer--;
            if (shootCooldown > 0) shootCooldown--;
            if (shootCooldown == 0) canShoot = true;
            if (bombCooldown > 0) bombCooldown--; // Cooldown for bombs
            
            if (healthDropTimer > 0) { healthDropTimer--; if (healthDropTimer == 0) spawnHealthDrop(); }
            
            renderHealth += (health - renderHealth) * 0.1;
            hue += 0.0005f; if (hue > 1.0f) hue = 0.0f;

            double currentSpeed = 10 + (score / 500.0);
            int spawnRate = Math.max(5, 20 - (score / 1000));

            double xySpeed = 4.0;
            if (leftPressed || aPressed) playerPos.x -= xySpeed;
            if (rightPressed || dPressed) playerPos.x += xySpeed;
            if (wPressed) playerPos.y -= xySpeed;
            if (sPressed) playerPos.y += xySpeed;

            // --- SHOOTING UPGRADES ---
            boolean triggerShoot = shootPressed || mouseLeftClicked; // 'K' key or Left Click
            if (triggerShoot && canShoot) {
                int shotCount = 1;
                if (score > 6000) shotCount = 4;
                else if (score > 3000) shotCount = 2;

                if (shotCount == 1) {
                    projectiles.add(new projectile(playerPos.x, playerPos.y, playerPos.z + 200, 0, 0, currentSpeed + 30, playerColor));
                } else if (shotCount == 2) {
                    projectiles.add(new projectile(playerPos.x - 15, playerPos.y, playerPos.z + 200, -2, 0, currentSpeed + 30, playerColor));
                    projectiles.add(new projectile(playerPos.x + 15, playerPos.y, playerPos.z + 200, 2, 0, currentSpeed + 30, playerColor));
                } else if (shotCount == 4) {
                    projectiles.add(new projectile(playerPos.x - 15, playerPos.y - 10, playerPos.z + 200, -2, -2, currentSpeed + 30, playerColor));
                    projectiles.add(new projectile(playerPos.x + 15, playerPos.y - 10, playerPos.z + 200, 2, -2, currentSpeed + 30, playerColor));
                    projectiles.add(new projectile(playerPos.x - 15, playerPos.y + 10, playerPos.z + 200, -2, 2, currentSpeed + 30, playerColor));
                    projectiles.add(new projectile(playerPos.x + 15, playerPos.y + 10, playerPos.z + 200, 2, 2, currentSpeed + 30, playerColor));
                }
                canShoot = false;
                shootCooldown = 10;
            }
            
            // --- BOMB LOGIC (L KEY OR RIGHT CLICK) ---
            boolean triggerBomb = bombPressed || mouseRightClicked; // 'L' key or Right Click
            if (score > 7000 && triggerBomb && bombAmmo > 0 && bombCooldown == 0) {
                bombAmmo--;
                bombCooldown = 30; // 0.5s cooldown to prevent accidental dumping
                spawnExplosion(new vector(playerPos.x, playerPos.y, playerPos.z + 500), Color.WHITE, 100); 
                int destroyed = 0;
                for (int i=0; i<obstacles.size(); i++) {
                    if (destroyed >= 10) break; 
                    if (obstacles.get(i).pos.z < playerPos.z + 3000) {
                         spawnExplosion(obstacles.get(i).pos, Color.ORANGE, 20);
                         obstacles.remove(i);
                         i--;
                         destroyed++;
                    }
                }
                mouseRightClicked = false; // Reset mouse click
            }

            double radius = 250;
            double dist = Math.sqrt(playerPos.x * playerPos.x + playerPos.y * playerPos.y);
            if (dist > radius) {
                playerPos.x = (playerPos.x / dist) * radius;
                playerPos.y = (playerPos.y / dist) * radius;
            }

            playerPos.z += currentSpeed;
            score++;

            if (rand.nextInt(spawnRate) == 0 && healthDropTimer == 0) spawnObstacle(2000, false);
            
            for (obstacle o : obstacles) {
                o.update(); 
                if (o.type == 4) { 
                    if (rand.nextInt(10) == 0) {
                        particles.add(new particle(o.pos.x, o.pos.y, o.pos.z, (rand.nextDouble()-0.5)*5, (rand.nextDouble()-0.5)*5, -10, 20, Color.ORANGE));
                    }
                }
            }

            for (int p = 0; p < projectiles.size(); p++) {
                projectile proj = projectiles.get(p);
                proj.update();
                if (proj.life <= 0 || proj.pos.z > playerPos.z + 3000) { projectiles.remove(p); p--; continue; }

                for (int i = 0; i < obstacles.size(); i++) {
                    obstacle o = obstacles.get(i);
                    if (o.type == 2) continue; 

                    if (Math.abs(o.pos.z - proj.pos.z) < (o.size + 10) &&
                        Math.abs(o.pos.x - proj.pos.x) < (o.size + 10) &&
                        Math.abs(o.pos.y - proj.pos.y) < (o.size + 10)) {
                        
                        spawnExplosion(o.pos, o.color, 30);
                        obstacles.remove(i); projectiles.remove(p);
                        i--; p--; break;
                    }
                }
            }

            double playerVisualZ = playerPos.z + 200;
            for (int i = 0; i < obstacles.size(); i++) {
                obstacle o = obstacles.get(i);
                if (o.pos.z < playerPos.z - 500) { obstacles.remove(i); i--; continue; }

                double xyDist = Math.sqrt(Math.pow(o.pos.x - playerPos.x, 2) + Math.pow(o.pos.y - playerPos.y, 2));
                double hitRad = o.size + 20; 

                // --- CORE COLLISION CHECK ---
                if (Math.abs(o.pos.z - playerVisualZ) < 50 && xyDist < hitRad) {
                    if (o.type == 2) { 
                        if (health < 3) health++;
                        spawnExplosion(o.pos, Color.GREEN, 20); obstacles.remove(i); i--;
                    } else if (o.type == 4) { 
                        health = 0;
                        state = State.GAMEOVER;
                        spawnExplosion(new vector(playerPos.x, playerPos.y, playerPos.z + 200), Color.MAGENTA, 100);
                        saveScore();
                    } else { 
                        if (invulnerabilityTimer == 0) {
                            health--;
                            invulnerabilityTimer = 60;
                            healthDropTimer = 120;
                            spawnExplosion(o.pos, o.color, 20); 
                            if (health <= 0) {
                                state = State.GAMEOVER;
                                spawnExplosion(new vector(playerPos.x, playerPos.y, playerPos.z + 200), Color.MAGENTA, 50);
                                saveScore();
                            }
                        }
                    }
                }
            }

            for (int i = 0; i < particles.size(); i++) {
                particle p = particles.get(i);
                p.update();
                if (p.life <= 0) { particles.remove(i); i--; }
            }
        }

        double lerpFactor = 0.1;
        cam.pos.x += (playerPos.x * 0.5 - cam.pos.x) * lerpFactor;
        cam.pos.y += (playerPos.y * 0.5 - cam.pos.y) * lerpFactor;
        cam.pos.z = playerPos.z - 200;
        double targetRoll = playerPos.x * 0.002;
        cam.roll += (targetRoll - cam.roll) * 0.05;
        
        mouseLeftClicked = false;
        mouseRightClicked = false;
    }
    
    public void saveScore() {
        long gameDuration = System.currentTimeMillis() - gameStartTime;
        highScores.add(new HighScore(score, gameDuration));
        Collections.sort(highScores);
        if (highScores.size() > 4) highScores.remove(highScores.size() - 1);
    }

    public void resetInputs() {
        enterPressed = false; spacePressed = false; mouseLeftClicked = false; mouseRightClicked = false;
    }

    public void initCountdown() {
        playerPos = new vector(0, 0, 0);
        obstacles.clear(); projectiles.clear(); particles.clear();
        score = 0; health = 3; renderHealth = 3.0; bombAmmo = 5;
        invulnerabilityTimer = 0; healthDropTimer = 0; hue = 0.5f;
        cam.pos = new vector(0, 0, -200); cam.roll = 0;
        countdownTimer = 3 * 60;
        gameInProgress = true;
    }

    public void spawnObstacle(double zDist, boolean isHealthDrop) {
        double angle = rand.nextDouble() * Math.PI * 2;
        
        // FIX: Increased spawn radius from 200 to 280 to ensure obstacles cover the 
        // entire playable area (max radius 250) and can hit the player in the corners.
        double r = rand.nextDouble() * 280; 
        
        double x = Math.cos(angle) * r;
        double y = Math.sin(angle) * r;
        double z = playerPos.z + zDist;

        int type = 0;
        Color c;
        double size = 40;

        if (isHealthDrop) {
            type = 2; c = Color.GREEN;
        } else {
            int roll = rand.nextInt(100);
            boolean canSpawnDragon = score > 6000;
            boolean canSpawnAnt = score > 3000;
            
            if (canSpawnDragon && roll < 15) { 
                type = 4; c = Color.WHITE; size = 60; 
            } else if (canSpawnAnt && roll < 40) { 
                type = 3; c = Color.RED; size = 20; 
            } else {
                type = rand.nextInt(2); 
                c = neonColors[rand.nextInt(neonColors.length)];
            }
        }
        obstacles.add(new obstacle(x, y, z, size, type, c));
    }
    
    public void spawnHealthDrop() { spawnObstacle(3000, true); }

    public void spawnExplosion(vector pos, Color c, int count) {
        for (int i = 0; i < count; i++) {
            double vx = (rand.nextDouble() - 0.5) * 20;
            double vy = (rand.nextDouble() - 0.5) * 20;
            double vz = (rand.nextDouble() - 0.5) * 20 + 10;
            int life = 30 + rand.nextInt(30);
            particles.add(new particle(pos.x, pos.y, pos.z, vx, vy, vz, life, c));
        }
    }

    public void render() {
        BufferStrategy bs = this.getBufferStrategy();
        if (bs == null) { this.createBufferStrategy(3); return; }
        Graphics g = bs.getDrawGraphics();
        g.setColor(Color.BLACK); g.fillRect(0, 0, WIDTH, HEIGHT);

        render3DScene(g);

        if (state == State.MENU) {
            renderMenuOverlay(g); 
        } else if (state == State.OPTIONS) {
            renderOverlay(g); renderHighScores(g);
        } else if (state == State.CREDITS) {
            renderOverlay(g); renderCredits(g);
        } else if (state == State.PAUSED) {
            renderPauseMenu(g);
        } else if (state == State.GAMEOVER) {
             renderOverlay(g); renderGameOver(g);
        }

        if (state == State.PLAYING || state == State.PAUSED) renderHUD(g);
        if (state == State.COUNTDOWN) renderCountdown(g);
        
        renderVHSOverlay(g);
        g.dispose(); bs.show();
    }
    
    public void render3DScene(Graphics g) {
        Color tunnelColor = Color.getHSBColor(hue, 1.0f, 1.0f);
        g.setColor(tunnelColor);
        int segments = 12; double radius = 300;
        int startZ = ((int) playerPos.z / 100) * 100;
        for (int z = startZ; z < startZ + 2000; z += 100) {
            for (int i = 0; i < segments; i++) {
                double a1 = (Math.PI * 2 * i) / segments; double a2 = (Math.PI * 2 * (i + 1)) / segments;
                renderer.drawLine(g, new vector(Math.cos(a1) * radius, Math.sin(a1) * radius, z),
                                     new vector(Math.cos(a2) * radius, Math.sin(a2) * radius, z), cam, tunnelColor);
            }
        }
        for (obstacle o : obstacles) {
            if (o.type == 0) renderer.drawCube(g, o.pos, o.size, cam, o.color);
            else if (o.type == 1) renderer.drawCone(g, o.pos, o.size / 2, o.size, cam, o.color);
            else if (o.type == 2) renderer.drawCapsule(g, o.pos, 20, 60, cam, o.color);
            else if (o.type == 3) renderer.drawAnt(g, o.pos, o.size, o.animationTick, cam, o.color);
            else if (o.type == 4) renderer.drawDragon(g, o.pos, o.size, o.animationTick, cam, o.color);
        }
        for (projectile p : projectiles) renderer.drawCube(g, p.pos, 5, cam, p.color);
        for (particle p : particles) renderer.drawLine(g, p.pos, new vector(p.pos.x - p.vel.x, p.pos.y - p.vel.y, p.pos.z - p.vel.z), cam, p.color);
        
        if (state != State.GAMEOVER) {
            if (state == State.PLAYING && (invulnerabilityTimer > 0 && (invulnerabilityTimer / 5) % 2 != 0)) return; 
            renderer.drawCone(g, new vector(playerPos.x, playerPos.y, playerPos.z + 200), 20, 60, cam, playerColor);
        }
    }
    
    public void renderOverlay(Graphics g) {
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, WIDTH, HEIGHT);
    }
    
    public void renderMenuOverlay(Graphics g) {
        g.setColor(new Color(0, 0, 0, 100)); 
        g.fillRect(0, 0, WIDTH, HEIGHT);
        renderTitleAnimation(g);
        renderMenu(g);
    }
    
    public void renderHUD(Graphics g) {
        g.setFont(new Font("Courier New", Font.BOLD, 20));
        g.setColor(Color.WHITE);
        g.drawString("SCORE: " + score, 20, 30);
        
        if (score > 7000) {
            g.setColor(Color.ORANGE);
            g.drawString("BOMBS: " + bombAmmo, 20, 55);
            g.setFont(new Font("Courier New", Font.PLAIN, 12));
            g.drawString("(RIGHT CLICK / L)", 20, 70); 
        }

        renderer.drawCrosshair(g, WIDTH / 2, HEIGHT / 2, Color.GREEN);
        
        int barW = 200; int barH = 20; int barX = WIDTH - barW - 20; int barY = 20;
        g.setColor(Color.DARK_GRAY); g.fillRect(barX, barY, barW, barH);
        int fillW = (int) ((renderHealth / 3.0) * barW);
        g.setColor(new Color(255, 0, 100)); g.fillRect(barX, barY, fillW, barH);
        g.setColor(Color.WHITE); g.drawRect(barX, barY, barW, barH);
        g.drawString("HP", barX - 30, barY + 15);
    }
    
    public void renderPauseMenu(Graphics g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setFont(new Font("Courier New", Font.BOLD, 40)); g.setColor(Color.WHITE);
        String p = "PAUSED"; g.drawString(p, WIDTH/2 - g.getFontMetrics().stringWidth(p)/2, HEIGHT/2 - 100);
        g.setFont(new Font("Courier New", Font.BOLD, 28));
        for (int i = 0; i < pauseItems.length; i++) {
            g.setColor(i == pauseSelection ? Color.CYAN : Color.GRAY);
            String item = pauseItems[i]; if (i == pauseSelection) item = "> " + item + " <";
            g.drawString(item, WIDTH/2 - g.getFontMetrics().stringWidth(item)/2, HEIGHT/2 + i * 50);
        }
    }
    
    public void renderVHSOverlay(Graphics g) {
        g.setColor(new Color(0, 0, 0, 50));
        for (int i = 0; i < HEIGHT; i += 4) g.drawLine(0, i, WIDTH, i);
    }
    
    public void renderCountdown(Graphics g) {
        g.setColor(new Color(0, 0, 0, 150)); g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setFont(new Font("Courier New", Font.BOLD, 150)); g.setColor(Color.GREEN);
        String text = (countdownTimer > -60 && countdownValue == 0) ? "GO!" : String.valueOf(countdownValue);
        if (countdownValue == 0 && countdownTimer <= 0) text = "GO!";
        if (countdownValue > 0) g.drawString(text, WIDTH/2 - g.getFontMetrics().stringWidth(text)/2, HEIGHT/2 + 50);
    }

    public void renderMenu(Graphics g) {
        int startY = HEIGHT / 2 - 80;
        g.setFont(new Font("Courier New", Font.BOLD, 28));
        for (int i = 0; i < menuItems.size(); i++) {
            Color color = (i == menuSelection) ? playerColor : Color.WHITE;
            g.setColor(color);
            String itemText = menuItems.get(i);
            if (itemText.equals("PLAYER COLOR")) itemText += " < Color #" + (selectedColorIndex + 1) + " > ";
            g.drawString(itemText, WIDTH / 2 - g.getFontMetrics().stringWidth(itemText) / 2, startY + i * 40);
        }
        g.setFont(new Font("Courier New", Font.PLAIN, 18)); g.setColor(Color.GRAY);
        String instruction = "W/S to navigate, ENTER/SPACE/CLICK to select.";
        g.drawString(instruction, WIDTH/2 - g.getFontMetrics().stringWidth(instruction)/2, HEIGHT - 50);
    }
    
    public void renderTitleAnimation(Graphics g) {
        String title = "RETRO RUNNER";
        g.setFont(new Font("Courier New", Font.BOLD, 48));
        int titleX = WIDTH / 2 - g.getFontMetrics().stringWidth(title) / 2;
        int titleY = 100;
        g.setFont(new Font("Courier New", Font.PLAIN, 12)); g.setColor(Color.WHITE);
        String subtext = "A PixelVerse Production";
        g.drawString(subtext, WIDTH / 2 - g.getFontMetrics().stringWidth(subtext) / 2, 130);
        g.setFont(new Font("Courier New", Font.BOLD, 48));
        for (int i = 0; i < title.length(); i++) {
            char c = title.charAt(i);
            int offset = (animationFrame % 10 == 0 && rand.nextBoolean()) ? 2 : 0;
            g.setColor(new Color(255, 0, 0, 100)); g.drawString(String.valueOf(c), titleX + g.getFontMetrics().stringWidth(title.substring(0, i)) - 2 - offset, titleY);
            g.setColor(new Color(0, 0, 255, 100)); g.drawString(String.valueOf(c), titleX + g.getFontMetrics().stringWidth(title.substring(0, i)) + 2 + offset, titleY);
            g.setColor(Color.MAGENTA); g.drawString(String.valueOf(c), titleX + g.getFontMetrics().stringWidth(title.substring(0, i)), titleY);
        }
    }
    
    public void renderHighScores(Graphics g) {
        g.setFont(new Font("Courier New", Font.BOLD, 36)); g.setColor(Color.YELLOW);
        String title = "HIGHEST SCORES";
        g.drawString(title, WIDTH / 2 - g.getFontMetrics().stringWidth(title) / 2, HEIGHT / 2 - 100);
        g.setFont(new Font("Courier New", Font.PLAIN, 24)); g.setColor(Color.WHITE);
        int startY = HEIGHT / 2 - 30;
        int leftAlignX = 150;
        for (int i = 0; i < highScores.size(); i++) {
            HighScore hs = highScores.get(i);
            String line = String.format("#%d. %06d pts - %.2fs", i + 1, hs.score, hs.time / 1000.0);
            g.drawString(line, leftAlignX, startY + i * 35);
        }
        g.setFont(new Font("Courier New", Font.PLAIN, 18)); g.setColor(Color.GRAY);
        String instruction = "PRESS ENTER/SPACE/CLICK TO RETURN";
        g.drawString(instruction, WIDTH/2 - g.getFontMetrics().stringWidth(instruction)/2, HEIGHT - 50);
    }
    
    public void renderCredits(Graphics g) {
        g.setFont(new Font("Courier New", Font.BOLD, 36)); g.setColor(Color.YELLOW);
        String title = "CREDITS"; g.drawString(title, WIDTH/2 - g.getFontMetrics().stringWidth(title)/2, HEIGHT/2 - 100);
        g.setFont(new Font("Courier New", Font.PLAIN, 24)); g.setColor(Color.WHITE);
        String[] lines = {"STUDIO: PixelVerse", "COLLABORATOR: Projukti Lipi"};
        for (int i=0; i<lines.length; i++) g.drawString(lines[i], WIDTH/2 - g.getFontMetrics().stringWidth(lines[i])/2, HEIGHT/2 + i*30);
    }

    public void renderGameOver(Graphics g) {
        g.setFont(new Font("Courier New", Font.BOLD, 40)); g.setColor(Color.RED);
        String go = "GAME OVER"; g.drawString(go, WIDTH/2 - g.getFontMetrics().stringWidth(go)/2, HEIGHT/2 - 40);
        g.setFont(new Font("Courier New", Font.PLAIN, 20)); g.setColor(Color.WHITE);
        String sc = "FINAL SCORE: " + score; g.drawString(sc, WIDTH/2 - g.getFontMetrics().stringWidth(sc)/2, HEIGHT/2 + 10);
        String res = "PRESS ENTER/SPACE/CLICK TO MENU"; g.drawString(res, WIDTH/2 - g.getFontMetrics().stringWidth(res)/2, HEIGHT/2 + 50);
    }

    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (k==KeyEvent.VK_LEFT) leftPressed=true; if (k==KeyEvent.VK_RIGHT) rightPressed=true;
        if (k==KeyEvent.VK_W || k==KeyEvent.VK_UP) wPressed=true;
        if (k==KeyEvent.VK_S || k==KeyEvent.VK_DOWN) sPressed=true;
        if (k==KeyEvent.VK_A) aPressed=true; if (k==KeyEvent.VK_D) dPressed=true;
        if (k==KeyEvent.VK_SPACE) spacePressed=true;
        if (k==KeyEvent.VK_ENTER) enterPressed=true;
        if (k==KeyEvent.VK_K) shootPressed=true; // Key K for shooting
        if (k==KeyEvent.VK_L) bombPressed=true;  // Key L for bombing
    }
    public void keyReleased(KeyEvent e) {
        int k = e.getKeyCode();
        if (k==KeyEvent.VK_LEFT) leftPressed=false; if (k==KeyEvent.VK_RIGHT) rightPressed=false;
        if (k==KeyEvent.VK_W || k==KeyEvent.VK_UP) wPressed=false;
        if (k==KeyEvent.VK_S || k==KeyEvent.VK_DOWN) sPressed=false;
        if (k==KeyEvent.VK_A) aPressed=false; if (k==KeyEvent.VK_D) dPressed=false;
        if (k==KeyEvent.VK_SPACE) spacePressed=false;
        if (k==KeyEvent.VK_ENTER) enterPressed=false;
        if (k==KeyEvent.VK_K) shootPressed=false; // Key K for shooting
        if (k==KeyEvent.VK_L) bombPressed=false;  // Key L for bombing
    }
    public void keyTyped(KeyEvent e) {}
    public void mouseClicked(MouseEvent e) {}
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) mouseLeftClicked = true;
        if (e.getButton() == MouseEvent.BUTTON3) mouseRightClicked = true;
    }
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) mouseLeftClicked = false;
        if (e.getButton() == MouseEvent.BUTTON3) mouseRightClicked = false;
    }
    public void mouseEntered(MouseEvent e) {} public void mouseExited(MouseEvent e) {}
}