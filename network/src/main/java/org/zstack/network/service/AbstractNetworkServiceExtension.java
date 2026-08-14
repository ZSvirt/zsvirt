package org.zstack.network.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.service.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 8:00 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractNetworkServiceExtension implements NetworkServiceExtensionPoint {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    protected ErrorFacade errf;


    public NetworkServiceExtensionPosition getNetworkServiceExtensionPosition() {
        return NetworkServiceExtensionPosition.BEFORE_VM_CREATED;
    }

    protected NetworkServiceProviderType getNetworkServiceProviderType(NetworkServiceType type, L3NetworkInventory l3) {
        for (NetworkServiceL3NetworkRefInventory ref : l3.getNetworkServices()) {
            if (!type.toString().equals(ref.getNetworkServiceType())) {
                continue;
            }

            String providerType = Q.New(NetworkServiceProviderVO.class)
                    .select(NetworkServiceProviderVO_.type)
                    .eq(NetworkServiceProviderVO_.uuid, ref.getNetworkServiceProviderUuid())
                    .findValue();

            return NetworkServiceProviderType.valueOf(providerType);
        }

        return null;
    }


    protected Map<NetworkServiceProviderType, List<L3NetworkInventory>> getNetworkServiceProviderMap(NetworkServiceType type, List<L3NetworkInventory> l3Networks) {
        Map<NetworkServiceProviderType, List<L3NetworkInventory>> ret = new HashMap<NetworkServiceProviderType, List<L3NetworkInventory>>();
        for (L3NetworkInventory l3 : l3Networks) {
            for (NetworkServiceL3NetworkRefInventory ref : l3.getNetworkServices()) {
                if (!type.toString().equals(ref.getNetworkServiceType())) {
                    continue;
                }

                String providerType = Q.New(NetworkServiceProviderVO.class)
                        .select(NetworkServiceProviderVO_.type)
                        .eq(NetworkServiceProviderVO_.uuid, ref.getNetworkServiceProviderUuid())
                        .findValue();

                NetworkServiceProviderType ptype = NetworkServiceProviderType.valueOf(providerType);
                List<L3NetworkInventory> l3s = ret.computeIfAbsent(ptype, k -> new ArrayList<>());
                l3s.add(l3);
            }
        }

        return ret;
    }
}
