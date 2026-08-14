package org.zstack.network.service.vipQos.flat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeWhileDoneCompletion;
import org.zstack.header.host.HostConstant;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.service.*;
import org.zstack.header.vipQos.*;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.network.service.eip.EipVO;
import org.zstack.network.service.eip.EipVO_;
import org.zstack.network.service.flat.FlatNetworkServiceConstant;
import org.zstack.network.service.vipQos.VipQosBackend;
import org.zstack.network.service.vipQos.VipQosManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 * Created by shixin.ruan on 17-12-29.
 */
public class FlatVipQosBackend implements VipQosBackend, AfterApplyFlatEipExtensionPoint, PrepareDbInitialValueExtensionPoint {
    private static final CLogger logger = Utils.getLogger(FlatVipQosBackend.class);

    @Autowired
    private ApiTimeoutManager apiTimeoutManager;
    @Autowired
    private VipQosManager vipQosManager;
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    public static final String FLAT_SET_VIP_QOS = "/flatnetworkprovider/vipqos/apply";
    public static final String FLAT_DELETE_VIP_QOS = "/flatnetworkprovider/vipqos/delete";
    public static final String FLAT_DELETE_VIPALL_QOS = "/flatnetworkprovider/vipqos/deleteall";

    public static class SetVipQosCmd extends FlatNetworkServiceConstant.AgentCmd {
        private List<VipQosStruct> vipQosSettings;

        public List<VipQosStruct> getVipQosSettings() {
            return vipQosSettings;
        }

        public void setVipQosSettings(List<VipQosStruct> vipQosSettings) {
            this.vipQosSettings = vipQosSettings;
        }

    }

    public static class SetVipQosRsp extends FlatNetworkServiceConstant.AgentRsp {

    }

    public static class DeleteVipQosCmd extends FlatNetworkServiceConstant.AgentCmd {
        private List<VipQosStruct> vipQosSettings;

        public List<VipQosStruct> getVipQosSettings() {
            return vipQosSettings;
        }

        public void setVipQosSettings(List<VipQosStruct> vipQosSettings) {
            this.vipQosSettings = vipQosSettings;
        }
    }

    public static class DeleteVipQosRsp extends FlatNetworkServiceConstant.AgentRsp {

    }

    public static class DeleteVipAllQosCmd extends FlatNetworkServiceConstant.AgentCmd {
        private List<VipQosStruct> vipQosSettings;

        public List<VipQosStruct> getVipQosSettings() {
            return vipQosSettings;
        }

        public void setVipQosSettings(List<VipQosStruct> vipQosSettings) {
            this.vipQosSettings = vipQosSettings;
        }
    }

    public static class DeleteVipAllQosRsp extends FlatNetworkServiceConstant.AgentRsp {

    }

    /* for flat, we need eip uuid, not vipUuid, and only eip is supported in flat network */
    private VipQosStruct replaceVipUuid(VipQosStruct struct) {
        EipVO vo = Q.New(EipVO.class).eq(EipVO_.vipUuid, struct.getVipUuid()).find();
        struct.setVipUuid(vo.getUuid());
        return struct;
    }

