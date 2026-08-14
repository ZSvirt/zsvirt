package org.zstack.zsv.telemetry.entity;

public class TelemetryConsentView {
    private String consentGrantedAt;

    public String getConsentGrantedAt() {
        return consentGrantedAt;
    }

    public void setConsentGrantedAt(String consentGrantedAt) {
        this.consentGrantedAt = consentGrantedAt;
    }

    public static TelemetryConsentView __example__() {
        TelemetryConsentView view = new TelemetryConsentView();
        view.setConsentGrantedAt("None");
        return view;
    }
}
