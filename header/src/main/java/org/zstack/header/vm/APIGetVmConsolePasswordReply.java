package org.zstack.header.vm;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.io.Serializable;


/**
 * Created by root on 7/29/16.
 */

@RestResponse(fieldsTo = {"password=consolePassword"})
public class APIGetVmConsolePasswordReply extends APIReply implements Serializable {
    @NoLogging
    private String consolePassword;

    public String getConsolePassword() {
        return consolePassword;
    }

    public void setConsolePassword(String consolePassword) {
        this.consolePassword = consolePassword;
    }

    public static APIGetVmConsolePasswordReply __example__() {
        APIGetVmConsolePasswordReply reply = new APIGetVmConsolePasswordReply();
        reply.consolePassword = "password";
        return reply;
    }
}

