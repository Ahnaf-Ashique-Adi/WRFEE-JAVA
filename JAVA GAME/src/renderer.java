import java.awt.Color;
import java.awt.Graphics;

public class renderer {

    public static void drawLine(Graphics g, vector v1, vector v2, camera cam, Color c) {
        vector p1 = cam.project(v1);
        vector p2 = cam.project(v2);

        if (p1 != null && p2 != null) {
            // VHS RGB Shift Effect (Subtle)
            g.setColor(new Color(255, 0, 0, 80));
            g.drawLine((int) p1.x - 2, (int) p1.y, (int) p2.x - 2, (int) p2.y);
            g.setColor(new Color(0, 0, 255, 80));
            g.drawLine((int) p1.x + 2, (int) p1.y, (int) p2.x + 2, (int) p2.y);

            g.setColor(c);
            g.drawLine((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y);
        }
    }

    public static void drawCube(Graphics g, vector pos, double size, camera cam, Color c) {
        double s = size / 2;
        vector[] v = new vector[8];
        v[0] = new vector(pos.x - s, pos.y - s, pos.z - s);
        v[1] = new vector(pos.x + s, pos.y - s, pos.z - s);
        v[2] = new vector(pos.x + s, pos.y + s, pos.z - s);
        v[3] = new vector(pos.x - s, pos.y + s, pos.z - s);
        v[4] = new vector(pos.x - s, pos.y - s, pos.z + s);
        v[5] = new vector(pos.x + s, pos.y - s, pos.z + s);
        v[6] = new vector(pos.x + s, pos.y + s, pos.z + s);
        v[7] = new vector(pos.x - s, pos.y + s, pos.z + s);

        drawLine(g, v[0], v[1], cam, c); drawLine(g, v[1], v[2], cam, c);
        drawLine(g, v[2], v[3], cam, c); drawLine(g, v[3], v[0], cam, c);
        drawLine(g, v[4], v[5], cam, c); drawLine(g, v[5], v[6], cam, c);
        drawLine(g, v[6], v[7], cam, c); drawLine(g, v[7], v[4], cam, c);
        drawLine(g, v[0], v[4], cam, c); drawLine(g, v[1], v[5], cam, c);
        drawLine(g, v[2], v[6], cam, c); drawLine(g, v[3], v[7], cam, c);
    }

    public static void drawCone(Graphics g, vector pos, double radius, double length, camera cam, Color c) {
        vector tip = new vector(pos.x, pos.y, pos.z + length / 2);
        vector baseCenter = new vector(pos.x, pos.y, pos.z - length / 2);
        int segments = 8;
        vector[] basePoints = new vector[segments];

        for (int i = 0; i < segments; i++) {
            double angle = (Math.PI * 2 * i) / segments;
            basePoints[i] = new vector(baseCenter.x + Math.cos(angle) * radius, baseCenter.y + Math.sin(angle) * radius, baseCenter.z);
        }
        for (int i = 0; i < segments; i++) {
            drawLine(g, tip, basePoints[i], cam, c);
            drawLine(g, basePoints[i], basePoints[(i + 1) % segments], cam, c);
        }
    }

    public static void drawCapsule(Graphics g, vector pos, double radius, double length, camera cam, Color c) {
        double halfLength = length / 2;
        int segments = 8;
        vector cap1Center = new vector(pos.x, pos.y, pos.z - halfLength);
        vector cap2Center = new vector(pos.x, pos.y, pos.z + halfLength);
        vector[] cap1Points = new vector[segments];
        vector[] cap2Points = new vector[segments];

        for (int i = 0; i < segments; i++) {
            double angle = (Math.PI * 2 * i) / segments;
            double dx = Math.cos(angle) * radius;
            double dy = Math.sin(angle) * radius;
            cap1Points[i] = new vector(cap1Center.x + dx, cap1Center.y + dy, cap1Center.z);
            cap2Points[i] = new vector(cap2Center.x + dx, cap2Center.y + dy, cap2Center.z);
        }

        for (int i = 0; i < segments; i++) {
            drawLine(g, cap1Points[i], cap1Points[(i + 1) % segments], cam, c);
            drawLine(g, cap2Points[i], cap2Points[(i + 1) % segments], cam, c);
        }
        int[] keySegments = {0, segments / 4, segments / 2, 3 * segments / 4};
        for (int index : keySegments) {
            drawLine(g, cap1Points[index], cap2Points[index], cam, c);
        }
    }
    
    // NEW: Ant Enemy Drawing
    public static void drawAnt(Graphics g, vector pos, double size, int tick, camera cam, Color c) {
        // Body (3 segments)
        double segmentSize = size / 3;
        drawCube(g, pos, segmentSize, cam, c); // Thorax
        drawCube(g, new vector(pos.x, pos.y, pos.z - segmentSize), segmentSize*0.8, cam, c); // Abdomen
        drawCube(g, new vector(pos.x, pos.y, pos.z + segmentSize), segmentSize*0.6, cam, c); // Head
        
        // Biting Animation (Mandibles)
        double biteOffset = Math.sin(tick * 0.5) * 5;
        vector mandibleL = new vector(pos.x - 5 - biteOffset, pos.y, pos.z + segmentSize + 5);
        vector mandibleR = new vector(pos.x + 5 + biteOffset, pos.y, pos.z + segmentSize + 5);
        vector headTip = new vector(pos.x, pos.y, pos.z + segmentSize + 15);
        drawLine(g, new vector(pos.x, pos.y, pos.z + segmentSize), headTip, cam, c);
        
        // Legs Animation
        for(int i=0; i<3; i++) {
            double legZ = pos.z + (i-1) * 5;
            double legMove = Math.sin(tick * 0.2 + i) * 10;
            
            // Left Legs
            vector kneeL = new vector(pos.x - 15, pos.y - 10 + legMove, legZ);
            vector footL = new vector(pos.x - 25, pos.y + 10, legZ);
            drawLine(g, pos, kneeL, cam, c);
            drawLine(g, kneeL, footL, cam, c);
            
            // Right Legs
            vector kneeR = new vector(pos.x + 15, pos.y - 10 - legMove, legZ);
            vector footR = new vector(pos.x + 25, pos.y + 10, legZ);
            drawLine(g, pos, kneeR, cam, c);
            drawLine(g, kneeR, footR, cam, c);
        }
    }
    
    // NEW: Dragon Enemy Drawing
    public static void drawDragon(Graphics g, vector pos, double size, int tick, camera cam, Color c) {
        // Body (Spine)
        vector headPos = new vector(pos.x, pos.y, pos.z + 40);
        drawCone(g, headPos, 10, 30, cam, c); // Head
        
        // Wings Flapping
        double wingFlap = Math.sin(tick * 0.1) * 40;
        vector bodyCenter = pos;
        vector leftWingTip = new vector(pos.x - 80, pos.y + wingFlap, pos.z);
        vector rightWingTip = new vector(pos.x + 80, pos.y + wingFlap, pos.z);
        
        // Wing structure
        drawLine(g, bodyCenter, leftWingTip, cam, c);
        drawLine(g, bodyCenter, rightWingTip, cam, c);
        drawLine(g, leftWingTip, new vector(pos.x - 20, pos.y, pos.z - 40), cam, c);
        drawLine(g, rightWingTip, new vector(pos.x + 20, pos.y, pos.z - 40), cam, c);
        
        // Tail (Undulating)
        vector prevTail = bodyCenter;
        for(int i=1; i<5; i++) {
            double tailWag = Math.sin(tick * 0.1 + i) * 10;
            vector currentTail = new vector(pos.x + tailWag, pos.y, pos.z - (i * 20));
            drawLine(g, prevTail, currentTail, cam, c);
            prevTail = currentTail;
        }
    }

    public static void drawCrosshair(Graphics g, int centerX, int centerY, Color c) {
        int size = 15;
        g.setColor(c);
        g.drawLine(centerX - size, centerY, centerX - 5, centerY);
        g.drawLine(centerX + 5, centerY, centerX + size, centerY);
        g.drawLine(centerX, centerY - size, centerX, centerY - 5);
        g.drawLine(centerX, centerY + 5, centerX, centerY + size);
    }
}