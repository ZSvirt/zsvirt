package org.zstack.expon.sdk.cluster;

import org.zstack.expon.sdk.ExponQueryResponse;

import java.util.List;

public class QueryTianshuClusterResponse extends ExponQueryResponse {
    private List<TianshuClusterModule> result;

    public List<TianshuClusterModule> getResult() {
        return result;
    }

    public void setResult(List<TianshuClusterModule> result) {
        this.result = result;
    }
}
