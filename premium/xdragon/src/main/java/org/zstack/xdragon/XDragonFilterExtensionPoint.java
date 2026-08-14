package org.zstack.xdragon;

import org.zstack.header.allocator.HostAllocatorFilterExtensionPoint;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.allocator.HostCandidate;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;

import java.util.List;

import static org.zstack.core.Platform.i18m;

/**
 * Created by Qi Le on 2021/10/15
 */
public class XDragonFilterExtensionPoint implements HostAllocatorFilterExtensionPoint {
    @Override
    public void filter(List<HostCandidate> candidates, HostAllocatorSpec spec) {
        ImageInventory imgInv = spec.getImage();
        if (imgInv == null || !ImageMediaType.ISO.toString().equals(imgInv.getMediaType())) {
            return;
        }
        for (HostCandidate candidate : candidates) {
            if (XDragonConstant.HYPERVISOR_TYPE.equals(candidate.host.getHypervisorType())) {
                candidate.markAsRejected(getClass(),
                        i18m("xdragon host not support create vm using an iso image."));
            }
        }
    }
}
