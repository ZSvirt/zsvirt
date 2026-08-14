package org.zstack.network.service;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.network.service.NetworkServiceProviderVO;
import org.zstack.header.network.service.NetworkServiceProviderVO_;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by xing5 on 2016/5/20.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class NetworkServiceProviderLookup {
    private static Map<String, String> typeToUuid = new HashMap<String, String>();

    @Autowired
    private DatabaseFacade dbf;

    public String lookupUuidByType(String type) {
        synchronized (typeToUuid) {
            String uuid = typeToUuid.get(type);
            if (uuid != null) {
                return uuid;
            }

            SimpleQuery<NetworkServiceProviderVO> q = dbf.createQuery(NetworkServiceProviderVO.class);
            q.select(NetworkServiceProviderVO_.uuid);
            q.add(NetworkServiceProviderVO_.type, Op.EQ, type);
            uuid = q.findValue();
            if (uuid == null) {
                throw new CloudRuntimeException(String.format("cannot find NetworkServiceProviderVO[type:%s]", type));
            }

            typeToUuid.put(type, uuid);
            return uuid;
        }
    }
}
