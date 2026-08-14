package org.zstack.network.l2.vxlan.vxlanNetwork;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.Component;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceMigrateExtensionPoint;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.zone.ZoneVO;
import org.zstack.identity.AccountManager;
import org.zstack.network.l2.L2NetworkCascadeFilterExtensionPoint;
import org.zstack.network.l2.L2NetworkDefaultMtu;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.AllocateVniMsg;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.AllocateVniReply;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolConstant;
import org.zstack.network.service.NetworkServiceGlobalConfig;
import org.zstack.query.QueryFacade;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

/**
 * Created by weiwang on 02/03/2017.
 */
public class VxlanNetworkFactory implements L2NetworkFactory, Component, VmInstanceMigrateExtensionPoint, L2NetworkDefaultMtu, L2NetworkGetVniExtensionPoint, L2NetworkCascadeFilterExtensionPoint {
    private static CLogger logger = Utils.getLogger(VxlanNetworkFactory.class);
    public static L2NetworkType type = new L2NetworkType(VxlanNetworkConstant.VXLAN_NETWORK_TYPE);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private QueryFacade qf;
    @Autowired
    protected AccountManager acntMgr;
    @Autowired
    private ResourceConfigFacade rcf;

    @Override
    public L2NetworkType getType() {
        return type;
    }

    @Override
    public void createL2Network(L2NetworkVO ovo, APICreateL2NetworkMsg msg, ReturnValueCompletion<L2NetworkInventory> completion) {
        APICreateL2VxlanNetworkMsg amsg = (APICreateL2VxlanNetworkMsg) msg;

        AllocateVniMsg vniMsg = new AllocateVniMsg();
        vniMsg.setL2NetworkUuid(amsg.getPoolUuid());
        vniMsg.setRequiredVni(amsg.getVni());
        bus.makeTargetServiceIdByResourceUuid(vniMsg, L2NetworkConstant.SERVICE_ID, amsg.getPoolUuid());
        MessageReply reply = bus.call(vniMsg);
        if (!reply.isSuccess()) {
            completion.fail(reply.getError());
            return;
        }
        AllocateVniReply r = reply.castReply();

        VxlanNetworkVO vo = new SQLBatchWithReturn<VxlanNetworkVO>() {
            @Override
            protected VxlanNetworkVO scripts() {
                VxlanNetworkVO vo = new VxlanNetworkVO(ovo);
                String uuid = msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid();
                vo.setUuid(uuid);
                vo.setVni(r.getVni());
                vo.setVirtualNetworkId(vo.getVni());
                vo.setAccountUuid(msg.getSession().getAccountUuid());
                vo.setPoolUuid((amsg.getPoolUuid()));
                if (vo.getPhysicalInterface() == null) {
                    vo.setPhysicalInterface(L2NetworkConstant.PHYSICAL_INTERFACE_EMPTY);
                }
                dbf.getEntityManager().persist(vo);

                SimpleQuery<L2NetworkClusterRefVO> q = dbf.createQuery(L2NetworkClusterRefVO.class);
                q.add(L2NetworkClusterRefVO_.l2NetworkUuid, SimpleQuery.Op.EQ, amsg.getPoolUuid());
                final List<L2NetworkClusterRefVO> refs = q.list();
                for (L2NetworkClusterRefVO ref : refs) {
                    L2NetworkClusterRefVO rvo = new L2NetworkClusterRefVO();
                    rvo.setClusterUuid(ref.getClusterUuid());
                    rvo.setL2NetworkUuid(uuid);
                    dbf.getEntityManager().persist(rvo);
                    dbf.getEntityManager().flush();
                    dbf.getEntityManager().refresh(rvo);
                }

                dbf.getEntityManager().flush();
                dbf.getEntityManager().refresh(vo);

                return vo;
            }
        }.execute();

        L2VxlanNetworkInventory inv = L2VxlanNetworkInventory.valueOf(vo);
        if (!VxlanNetworkGlobalConfig.CLUSTER_LAZY_ATTACH.value(Boolean.class)) {
// prepare the L2 network in all the hosts of the cluster
            List<String> hosts = new SQLBatchWithReturn<List<String>>() {
                @Override
                protected List<String> scripts() {
                    List<String> clusterUuids = Q.New(L2NetworkClusterRefVO.class).
                            select(L2NetworkClusterRefVO_.clusterUuid).eq(L2NetworkClusterRefVO_.l2NetworkUuid, inv.getPoolUuid()).listValues();
                    if (clusterUuids.isEmpty()) {
                        return new ArrayList<>();
                    }
                    return Q.New(HostVO.class).select(HostVO_.uuid).in(HostVO_.clusterUuid, clusterUuids).eq(HostVO_.status, HostStatus.Connected).listValues();
                }
            }.execute();

            if (hosts != null && !hosts.isEmpty()) {
                PrepareL2NetworkOnHostsMsg pmsg = new PrepareL2NetworkOnHostsMsg();
                pmsg.setL2NetworkUuid(inv.getUuid());
                pmsg.setHosts(hosts);
                bus.makeTargetServiceIdByResourceUuid(pmsg, L2NetworkConstant.SERVICE_ID, inv.getUuid());
                MessageReply rp = bus.call(pmsg);
                if (!rp.isSuccess()) {
                    logger.warn(String.format("fail to check and realize vxlan network[uuid: %s], it will try again while the vxlan network is used", inv.getUuid()));
                } else {
                    logger.debug(String.format("check and realize vxlan network[uuid: %s] successed", inv.getUuid()));
                }
            }
        }

        String info = String.format("successfully create L2VxlanNetwork, %s", JSONObjectUtil.toJsonString(inv));
        logger.debug(info);
        completion.success(inv);
    }

