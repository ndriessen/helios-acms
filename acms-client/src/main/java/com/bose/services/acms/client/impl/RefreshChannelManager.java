package com.bose.services.acms.client.impl;

import com.bose.services.acms.api.ConfigurationClientException;
import com.bose.services.acms.api.ConfigurationRefreshListener;
import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * Connects to the Spring Bus AMQP channel to listen for configuration refresh events.
 */
public class RefreshChannelManager {
    private static final Logger logger = LoggerFactory.getLogger(RefreshChannelManager.class);
    private static final String AMQP_REFRESH_CHANNEL = "binder.springCloudBus";
    private static final String CONSUMER_TAG_PREFIX = "acms.client-";
    private Connection connection;
    private Channel channel;

    public void open() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setAutomaticRecoveryEnabled(true);
            //factory.setUri("amqp://userName:password@hostName:portNumber/virtualHost");
            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.basicConsume(AMQP_REFRESH_CHANNEL, true, getConsumerTag(), new DefaultConsumer(channel){
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    RefreshChannelManager.this.onEvent(getConfigurationNameFromEvent(body));
                }
            });
        } catch (Exception e) {
            throw new ConfigurationClientException("Error opening RabbitMQ channel to listen for REFRESH events.", e);
        }
    }

    protected String getConsumerTag() {
        //each instance needs a different consumer tag...
        return CONSUMER_TAG_PREFIX + hashCode();
    }

    protected String getConfigurationNameFromEvent(byte[] body) {
        logger.info("Received configuration REFRESH notification");
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

    protected void onEvent(String configurationName) {

    }
}
