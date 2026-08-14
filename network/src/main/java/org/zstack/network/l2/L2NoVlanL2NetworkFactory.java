package org.zstack.network.l2;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.Component;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.network.l2.*;
import org.zstack.network.service.NetworkServiceGlobalConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.data.FieldPrinter;
import org.zstack.utils.logging.CLogger;

import static org.zstack.header.network.l2.L2NetworkType.L2NetworkTypeBuilder;

public class L2NoVlanL2NetworkFactory implements L2NetworkFactory, Component, L2NetworkDefaultMtu, L2NetworkGetVniExtensionPoint {
    private static L2NetworkType type = new L2NetworkTypeBuilder()
            .typeName(L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE)
            .sriovSupported(true)
            .build();
    private static CLogger logger = Utils.getLogger(L2NoVlanL2NetworkFactory.class);
    private static FieldPrinter printer = Utils.getFieldPrinter();
    
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ResourceConfigFacade rcf;

    @Override
    public L2NetworkType getType() {
        return type;
    }

    @Override
    public void createL2Network(L2NetworkVO vo, APICreateL2NetworkMsg msg, ReturnValueCompletion<L2NetworkInventory> completion) {
        vo = dbf.persistAndRefresh(vo);
        L2NetworkInventory inv = L2NetworkInventory.valueOf(vo);
        logger.debug("Successfully created NoVlanL2Network: " + printer.print(inv));
        completion.success(inv);
    }

    @Override
    public L2Network getL2Network(L2NetworkVO vo) {
        return new L2NoVlanNetwork(vo);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }


    @Override
    public String getL2NetworkType() {
        return L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE;
    }

    @Override
    public Integer getDefaultMtu(L2NetworkInventory inv) {
        return rcf.getResourceConfigValue(NetworkServiceGlobalConfig.DHCP_MTU_NO_VLAN, inv.getUuid(), Integer.class);
    }

    @Override
    public Integer getL2NetworkVni(String l2NetworkUuid, String hostUuid) {
        return 0;
    }

    @Override
    public String getL2NetworkVniType() {
        return type.toString();
    }
}
