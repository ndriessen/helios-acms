package com.bose.services.acms.client;

import com.bose.services.acms.api.ConfigurationClientException;
import com.bose.services.acms.api.ConfigurationRefreshListener;
import com.bose.services.acms.api.RefreshChannelProvider;
import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * Connects to the Spring Bus AMQP channel to listen for configuration refresh events.
 */
public class DefaultRefreshChannel implements RefreshChannelProvider {
    private static final Logger logger = LoggerFactory.getLogger(DefaultRefreshChannel.class);
    private static final String AMQP_REFRESH_CHANNEL = "binder.springCloudBus";
    private static final String CONSUMER_TAG_PREFIX = "acms.client-";
    private Connection connection;
    private Channel channel;

    private DefaultConfigurationClient client;

    public DefaultRefreshChannel(DefaultConfigurationClient client) {
        this.client = client;
    }

    @Override
    public void open() {
        try {
            //todo: make configurable
            ConnectionFactory factory = new ConnectionFactory();
            factory.setAutomaticRecoveryEnabled(true);
            //factory.setUri("amqp://userName:password@hostName:portNumber/virtualHost");
            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.basicConsume(AMQP_REFRESH_CHANNEL, true, getConsumerTag(), new DefaultConsumer(channel){
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String configName = getConfigurationNameFromEvent(body);
                    logger.info("Received configuration REFRESH event for " + configName);
                    DefaultRefreshChannel.this.onRefresh(configName);
                }
            });
        } catch (Exception e) {
            throw new ConfigurationClientException("Error opening RabbitMQ channel to listen for REFRESH events.", e);
        }
    }

    @Override
    public void onRefresh(String configurationName) {
        this.client.refresh(configurationName);
    }

    protected String getConsumerTag() {
        //each instance needs a different consumer tag...
        return CONSUMER_TAG_PREFIX + hashCode();
    }

    protected String getConfigurationNameFromEvent(byte[] body) {
        //Spring Cloud Bus is using Kryo to serialize the body. In AEM I couldn't get this to work (classloading issues)
        //this dirty workaround does the trick for now. We want to move away from AMQP anyway so we can also move to something easier payload wise (text based)
        String configurationName = null;
        try {
            StringBuilder name = new StringBuilder();
            //this should filter out all the unreadable chars...
            for (byte b : body) {
                if (b > '\u0001') {
                    name.append((char) b);
                }
            }
            //TODO: and for now, this gives us the name of the config... very BRITTLE!! needs fixing..
            configurationName = name.substring(0, name.indexOf(":"));
        } catch (Throwable e) {
            //because this is crappy code, make it as fail-safe as possible for now, if anything goes wrong, just refresh everything...
            logger.error("The very CRAPPY hack is causing errors, you might want to invest some time and fix this in a decent way", e);
            configurationName = null;
        }
        //ensure to default to everything...
        if (StringUtils.isEmpty(configurationName)) {
            configurationName = ConfigurationRefreshListener.REFRESH_ALL;
        }
        return configurationName;
    }

    @Override
    public void close() {
        try {
            if (channel != null) {
                channel.basicCancel(getConsumerTag());
                if (channel.isOpen()) {
                    channel.close();
                }
            }
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
        } catch (Exception e) {
            logger.warn("Error cleaning up consumer and connections...", e);
        }
    }
}
