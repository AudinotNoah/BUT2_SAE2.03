package serveur;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class HttpServer {
    private final Configuration config;

    public HttpServer() {
        config = new Configuration();
        System.out.println(config.toString());
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(config.getPort())) {
            System.out.println("En attente d'une connexion sur le port " + config.getPort() + "...");
            System.out.println("http://127.0.0.1:" + config.getPort());

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    Handler handler = new Handler(clientSocket, config.getRootDir());
                    new Thread(handler).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}