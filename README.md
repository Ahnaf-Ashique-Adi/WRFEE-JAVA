# Retro Runner

![Retro Runner Banner](path/to/your/screenshot.png)
*(Replace the link above with a screenshot or GIF of your gameplay!)*

**Retro Runner** is an infinite 3D tunnel runner built entirely from scratch in Java.

Unlike most modern games, this project uses a **custom software rendering engine**. It handles 3D perspective projection, wireframe rendering, and procedural animations using pure Java math, without relying on external GPU libraries like LWJGL, OpenGL, or JavaFX 3D.

It features a simulated VHS/CRT chromatic aberration effect for that authentic 80s arcade aesthetic.

---

## Features

  * **Custom 3D Engine:** Pure Java implementation of 3D vector mathematics and camera projection.
  * **Procedural Generation:** Infinite tunnel with randomized obstacles and enemy spawns.
  * **VHS Aesthetic:** Simulated RGB color shifting and scanlines implemented in the rendering pipeline.
  * **Progression System:**
      * **Score \> 3000:** Unlock Double Shot + "Ant" enemies appear.
      * **Score \> 6000:** Unlock Quad Shot + "Dragon" bosses appear.
      * **Score \> 7000:** Unlock Bombs.
  * **Local High Scores:** Tracks your best runs and times.
  * **Customizable:** Select your player color from 20 neon presets.

## Prerequisites

  * **Java Development Kit (JDK):** Version 8 or higher.

## Setup and Compilation

1.  **Clone or Download** this repository.
2.  Ensure your file structure looks like this:
    ```text
    /ProjectRoot
      /src
        game.java
        ... (other .java files)
      /bin  (empty folder)
    ```
3.  **Compile** the source code:
    ```bash
    javac -d bin src/*.java
    ```

## Running the Game

To launch the game, run the following command from the project root:

```bash
java -cp bin game
```

## Controls

The game supports both Keyboard and Mouse inputs.

| Action | Keyboard | Mouse |
| :--- | :--- | :--- |
| **Move** | `W`, `A`, `S`, `D` or Arrows | - |
| **Shoot** | `K` | Left Click |
| **Bomb** | `L` (Requires Ammo) | Right Click |
| **Pause** | `Space` | - |
| **Select** | `Enter` / `Space` | Left Click |

### Power-Ups & Mechanics

  * **Hearts:** You start with **3 HP**. Colliding with obstacles removes 1 HP.
  * **Health Drops:** Look for **Green Capsules**. Collecting one restores 1 HP.
  * **Bombs:** Unlocked at high scores. Clears all screen obstacles. (Max 5 ammo).
  * **Invulnerability:** You have a brief flashing invulnerability period after taking damage.

## Enemies & Obstacles

  * **Cubes & Cones:** Standard static obstacles.
  * **Red Ants:** Small, animated ground enemies.
  * **White Dragons:** Large flying enemies with animated wings and tails.

## Technical Details

For developers interested in how this works:

  * **`vector.java`**: Handles 3D coordinate math (addition, subtraction, cross/dot products).
  * **`camera.java`**: Manages perspective projection (converting 3D world coordinates to 2D screen pixels) and camera roll.
  * **`renderer.java`**: The "GPU" of the game. It draws lines and shapes using standard Java 2D Graphics. It purposefully draws lines three times (Red, Blue, Original) to create the **chromatic aberration (glitch)** effect.
  * **`game.java`**: The main game loop (fixed time step), state machine (MENU, PLAYING, GAMEOVER), and entity management.

## Credits

  * **Studio:** PixelVerse
  * **Collaborator:** Projukti Lipi

-----

### To Do / Known Issues

  * Add sound effects.
  * Implement persistent high score saving (file I/O).