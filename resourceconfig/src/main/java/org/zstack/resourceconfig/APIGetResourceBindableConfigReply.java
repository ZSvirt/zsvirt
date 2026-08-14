package org.zstack.resourceconfig;

import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.zone.Zone;
import org.zstack.header.zone.ZoneVO;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by MaJin on 2019/2/26.
 */

@RestResponse(allTo = "bindableConfigs")
public class APIGetResourceBindableConfigReply extends APIReply {
    private List<ResourceBindableConfigStruct> bindableConfigs;

    public List<ResourceBindableConfigStruct> getBindableConfigs() {
        return bindableConfigs;
    }

    public void setBindableConfigs(List<ResourceBindableConfigStruct> bindableConfigs) {
        this.bindableConfigs = bindableConfigs;
    }

    public static class ResourceBindableConfigStruct {
        private String name;
        private String category;
        private String description;
        private List<String> bindResourceTypes;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<String> getBindResourceTypes() {
            return bindResourceTypes;
        }

        public void setBindResourceTypes(List<String> bindResourceTypes) {
            this.bindResourceTypes = bindResourceTypes;
        }

        public static ResourceBindableConfigStruct valueOf(ResourceConfig resourceConfig) {
            ResourceBindableConfigStruct result = new ResourceBindableConfigStruct();
            result.setName(resourceConfig.globalConfig.getName());
            result.setCategory(resourceConfig.globalConfig.getCategory());
            result.setDescription(resourceConfig.globalConfig.getDescription());
            result.setBindResourceTypes(resourceConfig.getResourceClasses().stream().map(Class::getSimpleName).collect(Collectors.toList()));
            return result;
        }

        public static List<ResourceBindableConfigStruct> valueOf(Collection<ResourceConfig> resourceConfigs) {
            return resourceConfigs.stream().map(ResourceBindableConfigStruct::valueOf).collect(Collectors.toList());
        }
    }

    public static APIGetResourceBindableConfigReply __example__() {
        APIGetResourceBindableConfigReply reply = new APIGetResourceBindableConfigReply();
        ResourceBindableConfigStruct struct = new ResourceBindableConfigStruct();
        struct.category = "host";
        struct.name = "cpu.overProvisioning.ratio";
        struct.bindResourceTypes = Arrays.asList(
                HostVO.class.getSimpleName(), ClusterVO.class.getSimpleName(), ZoneVO.class.getSimpleName()
        );
        reply.setBindableConfigs(Collections.singletonList(struct));
        return reply;
    }
}
