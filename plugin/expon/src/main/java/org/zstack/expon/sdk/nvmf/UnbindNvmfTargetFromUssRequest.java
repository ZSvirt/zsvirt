package org.zstack.expon.sdk.nvmf;

import org.springframework.http.HttpMethod;
import org.zstack.expon.sdk.ExponRequest;
import org.zstack.expon.sdk.ExponRestRequest;
import org.zstack.expon.sdk.Param;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExponRestRequest(
        path = "/block/nvmf/unbind_uss",
        method = HttpMethod.PUT,
        responseClass = UnbindNvmfTargetFromUssResponse.class
)
public class UnbindNvmfTargetFromUssRequest extends ExponRequest {
    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();
    @Param
    private String nvmfId;
    @Param
    private List<String> ussGwId;

    public String getNvmfId() {
        return nvmfId;
    }

    public void setNvmfId(String nvmfId) {
        this.nvmfId = nvmfId;
    }

    public void setUssGwId(List<String> ussGwId) {
        this.ussGwId = ussGwId;
    }

    public List<String> getUssGwId() {
        return ussGwId;
    }

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }
}
