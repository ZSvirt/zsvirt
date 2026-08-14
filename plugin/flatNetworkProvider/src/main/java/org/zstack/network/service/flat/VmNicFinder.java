package org.zstack.network.service.flat;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmNicVO;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by xing5 on 2016/4/4.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmNicFinder {
    @Autowired
    private DatabaseFacade dbf;

    @Transactional
    public List<VmNicVO> findVmNicsByVmUuid(String vmUuid) {
        String sql = "select nic from VmNicVO nic, L3NetworkVO l3, NetworkServiceL3NetworkRefVO ref, NetworkServiceProviderVO provider where nic.l3NetworkUuid = l3.uuid" +
                " and ref.l3NetworkUuid = l3.uuid and ref.networkServiceProviderUuid = provider.uuid " +
                " and provider.type = :ptype and nic.vmInstanceUuid = :vmUuid group by nic.uuid";

        TypedQuery<VmNicVO> nq = dbf.getEntityManager().createQuery(sql, VmNicVO.class);
        nq.setParameter("ptype", FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING);
        nq.setParameter("vmUuid", vmUuid);
        List<VmNicVO> nics = nq.getResultList();
        if (nics.isEmpty()) {
            return null;
        }

        return nics;
    }

    @Transactional
    public List<VmNicVO> findVmNicsByHostUuid(String hostUuid) {
        String sql = "select vm.uuid, vm.defaultL3NetworkUuid from VmInstanceVO vm where vm.hostUuid = :huuid and vm.state in (:states) and vm.type = :vtype";
        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        q.setParameter("huuid", hostUuid);
        q.setParameter("states", list(VmInstanceState.Running, VmInstanceState.Unknown));
        q.setParameter("vtype", VmInstanceConstant.USER_VM_TYPE);
        List<Tuple> ts = q.getResultList();
        if (ts.isEmpty()) {
            return null;
        }

        Map<String, String> vmDefaultL3 = new HashMap<String, String>();
        for (Tuple t : ts) {
            vmDefaultL3.put(t.get(0, String.class), t.get(1, String.class));
        }

        sql = "select nic from VmNicVO nic, L3NetworkVO l3, NetworkServiceL3NetworkRefVO ref, NetworkServiceProviderVO provider where nic.l3NetworkUuid = l3.uuid" +
                " and ref.l3NetworkUuid = l3.uuid and ref.networkServiceProviderUuid = provider.uuid " +
                " and provider.type = :ptype and nic.vmInstanceUuid in (:vmUuids) group by nic.uuid";

        TypedQuery<VmNicVO> nq = dbf.getEntityManager().createQuery(sql, VmNicVO.class);
        nq.setParameter("ptype", FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING);
        nq.setParameter("vmUuids", vmDefaultL3.keySet());
        List<VmNicVO> nics = nq.getResultList();
        if (nics.isEmpty()) {
            return null;
        }

        return nics;
    }
}
