package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.util.StringUtils;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.allocator.AllocationScene;
import org.zstack.header.allocator.HostCandidate;
import org.zstack.header.allocator.ResourceBindingCollector;
import org.zstack.header.allocator.ResourceBindingStrategy;
import org.zstack.header.host.HostVO;
import org.zstack.resourceconfig.ResourceConfigFacade;

import java.util.*;

import static org.zstack.core.Platform.i18m;
import static org.zstack.utils.CollectionUtils.transform;

/**
 * @ Author : yh.w
 * @ Date   : Created in 18:11 2019/11/26
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ResourceBindingAllocatorFlow extends AbstractHostAllocatorFlow {

    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    private ResourceConfigFacade rcf;

    private static Map<String, ResourceBindingCollector> collectors = Collections.synchronizedMap(new HashMap<>());

    private static String SPLIT = ",";

    {
        List<ResourceBindingCollector> cs = pluginRgty.getExtensionList(ResourceBindingCollector.class);
        for (ResourceBindingCollector collector : cs) {
            collectors.put(collector.getType(), collector);
        }
    }

    private Map<String, List<String>> getBindedResourcesFromTag() {
        String resources = VmSystemTags.VM_RESOURCE_BINGDING
                .getTokenByResourceUuid(spec.getVmInstance().getUuid(), VmSystemTags.VM_RESOURCE_BINGDING_TOKEN);

        if (StringUtils.isEmpty(resources)) {
            return null;
        }

        Map<String, List<String>> resourceMap = new HashMap<>();
        for (String resource : resources.split(SPLIT)) {
            String type = resource.split(":")[0];
            String uuid = resource.split(":")[1];
            List<String> resourceList = resourceMap.computeIfAbsent(type, k -> new ArrayList<>());
            resourceList.add(uuid);
        }

        return resourceMap;
    }

    private boolean validateAllocationScene() {
        String as = rcf.getResourceConfigValue(VmGlobalConfig.RESOURCE_BINDING_SCENE, spec.getVmInstance().getUuid(), String.class);
        if (as.equals(AllocationScene.All.toString())) {
            return true;
        }

        if (spec.getAllocationScene() != null) {
            return as.equals(spec.getAllocationScene().toString());
        }

        return false;
    }

    @Override
    public void allocate() {
        Boolean resourceConfig = rcf.getResourceConfigValue(VmGlobalConfig.VM_HA_ACROSS_CLUSTERS, spec.getVmInstance().getUuid(), Boolean.class);
        if (!validateAllocationScene() || (!VmSystemTags.VM_RESOURCE_BINGDING.hasTag(spec.getVmInstance().getUuid()) && resourceConfig)) {
            next();
            return;
        }

        // get bind resources from system tag
        Map<String, List<String>> resources = getBindedResourcesFromTag();
        resources = resources != null ? resources : new HashMap<>();
        // get bind resources from config
        ResourceBindingClusterCollector clusterCollector = new ResourceBindingClusterCollector();
        if (!resourceConfig) {
            resources.computeIfAbsent(clusterCollector.getType(), k -> new ArrayList<>()).add(spec.getVmInstance().getClusterUuid());
        }

        if (resources.isEmpty()) {
            next();
            return;
        }

        List<HostVO> availableHost = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : resources.entrySet()) {
            ResourceBindingCollector collector = collectors.get(entry.getKey());
            if (collector == null) {
                fail(Platform.operr("resource binding not support type %s yet", entry.getKey()));
                return;
            }
            availableHost.addAll(collector.collect(entry.getValue()));
        }

        String strategy = rcf.getResourceConfigValue(VmGlobalConfig.RESOURCE_BINDING_STRATEGY, spec.getVmInstance().getUuid(), String.class);
        boolean isSoft = ResourceBindingStrategy.Soft.toString().equals(strategy);

        List<String> availableHostUuidList = transform(availableHost, HostVO::getUuid);
        for (HostCandidate candidate : candidates) {
            if (availableHostUuidList.contains(candidate.getUuid())) {
                continue;
            }

            if (isSoft) {
                notRecommend(candidate);
            } else {
                reject(candidate, i18m("not bound resource"));
            }
        }

        next();
    }
}
