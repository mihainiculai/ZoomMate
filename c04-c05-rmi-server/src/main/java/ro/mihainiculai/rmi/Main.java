package ro.mihainiculai.rmi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static final String RMI_SERVER_HOST = "rmi://0.0.0.0:1099/ZOOM-SERVER";
    private static final int RMI_SERVER_PORT = 1099;

    public static void main(String[] args) {
        try {
            LocateRegistry.createRegistry(RMI_SERVER_PORT);

            ZoomImageImplementation zoomImageService = new ZoomImageImplementation();
            Naming.rebind(RMI_SERVER_HOST, zoomImageService);

            logger.info("RMI Server started, waiting for calls...");

            // Keep the application running indefinitely
            while (true) {
                try {
                    synchronized (Main.class) {
                        Main.class.wait();
                    }
                } catch (InterruptedException e) {
                    logger.error("Application interrupted: {}", e.getMessage());
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("RMI Server error: {}", e.getMessage());
        }
    }
}
