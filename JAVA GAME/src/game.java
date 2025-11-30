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
        MENU, LEVEL_SELECT, OPTIONS, CREDITS, COUNTDOWN, PLAYING, PAUSED, GAMEOVER
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
    public int bombCooldown = 0; 

    public int countdownTimer = 0;
    public int countdownValue = 0;

    public vector playerPos = new vector(0, 0, 0);
    public camera cam = new camera(0, 0, -100);
    public Sound bgMusic;

    public ArrayList<obstacle> obstacles = new ArrayList<>();
    public ArrayList<particle> particles = new ArrayList<>();
    
    public ArrayList<vector> menuStars = new ArrayList<>(); 
    public float titleHoverPhase = 0f; 
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

    // --- LEVEL SELECTION VARIABLES ---
    public int selectedLevelIndex = 2; // Default to Normal
    public final String[] levelNames = {"CHILL", "EASY", "NORMAL", "HARD", "NIGHTMARE"};
    public final Color[] levelColors = {Color.CYAN, Color.GREEN, Color.BLUE, Color.ORANGE, Color.RED};
    public float levelOrbitAngle = 0f; 
    public double difficultyMultiplier = 1.0; 

    public boolean leftPressed, rightPressed, wPressed, sPressed, aPressed, dPressed;
    public boolean spacePressed, enterPressed, escPressed, fPressed; 
    public boolean mouseLeftClicked, mouseRightClicked;

    public void initMenuStars() {
        menuStars.clear();
        for(int i=0; i<200; i++) {
            double x = (rand.nextDouble() - 0.5) * WIDTH * 2;
            double y = (rand.nextDouble() - 0.5) * HEIGHT * 2;
            double z = rand.nextDouble() * 1000;
            menuStars.add(new vector(x, y, z));
        }
    }

    public game() {
        updateMenuItems();
        initMenuStars();
            // --- ADD MUSIC LOADING HERE ---
        bgMusic = new Sound("bin/music.wav"); // Make sure file name matches exactly
        bgMusic.loop(); // Starts playing immediately and loops forever
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
        JFrame frame = new JFrame("WIRE FLEE"); 
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
        
        highScores = ScoreManager.loadScores();
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
        boolean actionTriggered = enterPressed || mouseLeftClicked;

        // --- MENU / BACKGROUND AUTOPILOT LOGIC ---
        if (state == State.MENU || state == State.PAUSED || state == State.GAMEOVER || state == State.LEVEL_SELECT) {
            playerPos.z += 10;
            hue += 0.0005f; if (hue > 1.0f) hue = 0.0f;
            double time = System.currentTimeMillis() / 1000.0;
            playerPos.x = Math.sin(time * 0.8) * 150; 
            cam.pos.z = playerPos.z - 200;
            
            for(vector s : menuStars) {
                s.z -= 10;
                if(s.z <= 0) {
                    s.z = 1000;
                    s.x = (rand.nextDouble() - 0.5) * WIDTH * 2;
                    s.y = (rand.nextDouble() - 0.5) * HEIGHT * 2;
                }
            }
            titleHoverPhase += 0.05f; 
        }

        if (state == State.LEVEL_SELECT) {
            levelOrbitAngle += 0.01f;
            
            if (aPressed || leftPressed) {
                selectedLevelIndex = (selectedLevelIndex - 1 + levelNames.length) % levelNames.length;
                aPressed = false; leftPressed = false; 
            }
            if (dPressed || rightPressed) {
                selectedLevelIndex = (selectedLevelIndex + 1) % levelNames.length;
                dPressed = false; rightPressed = false;
            }

            if (actionTriggered || spacePressed) {
                // DIFFICULTY SETTINGS
                switch(selectedLevelIndex) {
                    case 0: difficultyMultiplier = 0.8; break; // Chill
                    case 1: difficultyMultiplier = 0.9; break; // Easy
                    case 2: difficultyMultiplier = 1.0; break; // Normal
                    case 3: difficultyMultiplier = 1.3; break; // Hard
                    case 4: difficultyMultiplier = 1.6; break; // Nightmare
                }
                state = State.COUNTDOWN; 
                initCountdown();
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
            if (menuSelection == colorOptionIndex) {
                 if (aPressed || leftPressed) {
                     selectedColorIndex = (selectedColorIndex - 1 + neonColors.length) % neonColors.length;
                     playerColor = neonColors[selectedColorIndex];
                     try { Thread.sleep(150); } catch (InterruptedException e) {}
                 }
                 if (dPressed || rightPressed) {
                     selectedColorIndex = (selectedColorIndex + 1) % neonColors.length;
                     playerColor = neonColors[selectedColorIndex];
                     try { Thread.sleep(150); } catch (InterruptedException e) {}
                 }
            }

            if (actionTriggered || spacePressed) {
                String selected = menuItems.get(menuSelection);
                if (selected.equals("CONTINUE PLAYING")) state = State.PLAYING;
                else if (selected.equals("START GAME") || selected.equals("NEW GAME")) { 
                    state = State.LEVEL_SELECT; 
                    levelOrbitAngle = 0; 
                }
                else if (selected.equals("HIGHEST SCORE")) state = State.OPTIONS;
                else if (selected.equals("CREDITS")) state = State.CREDITS;
                resetInputs();
            }
        } 
        else if (state == State.GAMEOVER) {
            if (actionTriggered || spacePressed) { state = State.MENU; gameInProgress = false; updateMenuItems(); resetInputs(); }
            if (state == State.GAMEOVER) {
                // bgMusic.stop(); // Uncomment if you want silence on death
            }
        }
        else if (state == State.OPTIONS || state == State.CREDITS) {
            if (actionTriggered || spacePressed) { state = State.MENU; resetInputs(); }
        } 
        else if (state == State.PAUSED) {
            if (wPressed || sPressed) {
                if (wPressed) pauseSelection = (pauseSelection - 1 + pauseItems.length) % pauseItems.length;
                if (sPressed) pauseSelection = (pauseSelection + 1) % pauseItems.length;
                try { Thread.sleep(150); } catch (InterruptedException e) {}
            }
            if (actionTriggered || spacePressed) { 
                if (pauseSelection == 0) state = State.PLAYING;
                else { state = State.MENU; gameInProgress = true; updateMenuItems(); }
                resetInputs();
            }
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
            if (escPressed) { state = State.PAUSED; resetInputs(); return; }

            if (invulnerabilityTimer > 0) invulnerabilityTimer--;
            if (shootCooldown > 0) shootCooldown--;
            if (shootCooldown == 0) canShoot = true;
            if (bombCooldown > 0) bombCooldown--; 
            
            if (healthDropTimer > 0) { healthDropTimer--; if (healthDropTimer == 0) spawnHealthDrop(); }
            
            renderHealth += (health - renderHealth) * 0.1;
            hue += 0.0005f; if (hue > 1.0f) hue = 0.0f;

            double currentSpeed = (10 + (score / 500.0)) * difficultyMultiplier;
            int baseSpawn = 20 - (score / 1000);
            int spawnRate = Math.max(5, (int)(baseSpawn / difficultyMultiplier));

            double xySpeed = 4.0;
            if (leftPressed || aPressed) playerPos.x -= xySpeed;
            if (rightPressed || dPressed) playerPos.x += xySpeed;
            if (wPressed) playerPos.y -= xySpeed;
            if (sPressed) playerPos.y += xySpeed;

            boolean triggerShoot = spacePressed || mouseLeftClicked; 
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
            
            boolean triggerBomb = fPressed || mouseRightClicked; 
            if (score > 7000 && triggerBomb && bombAmmo > 0 && bombCooldown == 0) {
                bombAmmo--;
                bombCooldown = 30; 
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
                mouseRightClicked = false; 
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
                if (o.type == 4 || o.type == 5) { // Dragon or T-Rex animation
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

                if (Math.abs(o.pos.z - playerVisualZ) < 50 && xyDist < hitRad) {
                    if (o.type == 2) { 
                        if (health < 3) health++;
                        spawnExplosion(o.pos, Color.GREEN, 20); obstacles.remove(i); i--;
                    } else if (o.type == 4 || o.type == 5) { // BOSS COLLISION (Dragon or T-Rex)
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
        if (highScores.size() > 5) highScores.remove(highScores.size() - 1);
        ScoreManager.saveScores(highScores);
    }

    public void resetInputs() {
        enterPressed = false; spacePressed = false; escPressed = false; fPressed = false;
        mouseLeftClicked = false; mouseRightClicked = false;
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
            // SPAWN LOGIC BASED ON LEVEL
            int antScore = 3000;
            int dragonScore = 6000;
            int trexScore = 999999; // Practically impossible unless in Chill

            if (selectedLevelIndex == 4) { // NIGHTMARE
                antScore = 1000;
                dragonScore = 2500;
            } else if (selectedLevelIndex == 3) { // HARD
                antScore = 2000;
                // Dragons stay default or slightly earlier? Keeping default for Hard to differentiate from Nightmare
            } else if (selectedLevelIndex == 0) { // CHILL (Chaos Mode)
                antScore = 1000;
                dragonScore = 2000;
                trexScore = 3000;
            }

            int roll = rand.nextInt(100);
            boolean canSpawnTRex = score > trexScore;
            boolean canSpawnDragon = score > dragonScore;
            boolean canSpawnAnt = score > antScore;
            
            if (canSpawnTRex && roll < 5) {
                type = 5; c = Color.YELLOW; size = 70; // T-REX
            } else if (canSpawnDragon && roll < 15) { 
                type = 4; c = Color.WHITE; size = 60; // DRAGON
            } else if (canSpawnAnt && roll < 40) { 
                type = 3; c = Color.RED; size = 20; // ANT
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

    // =========================================================
    // RENDER PIPELINE
    // =========================================================

    public void render() {
        BufferStrategy bs = this.getBufferStrategy();
        if (bs == null) { this.createBufferStrategy(3); return; }
        Graphics g = bs.getDrawGraphics();

        // 1. Clear Screen
        g.setColor(Color.BLACK); 
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 2. Space Background
        renderStars(g);

        // 3. Render World
        if (state == State.LEVEL_SELECT) {
            renderLevelSelectScene(g); 
        } else {
            render3DScene(g);
        }
        
        // **LENS FLARE REMOVED** // 4. UI Layers
        if (state == State.MENU) {
            renderMenuOverlay(g); 
        } else if (state == State.LEVEL_SELECT) {
            renderLevelSelectUI(g); 
        } else if (state == State.OPTIONS) {
            renderOverlay(g); renderHighScores(g);
        } else if (state == State.CREDITS) {
            renderOverlay(g); renderCredits(g);
        } else if (state == State.PAUSED) {
            renderPauseMenu(g);
        } else if (state == State.GAMEOVER) {
             renderOverlay(g); renderGameOver(g);
        }

        if (state == State.PLAYING || state == State.PAUSED) {
            renderHUD(g);
            renderCamcorderOverlay(g);
        }
        
        if (state == State.COUNTDOWN) renderCountdown(g);
        
        renderVHSOverlay(g); 
        renderStaticNoise(g);
        
        g.dispose(); bs.show();
    }

    public void renderStars(Graphics g) {
        for (vector s : menuStars) {
            if (s.z <= 0) continue;
            double fov = 300;
            double px = (s.x * fov) / s.z + WIDTH / 2;
            double py = (s.y * fov) / s.z + HEIGHT / 2;
            if (px < 0 || px > WIDTH || py < 0 || py > HEIGHT) continue;

            int brightness = (int) (255 - (s.z / 1000.0 * 255));
            if (brightness < 0) brightness = 0; if (brightness > 255) brightness = 255;
            g.setColor(new Color(brightness, brightness, brightness));
            int size = (s.z < 500) ? 3 : 2; 
            g.fillRect((int)px, (int)py, size, size);
        }
    }
    
    public void renderLevelSelectScene(Graphics g) {
        camera levelCam = new camera(0, -200, -400); 
        levelCam.roll = 0;
        double rot = levelOrbitAngle;

        g.setColor(new Color(0, 50, 50)); 
        for (int i = -300; i <= 300; i += 50) {
            vector v1 = new vector(-300, 200, i);
            vector v2 = new vector(300, 200, i);
            renderer.drawLine(g, vector.rotateY(v1, rot), vector.rotateY(v2, rot), levelCam, Color.DARK_GRAY);
            vector v3 = new vector(i, 200, -300);
            vector v4 = new vector(i, 200, 300);
            renderer.drawLine(g, vector.rotateY(v3, rot), vector.rotateY(v4, rot), levelCam, Color.DARK_GRAY);
        }

        double radius = 150;
        for (int i = 0; i < 5; i++) {
            double angle = (Math.PI * 2 * i) / 5;
            double px = Math.cos(angle) * radius;
            double pz = Math.sin(angle) * radius;
            
            Color c = levelColors[i];
            if (i == selectedLevelIndex) {
                c = Color.WHITE;
                vector diamondPos = new vector(px, 100 + Math.sin(animationFrame * 0.1) * 10, pz);
                renderer.drawCone(g, vector.rotateY(diamondPos, rot), 10, 20, levelCam, c);
            } else {
                c = new Color(c.getRed()/2, c.getGreen()/2, c.getBlue()/2);
            }

            vector pillarPos = new vector(px, 200 - 50, pz); 
            renderer.drawCube(g, vector.rotateY(pillarPos, rot), 30, levelCam, c);
        }
    }

    public void renderLevelSelectUI(Graphics g) {
        g.setFont(new Font("Courier New", Font.BOLD, 40));
        String lvlName = levelNames[selectedLevelIndex];
        Color c = levelColors[selectedLevelIndex];
        g.setColor(c);
        drawCenteredString(g, "< " + lvlName + " >", WIDTH / 2, 100);
        g.setFont(new Font("Courier New", Font.PLAIN, 16));
        g.setColor(Color.WHITE);
        String sub = "SELECT DIFFICULTY (A / D)";
        drawCenteredString(g, sub, WIDTH / 2, 130);
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
            else if (o.type == 5) renderer.drawTRex(g, o.pos, o.size, o.animationTick, cam, o.color); // T-REX
        }
        for (projectile p : projectiles) renderer.drawCube(g, p.pos, 5, cam, p.color);
        for (particle p : particles) renderer.drawLine(g, p.pos, new vector(p.pos.x - p.vel.x, p.pos.y - p.vel.y, p.pos.z - p.vel.z), cam, p.color);
        
        if (state != State.GAMEOVER) {
            if (state == State.PLAYING && (invulnerabilityTimer > 0 && (invulnerabilityTimer / 5) % 2 != 0)) return; 
            renderer.drawCone(g, new vector(playerPos.x, playerPos.y, playerPos.z + 200), 20, 60, cam, playerColor);
        }
    }
    
    public void renderCamcorderOverlay(Graphics g) {
        if ((animationFrame / 30) % 2 == 0) {
            g.setColor(Color.RED);
            g.fillOval(50, 50, 20, 20);
            g.setFont(new Font("Courier New", Font.BOLD, 25));
            g.drawString("REC", 80, 68);
        }
        g.setColor(new Color(255, 255, 255, 150));
        int m = 30; int l = 50; 
        g.drawLine(m, m, m + l, m); g.drawLine(m, m, m, m + l);
        g.drawLine(WIDTH - m, m, WIDTH - m - l, m); g.drawLine(WIDTH - m, m, WIDTH - m, m + l);
        g.drawLine(m, HEIGHT - m, m + l, HEIGHT - m); g.drawLine(m, HEIGHT - m, m, HEIGHT - m - l);
        g.drawLine(WIDTH - m, HEIGHT - m, WIDTH - m - l, HEIGHT - m); g.drawLine(WIDTH - m, HEIGHT - m, WIDTH - m, HEIGHT - m - l);
        g.drawRect(WIDTH - 100, 50, 60, 25);
        g.fillRect(WIDTH - 40, 55, 5, 15); 
        g.setColor(Color.GREEN);
        g.fillRect(WIDTH - 95, 55, 40, 15); 
        g.setColor(Color.WHITE);
        g.setFont(new Font("Consolas", Font.PLAIN, 18));
        long playTime = (System.currentTimeMillis() - gameStartTime) / 1000;
        String timeStr = String.format("PLAY: 00:%02d:%02d", playTime / 60, playTime % 60);
        g.drawString(timeStr, 50, HEIGHT - 50);
    }

    public void renderStaticNoise(Graphics g) {
        g.setColor(new Color(255, 255, 255, 30)); 
        for (int i = 0; i < 1000; i++) {
            g.fillRect(rand.nextInt(WIDTH), rand.nextInt(HEIGHT), 2, 2);
        }
        if (rand.nextInt(100) < 5) { 
            int y = rand.nextInt(HEIGHT);
            int h = rand.nextInt(50) + 10;
            g.setColor(new Color(255, 255, 255, 50));
            g.fillRect(0, y, WIDTH, h);
            g.setColor(new Color(0, 0, 0, 50));
            g.drawLine(0, y + h/2, WIDTH, y + h/2);
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
            g.drawString("(PRESS F / RMB)", 20, 70); 
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
        drawCenteredString(g, "PAUSED", WIDTH/2, HEIGHT/2 - 100);
        g.setFont(new Font("Courier New", Font.BOLD, 28));
        for (int i = 0; i < pauseItems.length; i++) {
            g.setColor(i == pauseSelection ? Color.CYAN : Color.GRAY);
            String item = pauseItems[i]; if (i == pauseSelection) item = "> " + item + " <";
            drawCenteredString(g, item, WIDTH/2, HEIGHT/2 + i * 50);
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
        if (countdownValue > 0) drawCenteredString(g, text, WIDTH/2, HEIGHT/2 + 50);
    }

    public void drawCenteredString(Graphics g, String text, int x, int y) {
        int width = g.getFontMetrics().stringWidth(text);
        g.drawString(text, x - width / 2, y);
    }

    public void renderMenu(Graphics g) {
        int startY = HEIGHT / 2 - 80;
        g.setFont(new Font("Courier New", Font.BOLD, 28));
        for (int i = 0; i < menuItems.size(); i++) {
            Color color = (i == menuSelection) ? playerColor : Color.WHITE;
            g.setColor(color);
            String itemText = menuItems.get(i);
            if (itemText.equals("PLAYER COLOR")) itemText += " < Color #" + (selectedColorIndex + 1) + " > ";
            drawCenteredString(g, itemText, WIDTH / 2, startY + i * 40);
        }
        g.setFont(new Font("Courier New", Font.PLAIN, 18)); g.setColor(Color.GRAY);
        String instruction = "W/S to navigate, SPACE/ENTER to select.";
        drawCenteredString(g, instruction, WIDTH/2, HEIGHT - 50);
    }
    
    public void renderTitleAnimation(Graphics g) {
        String title = "WIRE FLEE"; 
        g.setFont(new Font("Impact", Font.ITALIC, 80)); 
        int titleW = g.getFontMetrics().stringWidth(title);
        int centerX = WIDTH / 2 - titleW / 2;
        int hoverY = (int) (Math.sin(titleHoverPhase) * 6); 
        int baseY = 150 + hoverY;
        int depth = 15;
        for (int i = 0; i < depth; i++) {
            float ratio = (float) i / depth;
            Color layerColor;
            if (i == depth - 1) {
                layerColor = Color.WHITE; 
            } else {
                layerColor = new Color(
                    (int)(50 + ratio * 0),   
                    (int)(0 + ratio * 255),   
                    (int)(100 + ratio * 155)  
                ); 
            }
            g.setColor(layerColor);
            g.drawString(title, centerX - i, baseY - i); 
        }
        g.setFont(new Font("Courier New", Font.BOLD, 16));
        g.setColor(Color.YELLOW);
        String sub = "A Pixelverse Production"; 
        int subHoverY = (int) (Math.sin(titleHoverPhase - 1.0) * 5);
        drawCenteredString(g, sub, WIDTH / 2, baseY + 40 + subHoverY);
    }
    
    public void renderHighScores(Graphics g) {
        g.setFont(new Font("Courier New", Font.BOLD, 36)); g.setColor(Color.YELLOW);
        drawCenteredString(g, "HIGHEST SCORES", WIDTH / 2, HEIGHT / 2 - 100);
        g.setFont(new Font("Courier New", Font.PLAIN, 24)); g.setColor(Color.WHITE);
        int startY = HEIGHT / 2 - 30;
        int leftAlignX = 150;
        for (int i = 0; i < highScores.size(); i++) {
            HighScore hs = highScores.get(i);
            String line = String.format("#%d. %06d pts - %.2fs", i + 1, hs.score, hs.time / 1000.0);
            g.drawString(line, leftAlignX, startY + i * 35);
        }
        g.setFont(new Font("Courier New", Font.PLAIN, 18)); g.setColor(Color.GRAY);
        drawCenteredString(g, "PRESS ENTER/SPACE/CLICK TO RETURN", WIDTH/2, HEIGHT - 50);
    }
    
    public void renderCredits(Graphics g) {
        g.setFont(new Font("Courier New", Font.BOLD, 36)); g.setColor(Color.YELLOW);
        drawCenteredString(g, "CREDITS", WIDTH/2, HEIGHT/2 - 100);
        g.setFont(new Font("Courier New", Font.PLAIN, 24)); g.setColor(Color.WHITE);
        String[] lines = {
            "STUDIO: PixelVerse", 
            "Ahnaf Ashique Adi", 
            "Ahmed Abu Bakar",
            "STUDIO: Space Jam",
            "Colaboration: ProjuktiLipi"
        };
        for (int i=0; i<lines.length; i++) {
            drawCenteredString(g, lines[i], WIDTH/2, HEIGHT/2 + i*30);
        }
    }

    public void renderGameOver(Graphics g) {
        g.setFont(new Font("Courier New", Font.BOLD, 40)); g.setColor(Color.RED);
        drawCenteredString(g, "GAME OVER", WIDTH/2, HEIGHT/2 - 40);
        g.setFont(new Font("Courier New", Font.PLAIN, 20)); g.setColor(Color.WHITE);
        String sc = "FINAL SCORE: " + score; 
        drawCenteredString(g, sc, WIDTH/2, HEIGHT/2 + 10);
        String res = "PRESS SPACE/ENTER TO MENU"; 
        drawCenteredString(g, res, WIDTH/2, HEIGHT/2 + 50);
    }

    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (k==KeyEvent.VK_LEFT || k==KeyEvent.VK_A) leftPressed=true; 
        if (k==KeyEvent.VK_RIGHT || k==KeyEvent.VK_D) rightPressed=true;
        if (k==KeyEvent.VK_UP || k==KeyEvent.VK_W) wPressed=true;
        if (k==KeyEvent.VK_DOWN || k==KeyEvent.VK_S) sPressed=true;
        
        if (k==KeyEvent.VK_SPACE) spacePressed=true; 
        if (k==KeyEvent.VK_ESCAPE) escPressed=true;  
        if (k==KeyEvent.VK_F) fPressed=true;         
        if (k==KeyEvent.VK_ENTER) enterPressed=true; 
    }
    public void keyReleased(KeyEvent e) {
        int k = e.getKeyCode();
        if (k==KeyEvent.VK_LEFT || k==KeyEvent.VK_A) leftPressed=false; 
        if (k==KeyEvent.VK_RIGHT || k==KeyEvent.VK_D) rightPressed=false;
        if (k==KeyEvent.VK_UP || k==KeyEvent.VK_W) wPressed=false;
        if (k==KeyEvent.VK_DOWN || k==KeyEvent.VK_S) sPressed=false;
        
        if (k==KeyEvent.VK_SPACE) spacePressed=false;
        if (k==KeyEvent.VK_ESCAPE) escPressed=false;
        if (k==KeyEvent.VK_F) fPressed=false;
        if (k==KeyEvent.VK_ENTER) enterPressed=false;
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