package org.zstack.ipsec.vyos;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO_;
import org.zstack.ipsec.IPsecConnectionInventory;
import org.zstack.ipsec.IPsecConnectionVO;
import org.zstack.ipsec.IPsecConstants;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import org.zstack.network.service.virtualrouter.vyos.VyosVmFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by xing5 on 2016/11/8.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VyosIPsecSyncFlow extends NoRollbackFlow {
    @Autowired
    private VyosIPsecBackend bkd;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private VyosVmFactory vyosf;

    String __name__ = "sync-ipsec-connection";

    @Override
    public void run(FlowTrigger trigger, Map data) {
        final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());

        List<String> nwServed = vr.getGuestL3Networks();
        if (nwServed == null || nwServed.isEmpty()) {
            trigger.next();
            return;
        }
        SimpleQuery<NetworkServiceL3NetworkRefVO> q = dbf.createQuery(NetworkServiceL3NetworkRefVO.class);
        q.select(NetworkServiceL3NetworkRefVO_.l3NetworkUuid);
        q.add(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, Op.IN, nwServed);
        q.add(NetworkServiceL3NetworkRefVO_.networkServiceType, Op.EQ, IPsecConstants.IPSEC_NETWORK_SERVICE_TYPE.toString());
        q.add(NetworkServiceL3NetworkRefVO_.networkServiceProviderUuid, Op.EQ, vyosf.getNetworkServiceProviderUuid());
        List<String> l3Uuids = q.listValue();
        if (l3Uuids.isEmpty()) {
            trigger.next();
            return;
        }

        String sql = "select distinct ref.connectionUuid from IPsecL3NetworkRefVO ref where ref.l3NetworkUuid in (:l3Uuids)";
        List<String> ipsecs = SQL.New(sql, String.class).param("l3Uuids", l3Uuids).list();
        if (ipsecs == null || ipsecs.isEmpty()){
            trigger.next();
            return;
        }

        List<IPsecConnectionInventory> invs = ipsecs.stream()
                .map(ipsec -> IPsecConnectionInventory.valueOf(dbf.findByUuid(ipsec, IPsecConnectionVO.class))).collect(Collectors.toList());
        bkd.syncIPsecConnection(vr, invs, new Completion(trigger) {
            @Override
            public void success() {
                trigger.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                trigger.fail(errorCode);
            }
        });
    }
}
