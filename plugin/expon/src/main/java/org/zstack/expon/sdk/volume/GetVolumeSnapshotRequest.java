package org.zstack.expon.sdk.volume;

import org.springframework.http.HttpMethod;
import org.zstack.expon.sdk.ExponQuery;
import org.zstack.expon.sdk.ExponRequest;
import org.zstack.expon.sdk.ExponRestRequest;
import org.zstack.expon.sdk.Param;

import java.util.HashMap;
import java.util.Map;


@ExponRestRequest(
        path = "/block/snaps/{id}",
        method = HttpMethod.GET,
        responseClass = GetVolumeSnapshotResponse.class,
        sync = true
)
@ExponQuery(inventoryClass = VolumeModule.class, replyClass = QueryVolumeResponse.class)
public class GetVolumeSnapshotRequest extends ExponRequest {
    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Param
    private String id;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }
}
