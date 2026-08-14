package org.zstack.network.service;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;

import javax.persistence.TypedQuery;
import java.util.List;

/**
 * Created by xing5 on 2016/6/24.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class NetworkProviderFinder {
    @Autowired
    private DatabaseFacade dbf;

    @Transactional(readOnly = true)
    public String getNetworkProviderTypeByNetworkServiceType(String l3Uuid, String networkServiceType) {
        String sql = "select provider.type from NetworkServiceProviderVO provider, NetworkServiceL3NetworkRefVO ref" +
                " where provider.uuid = ref.networkServiceProviderUuid and ref.networkServiceType = :nsType" +
                " and ref.l3NetworkUuid = :l3Uuid";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("nsType", networkServiceType);
        q.setParameter("l3Uuid", l3Uuid);
        List<String> ret = q.getResultList();
        return ret.isEmpty() ? null : ret.get(0);
    }
}
