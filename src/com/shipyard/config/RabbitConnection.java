package com.shipyard.config;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

//central place to open a connection to RabbitMQ broker
//@Connection is a real TCP connection to the broker and is expensive.
//@Channel is a lightweight virtual connection multiplexed over a single TCP connection
//Channels aren't thread-safe. Hence each consumer gets a channel here
public final class RabbitConnection {
    private RabbitConnection(){

    }

    public static Connection open(String appName) throws Exception{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(env("RABBITMQ_USER", "localhost"));
        factory.setPort(Integer.parseInt(env("RABBITMQ_PORT", "5672")));
        factory.setUsername(env("RABBITMQ_USER", "guest"));
        factory.setPassword(env("RABBITMQ_PASS", "guest"));
        factory.setVirtualHost(env("RABBITMQ_VHOST", "/"));

        //client transparently reconnects and redeclares topology
        factory.setAutomaticRecoveryEnabled(true);
        factory.setTopologyRecoveryEnabled(true);

        return factory.newConnection("shipyard-" + appName);
    }

    private static String env(String key, String fallback){
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
