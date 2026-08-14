package org.zstack.expon.sdk.nvmf;

import org.springframework.http.HttpMethod;
import org.zstack.expon.sdk.ExponRequest;
import org.zstack.expon.sdk.ExponRestRequest;
import org.zstack.expon.sdk.Param;
import org.zstack.expon.sdk.volume.GetVolumeResponse;

import javax.persistence.Column;
import java.util.HashMap;
import java.util.Map;

@ExponRestRequest(
        path = "/block/nvmf/{nvmfId}/nvmf_binded_uss",
        method = HttpMethod.GET,
        responseClass = GetVolumeResponse.class,
        sync = false
)
public class GetNvmfTargetBoundUssRequest extends ExponRequest {
    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Param
    private String nvmfId;


    public String getNvmfId() {
        return nvmfId;
    }

    public void setNvmfId(String nvmfId) {
        this.nvmfId = nvmfId;
    }

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }

}
