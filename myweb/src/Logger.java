package serveur;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    private final String accessLogPath;
    private final String errorLogPath;

    public Logger(String accessLogPath, String errorLogPath) {
        this.accessLogPath = accessLogPath;
        this.errorLogPath = errorLogPath;
    }

    // Méthode pour enregistrer un accès dans le fichier de log des accès
    public void logAccess(String message) {
        logMessage(accessLogPath, message);
    }

    // Méthode pour enregistrer une erreur dans le fichier de log des erreurs
    public void logError(String message) {
        logMessage(errorLogPath, message);
    }

    // Méthode générique pour enregistrer un message dans un fichier de log
    private void logMessage(String logPath, String message) {
        try (FileWriter fw = new FileWriter(logPath, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            out.println(timeStamp + " " + message);
        } catch (IOException e) {
            System.err.println("Erreur d'écriture dans le fichier de log : " + logPath);
            e.printStackTrace();
        }
    }
}
