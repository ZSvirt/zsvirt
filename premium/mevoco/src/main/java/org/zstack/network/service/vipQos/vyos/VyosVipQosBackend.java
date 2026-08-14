package org.zstack.network.service.vipQos.vyos;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.service.VirtualRouterHaCallbackInterface;
import org.zstack.header.network.service.VirtualRouterHaCallbackStruct;
import org.zstack.header.network.service.VirtualRouterHaGetCallbackExtensionPoint;
import org.zstack.header.network.service.VirtualRouterHaTask;
import org.zstack.header.vipQos.VipQosInventory;
import org.zstack.header.vipQos.VipQosStruct;
import org.zstack.header.vipQos.VipQosVO;
import org.zstack.header.vipQos.VipQosVO_;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.vip.AfterAcquireVipExtensionPoint;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.vipQos.VipQosBackend;
import org.zstack.network.service.vipQos.VipQosManager;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.ha.VirtualRouterHaBackend;
import org.zstack.network.service.virtualrouter.vip.VipConfigProxy;
import org.zstack.network.service.virtualrouter.vyos.*;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.zstack.core.Platform.operr;

/**
 * Created by liangbo.zhou on 17-6-19.
 */
public class VyosVipQosBackend implements VipQosBackend, VyosPostCreateFlowExtensionPoint,
        VyosPostRebootFlowExtensionPoint, VyosPostStartFlowExtensionPoint, VyosProvisionConfigFlowExtensionPoint,
        AfterAcquireVipExtensionPoint, VirtualRouterHaGetCallbackExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VyosVipQosBackend.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private VipQosManager vipQosManager;
    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    private CloudBus bus;
    @Autowired
    private VirtualRouterHaBackend haBackend;
    @Autowired
    private VipConfigProxy vipProxy;

    public static final String VR_SET_VIP_QOS = "/setvipqos";
    public static final String VR_DELETE_VIP_QOS = "/deletevipqos";
    public static final String VR_DELETE_VIPALL_QOS = "/deletevipallqos";

    public static final String DELETE_VIPQOS_TASK = "/deleteVipQos";
    public static final String SET_VIPQOS_TASK = "/setVipQos";

    private Flow createSyncFlow() {
        return new VyosVipQosSyncFlow();
    }

    @Override
    public Flow vyosPostStartFlow() {
        return createSyncFlow();
    }

    @Override
    public Flow vyosPostCreateFlow() {
        return createSyncFlow();
    }

    @Override
    public Flow vyosPostRebootFlow() {
        return createSyncFlow();
    }

    @Override
    public Flow vyosProvisionConfigFlow() {
        return createSyncFlow();
    }

    public static class SetVipQosCmd extends VirtualRouterCommands.AgentCommand {
        private List<VipQosStruct> vipQosSettings;

        public List<VipQosStruct> getVipQosSettings() {
            return vipQosSettings;
        }

        public void setVipQosSettings(List<VipQosStruct> vipQosSettings) {
            this.vipQosSettings = vipQosSettings;
        }

    }

    public static class SetVipQosRsp extends VirtualRouterCommands.AgentResponse {

    }

    public static class DeleteVipQosCmd extends VirtualRouterCommands.AgentCommand {
        private List<VipQosStruct> vipQosSettings;

        public List<VipQosStruct> getVipQosSettings() {
            return vipQosSettings;
        }

        public void setVipQosSettings(List<VipQosStruct> vipQosSettings) {
            this.vipQosSettings = vipQosSettings;
        }
    }

    public static class DeleteVipQosRsp extends VirtualRouterCommands.AgentResponse {

    }

    public static class DeleteVipAllQosCmd extends VirtualRouterCommands.AgentCommand {
        private List<VipQosStruct> vipQosSettings;

        public List<VipQosStruct> getVipQosSettings() {
            return vipQosSettings;
        }

        public void setVipQosSettings(List<VipQosStruct> vipQosSettings) {
            this.vipQosSettings = vipQosSettings;
        }
    }

    public static class DeleteVipAllQosRsp extends VirtualRouterCommands.AgentResponse {

    }

    private void setVipQosToVirtualRouter(String vrUuid, List<VipQosStruct> structs, Completion completion) {
        VirtualRouterVmVO vr = Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, vrUuid).find();

        SetVipQosCmd cmd = new SetVipQosCmd();
        cmd.setVipQosSettings(structs);

        VirtualRouterAsyncHttpCallMsg vrMsg = new VirtualRouterAsyncHttpCallMsg();
        vrMsg.setVmInstanceUuid(vr.getUuid());
        vrMsg.setPath(VR_SET_VIP_QOS);
        vrMsg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(vrMsg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
        bus.send(vrMsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    VirtualRouterAsyncHttpCallReply ar = reply.castReply();
                    SetVipQosRsp rsp = ar.toResponse(SetVipQosRsp.class);
                    if (rsp.isSuccess()) {
                        completion.success();
                    } else {
                        completion.fail(operr("operation error, because:%s", rsp.getError()));
                    }
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    private void submitSetVipQosToHaRouter(VirtualRouterVmInventory vrInv, List<VipQosStruct> structs, Completion completion) {
        VirtualRouterHaTask task = new VirtualRouterHaTask();
        task.setTaskName(SET_VIPQOS_TASK);
        task.setOriginRouterUuid(vrInv.getUuid());
        task.setJsonData(JSONObjectUtil.toJsonString(structs));
        haBackend.submitVirtualRouterHaTask(task, completion);
    }

    @Override
    public void setVipQos(List<VipQosStruct> structs, String vrUuid, Completion completion) {
        String tarVrUuid = vrUuid;
        if (tarVrUuid == null) {
            List<String> vrUuids = vipProxy.getVrUuidsByNetworkService(VipVO.class.getSimpleName(), structs.get(0).getVipUuid());
            if (vrUuids != null && !vrUuids.isEmpty()) {
                tarVrUuid = vrUuids.get(0);
            }
        }
        if (tarVrUuid == null) {
            completion.success();
            return;
        }

        VirtualRouterVmInventory vrInv = VirtualRouterVmInventory.valueOf(dbf.findByUuid(tarVrUuid, VirtualRouterVmVO.class));
        String publicNic = null;
        for (VmNicInventory nic : vrInv.getVmNics()) {
            if (nic.getL3NetworkUuid().equals(structs.get(0).getL3NetworkUuid())) {
                publicNic = nic.getMac();
                break;
            }
        }
        for (VipQosStruct s : structs) {
            s.setPublicNic(publicNic);
        }

        setVipQosToVirtualRouter(tarVrUuid, structs, new Completion(completion) {
            @Override
            public void success() {
                if (vrUuid == null) {
                    submitSetVipQosToHaRouter(vrInv, structs, completion);
                } else {
                    completion.success();
                }
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void deleteVipQosFromVirtualRouter(String vrUuid, List<VipQosStruct> structs, Completion completion) {
        VirtualRouterVmVO vr = Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, vrUuid).find();
        if (vr.getState() != VmInstanceState.Running) {
            completion.success();
            return;
        }

        DeleteVipQosCmd cmd = new DeleteVipQosCmd();
        cmd.setVipQosSettings(structs);
        VirtualRouterAsyncHttpCallMsg vrMsg = new VirtualRouterAsyncHttpCallMsg();
        vrMsg.setVmInstanceUuid(vr.getUuid());
        vrMsg.setPath(VR_DELETE_VIP_QOS);
        vrMsg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(vrMsg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
        bus.send(vrMsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    VirtualRouterAsyncHttpCallReply ar = reply.castReply();
                    DeleteVipQosRsp rsp = ar.toResponse(DeleteVipQosRsp.class);
                    if (rsp.isSuccess()) {
                        completion.success();
                    } else {
                        completion.fail(operr("operation error, because:%s", rsp.getError()));
                    }
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    private void submitDeleteVipQosToHaRouter(VirtualRouterVmInventory vrInv, List<VipQosStruct> structs, Completion completion) {
        VirtualRouterHaTask task = new VirtualRouterHaTask();
        task.setTaskName(DELETE_VIPQOS_TASK);
        task.setOriginRouterUuid(vrInv.getUuid());
        task.setJsonData(JSONObjectUtil.toJsonString(structs));
        haBackend.submitVirtualRouterHaTask(task, completion);
    }

    @Override
    public void deleteVipQos(List<VipQosStruct> structs, Completion completion) {
        List<String> vrUuids = vipProxy.getVrUuidsByNetworkService(VipVO.class.getSimpleName(), structs.get(0).getVipUuid());
        if (vrUuids == null || vrUuids.isEmpty()) {
            completion.success();
            return;
        }

        String vrUuid = vrUuids.get(0);
        VirtualRouterVmInventory vrInv = VirtualRouterVmInventory.valueOf(dbf.findByUuid(vrUuid, VirtualRouterVmVO.class));
        String publicNic = null;
        for (VmNicInventory nic : vrInv.getVmNics()) {
            if (nic.getL3NetworkUuid().equals(structs.get(0).getL3NetworkUuid())) {
                publicNic = nic.getMac();
                break;
            }
        }
        for (VipQosStruct s : structs) {
            s.setPublicNic(publicNic);
        }

        deleteVipQosFromVirtualRouter(vrUuid, structs, new Completion(completion) {
            @Override
            public void success() {
                submitDeleteVipQosToHaRouter(vrInv, structs, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void deleteVipAllQos(List<VipQosStruct> structs, Completion completion) {
        completion.success();
    }

    @Override
    public void afterAcquireVip(VipInventory inv) {
        List<VipQosStruct> structs = new ArrayList<>();
        List<VipQosVO> vos = Q.New(VipQosVO.class).eq(VipQosVO_.vipUuid, inv.getUuid()).list();
        for (VipQosVO vo: vos) {
            VipQosStruct struct = vipQosManager.getVipQosStruct(VipQosInventory.valueOf(vo));
            structs.add(struct);
        }
        if (!structs.isEmpty()) {
            setVipQos(structs, null, new Completion(null) {
                @Override
                public void success() {
                    logger.debug(String.format("Apply Vyos Vip Qos for Vip [Uuid: %s] successfully", inv.getUuid()));
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    logger.debug(String.format("Apply Vyos Vip Qos for Vip [Uuid: %s] failed:%n%s",
                            inv.getUuid(), errorCode.getReadableDetails()));
                }
            });
        }
    }

    @Override
    public String getNetworkServiceProviderType() {
        return VyosConstants.VYOS_ROUTER_PROVIDER_TYPE;
    }

    @Override
    public List<VirtualRouterHaCallbackStruct> getCallback() {
        List<VirtualRouterHaCallbackStruct> structs = new ArrayList<>();

        VirtualRouterHaCallbackStruct deleteVipQos = new VirtualRouterHaCallbackStruct();
        deleteVipQos.type = DELETE_VIPQOS_TASK;
        deleteVipQos.callback = new VirtualRouterHaCallbackInterface() {
            @Override
            public void callBack(String vrUuid, VirtualRouterHaTask task, Completion completion) {
                VipQosStruct[] structs = JSONObjectUtil.toObject(task.getJsonData(), VipQosStruct[].class);
                VirtualRouterVmInventory vrInv = VirtualRouterVmInventory.valueOf(dbf.findByUuid(vrUuid, VirtualRouterVmVO.class));
                String publicNic = null;
                for (VmNicInventory nic : vrInv.getVmNics()) {
                    if (nic.getL3NetworkUuid().equals(structs[0].getL3NetworkUuid())) {
                        publicNic = nic.getMac();
                        break;
                    }
                }
                for (VipQosStruct s : structs) {
                    s.setPublicNic(publicNic);
                }

                deleteVipQosFromVirtualRouter(vrUuid, Arrays.asList(structs), completion);
            }
        };
        structs.add(deleteVipQos);

        VirtualRouterHaCallbackStruct setVipQos = new VirtualRouterHaCallbackStruct();
        setVipQos.type = SET_VIPQOS_TASK;
        setVipQos.callback = new VirtualRouterHaCallbackInterface() {
            @Override
            public void callBack(String vrUuid, VirtualRouterHaTask task, Completion completion) {
                VipQosStruct[] structs = JSONObjectUtil.toObject(task.getJsonData(), VipQosStruct[].class);
                VirtualRouterVmInventory vrInv = VirtualRouterVmInventory.valueOf(dbf.findByUuid(vrUuid, VirtualRouterVmVO.class));
                String publicNic = null;
                for (VmNicInventory nic : vrInv.getVmNics()) {
                    if (nic.getL3NetworkUuid().equals(structs[0].getL3NetworkUuid())) {
                        publicNic = nic.getMac();
                        break;
                    }
                }
                for (VipQosStruct s : structs) {
                    s.setPublicNic(publicNic);
                }

                setVipQosToVirtualRouter(vrUuid, Arrays.asList(structs), completion);
            }
        };
        structs.add(setVipQos);

        return structs;
    }

}
