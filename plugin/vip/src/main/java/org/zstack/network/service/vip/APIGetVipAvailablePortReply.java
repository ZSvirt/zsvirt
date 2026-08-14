package org.zstack.network.service.vip;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import java.util.ArrayList;
import java.sql.Timestamp;
import org.zstack.header.message.APIReply;
import java.util.List;

@RestResponse(fieldsTo = {"all"})
public class APIGetVipAvailablePortReply extends APIReply {
    private List<Integer> availablePort;

    public List<Integer> getAvailablePort() {
        return availablePort;
    }

    public void setAvailablePort(List<Integer> availablePort) {
        this.availablePort = availablePort;
    }
 
    public static APIGetVipAvailablePortReply __example__() {
        APIGetVipAvailablePortReply reply = new APIGetVipAvailablePortReply();
        List<Integer> availablePort = new ArrayList<Integer>();
        availablePort.add(1);
        availablePort.add(2);

        reply.setAvailablePort(availablePort);
        return reply;
    }

}
