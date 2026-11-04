package org.zstack.resourceconfig;

import org.springframework.http.HttpMethod;
import org.zstack.header.configuration.ConfigAO;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vo.ResourceVO;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestRequest(
        path = "/resource-configurations/{resourceUuid}/resource-configs/actions",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIUpdateResourceConfigsEvent.class
)
public class APIUpdateResourceConfigsMsg extends APIMessage {
    /**
     * use ConfigAO
     */
    @PythonClassInventory
    @Deprecated
    public static class ResourceConfigAO extends ConfigAO {
    }

    @APIParam(resourceType = ResourceVO.class)
    private String resourceUuid;

    @APIParam(nonempty = true)
    private List<ConfigAO> resourceConfigs;

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public List<ConfigAO> getResourceConfigs() {
        return resourceConfigs;
    }

    public void setResourceConfigs(List<ConfigAO> resourceConfigs) {
        this.resourceConfigs = resourceConfigs;
    }

    public static APIUpdateResourceConfigsMsg __example__() {
        APIUpdateResourceConfigsMsg msg = new APIUpdateResourceConfigsMsg();
        msg.setResourceUuid(uuid(ResourceVO.class));
        msg.setResourceConfigs(list(ConfigAO.__example__()));
        return msg;
    }
}
