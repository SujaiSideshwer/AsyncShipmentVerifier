package com.shipyard.consumer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.shipyard.config.RabbitConnection;
import com.shipyard.config.Topology;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

//Boots up every downstream processing stream at once so you can watch loads fan out across the warehouse.
public class WarehouseApp {
    public static void main(String[] args) throws Exception {
        Connection connection = RabbitConnection.open("warehouse");

        try (Channel setup = connection.createChannel()){
            Topology.declare(setup);
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }

        System.out.println("[warehouse] starting processing streams ...");

        //each stream - independent worker on its own channel
        new StreamConsumer(connection, Topology.HAZMAT_QUEUE, "HAZMAT ", 400).start();
        new StreamConsumer(connection, Topology.COLD_CHAIN_QUEUE, "COLD-CHAIN ", 300).start();
        new StreamConsumer(connection, Topology.SPECIAL_QUEUE, "SPECIAL ", 500).start();
        new StreamConsumer(connection, Topology.CUSTOMS_QUEUE, "CUSTOMS ", 600).start();
        new StreamConsumer(connection, Topology.STANDARD_QUEUE, "STANDARD ", 200).start();
        new StreamConsumer(connection, Topology.URGENT_QUEUE, "URGENT ", 100).start();

        new StreamConsumer(connection, Topology.REJECTED_QUEUE, "REJECTED ", 100).start();

        System.out.println("[warehouse] all streams live. Press Ctrl+C to stop.\n");
        awaitShutdown(connection);
    }

    private static void awaitShutdown(Connection connection) throws InterruptedException{
        CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(()-> {
            System.out.println("\n[warehouse] shutting down...");
            try{
                connection.close();
            } catch (Exception ignored){
            }
            latch.countDown();
        }));
        latch.await();
    }
}
