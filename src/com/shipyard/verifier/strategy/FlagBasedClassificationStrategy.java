package com.shipyard.verifier.strategy;

import com.shipyard.model.LoadCategory;
import com.shipyard.model.ShippingLoad;

//Strategy pattern - concrete
//categorizes a load from its boolean flag and weight
public class FlagBasedClassificationStrategy implements ClassificationStrategy{

    //threshold for weight, beyond which special handling is required
    public static final double OVERSIZED_THRESHOLD_KG = 10000;

    @Override
    public LoadCategory categorize(ShippingLoad load) {
        if(load.isHazardous()){
            return LoadCategory.HAZMAT;
        }
        if(load.isRequiresRefrigeration()){
            return LoadCategory.REFRIGERATED;
        }
        if(load.getWeightKg() > OVERSIZED_THRESHOLD_KG){
            return LoadCategory.OVERSIZED;
        }
        if(load.isInternational()){
            return LoadCategory.INTERNATIONAL;
        }
        return LoadCategory.STANDARD;
    }
}
