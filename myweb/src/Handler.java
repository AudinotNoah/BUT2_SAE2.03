package serveur;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;

public class Handler implements Runnable {
    private Socket clientSocket;
    private String rootDir;

    public Handler(Socket clientSocket, String rootDir) {
        this.clientSocket = clientSocket;
        this.rootDir = rootDir;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream out = clientSocket.getOutputStream()) {

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

            File file = new File(rootDir + "/" + page);
            if (!file.exists()) {
                sendError(out, 404, "Le fichier n'existe pas");
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendError(OutputStream out, int code, String message) throws IOException {
        String response = "HTTP/1.1 " + code + " " + message + "\r\n\r\n" + message;
        out.write(response.getBytes());
        out.flush();
    }
}
