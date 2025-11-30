public class camera {
    public vector pos;
    public double fov = 500.0;
    public double roll = 0; // Camera roll in radians

    public camera(double x, double y, double z) {
        this.pos = new vector(x, y, z);
    }

    public vector project(vector v) {
        // Standard Perspective Projection
        double x = v.x - pos.x;
        double y = v.y - pos.y;
        double z = v.z - pos.z;

        // Apply Camera Roll
        double cos = Math.cos(roll);
        double sin = Math.sin(roll);
        double rotX = x * cos - y * sin;
        double rotY = x * sin + y * cos;

        x = rotX;
        y = rotY;

        if (z <= 0)
            return null; // Behind camera

        double screenX = (x * fov) / z + game.WIDTH / 2;
        double screenY = (y * fov) / z + game.HEIGHT / 2;

        return new vector(screenX, screenY, z);
    }
}
