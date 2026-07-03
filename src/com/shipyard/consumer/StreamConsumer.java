package com.shipyard.consumer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import com.shipyard.model.ShippingLoad;
import com.shipyard.util.Json;

//reusable consumer that drains one processing stream (queue).
// Each instance gets its own channel because channels arent thread-safe and RabbitMQ dispatches deliveries for a channel on its own thread
public class StreamConsumer {
    private final Channel channel;
    private final String queue;
    private final String label;
    private final long simulatedWorkMillis;

    public StreamConsumer(Connection connection, String queue, String label, long simulatedWorkMillis) throws Exception{
        this.channel = connection.createChannel();
        this.queue = queue;
        this.label = label;
        this.simulatedWorkMillis = simulatedWorkMillis;
        this.channel.basicQos(1); //fair dispatch - never buffer > 1 unacked msg/worker
    }

    public void start() throws Exception{
        DeliverCallback onMessage = (consumerTag, delivery) -> {
            long tag = delivery.getEnvelope().getDeliveryTag();
            String routingKey = delivery.getEnvelope().getRoutingKey();
            ShippingLoad load = Json.from(delivery.getBody(), ShippingLoad.class);

            System.out.printf(" [%s] received via '%s' : %s%n", label, routingKey, load.summary());

            try{
                Thread.sleep(simulatedWorkMillis); //pretense of real work
            } catch (InterruptedException e){
                Thread.currentThread().interrupt();
            }

            channel.basicAck(tag, false); //tells broker to drop message
        };

        channel.basicConsume(queue, false, onMessage, consumerTag -> {});
        System.out.printf(" [%s] listening on '%s'%n", label, queue);
    }
}
