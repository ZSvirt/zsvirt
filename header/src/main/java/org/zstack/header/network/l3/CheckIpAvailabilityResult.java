package org.zstack.header.network.l3;


/**
 * Created by frank on 1/21/2016.
 */
public class CheckIpAvailabilityResult {
    private boolean available = true;
    private String reason;

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
