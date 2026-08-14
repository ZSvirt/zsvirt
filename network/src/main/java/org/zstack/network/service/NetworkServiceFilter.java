package org.zstack.network.service;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;

import javax.persistence.TypedQuery;
import java.util.Collection;
import java.util.List;

/**
 * Created by xing5 on 2016/4/22.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class NetworkServiceFilter {
    @Autowired
    private DatabaseFacade dbf;

    @Transactional(readOnly = true)
    public List<String> filterNicByServiceTypeAndProviderType(Collection<String> nicUuids, String serviceType, String providerType) {
        String sql = "select nic.uuid from VmNicVO nic, NetworkServiceL3NetworkRefVO l3ref, NetworkServiceProviderVO provider," +
                " NetworkServiceProviderL2NetworkRefVO l2ref, L3NetworkVO l3 where nic.l3NetworkUuid = l3.uuid" +
                " and nic.uuid in (:uuids)" +
                " and l3.uuid = l3ref.l3NetworkUuid and l3ref.networkServiceType = :serviceType and l3ref.networkServiceProviderUuid = provider.uuid" +
                " and provider.uuid = l2ref.networkServiceProviderUuid and l2ref.l2NetworkUuid = l3.l2NetworkUuid" +
                " and provider.type = :providerType";

        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("uuids", nicUuids);
        q.setParameter("serviceType", serviceType);
        q.setParameter("providerType", providerType);
        return q.getResultList();
    }

    @Transactional(readOnly = true)
    public List<String> filterVmByServiceTypeAndProviderType(Collection<String> vmUuids, String serviceType, String providerType) {
        String sql = "select distinct vm.uuid from VmNicVO nic, VmInstanceVO vm, NetworkServiceL3NetworkRefVO l3ref, NetworkServiceProviderVO provider," +
                " NetworkServiceProviderL2NetworkRefVO l2ref, L3NetworkVO l3 " +
                " where nic.l3NetworkUuid = l3.uuid and nic.l3NetworkUuid = vm.defaultL3NetworkUuid" +
                " and nic.vmInstanceUuid = vm.uuid and vm.uuid in (:uuids)" +
                " and l3.uuid = l3ref.l3NetworkUuid and l3ref.networkServiceType = :serviceType and l3ref.networkServiceProviderUuid = provider.uuid" +
                " and provider.uuid = l2ref.networkServiceProviderUuid and l2ref.l2NetworkUuid = l3.l2NetworkUuid" +
                " and provider.type = :providerType";

        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("uuids", vmUuids);
        q.setParameter("serviceType", serviceType);
        q.setParameter("providerType", providerType);
        return q.getResultList();
    }
}
