// HighScore.java
// Simple class to store a single high score record.

public class HighScore implements Comparable<HighScore> {
    public int score;
    public long time; // Time recorded in milliseconds (optional, but good for future persistence)

    public HighScore(int score, long time) {
        this.score = score;
        this.time = time;
    }

    // Used to sort the scores (highest score first)
    @Override
    public int compareTo(HighScore other) {
        // Compare scores first (descending)
        if (this.score != other.score) {
            return Integer.compare(other.score, this.score);
        }
        // If scores are equal, compare time (ascending, for tiebreaker)
        return Long.compare(this.time, other.time);
    }
    
    // Simple String representation for display
    public String toString() {
        return String.format("%06d (Time: %dms)", score, time);
    }
}
