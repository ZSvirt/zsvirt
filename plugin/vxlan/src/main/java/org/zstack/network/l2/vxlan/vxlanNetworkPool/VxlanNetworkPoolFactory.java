package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.Component;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.l3.APICreateL3NetworkMsg;
import org.zstack.identity.AccountManager;
import org.zstack.network.l2.L2NetworkManager;
import org.zstack.network.service.MtuGetter;
import org.zstack.network.service.NetworkServiceGlobalConfig;
import org.zstack.query.QueryFacade;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;
import java.util.List;

/**
 * Created by weiwang on 03/03/2017.
 */
public class VxlanNetworkPoolFactory implements L2NetworkFactory, Component, GlobalApiMessageInterceptor {
    private static CLogger logger = Utils.getLogger(VxlanNetworkPoolFactory.class);
    static L2NetworkType type = new L2NetworkType(VxlanNetworkPoolConstant.VXLAN_NETWORK_POOL_TYPE);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private QueryFacade qf;
    @Autowired
    private VxlanNetworkChecker vxlanInterceptor;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private ResourceConfigFacade rcf;

    @Override
    public L2NetworkType getType() {
        return type;
    }

    @Override
    @Transactional
    public void createL2Network(L2NetworkVO ovo, APICreateL2NetworkMsg msg, ReturnValueCompletion<L2NetworkInventory> completion) {
        VxlanNetworkPoolVO vo = new VxlanNetworkPoolVO(ovo);
        if (vo.getPhysicalInterface() == null) {
            vo.setPhysicalInterface(L2NetworkConstant.PHYSICAL_INTERFACE_EMPTY);
        }
        vo.setAccountUuid(msg.getSession().getAccountUuid());
        vo = dbf.persistAndRefresh(vo);

        L2VxlanNetworkPoolInventory inv = L2VxlanNetworkPoolInventory.valueOf(vo);
        String info = String.format("successfully create L2VxlanNetworkPool, %s", JSONObjectUtil.toJsonString(inv));
        logger.debug(info);
        completion.success(inv);
    }

    @Override
    public L2Network getL2Network(L2NetworkVO vo) {
        return new VxlanNetworkPool(vo);
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
    public List<Class> getMessageClassToIntercept() {
        return Arrays.asList(
                APIAttachL2NetworkToClusterMsg.class,
                APICreateL3NetworkMsg.class,
                APIUpdateL2NetworkVirtualNetworkIdMsg.class);
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        vxlanInterceptor.intercept(msg);
        return msg;
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.FRONT;
    }
}
