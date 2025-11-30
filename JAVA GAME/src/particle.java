import java.awt.Color;

public class particle {
    public vector pos;
    public vector vel;
    public int life;
    public Color color;

    public particle(double x, double y, double z, double vx, double vy, double vz, int life, Color color) {
        this.pos = new vector(x, y, z);
        this.vel = new vector(vx, vy, vz);
        this.life = life;
        this.color = color;
    }

    public void update() {
        pos.x += vel.x;
        pos.y += vel.y;
        pos.z += vel.z;
        life--;
    }
}
