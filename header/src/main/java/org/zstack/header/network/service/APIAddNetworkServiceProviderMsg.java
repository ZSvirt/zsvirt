package org.zstack.header.network.service;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

import java.util.List;

public class APIAddNetworkServiceProviderMsg extends APIMessage {
    @APIParam
    private String name;
    @APIParam
    private String description;
    @APIParam
    private List<String> networkServiceTypes;
    @APIParam
    private String type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getNetworkServiceTypes() {
        return networkServiceTypes;
    }

    public void setNetworkServiceTypes(List<String> networkServiceTypes) {
        this.networkServiceTypes = networkServiceTypes;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
 
    public static APIAddNetworkServiceProviderMsg __example__() {
        APIAddNetworkServiceProviderMsg msg = new APIAddNetworkServiceProviderMsg();


        return msg;
    }

}
