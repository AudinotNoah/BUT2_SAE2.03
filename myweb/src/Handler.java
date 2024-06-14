package serveur;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;

public class Handler implements Runnable {
    private final Socket clientSocket;
    private final Configuration config;
    private final Logger logger;

    public Handler(Socket clientSocket, Configuration config, Logger logger) {
        this.clientSocket = clientSocket;
        this.config = config;
        this.logger = logger;
    }

    @Override
    public void run() {
        try {
            InetAddress clientAddress = clientSocket.getInetAddress();
            String clientIP = clientAddress.getHostAddress();

            if (!checkip(clientIP)) {
                sendError(clientSocket.getOutputStream(), 403, "Forbidden: Access is denied");
                clientSocket.close();
                logger.logAccess("Requête refusée de " + clientIP);
                return;
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            OutputStream out = clientSocket.getOutputStream();

            String ligne = in.readLine();
            String[] lignecoupe = ligne.split(" ");
            if (lignecoupe.length < 3) {
                sendError(out, 400, "Erreur requete");
                logger.logError("Requête mal formée de " + clientIP);
                return;
            }

            String methode = lignecoupe[0];
            String page = lignecoupe[1];
            String version = lignecoupe[2];

            if (page.equals("/")) {
                page = "/index.html";
            }

            System.out.println("Requete de type " + methode + " reçu\nPour acceder à la page " + page + "\nEn version " + version);

            page = page.substring(1);

            File file = new File(config.getRootDir() + "/" + page);
            if (!file.exists()) {
                sendError(out, 404, "Not Found");
                logger.logError("Fichier non trouvé pour la requête de " + clientIP + " : " + page);
                return;
            }

            byte[] contenue = Files.readAllBytes(file.toPath());

            String typecontenue;
            if (page.contains(".html")) {
                typecontenue = "text/html";
            } else if (page.contains(".jpg")) {
                typecontenue = "image/jpg";
            } else if (page.contains(".gif")) {
                typecontenue = "image/gif";
            } else {
                sendError(out, 400, "Fichier d'un type inconnu");
                logger.logError("Type de fichier inconnu pour la requête de " + clientIP + " : " + page);
                return;
            }

            out.write(("HTTP/1.1 200 OK\r\n").getBytes());
            out.write(("Content-Type: " + typecontenue + "\r\n").getBytes());
            out.write(("Content-Length: " + contenue.length + "\r\n").getBytes());
            out.write(("\r\n").getBytes());
            out.write(contenue);
            out.flush();

            System.out.println("Réponse envoyée.");
            logger.logAccess("Requête " + methode + " pour " + page + " de " + clientIP + " avec succès.");
        } catch (Exception e) {
            e.printStackTrace();
            logger.logError("Erreur lors de la gestion de la requête : " + e.getMessage());
        }
    }

    private boolean checkip(String clientIP) {
        for (String rejectedIP : config.getReject()) {
            if (clientIP.startsWith(rejectedIP)) {
                return false;
            }
        }
        for (String acceptedIP : config.getAccept()) {
            if (clientIP.startsWith(acceptedIP)) {
                return true;
            }
        }
        return config.getAccept().isEmpty();
    }

    private void sendError(OutputStream out, int code, String message) throws IOException {
        String response = "HTTP/1.1 " + code + " " + message + "\r\n\r\n" + message;
        out.write(response.getBytes());
        out.flush();
    }
}
