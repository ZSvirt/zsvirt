package org.zstack.header.identity;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 9:39 PM
 * To change this template use File | Settings | File Templates.
 */
@RestResponse(fieldsTo = {"valid=validSession"})
public class APIValidateSessionReply extends APIReply {
    private boolean validSession;

    public boolean isValidSession() {
        return validSession;
    }

    public void setValidSession(boolean validSession) {
        this.validSession = validSession;
    }
 
    public static APIValidateSessionReply __example__() {
        APIValidateSessionReply reply = new APIValidateSessionReply();
        reply.setValidSession(true);
        return reply;
    }

}
