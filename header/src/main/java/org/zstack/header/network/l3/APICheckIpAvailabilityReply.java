package org.zstack.header.network.l3;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 1/21/2016.
 */
@RestResponse(fieldsTo = {"all"})
public class APICheckIpAvailabilityReply extends APIReply {
    private boolean available;
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

    public static APICheckIpAvailabilityReply __example__() {
        APICheckIpAvailabilityReply reply = new APICheckIpAvailabilityReply();
        reply.setAvailable(true);
        return reply;
    }

}
