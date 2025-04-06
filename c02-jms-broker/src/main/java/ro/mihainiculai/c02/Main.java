package ro.mihainiculai.c02;

import org.apache.activemq.broker.BrokerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String BROKER_IP = "0.0.0.0";
    private static final String BROKER_PORT = "61616";

    public static void main(String[] args) {
        BrokerService broker = new BrokerService();
        try {
            String connectorUri = "tcp://" + BROKER_IP + ":" + BROKER_PORT;
            broker.addConnector(connectorUri);
            broker.start();
            logger.info("Broker started at {}", connectorUri);
        } catch (Exception exception) {
            logger.error("Failed to start the broker: {}", exception.getMessage(), exception);
            System.exit(1);
        }

        // Keep the application running indefinitely
        while (true) {
            try {
                synchronized (Main.class) {
                    Main.class.wait();
                }
            } catch (InterruptedException interruptedException) {
                logger.error("Application interrupted: {}", interruptedException.getMessage());
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
