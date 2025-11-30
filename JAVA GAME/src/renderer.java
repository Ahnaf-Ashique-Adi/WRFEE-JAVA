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
        double segmentSize = size / 3;
        
        // 1. Draw Body Segments (Thorax, Abdomen, Head)
        // We tilt the head slightly up/down based on tick to make it look alive
        vector headOffset = new vector(0, Math.sin(tick * 0.2) * 5, segmentSize + 5); 
        
        drawCube(g, pos, segmentSize, cam, c); // Center
        drawCube(g, new vector(pos.x, pos.y, pos.z - segmentSize), segmentSize * 1.2, cam, c); // Rear
        drawCube(g, vector.add(pos, headOffset), segmentSize * 0.8, cam, c); // Head

        // 2. Draw Articulated Legs using Rotation
        for (int i = 0; i < 3; i++) {
            // Calculate a "cycle" for the leg movement so they don't all move at once
            double legCycle = tick * 0.2 + (i * 2); 
            double lift = Math.max(0, Math.sin(legCycle)) * 10; // Lift leg up
            double swing = Math.cos(legCycle) * 10; // Swing leg forward/back

            // Define leg shape RELATIVE to the body (Model Space)
            vector kneeBase = new vector(20, -10, (i - 1) * 15);
            vector footBase = new vector(35, 10, (i - 1) * 15);

            // Apply animation to model space
            kneeBase.y -= lift;
            footBase.y += lift; 
            footBase.z += swing;

            // Rotate legs for the Left side (Normal)
            vector kneeL = vector.add(pos, new vector(-kneeBase.x, kneeBase.y, kneeBase.z));
            vector footL = vector.add(pos, new vector(-footBase.x, footBase.y, footBase.z));
            
            // Rotate legs for the Right side (Mirror)
            vector kneeR = vector.add(pos, kneeBase);
            vector footR = vector.add(pos, footBase);

            // Draw Left Leg
            drawLine(g, pos, kneeL, cam, c);
            drawLine(g, kneeL, footL, cam, c);

            // Draw Right Leg
            drawLine(g, pos, kneeR, cam, c);
            drawLine(g, kneeR, footR, cam, c);
        }
        
        // 3. Draw Antennae (Rotated)
        vector antL = new vector(-5, -10, 10);
        vector antR = new vector(5, -10, 10);
        
        // Rotate antennae back and forth
        antL = vector.rotateX(antL, Math.sin(tick * 0.1) * 0.2);
        antR = vector.rotateX(antR, Math.sin(tick * 0.1) * 0.2);
        
        drawLine(g, vector.add(pos, headOffset), vector.add(vector.add(pos, headOffset), antL), cam, c);
        drawLine(g, vector.add(pos, headOffset), vector.add(vector.add(pos, headOffset), antR), cam, c);
    }
    
    // NEW: Dragon Enemy Drawing
    public static void drawDragon(Graphics g, vector pos, double size, int tick, camera cam, Color c) {
        // 1. The Head
        vector headPos = new vector(pos.x, pos.y, pos.z + 50);
        drawCone(g, headPos, 15, 40, cam, c); // Snout
        drawCube(g, new vector(pos.x, pos.y - 15, pos.z + 40), 10, cam, Color.RED); // Glowing Eyes

        // 2. The Wings (Complex Flapping)
        double flapAngle = Math.sin(tick * 0.15) * 0.5; // Flap range in radians
        
        // Define a wing shape flat
        vector wingBone1 = new vector(60, 0, 10);
        vector wingBone2 = new vector(120, 20, -20); // Tip
        
        // Apply Rotation to Left Wing
        vector L1 = vector.add(pos, vector.rotateZ(new vector(-wingBone1.x, wingBone1.y, wingBone1.z), -flapAngle));
        vector L2 = vector.add(pos, vector.rotateZ(new vector(-wingBone2.x, wingBone2.y, wingBone2.z), -flapAngle * 1.5));
        
        // Apply Rotation to Right Wing
        vector R1 = vector.add(pos, vector.rotateZ(wingBone1, flapAngle));
        vector R2 = vector.add(pos, vector.rotateZ(wingBone2, flapAngle * 1.5));

        // Draw Wings
        drawLine(g, pos, L1, cam, c); drawLine(g, L1, L2, cam, c); // Left Structure
        drawLine(g, L1, vector.add(pos, new vector(0,0,-20)), cam, c); // Membrane
        
        drawLine(g, pos, R1, cam, c); drawLine(g, R1, R2, cam, c); // Right Structure
        drawLine(g, R1, vector.add(pos, new vector(0,0,-20)), cam, c); // Membrane

        // 3. The Tail (3D Spiral / Helix)
        vector prevSegment = pos;
        for (int i = 1; i < 8; i++) {
            double tailLag = (tick * 0.2) - (i * 0.5);
            
            // This creates a circular/spiral motion for the tail
            double tx = Math.sin(tailLag) * (10 + i * 2); 
            double ty = Math.cos(tailLag) * (10 + i * 2);
            double tz = -i * 30;

            vector currSegment = vector.add(pos, new vector(tx, ty, tz));
            
            drawLine(g, prevSegment, currSegment, cam, c);
            
            // Draw small spikes on tail
            if (i % 2 == 0) {
                 vector spike = vector.add(currSegment, new vector(0, -15, 0));
                 drawLine(g, currSegment, spike, cam, c);
            }
            
            prevSegment = currSegment;
        }
    }
    // NEW: T-Rex Enemy Drawing
    public static void drawTRex(Graphics g, vector pos, double size, int tick, camera cam, Color c) {
        // 1. Body (Main Block)
        drawCube(g, pos, size, cam, c);
        
        // 2. Head (Large and boxy, slightly up and forward)
        vector headPos = new vector(pos.x, pos.y - size/2, pos.z + size/2 + 10);
        drawCube(g, headPos, size * 0.8, cam, c);
        
        // 3. Jaw (Opens and closes)
        double jawOpen = Math.abs(Math.sin(tick * 0.2)) * 10;
        vector jawPos = new vector(pos.x, pos.y - size/2 + 10 + jawOpen, pos.z + size/2 + 15);
        renderer.drawLine(g, headPos, jawPos, cam, c);
        
        // 4. Tiny Arms (Classic T-Rex feature)
        vector armL = new vector(pos.x - size/2, pos.y, pos.z + size/2);
        vector armR = new vector(pos.x + size/2, pos.y, pos.z + size/2);
        renderer.drawLine(g, armL, new vector(armL.x, armL.y + 10, armL.z + 10), cam, c);
        renderer.drawLine(g, armR, new vector(armR.x, armR.y + 10, armR.z + 10), cam, c);

        // 5. Legs (Walking Animation)
        double legMove = Math.sin(tick * 0.2) * 20;
        vector hipL = new vector(pos.x - size/3, pos.y + size/2, pos.z);
        vector hipR = new vector(pos.x + size/3, pos.y + size/2, pos.z);
        
        vector footL = new vector(hipL.x, hipL.y + 30, hipL.z + legMove);
        vector footR = new vector(hipR.x, hipR.y + 30, hipR.z - legMove);
        
        renderer.drawLine(g, hipL, footL, cam, c);
        renderer.drawLine(g, hipR, footR, cam, c);
        
        // 6. Tail (Balancing behind)
        vector tailTip = new vector(pos.x + Math.sin(tick*0.1)*10, pos.y, pos.z - size * 2);
        renderer.drawLine(g, pos, tailTip, cam, c);
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