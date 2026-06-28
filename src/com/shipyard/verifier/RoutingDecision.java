package com.shipyard.verifier;

import com.shipyard.model.LoadCategory;
import com.shipyard.model.Priority;

//outcome of inspecting a load - its accepted(gets topic routing key)/rejected(dead-lettered with reason)
public final class RoutingDecision {
    private final boolean accepted;
    private final String routingKey;
    private final String reason;

    public RoutingDecision(boolean accepted, String routingKey, String reason) {
        this.accepted = accepted;
        this.routingKey = routingKey;
        this.reason = reason;
    }

    public static RoutingDecision route(LoadCategory category, Priority priority){
        String key = "load.%s.%s".formatted(category.name().toLowerCase(),
                priority.name().toLowerCase());
        return new RoutingDecision(true, key, null);
    }

    public static RoutingDecision reject(String reason){
        return new RoutingDecision(false, null, reason);
    }

    public boolean isAccepted(){return accepted;}
    public String routingKey(){return routingKey;}
    public String reason(){return reason;}
}
