package org.zstack.compute.vdpa;

import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkType;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class KVMRealizeL2VlanOvsDpdkBackend extends KVMRealizeL2NoVlanOvsDpdkBackend {
    private static final CLogger logger = Utils.getLogger(KVMRealizeL2VlanOvsDpdkBackend.class);

    @Override
    public L2NetworkType getSupportedL2NetworkType() {
        return L2NetworkType.valueOf(L2NetworkConstant.L2_VLAN_NETWORK_TYPE);
    }
}
