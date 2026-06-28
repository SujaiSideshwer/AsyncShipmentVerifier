package com.shipyard.verifier.check;

import com.shipyard.model.ShippingLoad;

import java.util.Optional;

//Chain of Responsibility - concrete handler
//checks destination of load
public class DestinationCheck extends LoadCheck{
    @Override
    protected Optional<String> inspect(ShippingLoad load) {
        if(isBlank(load.getDestination())){
            return Optional.of("missing destination for " + load.getLoadId());
        }
        return Optional.empty();
    }
}