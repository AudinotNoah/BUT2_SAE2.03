package serveur;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class HttpServer {
    private final Configuration config;
    private final Logger logger;

    public HttpServer() {
        config = new Configuration();
        logger = new Logger(config.getAccessLogPath(), config.getErrorLogPath());
        System.out.println(config.toString());
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(config.getPort())) {
            System.out.println("En attente d'une connexion sur le port " + config.getPort() + "...");
            System.out.println("http://127.0.0.1:" + config.getPort());

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Client connecté depuis l'ip : " + clientSocket.getInetAddress().getHostAddress());

                    Handler handler = new Handler(clientSocket, config, logger);
                    new Thread(handler).start();
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.logError("Erreur lors de la gestion de la connexion : " + e.getMessage());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.logError("Erreur lors du démarrage du serveur : " + e.getMessage());
        }
    }
}
