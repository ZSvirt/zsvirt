package org.zstack.resourceconfig;

import org.springframework.http.HttpMethod;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vo.ResourceVO;

import java.util.Arrays;
import java.util.List;

@RestRequest(
        path = "/resource-configurations/{resourceUuid}/resource-configs/actions",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIUpdateResourceConfigsEvent.class
)
public class APIUpdateResourceConfigsMsg extends APIMessage {
    @PythonClassInventory
    public static class ResourceConfigAO {

        private String category;

        private String name;

        private String value;

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    @APIParam(resourceType = ResourceVO.class)
    private String resourceUuid;

    @APIParam(nonempty = true)
    private List<ResourceConfigAO> resourceConfigs;

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public List<ResourceConfigAO> getResourceConfigs() {
        return resourceConfigs;
    }

    public void setResourceConfigs(List<ResourceConfigAO> resourceConfigs) {
        this.resourceConfigs = resourceConfigs;
    }

    public static APIUpdateResourceConfigsMsg __example__() {
        APIUpdateResourceConfigsMsg msg = new APIUpdateResourceConfigsMsg();
        msg.setResourceUuid(uuid());
        ResourceConfigAO resourceConfigAO = new ResourceConfigAO();
        resourceConfigAO.setName("cleanTraffic");
        resourceConfigAO.setCategory("vm");
        resourceConfigAO.setValue("true");
        msg.setResourceConfigs(Arrays.asList(resourceConfigAO));
        return msg;
    }
}
