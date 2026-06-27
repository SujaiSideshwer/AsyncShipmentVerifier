package com.shipyard.model;

//nature of shipping load - determines its category and which topic it gets routed to
public enum LoadCategory {
    HAZMAT, //dangerous goods - chemicals, batteries
    REFRIGERATED, //cold-chain - food, pharma
    OVERSIZED, //heavy freight
    INTERNATIONAL, //cross-border shipments
    STANDARD
}
