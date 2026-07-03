package com.shipyard.setup;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.shipyard.config.RabbitConnection;
import com.shipyard.config.Topology;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class SetupTopologyApp {
    public static void main(String[] args) throws Exception{
        try(Connection connection = RabbitConnection.open("setup");
            Channel channel = connection.createChannel()){
            Topology.declare(channel);
            System.out.println("[setup] topology declared. Open http://localhost:15672 (guest/guest)");
        }
    }
}
