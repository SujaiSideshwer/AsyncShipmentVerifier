package com.shipyard.verifier.check;

import com.shipyard.model.ShippingLoad;

import java.util.Optional;

//Chain of Responsibility - concrete handler
//a non-positive weight signals corrupt data
public class WeightCheck extends LoadCheck{
    @Override
    protected Optional<String> inspect(ShippingLoad load) {
        if(load.getWeightKg() <= 0){
            return Optional.of("no-positive weight for " + load.getLoadId());
        }
        return Optional.empty();
    }
}
