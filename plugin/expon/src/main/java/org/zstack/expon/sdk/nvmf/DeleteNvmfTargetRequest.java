package org.zstack.expon.sdk.nvmf;

import org.springframework.http.HttpMethod;
import org.zstack.expon.sdk.ExponRequest;
import org.zstack.expon.sdk.ExponRestRequest;
import org.zstack.expon.sdk.Param;

import java.util.HashMap;
import java.util.Map;

@ExponRestRequest(
        path = "/block/nvmf/{nvmfId}",
        method = HttpMethod.DELETE,
        responseClass = CreateNvmfTargetResponse.class
)
public class DeleteNvmfTargetRequest extends ExponRequest {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Param
    private String nvmfId;

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }

    public void setNvmfId(String nvmfId) {
        this.nvmfId = nvmfId;
    }

    public String getNvmfId() {
        return nvmfId;
    }
}
