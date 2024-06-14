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
                sendStatus(out);
                return;
            }

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

            byte[] contenu = Files.readAllBytes(file.toPath());

            String typeContenu;
            boolean isBase64 = false;
            if (page.endsWith(".html")) {
                typeContenu = "text/html";
            } else if (page.endsWith(".jpg") || page.endsWith(".jpeg")) {
                typeContenu = "image/jpeg";
                isBase64 = true;
            } else if (page.endsWith(".gif")) {
                typeContenu = "image/gif";
                isBase64 = true;
            } else if (page.endsWith(".png")) {
                typeContenu = "image/png";
                isBase64 = true;
            } else if (page.endsWith(".mp3")) {
                typeContenu = "audio/mpeg";
                isBase64 = true;
            } else if (page.endsWith(".wav")) {
                typeContenu = "audio/wav";
                isBase64 = true;
            } else if (page.endsWith(".mp4")) {
                typeContenu = "video/mp4";
                isBase64 = true;
            } else {
                sendError(out, 400, "Unsupported Media Type");
                return;
            }

            if (isBase64) {
                String encoded = Base64.getEncoder().encodeToString(contenu);
                out.write(("HTTP/1.1 200 OK\r\n").getBytes());
                out.write(("Content-Type: " + typeContenu + "\r\n").getBytes());
                out.write(("Content-Transfer-Encoding: base64\r\n").getBytes());
                out.write(("Content-Length: " + encoded.length() + "\r\n").getBytes());
                out.write(("\r\n").getBytes());
                out.write(encoded.getBytes());
                out.flush();
            } else {
                out.write(("HTTP/1.1 200 OK\r\n").getBytes());
                out.write(("Content-Type: " + typeContenu + "\r\n").getBytes());
                out.write(("Content-Length: " + contenu.length + "\r\n").getBytes());
                out.write(("\r\n").getBytes());
                out.write(contenu);
                out.flush();
            }

            System.out.println("Réponse envoyée.");
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

    private void sendStatus(OutputStream out) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        long freeMemory = runtime.freeMemory();
        long totalMemory = runtime.totalMemory();
        long usedMemory = totalMemory - freeMemory;

        File root = new File("/");
        long freeSpace = root.getFreeSpace();
        long usableSpace = root.getUsableSpace();

        int processCount = new File("/proc").listFiles().length;

        String status = "Free memory: " + freeMemory + " bytes\n" +
                "Used memory: " + usedMemory + " bytes\n" +
                "Free disk space: " + freeSpace + " bytes\n" +
                "Usable disk space: " + usableSpace + " bytes\n" +
                "Number of processes: " + processCount;

        out.write(("HTTP/1.1 200 OK\r\n").getBytes());
        out.write(("Content-Type: text/plain\r\n").getBytes());
        out.write(("Content-Length: " + status.length() + "\r\n").getBytes());
        out.write(("\r\n").getBytes());
        out.write(status.getBytes());
        out.flush();
    }
}
