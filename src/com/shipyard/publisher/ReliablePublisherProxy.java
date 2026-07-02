package com.shipyard.publisher;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

//Proxy pattern - proxy (protection + smart reference)
//implements same LoadPublisher interface as real publisher - controls and augments access to real subject without caller noticing it
//protection - refuses to publish empty payloads; smart reference - counts and logs every successful publish
public class ReliablePublisherProxy implements LoadPublisher{
    private final LoadPublisher delegate; //real subject
    private final AtomicLong publishedCount = new AtomicLong();

    public ReliablePublisherProxy(LoadPublisher delegate) {
        this.delegate = delegate;
    }

    @Override
    public void publish(String exchange, String routingKey, byte[] body) throws IOException {
        if(body == null || body.length == 0){
            throw new IllegalArgumentException("refusing to publishan en empty payload");
        }

        delegate.publish(exchange, routingKey, body);

        long n = publishedCount.incrementAndGet();
        System.out.printf("    [publisher-proxy] confirmed #%d -> %s%n", n, routingKey);
    }

    public long publishedCount(){
        return publishedCount.get();
    }
}
