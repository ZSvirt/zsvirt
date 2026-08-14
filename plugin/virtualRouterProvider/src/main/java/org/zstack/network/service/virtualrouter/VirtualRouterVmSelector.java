package org.zstack.network.service.virtualrouter;

import java.util.List;

/**
 * Created by frank on 8/9/2015.
 */
public interface VirtualRouterVmSelector {
    VirtualRouterVmVO select(List<VirtualRouterVmVO> vrs);
}
