import java.awt.Color;

public class obstacle {
    public vector pos;
    public double size;
    public int type; // 0 = Cube, 1 = Pyramid
    public Color color;

    public obstacle(double x, double y, double z, double size, int type, Color c) {
        this.pos = new vector(x, y, z);
        this.size = size;
        this.type = type;
        this.color = c;
    }
}
