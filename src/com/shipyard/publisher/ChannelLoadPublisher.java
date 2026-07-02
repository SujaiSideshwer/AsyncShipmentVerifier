package com.shipyard.publisher;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;

import java.io.IOException;

//Proxy - RealSubject
//Publishes message to RabbitMQ - doesnt concern with logging, metrics or validation - those are taken care of by the proxy
public class ChannelLoadPublisher implements LoadPublisher{
    private final Channel channel;

    public ChannelLoadPublisher(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void publish(String exchange, String routingKey, byte[] body) throws IOException {
        AMQP.BasicProperties persistentJson = new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .deliveryMode(2) //2=persistent
                .build();
        channel.basicPublish(exchange, routingKey, persistentJson, body);
    }
}
