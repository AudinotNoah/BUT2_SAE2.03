import java.io.*;
import java.net.*;
import java.nio.file.*;

public class HttpServer {
    private static void envoie_erreur(OutputStream out, int code, String message) throws IOException {
        String response = "HTTP/1.1 " + code + " " + message + "\r\n\r\n" + message;
        out.write(response.getBytes());
        out.flush();
    }
    public static void main(String[] args) {
        int port;
        if (args.length != 1) {
            port = 80; 
            System.out.println("Port par défaut 80");
        } else {
            port = Integer.parseInt(args[0]);
        }

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("En attente d'une connexion sur le port " + port + "...");
            System.out.println("http://127.0.0.1:"+port);

            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    System.out.println("Requete en cours.");

                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    OutputStream out = clientSocket.getOutputStream();

                    

                    String ligne = in.readLine();

                    String[] lignecoupe = ligne.split(" ");
                    if (lignecoupe.length < 3) {
                        envoie_erreur(out, 400, "Erreur requete");
                        continue;
                    }

                    String methode = lignecoupe[0];
                    String page = lignecoupe[1];
                    String version = lignecoupe[2];

                    if (page.equals("/")) {
                        page = "/index.html"; 
                    }

                    System.out.println("Requete de type " + methode + " reçu\nPour acceder à la page "+page+"\nEn version "+version);



                    page = page.substring(1);

                    File file = new File("site/"+page);
                    if (!file.exists()) {
                        envoie_erreur(out, 404, "Le fichier n'existe pas");
                        continue;
                    }

                    byte[] contenue = Files.readAllBytes(file.toPath());


                    String typecontenue;
                    if (page.contains(".html")){
                        typecontenue = "text/html";
                    }
                    else if (page.contains(".jpg")){
                        typecontenue = "image/jpg";
                    }
                    else if (page.contains(".gif"))
                    {
                        typecontenue = "image/gif";
                    }
                    else{
                        envoie_erreur(out, 400, "Fichier d'un type inconnu");
                        continue;
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
