package com.shipyard.config;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

//broker topology - exchange, queue binding, routing keys live here
//declarations in rabbitmq are idempotent - hence, declaring something that already exists is not allowed
//hence, all apps on this project can safely call @Channel on startup and create topology- since others simply confirm to it
public final class Topology {
    public Topology() {
    }

    //Stage 1 - ingest + verify
    public static final String INGEST_EXCHANGE = "shipyard.ingest"; //producers drop raw unclassified loads here
    public static final String VERIFICATION_QUEUE = "load.verification"; //queue verifier reads from here
    public static final String INGEST_ROUTING_KEY = "incoming"; //routing key producers use to reach verification queue

    //dead letter - rejected loads from verifier end up here
    public static final String DLX_EXCHANGE = "shipyard.rejected";
    public static final String REJECTED_QUEUE = "stream.rejected";
    public static final String REJECTED_ROUTING_KEY = "rejected";

    //Stage 2 - routing to processing streams
    public static final String STREAM_EXCHANGE = "shipyard.streams";

    public static final String HAZMAT_QUEUE = "stream.hazmat";
    public static final String COLD_CHAIN_QUEUE = "stream.cold-chain";
    public static final String SPECIAL_QUEUE = "stream.special-handling";
    public static final String CUSTOMS_QUEUE = "stream.customs";
    public static final String STANDARD_QUEUE = "stream.standard";
    public static final String URGENT_QUEUE = "stream.urgent-monitor";

    //declares full topology
    public static void declare(Channel channel) throws IOException{
        declareDeadLetter(channel);
        declareIngest(channel);
        declareStreams(channel);
    }

    private static void declareStreams(Channel channel) throws IOException{
        //topic exchange routes with wildcards:
        channel.exchangeDeclare(STREAM_EXCHANGE, BuiltinExchangeType.TOPIC, true);

        bindStream(channel, HAZMAT_QUEUE, "load.hazmat.*");
        bindStream(channel, COLD_CHAIN_QUEUE, "load.refrigerated.*");
        bindStream(channel, SPECIAL_QUEUE, "load.oversized.*");
        bindStream(channel, CUSTOMS_QUEUE, "load.international.*");
        bindStream(channel, STANDARD_QUEUE, "load.standard.*");

        bindStream(channel, URGENT_QUEUE, "load.*.urgent");
    }

    private static void bindStream(Channel channel, String queue, String pattern) throws IOException{
        channel.queueDeclare(queue, true, false, false, null);
        channel.queueBind(queue, STREAM_EXCHANGE, pattern);
    }

    private static void declareIngest(Channel channel) throws IOException{
        channel.exchangeDeclare(INGEST_EXCHANGE, BuiltinExchangeType.DIRECT, true);

        //verification queue is wired to dead-letter into DLX; RabbitMQ automatically directs message to shipyard.rejected when verifier calls basicNack() on a bad load
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", REJECTED_ROUTING_KEY);

        channel.queueDeclare(VERIFICATION_QUEUE, true, false, false, args);
        channel.queueBind(VERIFICATION_QUEUE, INGEST_EXCHANGE, INGEST_ROUTING_KEY);
    }

    private static void declareDeadLetter(Channel channel) throws IOException{
        channel.exchangeDeclare(DLX_EXCHANGE, BuiltinExchangeType.DIRECT, true); //direct exchange acting as Dead Letter Exchange
        channel.queueDeclare(REJECTED_QUEUE, true, false, false, null); //durable=true, exclusive=false, auto-delete=false
        channel.queueBind(REJECTED_QUEUE, DLX_EXCHANGE, REJECTED_ROUTING_KEY);
    }


}
