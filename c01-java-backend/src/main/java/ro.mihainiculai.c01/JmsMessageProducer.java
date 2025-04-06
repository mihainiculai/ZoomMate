package ro.mihainiculai.c01;

import jakarta.jms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Base64;
import java.util.Properties;

public class JmsMessageProducer implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(JmsMessageProducer.class);
    private static final String INITIAL_CONTEXT_FACTORY = "org.apache.activemq.jndi.ActiveMQInitialContextFactory";
    private static final String CONNECTION_FACTORY_JNDI = "ConnectionFactory";

    private final Connection connection;
    private final Session session;
    private final MessageProducer producer;

    public JmsMessageProducer(String host, String port, String topicName) throws NamingException, JMSException {
        Context jndiContext = createJndiContext(host, port);
        ConnectionFactory connectionFactory = lookupConnectionFactory(jndiContext);
        connection = connectionFactory.createConnection();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination = session.createTopic(topicName);
        producer = session.createProducer(destination);
        connection.start();

        logger.info("JMS Producer initialized and connected to topic '{}'", topicName);
    }

    private Context createJndiContext(String host, String port) throws NamingException {
        Properties props = new Properties();
        props.setProperty(Context.INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY);
        props.setProperty(Context.PROVIDER_URL, "tcp://" + host + ":" + port);
        return new InitialContext(props);
    }

    private ConnectionFactory lookupConnectionFactory(Context jndiContext) throws NamingException {
        return (ConnectionFactory) jndiContext.lookup(CONNECTION_FACTORY_JNDI);
    }

    public void sendMessage(String jobId, double zoomLevel, byte[] imageBytes) throws JMSException {
        TextMessage message = session.createTextMessage();

        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String content = String.format(
                "{\n  \"jobId\": \"%s\",\n  \"zoomLevel\": %.2f,\n  \"imageBase64\": \"%s\"\n}",
                jobId, zoomLevel, base64Image
        );

        message.setText(content);
        producer.send(message);

        logger.info("Sent JMS message for jobId='{}' with zoomLevel={}", jobId, zoomLevel);
    }

    @Override
    public void close() {
        try {
            if (producer != null) {
                producer.close();
                logger.debug("JMS MessageProducer closed.");
            }
            if (session != null) {
                session.close();
                logger.debug("JMS Session closed.");
            }
            if (connection != null) {
                connection.close();
                logger.debug("JMS Connection closed.");
            }
        } catch (JMSException e) {
            logger.error("Error closing JMS resources: {}", e.getMessage(), e);
        }
    }
}
