public class vector {
    public double x;
    public double y;
    public double z;

    public vector(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static vector add(vector v1, vector v2) {
        return new vector(v1.x + v2.x, v1.y + v2.y, v1.z + v2.z);
    }

    public static vector sub(vector v1, vector v2) {
        return new vector(v1.x - v2.x, v1.y - v2.y, v1.z - v2.z);
    }

    public static vector mul(vector v, double s) {
        return new vector(v.x * s, v.y * s, v.z * s);
    }

    // Dot product
    public static double dot(vector v1, vector v2) {
        return v1.x * v2.x + v1.y * v2.y + v1.z * v2.z;
    }

    // Cross product
    public static vector cross(vector v1, vector v2) {
        return new vector(
            v1.y * v2.z - v1.z * v2.y,
            v1.z * v2.x - v1.x * v2.z,
            v1.x * v2.y - v1.y * v2.x
        );
    }
    
    public static double mag(vector v) {
        return Math.sqrt(v.x*v.x + v.y*v.y + v.z*v.z);
    }
    
    public static vector norm(vector v) {
        double m = mag(v);
        if (m == 0) return new vector(0,0,0);
        return new vector(v.x/m, v.y/m, v.z/m);
    }
    // --- ROTATION MATH ---

    // Rotate around X-Axis (Pitch) - Good for flapping wings or nodding heads
    public static vector rotateX(vector v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new vector(
            v.x,
            v.y * cos - v.z * sin,
            v.y * sin + v.z * cos
        );
    }

    // Rotate around Y-Axis (Yaw) - Good for turning left/right or snake-like movement
    public static vector rotateY(vector v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new vector(
            v.x * cos + v.z * sin,
            v.y,
            -v.x * sin + v.z * cos
        );
    }

    // Rotate around Z-Axis (Roll) - Good for banking or tilting
    public static vector rotateZ(vector v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new vector(
            v.x * cos - v.y * sin,
            v.x * sin + v.y * cos,
            v.z
        );
    }
}
