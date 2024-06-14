package serveur;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Base64;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;

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
                contenu = gererHTML(contenu);
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


    private byte[] gererHTML(byte[] contenu)  {
        try {
            String s_contenue = new String(contenu);
            s_contenue =  s_contenue.replaceAll("«", "\"").replaceAll("»", "\"");
            contenu = s_contenue.getBytes();
            // Créer un constructeur de documents
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            // Parser le HTML
            Document doc = builder.parse(new ByteArrayInputStream(contenu));

            // Sélectionner toutes les balises <code>
            NodeList codeTags = doc.getElementsByTagName("code");

            // Parcourir chaque balise <code>
            for (int i = 0; i < codeTags.getLength(); i++) {
                Element codeTag = (Element) codeTags.item(i);

                // Obtenir l'attribut interpreteur
                String interpreteur = codeTag.getAttribute("interpreteur");

                // Obtenir le contenu de la balise <code>
                String code = codeTag.getTextContent().trim();

                // Afficher l'interpreteur et le code
                System.out.println("Interpreteur: " + interpreteur);
                System.out.println("Code:");
                System.out.println(code);
                System.out.println("--------------------");
                System.out.println(lancercode(interpreteur,code));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return contenu;
    }

    private String lancercode(String interpreteur, String code) {
        StringBuilder output = new StringBuilder();

        try {
            ProcessBuilder processBuilder = new ProcessBuilder();

            if (interpreteur.equals("/bin/bash")) {
                // Exécution de code bash
                processBuilder.command("bash", "-c", code);

            } else if (interpreteur.equals("/usr/bin/python")) {
                // Exécution de code Python
                processBuilder.command("python", "-c", code);

            } else {
                // Gérer d'autres interpréteurs si nécessaire
                System.out.println("Interpreteur non pris en charge: " + interpreteur);
                return "";
            }

            // Définir le répertoire de travail du processus (facultatif, dépend de votre besoin)
            // Par exemple, définir le répertoire du script à exécuter
            if (!System.getProperty("os.name").toLowerCase().contains("win")) { // pour nos tests sur windows
                processBuilder.directory(new File(interpreteur));
            }


            // Démarrer le processus et récupérer la sortie
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // Attendre la fin de l'exécution du processus
            int exitCode = process.waitFor();
            System.out.println("Exit code: " + exitCode);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return output.toString();
    }

}