    @Override
    public L2Network getL2Network(L2NetworkVO vo) {
        return new VxlanNetwork(vo);
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
    public void preMigrateVm(VmInstanceInventory inv, String destHostUuid) {
        List<VmNicInventory> nics = inv.getVmNics();
        List<L3NetworkVO> l3vos = new ArrayList<>();
        /* FIXME: shixin need add ipv6 on vlxan network */
        nics.stream().forEach((nic -> l3vos.add(Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid ,nic.getL3NetworkUuid()).find())));

        List<String> vxlanUuids = new ArrayList<>();
        for (L3NetworkVO l3 : l3vos) {
            String type = Q.New(L2NetworkVO.class).select(L2NetworkVO_.type).eq(L2NetworkVO_.uuid, l3.getL2NetworkUuid()).findValue();
            if (type.equals(VxlanNetworkConstant.VXLAN_NETWORK_TYPE)) {
                vxlanUuids.add(l3.getL2NetworkUuid());
            }
        }

        if (vxlanUuids.isEmpty()) {
            return;
        }

        ErrorCodeList errList = new ErrorCodeList();
        FutureCompletion completion = new FutureCompletion(null);

        new While<>(vxlanUuids).all((uuid, completion1) -> {
            PrepareL2NetworkOnHostMsg msg = new PrepareL2NetworkOnHostMsg();
            msg.setL2NetworkUuid(uuid);
            msg.setHost(HostInventory.valueOf((HostVO) Q.New(HostVO.class).eq(HostVO_.uuid, destHostUuid).find()));
            bus.makeTargetServiceIdByResourceUuid(msg, L2NetworkConstant.SERVICE_ID, uuid);
            bus.send(msg, new CloudBusCallBack(completion1) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(reply.getError().toString());
                        errList.getCauses().add(reply.getError());
                    } else {
                        logger.debug(String.format("check and realize vxlan network[uuid: %s] for vm[uuid: %s] successed", uuid, inv.getUuid()));
                    }
                    completion1.done();

                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errList.getCauses().isEmpty()) {
                    completion.fail(errList.getCauses().get(0));
                    return;
                }
                logger.info(String.format("check and realize vxlan networks[uuid: %s] for vm[uuid: %s] done", vxlanUuids, inv.getUuid()));
                completion.success();
            }
        });

        completion.await(TimeUnit.MINUTES.toMillis(30));
        if (!completion.isSuccess()) {
            throw new OperationFailureException(operr("cannot configure vxlan network for vm[uuid:%s] on the destination host[uuid:%s]",
                    inv.getUuid(), destHostUuid).causedBy(completion.getErrorCode()));
        }
    }

    @Override
    public String getL2NetworkType() {
        return VxlanNetworkConstant.VXLAN_NETWORK_TYPE;
    }

    @Override
    public Integer getDefaultMtu(L2NetworkInventory inv) {
        return rcf.getResourceConfigValue(NetworkServiceGlobalConfig.DHCP_MTU_VXLAN, inv.getUuid(), Integer.class);
    }

    @Override
    public Integer getL2NetworkVni(String l2NetworkUuid, String hostUuid) {
        VxlanNetworkVO vxlanNetworkVO = Q.New(VxlanNetworkVO.class).eq(VxlanNetworkVO_.uuid, l2NetworkUuid).find();
        return vxlanNetworkVO.getVni();
    }

    @Override
    public String getL2NetworkVniType() {
        return type.toString();
    }

    @Override
    public List<L2NetworkInventory> filterL2NetworkCascade(List<L2NetworkInventory> l2invs, CascadeAction action) {
        if (ZoneVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return l2invs.stream()
                    .filter(l2inv -> !l2inv.getType().equals(VxlanNetworkConstant.VXLAN_NETWORK_TYPE))
                    .collect(Collectors.toList());
        } else if (AccountVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<L2NetworkInventory> vxlans = l2invs.stream()
                    .filter(l2inv -> l2inv.getType().equals(VxlanNetworkConstant.VXLAN_NETWORK_TYPE))
                    .collect(Collectors.toList());
            List<String> poolUuids = l2invs.stream()
                    .filter(l2inv -> l2inv.getType().equals(VxlanNetworkPoolConstant.VXLAN_NETWORK_POOL_TYPE))
                    .map(l2inv -> l2inv.getUuid())
                    .collect(Collectors.toList());
            if (vxlans.isEmpty()) {
                return l2invs;
            }
            vxlans.forEach(vxlan ->{
                VxlanNetworkVO vxlanNetworkVO = dbf.findByUuid(vxlan.getUuid(), VxlanNetworkVO.class);
                if(poolUuids.contains(vxlanNetworkVO.getPoolUuid())){
                    l2invs.remove(vxlan);
                }
            });
            return l2invs;
        } else {
            return l2invs;
        }
    }
}
