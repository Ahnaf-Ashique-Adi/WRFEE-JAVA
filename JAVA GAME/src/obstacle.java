import java.awt.Color;

public class obstacle {
    public vector pos;
    public double size;
    public int type; 
    // 0=Cube, 1=Cone, 2=Health, 3=Ant, 4=Dragon
    public Color color;
    public int animationTick = 0; // For biting/flapping animation

    public obstacle(double x, double y, double z, double size, int type, Color c) {
        this.pos = new vector(x, y, z);
        this.size = size;
        this.type = type;
        this.color = c;
    }
    
    public void update() {
        animationTick++;
    }
}