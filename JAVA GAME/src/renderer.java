import java.awt.Graphics;
import java.awt.Color;

public class renderer {

    public static void drawLine(Graphics g, vector v1, vector v2, camera cam, Color c) {
        vector p1 = cam.project(v1);
        vector p2 = cam.project(v2);

        if (p1 != null && p2 != null) {
            // VHS RGB Shift Effect
            // Draw Red channel offset
            g.setColor(new Color(255, 0, 0, 100)); // Red with transparency
            g.drawLine((int) p1.x - 2, (int) p1.y, (int) p2.x - 2, (int) p2.y);

            // Draw Blue channel offset
            g.setColor(new Color(0, 0, 255, 100)); // Blue with transparency
            g.drawLine((int) p1.x + 2, (int) p1.y, (int) p2.x + 2, (int) p2.y);

            // Draw Main Line
            g.setColor(c);
            g.drawLine((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y);
        }
    }

    public static void drawCube(Graphics g, vector pos, double size, camera cam, Color c) {
        double s = size / 2;

        // Vertices
        vector[] v = new vector[8];
        v[0] = new vector(pos.x - s, pos.y - s, pos.z - s);
        v[1] = new vector(pos.x + s, pos.y - s, pos.z - s);
        v[2] = new vector(pos.x + s, pos.y + s, pos.z - s);
        v[3] = new vector(pos.x - s, pos.y + s, pos.z - s);
        v[4] = new vector(pos.x - s, pos.y - s, pos.z + s);
        v[5] = new vector(pos.x + s, pos.y - s, pos.z + s);
        v[6] = new vector(pos.x + s, pos.y + s, pos.z + s);
        v[7] = new vector(pos.x - s, pos.y + s, pos.z + s);

        // Edges
        drawLine(g, v[0], v[1], cam, c);
        drawLine(g, v[1], v[2], cam, c);
        drawLine(g, v[2], v[3], cam, c);
        drawLine(g, v[3], v[0], cam, c);

        drawLine(g, v[4], v[5], cam, c);
        drawLine(g, v[5], v[6], cam, c);
        drawLine(g, v[6], v[7], cam, c);
        drawLine(g, v[7], v[4], cam, c);

        drawLine(g, v[0], v[4], cam, c);
        drawLine(g, v[1], v[5], cam, c);
        drawLine(g, v[2], v[6], cam, c);
        drawLine(g, v[3], v[7], cam, c);
    }

    public static void drawCone(Graphics g, vector pos, double radius, double length, camera cam, Color c) {
        // Tip of the cone (pointing forward)
        vector tip = new vector(pos.x, pos.y, pos.z + length / 2);
        // Center of base
        vector baseCenter = new vector(pos.x, pos.y, pos.z - length / 2);

        int segments = 8;
        vector[] basePoints = new vector[segments];

        // Calculate base points
        for (int i = 0; i < segments; i++) {
            double angle = (Math.PI * 2 * i) / segments;
            // Rotate base points to align with Z axis (XY plane)
            basePoints[i] = new vector(
                    baseCenter.x + Math.cos(angle) * radius,
                    baseCenter.y + Math.sin(angle) * radius,
                    baseCenter.z);
        }

        // Draw lines from tip to base points
        for (int i = 0; i < segments; i++) {
            drawLine(g, tip, basePoints[i], cam, c);
        }

        // Draw base circle
        for (int i = 0; i < segments; i++) {
            drawLine(g, basePoints[i], basePoints[(i + 1) % segments], cam, c);
        }
    }
}
