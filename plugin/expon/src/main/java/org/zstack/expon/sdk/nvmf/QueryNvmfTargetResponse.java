package org.zstack.expon.sdk.nvmf;

import org.zstack.expon.sdk.ExponQueryResponse;

import java.util.List;

public class QueryNvmfTargetResponse extends ExponQueryResponse {
    private List<NvmfModule> nvmfs;

    public List<NvmfModule> getNvmfs() {
        return nvmfs;
    }

    public void setNvmfs(List<NvmfModule> nvmfs) {
        this.nvmfs = nvmfs;
    }
}
