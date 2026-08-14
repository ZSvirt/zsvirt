package org.zstack.expon.sdk.iscsi;

import org.zstack.expon.sdk.ExponQueryResponse;

import java.util.List;

public class QueryIscsiClientGroupResponse extends ExponQueryResponse {
    private List<IscsiClientGroupModule> clients;

    public void setClients(List<IscsiClientGroupModule> clients) {
        this.clients = clients;
    }

    public List<IscsiClientGroupModule> getClients() {
        return clients;
    }
}
