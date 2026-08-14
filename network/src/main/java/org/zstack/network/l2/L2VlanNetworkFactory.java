package org.zstack.network.l2;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.AbstractService;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.network.l2.*;
import org.zstack.network.service.NetworkServiceGlobalConfig;
import org.zstack.query.QueryFacade;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import static org.zstack.header.network.l2.L2NetworkType.L2NetworkTypeBuilder;

public class L2VlanNetworkFactory extends AbstractService implements L2NetworkFactory, L2NetworkDefaultMtu, L2NetworkGetVniExtensionPoint {
    private static CLogger logger = Utils.getLogger(L2VlanNetworkFactory.class);
    static L2NetworkType type = new L2NetworkTypeBuilder()
            .typeName(L2NetworkConstant.L2_VLAN_NETWORK_TYPE)
            .sriovSupported(true)
            .build();

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private QueryFacade qf;
    @Autowired
    private ResourceConfigFacade rcf;

    @Override
    public L2NetworkType getType() {
        return type;
    }

    @Override
    public void createL2Network(L2NetworkVO ovo, APICreateL2NetworkMsg msg, ReturnValueCompletion<L2NetworkInventory> completion) {
        APICreateL2VlanNetworkMsg amsg = (APICreateL2VlanNetworkMsg) msg;
        L2VlanNetworkVO vo = new L2VlanNetworkVO(ovo);
        vo.setVlan(amsg.getVlan());
        vo.setVirtualNetworkId(vo.getVlan());
        vo = dbf.persistAndRefresh(vo);
        L2VlanNetworkInventory inv = L2VlanNetworkInventory.valueOf(vo);
        String info = String.format("successfully create L2VlanNetwork, %s", JSONObjectUtil.toJsonString(inv));
        logger.debug(info);
        completion.success(inv);
    }

    @Override
    public L2Network getL2Network(L2NetworkVO vo) {
        return new L2VlanNetwork(vo);
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
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage)msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        bus.dealWithUnknownMessage(msg);
    }


    @Override
    public String getId() {
        return bus.makeLocalServiceId(L2NetworkConstant.L2_VLAN_NETWORK_FACTORY_SERVICE_ID);
    }

    @Override
    public String getL2NetworkType() {
        return L2NetworkConstant.L2_VLAN_NETWORK_TYPE;
    }

    @Override
    public Integer getDefaultMtu(L2NetworkInventory inv) {
        return rcf.getResourceConfigValue(NetworkServiceGlobalConfig.DHCP_MTU_VLAN, inv.getUuid(), Integer.class);
    }

    @Override
    public Integer getL2NetworkVni(String l2NetworkUuid, String hostUuid) {
        L2VlanNetworkVO l2VlanNetworkVO = Q.New(L2VlanNetworkVO.class).eq(L2VlanNetworkVO_.uuid, l2NetworkUuid).find();
        return l2VlanNetworkVO.getVlan();
    }

    @Override
    public String getL2NetworkVniType() {
        return type.toString();
    }
}
