package com.shipyard.model;

//message payload travels through the system - load enters as unclassified shipment; verifier reads these flags and decide to send load to
public class ShippingLoad {
    private String loadId;
    private String description;
    private boolean weightKg;
    private boolean hazardous;
    private boolean requiresRefrigeration;
    private boolean international;
    private boolean urgent;
    private String origin;
    private String destination;

    public ShippingLoad() {
    }

    public ShippingLoad(String loadId, String description, boolean weightKg, boolean hazardous, boolean requiresRefrigeration, boolean international, boolean urgent, String origin, String destination) {
        this.loadId = loadId;
        this.description = description;
        this.weightKg = weightKg;
        this.hazardous = hazardous;
        this.requiresRefrigeration = requiresRefrigeration;
        this.international = international;
        this.urgent = urgent;
        this.origin = origin;
        this.destination = destination;
    }

    public String getLoadId() {
        return loadId;
    }

    public void setLoadId(String loadId) {
        this.loadId = loadId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isWeightKg() {
        return weightKg;
    }

    public void setWeightKg(boolean weightKg) {
        this.weightKg = weightKg;
    }

    public boolean isHazardous() {
        return hazardous;
    }

    public void setHazardous(boolean hazardous) {
        this.hazardous = hazardous;
    }

    public boolean isRequiresRefrigeration() {
        return requiresRefrigeration;
    }

    public void setRequiresRefrigeration(boolean requiresRefrigeration) {
        this.requiresRefrigeration = requiresRefrigeration;
    }

    public boolean isInternational() {
        return international;
    }

    public void setInternational(boolean international) {
        this.international = international;
    }

    public boolean isUrgent() {
        return urgent;
    }

    public void setUrgent(boolean urgent) {
        this.urgent = urgent;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String summary(){
        return "%s | %s | %.0fkg | %s -> %s".formatted(loadId, description, weightKg, origin, destination);
    }
}
