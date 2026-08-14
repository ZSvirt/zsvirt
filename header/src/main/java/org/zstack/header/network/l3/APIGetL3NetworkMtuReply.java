package org.zstack.header.network.l3;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * Created by weiwang on 19/05/2017.
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetL3NetworkMtuReply extends APIReply {
    private Integer mtu;

    public Integer getMtu() {
        return mtu;
    }

    public void setMtu(Integer mtu) {
        this.mtu = mtu;
    }

    public static APIGetL3NetworkMtuReply __example__() {
        APIGetL3NetworkMtuReply reply = new APIGetL3NetworkMtuReply();
        reply.setMtu(9216);

        return reply;
    }
}
