package serveur;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;

public class Handler implements Runnable {
    private final Socket clientSocket;
    private final Configuration config;

    public Handler(Socket clientSocket, Configuration config) {
        this.clientSocket = clientSocket;
        this.config = config;
    }

    @Override
    public void run() {
        try {
            InetAddress clientAddress = clientSocket.getInetAddress();
            String clientIP = clientAddress.getHostAddress();

            if (!checkip(clientIP)) {
                sendError(clientSocket.getOutputStream(), 403, "Forbidden: Access is denied");
                clientSocket.close();
                return;
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            OutputStream out = clientSocket.getOutputStream();

            String ligne = in.readLine();
            String[] lignecoupe = ligne.split(" ");
            if (lignecoupe.length < 3) {
                sendError(out, 400, "Erreur requete");
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
                return;
            }

            out.write(("HTTP/1.1 200 OK\r\n").getBytes());
            out.write(("Content-Type: " + typecontenue + "\r\n").getBytes());
            out.write(("Content-Length: " + contenue.length + "\r\n").getBytes());
            out.write(("\r\n").getBytes());
            out.write(contenue);
            out.flush();

            System.out.println("Réponse envoyée.");
        }
        catch (Exception e) {
            e.printStackTrace();
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
