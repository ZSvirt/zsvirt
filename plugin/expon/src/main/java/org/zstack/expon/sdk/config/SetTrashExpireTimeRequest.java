package org.zstack.expon.sdk.config;

import org.springframework.http.HttpMethod;
import org.zstack.expon.sdk.ExponRequest;
import org.zstack.expon.sdk.ExponRestRequest;
import org.zstack.expon.sdk.Param;

import java.util.HashMap;
import java.util.Map;


@ExponRestRequest(
        path = "/sys_config/trash_recycle",
        method = HttpMethod.PUT,
        responseClass = SetTrashExpireTimeResponse.class,
        sync = false,
        version = "v1"
)
public class SetTrashExpireTimeRequest extends ExponRequest {
    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Param
    private int trashRecycle;

    public void setTrashRecycle(int trashRecycle) {
        this.trashRecycle = trashRecycle;
    }

    public int getTrashRecycle() {
        return trashRecycle;
    }

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }
}
