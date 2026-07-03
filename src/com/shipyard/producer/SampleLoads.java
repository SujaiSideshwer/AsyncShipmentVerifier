package com.shipyard.producer;

import com.shipyard.model.ShippingLoad;

import java.util.List;

public final class SampleLoads {

    private SampleLoads(){
    }

    public static List<ShippingLoad> demoBatch(){
        return List.of(
                //hazardous, urgent -> hazmat, urgent-monitor
                new ShippingLoad("LD-1001", "Lithium batter", 850, true, false, false, true, "Chennai", "Mumbai"),
                //refrigerated, standard -> cold chain
                new ShippingLoad("LD-1002", "Frozen seafood", 1200, false, true, false, false, "Kochi", "Delhi"),
                //heavy -> oversized/special handling
                new ShippingLoad("LD-1003", "Industrial turbine", 18500, false, false, false, false, "Pune", "Bangalore")
        );
    }
}
