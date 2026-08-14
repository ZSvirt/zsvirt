package org.zstack.zwatch.api;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.zwatch.datatype.CapacityData;

import java.util.List;
import java.util.Map;

@RestResponse(fieldsTo = {"all"})
public class APIGetManagementNodeDirCapacityReply extends APIReply {
    private Map<String, List<CapacityData>> result;

    public Map<String, List<CapacityData>> getResult() {
        return result;
    }

    public void setResult(Map<String, List<CapacityData>> result) {
        this.result = result;
    }
}
