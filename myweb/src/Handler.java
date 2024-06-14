package serveur;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Base64;

public class Handler implements Runnable {
    private final Socket clientSocket;
    private final Configuration config;

    private final Logger logger;

    public Handler(Socket clientSocket, Configuration config,Logger logger) {
        this.clientSocket = clientSocket;
        this.config = config;
        this.logger = logger;
    }

    @Override
    public void run() {
        try {
            InetAddress clientAddress = clientSocket.getInetAddress();
            String clientIP = clientAddress.getHostAddress();

            if (!checkIP(clientIP)) {
                sendError(clientSocket.getOutputStream(), 403, "Forbidden: Access is denied");
                clientSocket.close();
                return;
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            OutputStream out = clientSocket.getOutputStream();

            String ligne = in.readLine();
            String[] ligneCoupe = ligne.split(" ");

            if (ligneCoupe.length < 3) {
                sendError(out, 400, "Bad Request");
                return;
            }

            String methode = ligneCoupe[0];
            String page = ligneCoupe[1];
            String version = ligneCoupe[2];

            if (methode.equals("GET") && page.equals("/status")) {
                sendPage(out,Status.sendStatus(config));
                return;
            }

            if (page.equals("/")) {
                page = "/index.html";
            }

            System.out.println("Requete de type " + methode + " reçu\nPour acceder à la page " + page + " en version " + version);

            page = page.substring(1);

            File file = new File(config.getRootDir() + "/" + page);
            if (!file.exists()) {
                sendError(out, 404, "Not Found");
                return;
            }

            byte[] contenu = Files.readAllBytes(file.toPath());

            if(page.endsWith(".html")) {
                sendPage(out,contenu);
            }
            else{
                String pagehtml = "";
                String version64 = Base64.getEncoder().encodeToString(contenu);
                if (page.endsWith(".jpg") || page.endsWith(".jpeg")){
                    pagehtml = "<img src=\"data:image/jpeg;base64," + version64 + "\">";
                }  else if (page.endsWith(".gif")) {
                    pagehtml = "<img src=\"data:image/gif;base64," + version64 + "\">";
                } else if (page.endsWith(".png")) {
                    pagehtml = "<img src=\"data:image/png;base64," + version64 + "\">";
                } else if (page.endsWith(".mp3")) {
                    pagehtml = "<audio controls><source type=\"audio/mpeg\" src=\"data:audio/mpeg;base64," + version64 + "\"></audio>";
                } else if (page.endsWith(".wav")) {
                    pagehtml = "<audio controls><source type=\"audio/wav\" src=\"data:audio/wav;base64," + version64 + "\"></audio>";
                } else if (page.endsWith(".mp4")) {
                    pagehtml = "<video controls><source type=\"video/mp4\" src=\"data:video/mp4;base64," + version64 + "\"></video>";
                } else {
                    sendError(out, 415, "Unsupported Media Type");
                    return;
                }
                contenu = pagehtml.getBytes();
                sendPage(out,contenu);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean checkIP(String clientIP) {
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

    private void sendPage(OutputStream out,byte[] contenu ) throws IOException {
        out.write(("HTTP/1.1 200 OK\r\n").getBytes());
        out.write(("Content-Type: text/html\r\n").getBytes());
        out.write(("Content-Length: " + contenu.length + "\r\n").getBytes());
        out.write(("\r\n").getBytes());
        out.write(contenu);
        out.flush();
        System.out.println("Réponse envoyée.");
    }
}
