package com.shipyard.verifier.check;

import com.shipyard.model.ShippingLoad;

import java.util.Optional;

//Chain of Responsibility - concrete handler
//rejects load that is without id, and guards the rest of chain against null load
public class LoadIdCheck extends LoadCheck{
    @Override
    protected Optional<String> inspect(ShippingLoad load) {
        if(load == null){
            return Optional.of("null load");
        }
        if(isBlank(load.getLoadId())){
            return Optional.of("missing loadId");
        }
        return Optional.empty();
    }
}
