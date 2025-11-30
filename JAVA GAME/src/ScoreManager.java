import java.io.*;
import java.util.ArrayList;
import java.util.Collections;

public class ScoreManager {
    
    private static final String FILE_PATH = "highscores.dat";

    // LOAD scores from file
    public static ArrayList<HighScore> loadScores() {
        ArrayList<HighScore> scores = new ArrayList<>();
        File file = new File(FILE_PATH);

        if (!file.exists()) {
            // Return defaults if no file exists
            scores.add(new HighScore(3000, 15000));
            scores.add(new HighScore(1500, 8000));
            return scores;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    int s = Integer.parseInt(parts[0]);
                    long t = Long.parseLong(parts[1]);
                    scores.add(new HighScore(s, t));
                }
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
        
        Collections.sort(scores);
        return scores;
    }

    // SAVE scores to file
    public static void saveScores(ArrayList<HighScore> scores) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_PATH))) {
            for (HighScore hs : scores) {
                bw.write(hs.score + "," + hs.time);
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
