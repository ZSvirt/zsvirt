package org.zstack.header.query;

import org.zstack.header.message.APIReply;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class APIQueryReply extends APIReply {
    private Long total;

    public Long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
 
    public static APIQueryReply __example__() {
        APIQueryReply reply = new APIQueryReply();


        return reply;
    }

}
