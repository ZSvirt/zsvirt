package org.zstack.expon.sdk.nvmf;

import org.zstack.expon.sdk.ExponQueryResponse;

import java.util.List;

public class QueryNvmfClientGroupResponse extends ExponQueryResponse {
    private List<NvmfClientGroupModule> clients;

    public void setClients(List<NvmfClientGroupModule> clients) {
        this.clients = clients;
    }

    public List<NvmfClientGroupModule> getClients() {
        return clients;
    }
}
