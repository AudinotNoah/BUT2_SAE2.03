package serveur;

import java.io.*;


public class Status {
    public static void sendStatus(OutputStream out,Configuration config) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        long memoireutilise = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memoirelibre = Runtime.getRuntime().maxMemory() - memoireutilise;

        File root = new File(config.getRootDir());
        long disquelibre = root.getFreeSpace();
        long disquetotal = root.getTotalSpace();
        long disqueutilise = disquetotal - disquelibre;

        int availableProcessors = runtime.availableProcessors();

        String status = "<html><body>" +
                "<h1>Status du serveur</h1>" +
                "<p>Memoire disponible: " + memoireutilise / 1024 / 1024 + " Mo</p>" +
                "<p>Memoire utilisée: " + memoirelibre / 1024 / 1024 + " Mo</p>" +
                "<p>Espace disque disponible: " + disquelibre / 1024 / 1024 + " Mo</p>" +
                "<p>Espace disque utilisé: " + disqueutilise / 1024 / 1024 + " Mo</p>" +
                "<p>Nombre de processeurs: " + availableProcessors + "</p>" +
                "</body></html>";

        byte[] content = status.getBytes();

        out.write(("HTTP/1.1 200 OK\r\n").getBytes());
        out.write(("Content-Type: text/html\r\n").getBytes());
        out.write(("Content-Length: " + content.length + "\r\n").getBytes());
        out.write(("\r\n").getBytes());
        out.write(content);
        out.flush();
    }
}
