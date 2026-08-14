package org.zstack.expon.sdk.nvmf;

import org.zstack.expon.sdk.ExponResponse;

import java.util.List;

public class GetNvmfTargetBoundUssResponse extends ExponResponse {
    private List<NvmfBoundUssGatewayRefModule> result;

    public List<NvmfBoundUssGatewayRefModule> getResult() {
        return result;
    }

    public void setResult(List<NvmfBoundUssGatewayRefModule> result) {
        this.result = result;
    }
}
