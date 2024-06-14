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
        BufferedReader in = null;
        OutputStream out = null;

        try {
            InetAddress clientAddress = clientSocket.getInetAddress();
            String clientIP = clientAddress.getHostAddress();

            if (!checkip(clientIP)) {
                sendError(clientSocket.getOutputStream(), 403, "Forbidden: Access is denied");
                logger.logAccess("Requête refusée de " + clientIP);
                return;
            }

            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = clientSocket.getOutputStream();

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
            if (!file.exists() || !file.isFile()) {
                sendError(out, 404, "Not Found");
                logger.logError("Fichier non trouvé pour la requête de " + clientIP + " : " + page);
                return;
            }

            byte[] content = Files.readAllBytes(file.toPath());

            // Convertir le contenu en chaîne de caractères UTF-8
            String contentStr = new String(content, "UTF-8");

            // Traiter les balises <code> avec attribut interpreteur
            contentStr = processInterpreteurTags(contentStr);

            // Envoyer la réponse HTTP
            sendHttpResponse(out, 200, contentStr);

            logger.logAccess("Requête " + methode + " pour " + page + " de " + clientIP + " avec succès.");
        } catch (IOException e) {
            e.printStackTrace();
            logger.logError("Erreur lors de la gestion de la requête : " + e.getMessage());
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String executeCode(String interpreteur, String code) {
        StringBuilder output = new StringBuilder();
        try {
            ProcessBuilder builder = new ProcessBuilder(interpreteur);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            writer.write(code);
            writer.newLine();
            writer.flush();
            writer.close();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            reader.close();

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                output.append("Erreur lors de l'exécution du code : Exit code ").append(exitCode);
            }
        } catch (IOException | InterruptedException e) {
            output.append("Erreur lors de l'exécution du code : ").append(e.getMessage());
        }
        return output.toString();
    }

    private String processInterpreteurTags(String content) {
        StringBuilder processedContent = new StringBuilder();
        int currentIndex = 0;

        while (currentIndex < content.length()) {
            int startIndex = content.indexOf("<code interpreteur=\"", currentIndex);
            if (startIndex == -1) {
                // Aucune balise <code> avec attribut interpreteur trouvée
                processedContent.append(content.substring(currentIndex));
                break;
            }

            int endIndex = content.indexOf("</code>", startIndex);
            if (endIndex == -1) {
                // Balise de fin </code> manquante, traiter jusqu'à la fin du contenu
                processedContent.append(content.substring(currentIndex));
                break;
            }

            // Ajouter le contenu avant la balise <code>
            processedContent.append(content, currentIndex, startIndex);

            // Extraire l'attribut interpreteur
            int interpreteurStartIndex = startIndex + "<code interpreteur=\"".length();
            int interpreteurEndIndex = content.indexOf("\"", interpreteurStartIndex);
            if (interpreteurEndIndex == -1) {
                // Attribut interpreteur mal formé, sauter cette balise
                processedContent.append(content, startIndex, endIndex + "</code>".length());
            } else {
                String interpreteur = content.substring(interpreteurStartIndex, interpreteurEndIndex);
                String codeToExecute = content.substring(endIndex + "</code>".length(), endIndex); // Récupérer le contenu entre les balises

                // Exécuter le code selon l'interpreteur spécifié
                String result = executeCode(interpreteur, codeToExecute.trim());

                // Remplacer la balise <code> par le résultat de l'exécution
                processedContent.append(result);
            }

            currentIndex = endIndex + "</code>".length();
        }

        return processedContent.toString();
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

    private void sendHttpResponse(OutputStream out, int statusCode, String content) throws IOException {
        String response = "HTTP/1.1 " + statusCode + " OK\r\n"
                + "Content-Type: text/html\r\n"
                + "Content-Length: " + content.getBytes("UTF-8").length + "\r\n"
                + "\r\n"
                + content;
        out.write(response.getBytes("UTF-8"));
        out.flush();
    }

}
