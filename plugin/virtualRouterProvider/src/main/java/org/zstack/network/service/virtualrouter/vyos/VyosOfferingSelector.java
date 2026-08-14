package org.zstack.network.service.virtualrouter.vyos;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterOfferingInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterOfferingSelector;
import org.zstack.network.service.virtualrouter.VirtualRouterSystemTags;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by xing5 on 2016/11/2.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VyosOfferingSelector implements VirtualRouterOfferingSelector {
    @Override
    public VirtualRouterOfferingInventory selectVirtualRouterOffering(L3NetworkInventory l3, List<VirtualRouterOfferingInventory> candidates) {
        Map<String, List<String>> tags = VirtualRouterSystemTags.VYOS_OFFERING.getTags(candidates.stream().map(VirtualRouterOfferingInventory::getUuid).collect(Collectors.toList()));
        if (tags.isEmpty()) {
            Optional p = candidates.stream().filter(VirtualRouterOfferingInventory::isDefault).findAny();
            return p.isPresent() ? (VirtualRouterOfferingInventory) p.get() : candidates.get(0);
        } else {
            List<VirtualRouterOfferingInventory> offerings = candidates.stream().filter(i->tags.containsKey(i.getUuid())).collect(Collectors.toList());
            Optional p = offerings.stream().filter(VirtualRouterOfferingInventory::isDefault).findAny();
            return p.isPresent() ? (VirtualRouterOfferingInventory) p.get() : offerings.get(0);
        }
    }
}
