package org.zstack.network.service.virtualrouter.portforwarding;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant.Param;

import java.util.List;
import java.util.Map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualRouterCleanupPortforwardingOnDestroyFlow extends NoRollbackFlow {
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        final String vrUuid = (String) data.get(Param.VR_UUID.toString());
        SimpleQuery<VirtualRouterPortForwardingRuleRefVO> q = dbf.createQuery(VirtualRouterPortForwardingRuleRefVO.class);
        q.add(VirtualRouterPortForwardingRuleRefVO_.virtualRouterVmUuid, Op.EQ, vrUuid);
        List<VirtualRouterPortForwardingRuleRefVO> refs = q.list();
        if (!refs.isEmpty()) {
            dbf.removeCollection(refs, VirtualRouterPortForwardingRuleRefVO.class);
        }
        trigger.next();
    }
}
