package com.shipyard.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

//JSON is chosen here as payload fomat - convert ShippingLoad objects to/from bytes here
public final class Json {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public Json() {
    }

    public static byte[] toBytes(Object value){
        try {
            return MAPPER.writeValueAsBytes(value);
        } catch (Exception e){
            throw new RuntimeException("Failed to serialize message", e);
        }
    }

    public static <T> T from(byte[] body, Class<T> type){
        try{
            return MAPPER.readValue(body, type);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize message", e);
        }
    }
}
