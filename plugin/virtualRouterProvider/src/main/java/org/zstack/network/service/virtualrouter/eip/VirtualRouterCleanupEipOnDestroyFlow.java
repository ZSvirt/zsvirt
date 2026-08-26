package org.zstack.network.service.virtualrouter.eip;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant.Param;

import java.util.List;
import java.util.Map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualRouterCleanupEipOnDestroyFlow extends NoRollbackFlow {
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        final String vrUuid = (String) data.get(Param.VR_UUID.toString());
        List<VirtualRouterEipRefVO> refs = Q.New(VirtualRouterEipRefVO.class)
                .eq(VirtualRouterEipRefVO_.virtualRouterVmUuid, vrUuid)
                .list();
        if (!refs.isEmpty()) {
            dbf.removeCollection(refs, VirtualRouterEipRefVO.class);
        }
        trigger.next();
    }
}
