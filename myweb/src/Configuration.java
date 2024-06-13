package serveur;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;

public class Configuration {
    private int port = 80;
    private String rootDir = "site";
    private final ArrayList<String> accept = new ArrayList<String>();
    private final ArrayList<String> reject = new ArrayList<String>();

    public Configuration() {
        lireConfiguration();
    }

    private void lireConfiguration() {
        try {
            File configFile;
            if (System.getProperty("os.name").toLowerCase().contains("win") ){ // pour nos tests sur windows
                configFile = new File("./etc/myweb/myweb.conf");
            }
            else{
                configFile = new File("/etc/myweb/myweb.conf");
            }
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(configFile);
            doc.getDocumentElement().normalize();
            Element root = (Element) doc.getElementsByTagName("webconf").item(0);
            NodeList liste = root.getChildNodes();
            for (int i = 0; i < liste.getLength(); i++) {
                Node node = liste.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element enfant = (Element) node;
                    String tag = enfant.getTagName();
                    String contenu = enfant.getTextContent();
                    switch (tag) {
                        case "port":
                            port = Integer.parseInt(contenu);
                            break;
                        case "root":
                            rootDir = contenu;
                            break;
                        case "accept":
                            for (String s : contenu.split("\n")) {
                                if (s.contains("."))
                                    accept.add(s.trim());
                            }
                            break;
                        case "reject":
                            for (String s : contenu.split("\n")) {
                                if (s.contains("."))
                                    reject.add(s.trim());
                            }
                            break;
                        default:
                            System.out.println("Element inconnu " + tag + ": " + contenu);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Erreur de lecture du fichier de configuration, utilisation des valeurs par défaut : port=" + port + ", rootDir=" + rootDir);
        }
    }

    public int getPort() {
        return port;
    }

    public String getRootDir() {
        return rootDir;
    }

    public ArrayList<String> getAccept() {
        return accept;
    }
    public ArrayList<String> getReject() {
        return reject;
    }

    public String toString(){
        StringBuilder sb  = new StringBuilder();
        sb.append("Configuration actuelle : ");
        sb.append("\n   port=").append(port);
        sb.append("\n   rootDir=").append(rootDir);
        sb.append("\n   accept=").append(accept);
        sb.append("\n   reject=").append(reject);
        return sb.toString();
    }
}
