package nw;

import com.sun.net.httpserver.HttpServer;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import javax.swing.SwingUtilities;

import java.awt.*;

public class App {
    public static final int PORT = 17000; // which local port to connect to
    public static final int TRAY_DURATION = 5000; // in millis
    
    private static final URI NYLAS_IMAGE = URI.create("https://nylas-static-assets.s3.us-west-2.amazonaws.com/nylas-logo-300x300.png");
    private static final SystemTray TRAY; // the system tray

    public static void main(String[] args) throws IOException {
        // create a local server that can receive info
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/", new Handler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server started on http://localhost:%d".formatted(PORT));
    }

    private static void createNotification(String title, String text) {
        // simple custom image & auditory cue
        if (title.contains("yippee") || text.contains("yippee"))
            yippee();

        Image image;
        try {
            image = Toolkit.getDefaultToolkit().createImage(URL.of(NYLAS_IMAGE, null));
        } catch (MalformedURLException e) {
            throw new RuntimeException("Malformed static Nylas icon image URL provided.");
        }
        TrayIcon trayIcon = new TrayIcon(image, "Nylas Desktop Notification");

        // let Java manage the tray icon's size
        trayIcon.setImageAutoSize(true);

        try {
            // add the tray icon to the tray
            TRAY.add(trayIcon);
        } catch (AWTException e) {
            e.printStackTrace();
            return;
        }

        // show the message
        trayIcon.displayMessage(title, text, TrayIcon.MessageType.INFO);

        // remove notification from the tray after TRAY_DURATION has elapsed
        new Thread(() -> {
            try {
                Thread.sleep(TRAY_DURATION);
            } catch (InterruptedException e) {
                System.err.println("Notification display interrupted, deleting notification...");
            } finally {
                TRAY.remove(trayIcon);
            }
        }).start();
    }

    private static void yippee() {
        // show yippee
        SwingUtilities.invokeLater(YippeeDisplay::new);
    }

    static class Handler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String ret = null;
            
            switch (method) {
                case "GET":
                    System.out.println(method);
                    // handle nylas's challenge stuff
                    String query = exchange.getRequestURI().getQuery();
                    System.out.println(query);

                    if (query != null) {
                        String[] pair = query.split("=", 2);
                        if (pair[0].equals("challenge"))
                            ret = pair[1]; // return the challenge string
                    }
                    break;
                case "POST":
                    // webhook event
                    String json = new String(exchange.getRequestBody().readAllBytes());

                    // decode json body
                    JsonObject jobj = new Gson().fromJson(json, JsonObject.class);

                    // get event type
                    String event = jobj.get("type").getAsString();
                    
                    System.out.println(method + " " + event);

                    // handle the data for each event to display a notification
                    switch (event) {
                        case "message.created": // received email
                            JsonObject obj = jobj.get("data").getAsJsonObject()
                                    .get("object").getAsJsonObject();

                            createNotification(obj.get("subject").getAsString(),
                                    obj.get("snippet").getAsString());
                            break;
                        case "message.updated": // email had its data updated (set to read, flagged, etc)
                            break;
                        default: // unknown event
                            System.out.println(json);
                            break;
                    }
                    break;
                default:
                    // print basic info
                    System.out.println(method);
                    System.out.println(exchange);
                    break;
            }
            
            if (ret != null) {
                // send return value if we have something to say
                exchange.getResponseHeaders().add("Content-Type", "text/plain");
                exchange.sendResponseHeaders(200, ret.getBytes().length);
                OutputStream stream = exchange.getResponseBody();
                stream.write(ret.getBytes());
                stream.close();
            }
        }
    }

    static {
        // load the system tray
        if (SystemTray.isSupported()) {
            TRAY = SystemTray.getSystemTray();
        } else {
            TRAY = null;
            System.out.println("SystemTray is not supported on this platform.");
        }
    }
}