package org.zstack.network.service.flat;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l2.L2NetworkVO_;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.network.l2.L2NetworkHostUtils;
import org.zstack.utils.CollectionUtils;

import javax.persistence.Tuple;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;

/**
 * Created by boce.wang on 03/12/2024.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class BridgeVirtualNetworkIdFinder {
    @Autowired
    private DatabaseFacade dbf;

    public String findByL2Uuid(String l2Uuid, String hostUuid) {
        return findByL2Uuid(l2Uuid, hostUuid, true);
    }

    public String findByL2Uuid(String l2Uuid, String hostUuid, boolean exceptionOnNotFound) {
        Map<String, String> virtualNetworkIdMap = findByL2Uuids(asList(l2Uuid), hostUuid);
        String bridgeName = L2NetworkHostUtils.getBridgeNameFromL2NetworkHostRef(l2Uuid, hostUuid);
        if (bridgeName == null) {
            bridgeName = KVMSystemTags.L2_BRIDGE_NAME.getTokenByResourceUuid(l2Uuid, KVMSystemTags.L2_BRIDGE_NAME_TOKEN);
        }
        if (virtualNetworkIdMap.get(bridgeName) == null && exceptionOnNotFound) {
            throw new CloudRuntimeException(String.format("cannot find L2 bridge vlan id for the L2 network[uuid:%s]",
                    l2Uuid));
        }
        return virtualNetworkIdMap.get(bridgeName);
    }

    public Map<String, String> findByL2Uuids(Collection<String> l2Uuids, String hostUuid) {
        if (CollectionUtils.isEmpty(l2Uuids)) {
            return new HashMap<>();
        }

        Map<String, String> virtualNetworkIdMap = new HashMap<>();
        List<Tuple> tuples = Q.New(L2NetworkVO.class).in(L2NetworkVO_.uuid, asList(l2Uuids))
                .select(L2NetworkVO_.uuid, L2NetworkVO_.virtualNetworkId, L2NetworkVO_.type).listTuple();
        Map<String, String> bridgeNameMap = L2NetworkHostUtils.getBridgeNameMapFromL2NetworkHostRef((List<String>) l2Uuids, hostUuid);
        tuples.forEach(tuple -> {
            String l2Uuid = tuple.get(0, String.class);
            Integer virtualNetworkId = tuple.get(1, Integer.class);
            String l2Type = tuple.get(2, String.class);
            String bridgeName = bridgeNameMap.get(l2Uuid) != null ?
                    bridgeNameMap.get(l2Uuid) : KVMSystemTags.L2_BRIDGE_NAME.getTokenByResourceUuid(l2Uuid, KVMSystemTags.L2_BRIDGE_NAME_TOKEN);
            if (virtualNetworkId != null && !virtualNetworkId.equals(0)) {
                // ff l2 updates the vlanId, bridgeName will not be updated
                if (!bridgeName.contains(virtualNetworkId.toString())) {
                    String virtualNetworkIdWithType;
                    if (L2NetworkConstant.VXLAN_NETWORK_POOL_TYPE.equals(l2Type) ||
                            L2NetworkConstant.VXLAN_NETWORK_TYPE.equals(l2Type)) {
                        virtualNetworkIdWithType = "vxlan" + virtualNetworkId;
                    } else {
                        virtualNetworkIdWithType = "vlan" + virtualNetworkId;
                    }

                    virtualNetworkIdMap.put(bridgeName, virtualNetworkIdWithType);
                }
            }
        });
        return virtualNetworkIdMap;
    }
}
