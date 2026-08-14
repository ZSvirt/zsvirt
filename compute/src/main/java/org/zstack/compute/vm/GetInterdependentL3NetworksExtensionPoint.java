package org.zstack.compute.vm;

import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;

import java.util.List;

/**
 * Created by kayo on 2018/10/29.
 */
public interface GetInterdependentL3NetworksExtensionPoint {
    List<L3NetworkInventory> afterFilterByImage(List<L3NetworkInventory> l3s, List<String> bsUuids, String imageUuid);
}
