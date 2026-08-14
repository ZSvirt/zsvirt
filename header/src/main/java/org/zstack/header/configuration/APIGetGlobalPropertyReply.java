package org.zstack.header.configuration;

import org.zstack.header.message.APIReply;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by frank on 7/23/2015.
 */
public class APIGetGlobalPropertyReply extends APIReply {
    private List<String> properties;

    public List<String> getProperties() {
        return properties;
    }

    public void setProperties(List<String> properties) {
        this.properties = properties;
    }
 
    public static APIGetGlobalPropertyReply __example__() {
        APIGetGlobalPropertyReply reply = new APIGetGlobalPropertyReply();
        List<String> list = new ArrayList<>();
        list.add("tapResourcesForBilling: false");
        list.add("unitTestOn: false");

        reply.setProperties(list);

        return reply;
    }

}
