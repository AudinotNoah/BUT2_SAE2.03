package serveur;

import java.io.*;


public class Status {
    public static byte[] sendStatus(Configuration config) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        long memoireutilise = Runtime.getRuntime().maxMemory() - Runtime.getRuntime().freeMemory();
        long memoirelibre = Runtime.getRuntime().maxMemory() - memoireutilise;

        File root = new File(config.getRootDir());
        long disquelibre = root.getFreeSpace();
        long disquetotal = root.getTotalSpace();
        long disqueutilise = disquetotal - disquelibre;

        int n_process = runtime.availableProcessors();

        String status = "<html><body>" +
                "<h1>Status du serveur</h1>" +
                "<p>Memoire disponible: " + memoireutilise / 1024 / 1024 + " Mo</p>" +
                "<p>Memoire utilisée: " + memoirelibre / 1024 / 1024 + " Mo</p>" +
                "<p>Espace disque disponible: " + disquelibre / 1024 / 1024 + " Mo</p>" +
                "<p>Espace disque utilisé: " + disqueutilise / 1024 / 1024 + " Mo</p>" +
                "<p>Nombre de processeurs: " + n_process + "</p>" +
                "</body></html>";

        return status.getBytes();
    }
}
