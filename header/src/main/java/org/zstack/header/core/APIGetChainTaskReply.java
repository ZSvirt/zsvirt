package org.zstack.header.core;

import org.zstack.header.core.progress.ChainInfo;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.HashMap;
import java.util.Map;

@RestResponse(fieldsTo = {"all"})
public class APIGetChainTaskReply extends APIReply {
    private Map<String, ChainInfo> results = new HashMap<>();

    public Map<String, ChainInfo> getResults() {
        return results;
    }

    public void setResults(Map<String, ChainInfo> results) {
        this.results = results;
    }

    public void putResults(String hostUuid, ChainInfo info) {
        results.put(hostUuid, info);
    }

    public void putAllResults(Map<String, ChainInfo> results) {
        results.forEach((key, value) -> {
            ChainInfo chainInfo = this.results.getOrDefault(key, new ChainInfo());
            chainInfo.getRunningTask().addAll(value.getRunningTask());
            chainInfo.getPendingTask().addAll(value.getPendingTask());
            this.results.put(key, chainInfo);
        });
    }
}
