package com.shipyard.producer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.shipyard.config.RabbitConnection;
import com.shipyard.config.Topology;
import com.shipyard.model.ShippingLoad;
import com.shipyard.publisher.ChannelLoadPublisher;
import com.shipyard.publisher.LoadPublisher;
import com.shipyard.publisher.ReliablePublisherProxy;
import com.shipyard.util.Json;

//Producer - represents loading dock; All incoming shipment - dropped to ingest exchange - then verifier diverts it to respective stream
public class DispatcherApp {
    public static void main(String[] args) throws Exception{
        try(Connection connection = RabbitConnection.open("dispatcher");
            Channel channel = connection.createChannel()){
            Topology.declare(channel);

            LoadPublisher publisher = new ReliablePublisherProxy(new ChannelLoadPublisher(channel));

            System.out.println("[dispatcher] sending loads to the verification stage... \n");

            for(ShippingLoad load: SampleLoads.demoBatch()){
                byte[] body = Json.toBytes(load);
                publisher.publish(Topology.INGEST_EXCHANGE, Topology.INGEST_ROUTING_KEY, body);
                System.out.println("    -> dispatched " + load.summary());
                Thread.sleep(250);
            }

            System.out.println("\n[dispatcher] done. Check verifier and warehouse terminals.");
        }

    }
}
