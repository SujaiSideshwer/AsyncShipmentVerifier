package com.shipyard.publisher;

import java.io.IOException;

//Proxy Pattern - Subject
//real publisher and proxy implement this interface - hence callers depend on abstraction and cant tell which one theyre interacting with
public interface LoadPublisher {
    void publish(String exchange, String routingKey, byte[] body) throws IOException;
}