    @Override
    public void setVipQos(List<VipQosStruct> structs, String vrUuid, Completion completion) {
        String hostUuid = getHostUuidByVipUuid(structs.get(0).getVipUuid());
        if (hostUuid == null) {
            completion.fail(operr("operation error, vip %s has not bind to vm", structs.get(0).getVipUuid()));
            return;
        }

        for (VipQosStruct struct: structs) {
            replaceVipUuid(struct);
        }
        SetVipQosCmd cmd = new SetVipQosCmd();
        cmd.setVipQosSettings(structs);

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(hostUuid);
        msg.setPath(FLAT_SET_VIP_QOS);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply ar = reply.castReply();
                FlatNetworkServiceConstant.AgentRsp rsp = ar.toResponse(FlatNetworkServiceConstant.AgentRsp.class);
                if (!rsp.success) {
                    completion.fail(operr("operation error, because:%s", rsp.error));
                    return;
                }

                completion.success();
            }
        });
    }

    @Override
    public void deleteVipQos(List<VipQosStruct> structs, Completion completion) {
        String hostUuid = getHostUuidByVipUuid(structs.get(0).getVipUuid());
        if (hostUuid == null) {
            completion.fail(operr("operation error, vip %s has not bind to vm", hostUuid));
            return;
        }

        for (VipQosStruct struct: structs) {
            replaceVipUuid(struct);
        }

        DeleteVipQosCmd cmd = new DeleteVipQosCmd();
        cmd.setVipQosSettings(structs);

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(hostUuid);
        msg.setPath(FLAT_DELETE_VIP_QOS);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply ar = reply.castReply();
                FlatNetworkServiceConstant.AgentRsp rsp = ar.toResponse(FlatNetworkServiceConstant.AgentRsp.class);
                if (!rsp.success) {
                    completion.fail(operr("operation error, because:%s", rsp.error));
                    return;
                }

                completion.success();
            }
        });
    }

    @Override
    public void deleteVipAllQos(List<VipQosStruct> structs, Completion completion) {
        String hostUuid = getHostUuidByVipUuid(structs.get(0).getVipUuid());
        if (hostUuid == null) {
            completion.fail(operr("operation error, vip %s has not bind to vm", hostUuid));
            return;
        }

        for (VipQosStruct struct: structs) {
            replaceVipUuid(struct);
        }

        DeleteVipAllQosCmd cmd = new DeleteVipAllQosCmd();
        cmd.setVipQosSettings(structs);

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(hostUuid);
        msg.setPath(FLAT_DELETE_VIPALL_QOS);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply ar = reply.castReply();
                FlatNetworkServiceConstant.AgentRsp rsp = ar.toResponse(FlatNetworkServiceConstant.AgentRsp.class);
                if (!rsp.success) {
                    completion.fail(operr("operation error, because:%s", rsp.error));
                    return;
                }

                completion.success();
            }
        });
    }

    @Override
    public void AfterApplyFlatEip(List<String> vipUuids, String hostUuid) {

        new While(vipUuids).all((vipUuid, completion) -> {
            List<VipQosVO> vos = Q.New(VipQosVO.class).eq(VipQosVO_.vipUuid, vipUuid).list();
            List<VipQosStruct> structs = transformAndRemoveNull(vos,
                    arg -> vipQosManager.getVipQosStruct(VipQosInventory.valueOf(arg)));

            for (VipQosStruct struct: structs) {
                replaceVipUuid(struct);
            }
            SetVipQosCmd cmd = new SetVipQosCmd();
            cmd.setVipQosSettings(structs);

            KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
            msg.setCommand(cmd);
            msg.setHostUuid(hostUuid);
            msg.setPath(FLAT_SET_VIP_QOS);
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
            bus.send(msg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.debug(String.format("Apply operation for vip [uuid:%s] failed because %s", (String) vipUuid, reply.getError().toString()));
                        completion.done();
                        return;
                    }

                    KVMHostAsyncHttpCallReply ar = reply.castReply();
                    FlatNetworkServiceConstant.AgentRsp rsp = ar.toResponse(FlatNetworkServiceConstant.AgentRsp.class);
                    if (!rsp.success) {
                        logger.debug(String.format("Apply operation for vip [uuid:%s] failed because %s", (String) vipUuid, rsp.error.toString()));
                        completion.done();
                        return;
                    }

                    completion.done();
                }
            });
        }).run(new NopeWhileDoneCompletion());
    }

    @Transactional(readOnly = true)
    private String getHostUuidByVipUuid(String vipUuid) {
        String sql = "select vm.hostUuid from VmInstanceVO vm, VmNicVO nic, EipVO eip where vm.uuid = nic.vmInstanceUuid "+
                "and nic.uuid = eip.vmNicUuid and eip.vipUuid = :vipUuid";
        String hostUuid = SQL.New(sql, String.class).param("vipUuid", vipUuid).limit(1).find();

        return hostUuid;
    }

    @Override
    public String getNetworkServiceProviderType() {
        return FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING;
    }

    @Override
    public void prepareDbInitialValue() {
        SimpleQuery<NetworkServiceProviderVO> query = dbf.createQuery(NetworkServiceProviderVO.class);
        query.add(NetworkServiceProviderVO_.type, SimpleQuery.Op.EQ, FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING);
        NetworkServiceProviderVO rpvo = query.find();
        if (rpvo != null) {
            // check if any network service type missing, if any, complement them
            SimpleQuery<NetworkServiceTypeVO> q = dbf.createQuery(NetworkServiceTypeVO.class);
            q.add(NetworkServiceTypeVO_.networkServiceProviderUuid, SimpleQuery.Op.EQ, rpvo.getUuid());
            List<NetworkServiceTypeVO> refs = q.list();
            Set<String> types = new HashSet<String>();
            for (NetworkServiceTypeVO ref : refs) {
                types.add(ref.getType());
            }

            if (!types.contains(VipQosConstants.VIPQOS_NETWORK_SERVICE_TYPE.toString())) {
                NetworkServiceTypeVO ref = new NetworkServiceTypeVO();
                ref.setNetworkServiceProviderUuid(rpvo.getUuid());
                ref.setType(VipQosConstants.VIPQOS_NETWORK_SERVICE_TYPE.toString());
                dbf.persist(ref);
            }

            return;
        }

        rpvo = new NetworkServiceProviderVO();
        rpvo.setUuid(Platform.getUuid());
        rpvo.setName("Flat Network Service Provider");
        rpvo.setDescription("Flat Network Service Provider");
        rpvo.getNetworkServiceTypes().add(VipQosConstants.VIPQOS_NETWORK_SERVICE_TYPE.toString());
        rpvo.setType(FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING);
        rpvo = dbf.persistAndRefresh(rpvo);
    }
}
