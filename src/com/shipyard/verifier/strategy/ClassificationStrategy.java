package com.shipyard.verifier.strategy;

import com.shipyard.model.LoadCategory;
import com.shipyard.model.ShippingLoad;

//Strategy pattern - decides load's category (abstract)
//this interface decouples LoadVerifier from how categorization happens
public interface ClassificationStrategy {
    LoadCategory categorize(ShippingLoad load);
}
