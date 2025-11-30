// projectile.java
// Represents a player's shot moving rapidly forward.

import java.awt.Color;

public class projectile {
    public vector pos;
    public vector vel;
    public int life = 300; // Max life of 5 seconds (300 frames)
    public Color color;
    
    public projectile(double x, double y, double z, double vx, double vy, double vz, Color color) {
        this.pos = new vector(x, y, z);
        this.vel = new vector(vx, vy, vz);
        this.color = color;
    }

    public void update() {
        pos.x += vel.x;
        pos.y += vel.y;
        pos.z += vel.z;
        life--;
    }
}