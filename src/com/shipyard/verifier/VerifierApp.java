package com.shipyard.verifier;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import com.shipyard.config.RabbitConnection;
import com.shipyard.config.Topology;
import com.shipyard.model.ShippingLoad;
import com.shipyard.publisher.ChannelLoadPublisher;
import com.shipyard.publisher.LoadPublisher;
import com.shipyard.publisher.ReliablePublisherProxy;
import com.shipyard.util.Json;

import java.util.concurrent.CountDownLatch;

//Consumer+Producer - Brain of the system
//reads raw loads from verification queue and publishes classified loads to topic exchange
//1. Manual Acknowledgement - only acks after the load is routed. mid-processing crash means broker redelivers message instead of losing it
//2. Negative Acknowledgement + Dead Letter Exchange - invalid loads are nacked with requeue=false, dead lettering them to stream.rejected
//Design Patterns:
//1. Chain of Responsibility + Strategy live inside
//2. Proxy - publish through LoadPublisher, which is a ReliablePublisherProxy wrapping a ChannelLoadPublisher
public class VerifierApp {
    private static final LoadVerifier VERIFIER = new LoadVerifier();

    public static void main(String[] args) throws Exception{
        Connection connection = RabbitConnection.open("verifier");
        Channel channel = connection.createChannel();

        Topology.declare(channel);
        channel.basicQos(1);
        
        //Proxy pattern - caller depends only on LoadPublisher interface
        // proxy transparently adds persistence guarantees, payload validation and publish bookkeeping
        LoadPublisher publisher = new ReliablePublisherProxy(new ChannelLoadPublisher(channel));

        System.out.println("[verifier] waiting for loads on '" + Topology.VERIFICATION_QUEUE + "' ...");

        DeliverCallback onLoad = (consumerTag, delivery) -> {
            long tag = delivery.getEnvelope().getDeliveryTag();
            try{
                ShippingLoad load = Json.from(delivery.getBody(), ShippingLoad.class);
                RoutingDecision decision = VERIFIER.verify(load);
                
                if(decision.isAccepted()){
                    publisher.publish(Topology.STREAM_EXCHANGE, decision.routingKey(), delivery.getBody());
                    System.out.printf("[verifier] ACCEPTED %-22s -> %s%n", decision.routingKey(), load.summary());
                    channel.basicAck(tag, false);
                } else {
                    System.out.printf("[verifier] REJECTED (%s)%n", decision.reason());
                    channel.basicNack(tag, false, false); //requeue=false -> message is dead-lettered to stream.rejected
                }
            } catch (Exception parseError){
                System.out.println("[verifier] REJECTED (unreadable payload): " + parseError.getMessage());
                channel.basicNack(tag, false, false);
            }
        };
        channel.basicConsume(Topology.VERIFICATION_QUEUE, false, onLoad,consumerTag -> { });
        awaitShutdown(connection);
    }

    private static void awaitShutdown(Connection connection) throws InterruptedException{
        CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[verifier] shutting down ...");
            try {
                connection.close();
            } catch (Exception ignored){
            }
            latch.countDown();
        }));
        latch.await();
    }
}
