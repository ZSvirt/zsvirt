package org.zstack.network.service.virtualrouter.lb;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant.Param;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualRouterCleanupLoadBalancerOnDestroyFlow extends NoRollbackFlow {
    @Autowired
    private DatabaseFacade dbf;
    private static final CLogger logger = Utils.getLogger(VirtualRouterCleanupLoadBalancerOnDestroyFlow.class);

    @Override
    public void run(FlowTrigger trigger, Map data) {
        final String vrUuid = (String) data.get(Param.VR_UUID.toString());
        List<VirtualRouterLoadBalancerRefVO> refs = Q.New(VirtualRouterLoadBalancerRefVO.class).eq(VirtualRouterLoadBalancerRefVO_.virtualRouterVmUuid, vrUuid).list();
        if (!refs.isEmpty()){
            dbf.removeCollection(refs, VirtualRouterLoadBalancerRefVO.class);
        }
        trigger.next();
    }
}
