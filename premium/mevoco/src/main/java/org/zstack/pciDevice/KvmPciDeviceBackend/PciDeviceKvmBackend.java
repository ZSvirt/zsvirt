package org.zstack.pciDevice.KvmPciDeviceBackend;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.*;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.sriov.VmVfNicVO;
import org.zstack.header.sriov.VmVfNicVO_;
import org.zstack.header.tag.FormTagExtensionPoint;
import org.zstack.header.vm.*;
import org.zstack.identity.AccountManager;
import org.zstack.kvm.*;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO_;
import org.zstack.pciDevice.*;
import org.zstack.pciDevice.specification.mdev.*;
import org.zstack.pciDevice.specification.pci.*;
import org.zstack.pciDevice.virtual.PciDeviceVirtStatus;
import org.zstack.pciDevice.virtual.vfio_mdev.*;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 * Created by weiwang on 10/07/2017.
 */
public class PciDeviceKvmBackend implements PciDeviceBackend,
        HostConnectionReestablishExtensionPoint, FormTagExtensionPoint {
    private static CLogger logger = Utils.getLogger(PciDeviceKvmBackend.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ApiTimeoutManager apiTimeoutManager;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    protected RESTFacade restf;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private PciDeviceManager pciDeviceMgr;

    public static final String GET_PCI_DEVICES = "/pcidevice/get";
    public static final String HOT_PLUG_PCI_DEVICE = "/pcidevice/hotplug";
    public static final String HOT_UNPLUG_PCI_DEVICE = "/pcidevice/hotunplug";
    public static final String ATTACH_PCI_DEVICE_TO_HOST = "/pcidevice/attachtohost";
    public static final String DETACH_PCI_DEVICE_FROM_HOST = "/pcidevice/detachfromhost";
    public static final String CREATE_PCI_DEVICE_ROM_FILE = "/pcidevice/createrom";
    public static final String GENERATE_SRIOV_PCI_DEVICES = "/pcidevice/generate";
    public static final String UNGENERATE_SRIOV_PCI_DEVICES = "/pcidevice/ungenerate";
    public static final String GENERATE_VFIO_MDEV_DEVICES = "/mdevdevice/generate";
    public static final String UNGENERATE_VFIO_MDEV_DEVICES = "/mdevdevice/ungenerate";

    @Override
    public Map<String, Function<String, String>> getTagMappers(Class clz) {
        if (!AddKVMHostMsg.class.isAssignableFrom(clz)) {
            return new HashMap<>();
        }

        return Collections.singletonMap("IOMMU", enabled -> {
            Boolean iommuEnabled = PageTableExtensionBackend.getTextBool(enabled, null);
            if (iommuEnabled == null) {
                throw new IllegalArgumentException(String.format("IOMMU invalid value [%s]", enabled));
            }
            PciDeviceState iommuState = iommuEnabled ? PciDeviceState.Enabled : PciDeviceState.Disabled;
            return PciDeviceSystemTags.HOST_IOMMU_STATE.instantiateTag(
                    Collections.singletonMap(PciDeviceSystemTags.HOST_IOMMU_STATE_TOKEN, iommuState)
            );
        });
    }

    public static class GetPciDevicesCmd extends KVMAgentCommands.AgentCommand {
        public String filterString = "'\\w\\w:\\w\\w\\.\\w' -A1";
        public Boolean enableIommu = false;
        public Boolean skipGrubConfig = false;
    }

    public static class GetPciDevicesRsp extends KVMAgentCommands.AgentResponse {
        public List<PciDeviceTO> pciDevicesInfo = new ArrayList<>();
        public Boolean hostIommuStatus;

        public List<PciDeviceTO> getPciDevicesInfo() {
            return pciDevicesInfo;
        }

        public void setPciDevicesInfo(List<PciDeviceTO> pciDevicesInfo) {
            this.pciDevicesInfo = pciDevicesInfo;
        }

        public Boolean getHostIommuStatus() {
            return hostIommuStatus;
        }

        public void setHostIommuStatus(Boolean hostIommuStatus) {
            this.hostIommuStatus = hostIommuStatus;
        }
    }

    public static class HotPlugPciDeviceCommand extends KVMAgentCommands.AgentCommand {
        public String pciDeviceAddress;
        public String vmUuid;

        public String getPciDeviceAddress() {
            return pciDeviceAddress;
        }

        public void setPciDeviceAddress(String pciDeviceAddress) {
            this.pciDeviceAddress = pciDeviceAddress;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }
    }

    public static class HotPlugPciDeviceRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class HotUnplugPciDeviceCommand extends KVMAgentCommands.AgentCommand {
        public String pciDeviceAddress;
        public String vmUuid;

        public String getPciDeviceAddress() {
            return pciDeviceAddress;
        }

        public void setPciDeviceAddress(String pciDeviceAddress) {
            this.pciDeviceAddress = pciDeviceAddress;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }
    }

    public static class HotUnplugPciDeviceRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class AttachPciDeviceToHostCommand extends KVMAgentCommands.AgentCommand {
        public String pciDeviceAddress;

        public String getPciDeviceAddress() {
            return pciDeviceAddress;
        }

        public void setPciDeviceAddress(String pciDeviceAddress) {
            this.pciDeviceAddress = pciDeviceAddress;
        }
    }

    public static class AttachPciDeviceToHostRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class DetachPciDeviceFromHostCommand extends KVMAgentCommands.AgentCommand {
        public String pciDeviceAddress;

        public String getPciDeviceAddress() {
            return pciDeviceAddress;
        }

        public void setPciDeviceAddress(String pciDeviceAddress) {
            this.pciDeviceAddress = pciDeviceAddress;
        }
    }

    public static class DetachPciDeviceFromHostRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class CreatePciDeviceRomFileCommand extends KVMAgentCommands.AgentCommand {
        public String specUuid;
        public String romContent;
        public String romMd5sum;

        public String getRomContent() {
            return romContent;
        }

        public void setRomContent(String romContent) {
            this.romContent = romContent;
        }

        public String getRomMd5sum() {
            return romMd5sum;
        }

        public void setRomMd5sum(String romMd5sum) {
            this.romMd5sum = romMd5sum;
        }

        public String getSpecUuid() {
            return specUuid;
        }

        public void setSpecUuid(String specUuid) {
            this.specUuid = specUuid;
        }
    }

    public static class CreatePciDeviceRomFileRsp extends KVMAgentCommands.AgentResponse {

    }

    public static class GenerateSriovPciDevicesCommand extends KVMAgentCommands.AgentCommand {
        public String pciDeviceType;
        public String pciDeviceAddress;
        public Integer virtPartNum;
        public Boolean reSplite;

        public String getPciDeviceType() {
            return pciDeviceType;
        }

        public void setPciDeviceType(String pciDeviceType) {
            this.pciDeviceType = pciDeviceType;
        }

        public String getPciDeviceAddress() {
            return pciDeviceAddress;
        }

        public void setPciDeviceAddress(String pciDeviceAddress) {
            this.pciDeviceAddress = pciDeviceAddress;
        }

        public Integer getVirtPartNum() {
            return virtPartNum;
        }

        public void setVirtPartNum(Integer virtPartNum) {
            this.virtPartNum = virtPartNum;
        }

        public Boolean getReSplite() {
            return reSplite;
        }

        public void setReSplite(Boolean reSplite) {
            this.reSplite = reSplite;
        }
    }

    public static class GenerateSriovPciDevicesRsp extends KVMAgentCommands.AgentResponse {

    }

    public static class UngenerateSriovPciDevicesCommand extends KVMAgentCommands.AgentCommand {
        public String pciDeviceType;
        public String pciDeviceAddress;

        public String getPciDeviceType() {
            return pciDeviceType;
        }

        public void setPciDeviceType(String pciDeviceType) {
            this.pciDeviceType = pciDeviceType;
        }

        public String getPciDeviceAddress() {
            return pciDeviceAddress;
        }

        public void setPciDeviceAddress(String pciDeviceAddress) {
            this.pciDeviceAddress = pciDeviceAddress;
        }
    }

    public static class UngenerateSriovPciDevicesRsp extends KVMAgentCommands.AgentResponse {

    }

    public static class GenerateVfioMdevDevicesCommand extends KVMAgentCommands.AgentCommand {
        public String pciDeviceAddress;
        public String mdevSpecTypeId;
        public List<String> mdevUuids;

        public String getPciDeviceAddress() {
            return pciDeviceAddress;
        }

        public void setPciDeviceAddress(String pciDeviceAddress) {
            this.pciDeviceAddress = pciDeviceAddress;
        }

        public String getMdevSpecTypeId() {
            return mdevSpecTypeId;
        }

        public void setMdevSpecTypeId(String mdevSpecTypeId) {
            this.mdevSpecTypeId = mdevSpecTypeId;
        }

        public List<String> getMdevUuids() {
            return mdevUuids;
        }

        public void setMdevUuids(List<String> mdevUuids) {
            this.mdevUuids = mdevUuids;
        }
    }

    public static class GenerateVfioMdevDevicesRsp extends KVMAgentCommands.AgentResponse {
        List<String> mdevUuids;

        public List<String> getMdevUuids() {
            return mdevUuids;
        }

        public void setMdevUuids(List<String> mdevUuids) {
            this.mdevUuids = mdevUuids;
        }
    }

    public static class UngenerateVfioMdevDevicesCommand extends KVMAgentCommands.AgentCommand {
        public String pciDeviceAddress;
        public String mdevSpecTypeId;

        public String getPciDeviceAddress() {
            return pciDeviceAddress;
        }

        public void setPciDeviceAddress(String pciDeviceAddress) {
            this.pciDeviceAddress = pciDeviceAddress;
        }

        public String getMdevSpecTypeId() {
            return mdevSpecTypeId;
        }

        public void setMdevSpecTypeId(String mdevSpecTypeId) {
            this.mdevSpecTypeId = mdevSpecTypeId;
        }
    }

    public static class UngenerateVfioMdevDevicesRsp extends KVMAgentCommands.AgentResponse {

    }

    @Override
    public void attachPciDeviceToVm(PciDeviceInventory pciDevice, String vmUuid, Completion completion) {
        VmInstanceVO vmvo = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid).find();
        if (!(vmvo.getState().equals(VmInstanceState.Running))) {
            logger.debug(String.format("the vm[uuid:%s] to attach pci device[uuid:%s] is not in %s state, no need to hot_plug",
                    vmUuid, pciDevice.getUuid(), VmInstanceState.Running));
            completion.success();
            return;
        }

        HotPlugPciDeviceCommand cmd = new HotPlugPciDeviceCommand();
        cmd.setPciDeviceAddress(pciDevice.getPciDeviceAddress());
        cmd.setVmUuid(vmUuid);
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(vmvo.getHostUuid());
        msg.setPath(HOT_PLUG_PCI_DEVICE);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, vmvo.getHostUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    KVMHostAsyncHttpCallReply r = reply.castReply();
                    HotPlugPciDeviceRsp rsp = r.toResponse(HotPlugPciDeviceRsp.class);
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

    @Override
    public void detachPciDeviceFromVm(PciDeviceInventory pciDevice, String vmUuid, Completion completion) {
        VmInstanceVO vmvo = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid).find();
        if (!(vmvo.getState().equals(VmInstanceState.Running))) {
            logger.debug(String.format("the vm[uuid:%s] to detach pci device[uuid:%s] is not in %s state, no need to hot_unplug",
                    vmUuid, pciDevice.getUuid(), VmInstanceState.Running));
            completion.success();
            return;
        }

        HotUnplugPciDeviceCommand cmd = new HotUnplugPciDeviceCommand();
        cmd.setPciDeviceAddress(pciDevice.getPciDeviceAddress());
        cmd.setVmUuid(vmUuid);
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(vmvo.getHostUuid());
        msg.setPath(HOT_UNPLUG_PCI_DEVICE);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, vmvo.getHostUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    KVMHostAsyncHttpCallReply r = reply.castReply();
                    HotUnplugPciDeviceRsp rsp = r.toResponse(HotUnplugPciDeviceRsp.class);
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

    @Override
    public void attachPciDeviceToHost(PciDeviceInventory pciDevice, Completion completion) {
        AttachPciDeviceToHostCommand cmd = new AttachPciDeviceToHostCommand();
        cmd.setPciDeviceAddress(pciDevice.getPciDeviceAddress());
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(pciDevice.getHostUuid());
        msg.setPath(ATTACH_PCI_DEVICE_TO_HOST);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, pciDevice.getHostUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    KVMHostAsyncHttpCallReply r = reply.castReply();
                    AttachPciDeviceToHostRsp rsp = r.toResponse(AttachPciDeviceToHostRsp.class);
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

    @Override
    public void detachPciDeviceFromHost(PciDeviceInventory pciDevice, Completion completion) {
        DetachPciDeviceFromHostCommand cmd = new DetachPciDeviceFromHostCommand();
        cmd.setPciDeviceAddress(pciDevice.getPciDeviceAddress());
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(pciDevice.getHostUuid());
        msg.setPath(DETACH_PCI_DEVICE_FROM_HOST);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, pciDevice.getHostUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    KVMHostAsyncHttpCallReply r = reply.castReply();
                    DetachPciDeviceFromHostRsp rsp = r.toResponse(DetachPciDeviceFromHostRsp.class);
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

    @Override
    public HypervisorType getHypervisorType() {
        return HypervisorType.valueOf(KVMConstant.KVM_HYPERVISOR_TYPE);
    }

    @Override
    public HypervisorType getHypervisorTypeForReestablishExtensionPoint() {
        return HypervisorType.valueOf(KVMConstant.KVM_HYPERVISOR_TYPE);
    }

    @Override
    public void connectionReestablished(HostInventory inv) throws HostException {
        HostIommuStateType hostIommuState = new HostIommuGetter().getState(inv.getUuid());
        FutureCompletion completion = new FutureCompletion(null);
        SyncPciDeviceMsg msg = new SyncPciDeviceMsg();
        msg.setHostUuid(inv.getUuid());
        msg.setHostIommuState(hostIommuState.toString());
        bus.makeTargetServiceIdByResourceUuid(msg, PciDeviceConstants.SERVICE_ID, inv.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                } else {
                    completion.success();
                }
            }
        });
        completion.await(TimeUnit.SECONDS.toMillis(600));
        if (!completion.isSuccess()) {
            throw new OperationFailureException(completion.getErrorCode());
        }
    }

    @Override
    public void createPciDeviceRomFileInHost(PciDeviceSpecInventory pciSpec, String hostUuid, Completion completion) {
        CreatePciDeviceRomFileCommand cmd = new CreatePciDeviceRomFileCommand();
        cmd.setSpecUuid(pciSpec.getUuid());
        cmd.setRomContent(pciSpec.getRomContent());
        cmd.setRomMd5sum(pciSpec.getRomMd5sum());
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(hostUuid);
        msg.setPath(CREATE_PCI_DEVICE_ROM_FILE);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    KVMHostAsyncHttpCallReply r = reply.castReply();
                    HotPlugPciDeviceRsp rsp = r.toResponse(HotPlugPciDeviceRsp.class);
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

    @Override
    public void generateSriovPciDevices(String hostUuid, PciDeviceInventory pci, Integer virtPartNum, Boolean reSplite, Completion completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("generate sriov pci devices[type:%s, count:%d] on host[uuid:%s]", pci.getType(), virtPartNum, hostUuid));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "call kvm agent to generate sriov pci devices";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        GenerateSriovPciDevicesCommand cmd = new GenerateSriovPciDevicesCommand();
                        cmd.setPciDeviceType(pci.getType().toString());
                        cmd.setPciDeviceAddress(pci.getPciDeviceAddress());
                        cmd.setVirtPartNum(virtPartNum);
                        cmd.setReSplite(reSplite);
                        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                        msg.setCommand(cmd);
                        msg.setHostUuid(hostUuid);
                        msg.setPath(GENERATE_SRIOV_PCI_DEVICES);
                        msg.setNoStatusCheck(true);
                        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
                        bus.send(msg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    KVMHostAsyncHttpCallReply rly = reply.castReply();
                                    GenerateSriovPciDevicesRsp rsp = rly.toResponse(GenerateSriovPciDevicesRsp.class);
                                    if (rsp.isSuccess()) {
                                        trigger.next();
                                    } else {
                                        trigger.fail(operr("operation error, because:%s", rsp.getError()));
                                    }
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        UngenerateSriovPciDevicesCommand cmd = new UngenerateSriovPciDevicesCommand();
                        cmd.setPciDeviceType(pci.getType().toString());
                        cmd.setPciDeviceAddress(pci.getPciDeviceAddress());
                        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                        msg.setCommand(cmd);
                        msg.setHostUuid(hostUuid);
                        msg.setPath(UNGENERATE_SRIOV_PCI_DEVICES);
                        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
                        bus.send(msg, new CloudBusCallBack(completion) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    logger.debug("rollback: ungenerate sriov pci devices in host " + hostUuid);
                                } else {
                                    logger.warn("rollback: faild to ungenerate sriov pci devices in host " + hostUuid);
                                }
                                trigger.rollback();
                            }
                        });
                    }
                });

                // not need to sync pci devices if re-splite
                if (!reSplite) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "pull virtual pci device infos from host";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            SyncPciDeviceMsg smsg = new SyncPciDeviceMsg();
                            smsg.setHostUuid(hostUuid);
                            smsg.setHostIommuState(new HostIommuGetter().getState(hostUuid).toString());
                            bus.makeTargetServiceIdByResourceUuid(smsg, PciDeviceConstants.SERVICE_ID, hostUuid);
                            bus.send(smsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (reply.isSuccess()) {
                                        trigger.next();
                                    } else {
                                        trigger.fail(reply.getError());
                                    }
                                }
                            });
                        }
                    });
                }

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        // update pci device virt status
                        if (pci.getType().equals(PciDeviceType.GPU_Video_Controller) || pci.getType().equals(PciDeviceType.GPU_3D_Controller)) {
                            // per host for gpu pf
                            SQL.New(PciDeviceVO.class)
                                    .eq(PciDeviceVO_.hostUuid, pci.getHostUuid())
                                    .eq(PciDeviceVO_.type, pci.getType())
                                    .eq(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUALIZABLE)
                                    .set(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUALIZED)
                                    .update();
                        } else {
                            // per device for net pf
                            SQL.New(PciDeviceVO.class)
                                    .eq(PciDeviceVO_.uuid, pci.getUuid())
                                    .set(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUALIZED)
                                    .update();

                            SQL.New(HostNetworkInterfaceVO.class)
                                    .eq(HostNetworkInterfaceVO_.hostUuid, pci.getHostUuid())
                                    .eq(HostNetworkInterfaceVO_.pciDeviceAddress, pci.getPciDeviceAddress())
                                    .set(HostNetworkInterfaceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUALIZED.toString())
                                    .update();
                        }
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        // recover pci device virt status
                        if (pci.getType().equals(PciDeviceType.GPU_Video_Controller) || pci.getType().equals(PciDeviceType.GPU_3D_Controller)) {
                            // per host for gpu pf
                            SQL.New(PciDeviceVO.class)
                                    .eq(PciDeviceVO_.hostUuid, pci.getHostUuid())
                                    .eq(PciDeviceVO_.type, pci.getType())
                                    .eq(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUALIZED)
                                    .set(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUALIZABLE)
                                    .update();
                        } else {
                            // per device for net pf
                            SQL.New(PciDeviceVO.class)
                                    .eq(PciDeviceVO_.uuid, pci.getUuid())
                                    .set(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUALIZABLE)
                                    .update();
                        }
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    public void ungenerateSriovPciDevices(String hostUuid, PciDeviceInventory pci, Completion completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("ungenerate sriov pci devices[type:%s] on host[uuid:%s]", pci.getType(), hostUuid));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "auto-detach-vf-nics-before-ungenerate";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (!pci.getType().equals(PciDeviceType.Ethernet_Controller)) {
                            trigger.next();
                            return;
                        }

                        List<String> pciUuids = Q.New(PciDeviceVO.class)
                                .eq(PciDeviceVO_.parentUuid, pci.getUuid())
                                .eq(PciDeviceVO_.status, PciDeviceStatus.Attached)
                                .select(PciDeviceVO_.uuid)
                                .listValues();
                        if (pciUuids.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        List<String> vmNicUuids = Q.New(VmVfNicVO.class)
                                .in(VmVfNicVO_.pciDeviceUuid, pciUuids)
                                .select(VmVfNicVO_.uuid)
                                .listValues();

                        List<DetachNicFromVmMsg> dmsgs = transformAndRemoveNull(vmNicUuids,
                            vnicUuid -> {
                                    String vmUuid = Q.New(VmNicVO.class)
                                            .eq(VmNicVO_.uuid, vnicUuid)
                                            .select(VmNicVO_.vmInstanceUuid)
                                            .findValue();
                                    DetachNicFromVmMsg dmsg = new DetachNicFromVmMsg();
                                    dmsg.setVmInstanceUuid(vmUuid);
                                    dmsg.setVmNicUuid(vnicUuid);
                                    bus.makeTargetServiceIdByResourceUuid(dmsg, VmInstanceConstant.SERVICE_ID, vmUuid);
                                    return dmsg;
                            }
                        );

                        new While<>(dmsgs).all((dmsg, comp) -> {
                            bus.send(dmsg, new CloudBusCallBack(comp) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        logger.error(String.format("failed to auto detach vf nic[uuid:%s] whose pci device has vanished", dmsg.getVmNicUuid()));
                                    } else {
                                        DetachNicFromVmReply drly = reply.castReply();
                                        if (!drly.isSuccess()) {
                                            logger.error(String.format("failed to auto detach vf nic[uuid:%s] whose pci device has vanished", dmsg.getVmNicUuid()));
                                        } else {
                                            logger.debug(String.format("successfully auto detach vf nic[uuid:%s] whose pci device has vanished", dmsg.getVmNicUuid()));
                                        }
                                    }

                                    comp.done();
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "call kvm agent to ungenerate sriov pci devices";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        UngenerateSriovPciDevicesCommand cmd = new UngenerateSriovPciDevicesCommand();
                        cmd.setPciDeviceType(pci.getType().toString());
                        cmd.setPciDeviceAddress(pci.getPciDeviceAddress());
                        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                        msg.setCommand(cmd);
                        msg.setHostUuid(hostUuid);
                        msg.setPath(UNGENERATE_SRIOV_PCI_DEVICES);
                        msg.setNoStatusCheck(true);
                        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
                        bus.send(msg, new CloudBusCallBack(completion) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    KVMHostAsyncHttpCallReply rly = reply.castReply();
                                    UngenerateSriovPciDevicesRsp rsp = rly.toResponse(UngenerateSriovPciDevicesRsp.class);
                                    if (rsp.isSuccess()) {
                                        trigger.next();
                                    } else {
                                        trigger.fail(operr("operation error, because:%s", rsp.getError()));
                                    }
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "pull virtual pci device infos from host";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        SyncPciDeviceMsg smsg = new SyncPciDeviceMsg();
                        smsg.setHostUuid(hostUuid);
                        smsg.setHostIommuState(new HostIommuGetter().getState(hostUuid).toString());
                        bus.makeTargetServiceIdByResourceUuid(smsg, PciDeviceConstants.SERVICE_ID, hostUuid);
                        bus.send(smsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    trigger.next();
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        // update pci device virt status
                        if (pci.getType().equals(PciDeviceType.GPU_Video_Controller) || pci.getType().equals(PciDeviceType.GPU_3D_Controller)) {
                            // per host for gpu pf
                            SQL.New(PciDeviceVO.class)
                                    .eq(PciDeviceVO_.hostUuid, pci.getHostUuid())
                                    .eq(PciDeviceVO_.type, pci.getType())
                                    .eq(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUALIZED)
                                    .set(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUALIZABLE)
                                    .update();
                        } else {
                            // per device for net pf
                            SQL.New(PciDeviceVO.class)
                                    .eq(PciDeviceVO_.uuid, pci.getUuid())
                                    .set(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUALIZABLE)
                                    .update();

                            SQL.New(HostNetworkInterfaceVO.class)
                                    .eq(HostNetworkInterfaceVO_.hostUuid, pci.getHostUuid())
                                    .eq(HostNetworkInterfaceVO_.pciDeviceAddress, pci.getPciDeviceAddress())
                                    .set(HostNetworkInterfaceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUALIZABLE.toString())
                                    .update();
                        }
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        // recover pci device virt status
                        if (pci.getType().equals(PciDeviceType.GPU_Video_Controller) || pci.getType().equals(PciDeviceType.GPU_3D_Controller)) {
                            // per host for gpu pf
                            SQL.New(PciDeviceVO.class)
                                    .eq(PciDeviceVO_.hostUuid, pci.getHostUuid())
                                    .eq(PciDeviceVO_.type, pci.getType())
                                    .eq(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUALIZABLE)
                                    .set(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUALIZED)
                                    .update();
                        } else {
                            // per device for net pf
                            SQL.New(PciDeviceVO.class)
                                    .eq(PciDeviceVO_.uuid, pci.getUuid())
                                    .set(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUALIZED)
                                    .update();

                            SQL.New(HostNetworkInterfaceVO.class)
                                    .eq(HostNetworkInterfaceVO_.hostUuid, pci.getHostUuid())
                                    .eq(HostNetworkInterfaceVO_.pciDeviceAddress, pci.getPciDeviceAddress())
                                    .set(HostNetworkInterfaceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUALIZED.toString())
                                    .update();
                        }
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    public void generateVfioMdevDevices(String hostUuid, PciDeviceInventory pciDevice, MdevDeviceSpecInventory mdevSpec, List<String> mdevUuids, Completion completion) {
        GenerateVfioMdevDevicesCommand cmd = new GenerateVfioMdevDevicesCommand();
        cmd.setPciDeviceAddress(pciDevice.getPciDeviceAddress());
        HashMap<String, String> specMap = JSONObjectUtil.toObject(mdevSpec.getSpecification(), HashMap.class);
        cmd.setMdevSpecTypeId(specMap.getOrDefault("TypeId", "UNKNOWN"));
        cmd.setMdevUuids(mdevUuids);
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(hostUuid);
        msg.setPath(GENERATE_VFIO_MDEV_DEVICES);
        msg.setNoStatusCheck(true);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply rly = reply.castReply();
                GenerateVfioMdevDevicesRsp rsp = rly.toResponse(GenerateVfioMdevDevicesRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("operation error, because:%s", rsp.getError()));
                    return;
                }

                // if re-split after host reboot, then returns no mdev uuids
                List<String> mdevUuids = rsp.getMdevUuids();
                if (mdevUuids == null || mdevUuids.isEmpty()) {
                    completion.success();
                    return;
                }

                // create mdev devices in database
                for (String mdevUuid : mdevUuids) {
                    MdevDeviceVO mdev = new MdevDeviceVO();
                    // mdev uuid return from kvmagent is like `7ce5b19c-e8cd-47c3-9bab-6661ec3539d0`
                    mdev.setUuid(mdevUuid.replaceAll("-", ""));
                    mdev.setName(specMap.getOrDefault("Name", "UNKNOWN") + '_' + mdev.getUuid().substring(0, 8));
                    mdev.setDescription(String.format("mdev device generated from pci device[uuid:%s]", pciDevice.getUuid()));
                    mdev.setHostUuid(hostUuid);
                    mdev.setParentUuid(pciDevice.getUuid());
                    mdev.setMdevSpecUuid(mdevSpec.getUuid());
                    mdev.setType(MdevDeviceType.valueOf(pciDevice.getType().toString()));
                    mdev.setState(MdevDeviceState.Enabled);
                    mdev.setStatus(MdevDeviceStatus.Active);
                    mdev.setChooser(MdevDeviceChooser.None);
                    mdev.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                    mdev.setVendor(pciDevice.getVendor());
                    dbf.persist(mdev);
                    logger.debug("created mdev device: " + mdev.getName());
                }

                // update pci device virt status
                SQL.New(PciDeviceVO.class)
                        .eq(PciDeviceVO_.uuid, pciDevice.getUuid())
                        .set(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.VFIO_MDEV_VIRTUALIZED)
                        .update();

                SQL.New(HostNetworkInterfaceVO.class)
                        .eq(HostNetworkInterfaceVO_.hostUuid, pciDevice.getHostUuid())
                        .eq(HostNetworkInterfaceVO_.pciDeviceAddress, pciDevice.getPciDeviceAddress())
                        .set(HostNetworkInterfaceVO_.virtStatus, PciDeviceVirtStatus.VFIO_MDEV_VIRTUALIZED.toString())
                        .update();

                // update ref
                SQL.New(PciDeviceMdevSpecRefVO.class)
                        .eq(PciDeviceMdevSpecRefVO_.pciDeviceUuid, pciDevice.getUuid())
                        .eq(PciDeviceMdevSpecRefVO_.mdevSpecUuid, mdevSpec.getUuid())
                        .set(PciDeviceMdevSpecRefVO_.effective, true)
                        .update();
                completion.success();
            }
        });
    }

    @Override
    public void unGenerateVfioMdevDevices(String hostUuid, PciDeviceInventory pciDevice, MdevDeviceSpecInventory mdevSpec, List<String> mdevUuids, Completion completion) {
        UngenerateVfioMdevDevicesCommand cmd = new UngenerateVfioMdevDevicesCommand();
        cmd.setPciDeviceAddress(pciDevice.getPciDeviceAddress());
        HashMap<String, String> specMap = JSONObjectUtil.toObject(mdevSpec.getSpecification(), HashMap.class);
        cmd.setMdevSpecTypeId(specMap.getOrDefault("TypeId", "UNKNOWN"));
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(hostUuid);
        msg.setPath(UNGENERATE_VFIO_MDEV_DEVICES);
        msg.setNoStatusCheck(true);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply rly = reply.castReply();
                UngenerateVfioMdevDevicesRsp rsp = rly.toResponse(UngenerateVfioMdevDevicesRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("operation error, because:%s", rsp.getError()));
                    return;
                }

                // delete mdev devices in database
                SQL.New(MdevDeviceVO.class)
                        .eq(MdevDeviceVO_.parentUuid, pciDevice.getUuid())
                        .hardDelete();

                // update pci device virt status
                SQL.New(PciDeviceVO.class)
                        .eq(PciDeviceVO_.uuid, pciDevice.getUuid())
                        .set(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.VFIO_MDEV_VIRTUALIZABLE)
                        .update();

                SQL.New(HostNetworkInterfaceVO.class)
                        .eq(HostNetworkInterfaceVO_.hostUuid, pciDevice.getHostUuid())
                        .eq(HostNetworkInterfaceVO_.pciDeviceAddress, pciDevice.getPciDeviceAddress())
                        .set(HostNetworkInterfaceVO_.virtStatus, PciDeviceVirtStatus.VFIO_MDEV_VIRTUALIZABLE.toString())
                        .update();

                // update ref
                SQL.New(PciDeviceMdevSpecRefVO.class)
                        .eq(PciDeviceMdevSpecRefVO_.pciDeviceUuid, pciDevice.getUuid())
                        .eq(PciDeviceMdevSpecRefVO_.mdevSpecUuid, mdevSpec.getUuid())
                        .set(PciDeviceMdevSpecRefVO_.effective, false)
                        .update();
                completion.success();
            }
        });
    }

    @Override
    public void syncPciDeviceFromHost(final String hostUuid, List<String> pciDeviceAddresses, String hostIommuState, boolean skipGrubConfig, Completion completion) {
        syncPciDeviceFromHost(hostUuid, pciDeviceAddresses, hostIommuState, true, skipGrubConfig, completion);
    }

    // call GET_PCI_DEVICES and sync with PciDeviceVO
    private void syncPciDeviceFromHost(final String hostUuid, List<String> pciDeviceAddresses, String hostIommuState, boolean noStatusCheck, boolean skipGrubConfig, Completion completion) {
        GetPciDevicesCmd cmd = new GetPciDevicesCmd();
        Boolean enableIommu;
        if (StringUtils.isEmpty(hostIommuState)) {
            enableIommu = PciDeviceGlobalConfig.ENABLE_IOMMU.value(Boolean.class);
            logger.debug(String.format("pciDevice enableIommu config: %s", enableIommu));
        } else {
            if (HostIommuStateType.Enabled.toString().equals(hostIommuState)) {
                enableIommu = Boolean.TRUE;
            } else {
                enableIommu = Boolean.FALSE;
            }
        }

        cmd.skipGrubConfig = skipGrubConfig;
        cmd.enableIommu = enableIommu;
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(hostUuid);
        msg.setPath(GET_PCI_DEVICES);
        msg.setNoStatusCheck(noStatusCheck);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    fail();
                    return;
                }
                KVMHostAsyncHttpCallReply r = reply.castReply();
                GetPciDevicesRsp rsp = r.toResponse(GetPciDevicesRsp.class);
                if (!rsp.isSuccess()) {
                    fail();
                    return;
                }

                List<PciDeviceTO> tos = rsp.pciDevicesInfo;
                if (tos == null || tos.isEmpty()) {
                    logger.debug("no pci device info received, which is unlikely");
                    completion.success();
                    return;
                }

                // support custom pci devices
                File customPciDevices = PathUtil.findFileOnClassPath(PciDeviceConstants.CUSTOM_PCI_DEVICES);
                if (customPciDevices != null) {
                    try {
                        JAXBContext jContext = JAXBContext.newInstance(CustomPciDevices.class);
                        Unmarshaller unmarshaller = jContext.createUnmarshaller();
                        CustomPciDevices pciDevices = (CustomPciDevices) unmarshaller.unmarshal(customPciDevices);
                        for (PciDeviceTO to : tos) {
                            for (CustomPciDevices.CustomPciDevice pciDevice : pciDevices.getPciDevices()) {
                                if (!to.getVendorId().equals(pciDevice.getVendorId())) {
                                    continue;
                                }

                                if (!to.getDeviceId().equals(pciDevice.getDeviceId())) {
                                    continue;
                                }

                                if (StringUtils.isNotEmpty(pciDevice.getSubVendorId()) && !pciDevice.getSubVendorId().equals(to.getSubvendorId())) {
                                    continue;
                                }

                                if (StringUtils.isNotEmpty(pciDevice.getSubDeviceId()) && !pciDevice.getSubDeviceId().equals(to.getSubdeviceId())) {
                                    continue;
                                }

                                to.setName(StringUtils.isNotEmpty(pciDevice.getName()) ? pciDevice.getName() : to.getName());
                                to.setDescription(StringUtils.isNotEmpty(pciDevice.getDescription()) ? pciDevice.getDescription() : to.getDescription());
                                to.setType(PciDeviceConstants.Custom);
                                break;
                            }
                        }
                    } catch (JAXBException e) {
                        logger.warn(String.format("failed to parse %s: %s", PciDeviceConstants.CUSTOM_PCI_DEVICES, e.getMessage()));
                    }
                }

                // sync info of specified pci devices if pciDeviceAddresses is not empty
                /*
                if (pciDeviceAddresses != null && !pciDeviceAddresses.isEmpty()) {
                    List<PciDeviceVO> pcis = Q.New(PciDeviceVO.class)
                            .eq(PciDeviceVO_.hostUuid, hostUuid)
                            .in(PciDeviceVO_.pciDeviceAddress, pciDeviceAddresses)
                            .list();
                    if (pcis.isEmpty()) {
                        completion.success();
                        return;
                    }

                    tos.forEach(to -> to.setHostUuid(hostUuid));
                    for (PciDeviceVO pci : pcis) {
                        for (PciDeviceTO to : tos) {
                            if (to.equals(PciDeviceTO.valueOf(pci))) {
                                // Fixes ZSTAC-21304: update virtStatus of pci device after attaching/detaching
                                PciDeviceVirtStatus status = PciDeviceVirtStatus.valueOf(to.getVirtStatus());
                                if (status != pci.getVirtStatus()) {
                                    pci.setVirtStatus(status);
                                    dbf.update(pci);
                                }
                                break;
                            }
                        }
                    }

                    completion.success();
                    return;
                }
                 */

                // sync host iommu
                if (rsp.hostIommuStatus != null) {
                    syncHostIommuStatus(hostUuid, HostIommuStatusType.valueOf(rsp.hostIommuStatus));
                }

                PciDeviceSyncer syncer = new PciDeviceSyncer(hostUuid, tos);
                // sync info of all pci devices in host
                syncer.syncPciWithDb(completion);
            }

            private void fail() {
                completion.fail(operr(String.format("get pci device info from host[uuid:%s] failed", hostUuid)));
            }
        });
    }

    private void syncHostIommuStatus(String hostUuid, HostIommuStatusType status) {
        if (PciDeviceSystemTags.HOST_IOMMU_STATUS.hasTag(hostUuid)) {
            PciDeviceSystemTags.HOST_IOMMU_STATUS.deleteInherentTag(hostUuid);
        }
        SystemTagCreator creator = PciDeviceSystemTags.HOST_IOMMU_STATUS.newSystemTagCreator(hostUuid);
        creator.ignoreIfExisting = false;
        creator.inherent = true;
        creator.setTagByTokens(
                map(
                        e(PciDeviceSystemTags.HOST_IOMMU_STATUS_TOKEN, status.toString())
                )
        );
        creator.create();
    }

    private class PciDeviceSyncer {
        final String hostUuid;
        final List<PciDeviceTO> tos;
        final List<PciDeviceVO> vos = new ArrayList<>();
        final List<PciDeviceSpecVO> pciSpecs = new ArrayList<>();
        final List<PciDeviceOfferingVO> offerings = new ArrayList<>();

        public PciDeviceSyncer(String hostUuid, List<PciDeviceTO> tos) {
            this.hostUuid = hostUuid;
            this.tos = tos;
            this.tos.forEach(to -> to.setHostUuid(hostUuid));
        }

        void prepareSyncPicDevice() {
            vos.clear();
            vos.addAll(Q.New(PciDeviceVO.class).eq(PciDeviceVO_.hostUuid, hostUuid).list());
            pciSpecs.clear();
            pciSpecs.addAll(Q.New(PciDeviceSpecVO.class).list());
            offerings.clear();
            offerings.addAll(Q.New(PciDeviceOfferingVO.class).list());
        }

        void doSyncPciDevice() {
            List<PciDeviceTO> dbTos = PciDeviceTO.valueOf(vos);
            tos.forEach(to -> dbTos.stream().filter(dt -> dt.equals(to)).findFirst().ifPresent(dto -> to.setUuid(dto.getUuid())));
            List<PciDeviceTO> newTos = tos.stream().filter(to -> !dbTos.contains(to)).collect(Collectors.toList());
            List<PciDeviceTO> oldTos = tos.stream().filter(to -> to.getUuid() != null).collect(Collectors.toList());
            List<PciDevicePciDeviceOfferingRefVO> refVOS = new ArrayList<>();

            // 1. create new pci device. we suppose that vf device will not be parents of another vf
            List<PciDeviceTO> parentNewTos = newTos.stream()
                    .filter(t -> !StringUtils.isNotBlank(t.getParentAddress()))
                    .collect(Collectors.toList());
            List<PciDeviceTO> childNewTos = newTos.stream()
                    .filter(t -> StringUtils.isNotBlank(t.getParentAddress()))
                    .collect(Collectors.toList());

            HashMap<String, String> pciAddressUuidMap = new HashMap<>();
            HashMap<PciDeviceType, List<PciDeviceVO>> pciDeviceTypeMap = new HashMap<>();
            List<PciDeviceVO> newVOs = new ArrayList<>();

            for (PciDeviceTO to : parentNewTos) {
                PciDeviceVO pciVo = PciDeviceTO.getPciDeviceVO(to, pciSpecs, offerings, refVOS);

                pciAddressUuidMap.put(pciVo.getPciDeviceAddress(), pciVo.getUuid());
                newVOs.add(pciVo);
                pciDeviceTypeMap.computeIfAbsent(pciVo.getType(), k -> new ArrayList<>()).add(pciVo);
            }

            for (PciDeviceTO to : childNewTos) {
                PciDeviceVO pciVo = PciDeviceTO.getPciDeviceVO(to, pciSpecs, offerings, refVOS);

                String parentUuid = pciAddressUuidMap.get(to.getParentAddress());
                if (parentUuid == null) {
                    parentUuid = Q.New(PciDeviceVO.class)
                            .eq(PciDeviceVO_.hostUuid, hostUuid)
                            .eq(PciDeviceVO_.pciDeviceAddress, to.getParentAddress())
                            .select(PciDeviceVO_.uuid)
                            .limit(1)
                            .findValue();
                    if (parentUuid == null) {
                        logger.error(String.format("virtual pci device[pci:%s, parent pci:%s] can not " +
                                " find parent pci device", pciVo.getPciDeviceAddress(), to.getParentAddress()));
                    } else {
                        /* parent is not new device */
                        pciAddressUuidMap.put(to.getParentAddress(), parentUuid);
                    }
                }
                pciVo.setParentUuid(parentUuid);

                newVOs.add(pciVo);
                pciDeviceTypeMap.computeIfAbsent(pciVo.getType(), k -> new ArrayList<>()).add(pciVo);
            }

            for (Map.Entry<PciDeviceType, List<PciDeviceVO>> e : pciDeviceTypeMap.entrySet()) {
                PciDeviceType pciDeviceType = e.getKey();
                List<PciDeviceVO> pciDeviceVos = e.getValue();

                PciDeviceTypeFactory f = pciDeviceMgr.getPciDeviceTypeFactory(pciDeviceType);
                f.createPciDevices(pciDeviceVos);
            }

            if (!refVOS.isEmpty()) {
                dbf.persistCollection(refVOS);
            }

            // 2. update existing pci devices
            Map<String, PciDeviceVO> vosMap = vos.stream().collect(Collectors.toMap(PciDeviceVO::getUuid, vo -> vo));
            HashMap<PciDeviceType, List<PciDeviceVO>> updatePciDeviceTypeMap = new HashMap<>();
            for (PciDeviceTO to : oldTos) {
                PciDeviceVO pci = vosMap.get(to.getUuid());
                if (pci == null) {
                    logger.warn(String.format("cannot update pci device[uuid:%s] because it doesn't exist", to.getUuid()));
                    continue;
                }

                PciDeviceTO.updatePciDeviceVO(pci, to);
                pciSpecs.stream().filter(s -> s.matchPciDevice(to)).findFirst().ifPresent(spec -> pci.setPciSpecUuid(spec.getUuid()));
                if (pci.getVmInstanceUuid() != null && !pci.getVmInstanceUuid().isEmpty()) {
                    pci.setStatus(PciDeviceStatus.Attached);
                } else if (offerings.stream().anyMatch(offeringVO -> offeringVO.matchPciDevice(pci))) {
                    pci.setStatus(PciDeviceStatus.Active);
                } else {
                    pci.setStatus(PciDeviceStatus.System);
                }

                if (StringUtils.isEmpty(pci.getDevice())) {
                    pci.setDevice(to.getDevice());
                }

                if (StringUtils.isEmpty(pci.getVendor())) {
                    pci.setVendor(to.getVendor());
                }

                pci.setAddonInfo(to.getAddonInfo());
                updatePciDeviceTypeMap.computeIfAbsent(pci.getType(), k -> new ArrayList<>()).add(pci);
                logger.debug(String.format("updated existing pci device[uuid:%s, name:%s]", pci.getUuid(), pci.getName()));
            }


            for (Map.Entry<PciDeviceType, List<PciDeviceVO>> e : updatePciDeviceTypeMap.entrySet()) {
                PciDeviceType pciDeviceType = e.getKey();
                List<PciDeviceVO> pciDeviceVos = e.getValue();

                PciDeviceTypeFactory f = pciDeviceMgr.getPciDeviceTypeFactory(pciDeviceType);
                f.updatePciDevices(pciDeviceVos);
            }

            // 3. delete garbage pci device
            List<PciDeviceTO> garbageTos = new ArrayList<>(dbTos);
            garbageTos.removeAll(tos);
            HashMap<PciDeviceType, List<PciDeviceTO>> garbagePciDeviceTypeMap = new HashMap<>();
            List<String> garbageUuids = garbageTos.stream().map(PciDeviceTO::getUuid).collect(Collectors.toList());
            for (PciDeviceTO pciTo :garbageTos) {
                garbagePciDeviceTypeMap.computeIfAbsent(PciDeviceType.valueOf(pciTo.getType()),
                        k -> new ArrayList<>()).add(pciTo);
            }

            for (Map.Entry<PciDeviceType, List<PciDeviceTO>> e : garbagePciDeviceTypeMap.entrySet()) {
                PciDeviceType pciDeviceType = e.getKey();
                List<PciDeviceTO> pciDeviceTos = e.getValue();
                PciDeviceTypeFactory f = pciDeviceMgr.getPciDeviceTypeFactory(pciDeviceType);
                f.removePciDevices(pciDeviceTos);
            }

            /* 4. move to step #1
             link virtual pci devices with physical pci devices
            List<PciDeviceTO> vTos = newTos.stream()
                    .filter(t -> PciDeviceVirtStatus.SRIOV_VIRTUAL.isEqual(t.getVirtStatus())
                            && StringUtils.isNotBlank(t.getParentAddress()))
                    .collect(Collectors.toList());

            List<PciDeviceVO> virtualPcis = new ArrayList<>();
            Map<String, String> physicalPcis = new HashMap<>();
            Map<String, PciDeviceVO> newVOsMap = newVOs.stream().collect(Collectors.toMap(PciDeviceVO::getUuid, vo -> vo));
            for (PciDeviceTO vTo : vTos) {
                String parentUuid;
                PciDeviceVO virtual = newVOsMap.get(vTo.getUuid());
                if (physicalPcis.containsKey(vTo.getParentAddress())) {
                    parentUuid = physicalPcis.get(vTo.getParentAddress());
                } else {
                    parentUuid = Q.New(PciDeviceVO.class)
                            .eq(PciDeviceVO_.hostUuid, hostUuid)
                            .eq(PciDeviceVO_.pciDeviceAddress, vTo.getParentAddress())
                            .select(PciDeviceVO_.uuid)
                            .limit(1)
                            .findValue();
                    physicalPcis.put(vTo.getParentAddress(), parentUuid);
                }

                if (StringUtils.equals(parentUuid, virtual.getParentUuid())) {
                    continue;
                }

                virtual.setParentUuid(parentUuid);
                virtualPcis.add(virtual);
                logger.debug(String.format("update parent uuid of pci device[uuid:%s] to [%s]", vTo.getUuid(), parentUuid));
            }

            if (!virtualPcis.isEmpty()) {
                dbf.updateCollection(virtualPcis);
            }*/

            // 5. clean up virtual pci devices that are created bypass zstack
            List<String> pfOutZstack = Q.New(PciDeviceVO.class)
                    .eq(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.VIRTUALIZED_BYPASS_ZSTACK)
                    .select(PciDeviceVO_.uuid).listValues();

            if (!pfOutZstack.isEmpty()) {
                SQL.New(PciDeviceVO.class)
                        .eq(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUAL)
                        .in(PciDeviceVO_.parentUuid, pfOutZstack)
                        .hardDelete();
                logger.debug(String.format("virtual pci devices whose parent uuid in [uuids:%s] are deleted, " +
                        "because they are generated bypass zstack.", pfOutZstack));
            }

            // 6. after sync pci device
            vos.removeIf(vo -> garbageUuids.contains(vo.getUuid()));
        }

        void doCreateOrUpdatePciDeviceSpec() {
            List<PciDeviceTO> distinctTOs = tos.stream().collect(Collectors.collectingAndThen(
                    Collectors.toCollection(() -> new TreeSet<>(Comparator
                            .comparing(PciDeviceTO::getVendorId)
                            .thenComparing(PciDeviceTO::getDeviceId)
                            .thenComparing(PciDeviceTO::getSubvendorId, Comparator.nullsFirst(Comparator.naturalOrder()))
                            .thenComparing(PciDeviceTO::getSubdeviceId, Comparator.nullsFirst(Comparator.naturalOrder()))
                            .thenComparing(PciDeviceTO::getMaxPartNum))),
                    ArrayList::new));

            List<PciDeviceSpecVO> newSpecs = new ArrayList<>();
            List<PciDeviceSpecVO> oldSpecs = new ArrayList<>();
            for (PciDeviceTO to : distinctTOs) {
                Optional<PciDeviceSpecVO> op = pciSpecs.stream().filter(spec -> spec.matchPciDevice(to)).findFirst();
                if (!op.isPresent()) {
                    // 1. create new pci device spec
                    newSpecs.add(PciDeviceTO.createPciDeviceSpec(to));
                    logger.debug(String.format("create new pci device spec[%s:%s:%s:%s]",
                            to.getVendorId(), to.getDeviceId(), to.getSubvendorId(), to.getSubdeviceId()));
                } else {
                    // 2. update existing pci device spec
                    PciDeviceSpecVO spec = op.get();
                    PciDeviceTO.updatePciDeviceSpec(spec, to);
                    oldSpecs.add(spec);
                    logger.debug(String.format("updated existing pci device spec[%s:%s:%s:%s]",
                            to.getVendorId(), to.getDeviceId(), to.getSubvendorId(), to.getSubdeviceId()));

                    // when upgrade from 3.3.1, there may be some orphan pci device specs
                    if (spec.getAccountUuid() == null) {
                        dbf.persist(AccountResourceRefVO.forOwner(
                                AccountConstant.INITIAL_SYSTEM_ADMIN_UUID,
                                spec.getUuid(),
                                PciDeviceSpecVO.class
                        ));
                    }
                }
            }

            if (!newSpecs.isEmpty()) {
                dbf.persistCollection(newSpecs);
                pciSpecs.addAll(newSpecs);
            }

            if (!oldSpecs.isEmpty()) {
                dbf.updateCollection(oldSpecs);
            }
        }

        void doDeleteGarbagePciDeviceSpec() {
            List<String> garbagePciSpecUuids;
            List<String> pciSpecUuids = Q.New(PciDeviceVO.class)
                    .notNull(PciDeviceVO_.pciSpecUuid)
                    .select(PciDeviceVO_.pciSpecUuid)
                    .groupBy(PciDeviceVO_.pciSpecUuid)
                    .listValues();

            if (pciSpecUuids.isEmpty()) {
                garbagePciSpecUuids = Q.New(PciDeviceSpecVO.class).select(PciDeviceSpecVO_.uuid).listValues();
            } else {
                garbagePciSpecUuids = Q.New(PciDeviceSpecVO.class)
                        .select(PciDeviceSpecVO_.uuid)
                        .notIn(PciDeviceSpecVO_.uuid, pciSpecUuids)
                        .listValues();
            }

            if (!garbagePciSpecUuids.isEmpty()) {
                List<String> garbagePciSpecNames = Q.New(PciDeviceSpecVO.class)
                        .in(PciDeviceSpecVO_.uuid, garbagePciSpecUuids)
                        .select(PciDeviceSpecVO_.name)
                        .listValues();
                SQL.New(PciDeviceSpecVO.class).in(PciDeviceSpecVO_.uuid, garbagePciSpecUuids).hardDelete();
                logger.debug(String.format("deleted pci specs[uuids:%s, names:%s] because they are not existing any more",
                        garbagePciSpecUuids, garbagePciSpecNames));

                // delete VmInstancePciDeviceSpecRef
                SQL.New(VmInstancePciDeviceSpecRefVO.class).in(VmInstancePciDeviceSpecRefVO_.pciSpecUuid, garbagePciSpecUuids).hardDelete();
                logger.debug(String.format("clean up VmInstancePciDeviceSpecRefVO because pci specs[uuids:%s] vanished", garbagePciSpecUuids));
            }
        }

        void doSyncMdevDeviceSpecs() {
            final String key = "Name";
            List<MdevDeviceSpecVO> mdevSpecs = Q.New(MdevDeviceSpecVO.class).list();
            tos.stream().filter(to -> CollectionUtils.isNotEmpty(to.getMdevSpecifications())).forEach(to -> {
                logger.debug("pci device mdev specification: " + to.getMdevSpecifications());
                for (Map<String, String> specItem : to.getMdevSpecifications()) {
                    MdevDeviceSpecVO dbSpec = null;
                    for (MdevDeviceSpecVO spec : mdevSpecs) {
                        HashMap specMap = JSONObjectUtil.toObject(spec.getSpecification(), HashMap.class);
                        if (specItem.containsKey(key) && specMap.containsKey(key) && specItem.get(key).equals(specMap.get(key))) {
                            dbSpec = spec;
                            break;
                        }
                    }

                    if (dbSpec == null) {
                        // 1. create new mdev specs
                        dbSpec = new MdevDeviceSpecVO();
                        dbSpec.setUuid(Platform.getUuid());
                        dbSpec.setName(specItem.getOrDefault(key, "UNKNOWN"));
                        dbSpec.setSpecification(JSONObjectUtil.toJsonString(specItem));
                        dbSpec.setType(MdevDeviceType.valueOf(to.getType().toString()));
                        dbSpec.setState(MdevDeviceSpecState.Enabled);
                        dbSpec.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                        dbf.persist(dbSpec);
                        logger.debug(String.format("create new mdev device spec[uuid:%s, name:%s]",
                                dbSpec.getUuid(), dbSpec.getName()));
                    } else {
                        // 2. update existing mdev specs
                        dbSpec.setSpecification(JSONObjectUtil.toJsonString(specItem));
                        dbSpec.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                        dbf.update(dbSpec);
                        logger.debug(String.format("updated existing mdev device spec[uuid:%s, name:%s]",
                                dbSpec.getUuid(), dbSpec.getName()));
                    }

                    // 3. create PciDeviceMdevSpecRefVO if not exist
                    boolean exists = Q.New(PciDeviceMdevSpecRefVO.class)
                            .eq(PciDeviceMdevSpecRefVO_.pciDeviceUuid, to.getUuid())
                            .eq(PciDeviceMdevSpecRefVO_.mdevSpecUuid, dbSpec.getUuid())
                            .isExists();

                    if (!exists) {
                        PciDeviceMdevSpecRefVO ref = new PciDeviceMdevSpecRefVO();
                        ref.setMdevSpecUuid(dbSpec.getUuid());
                        ref.setPciDeviceUuid(to.getUuid());
                        dbf.persist(ref);
                        logger.debug(String.format("create PciDeviceMdevSpecRef for pci device[uuid:%s] and mdev spec[uuid:%s]",
                                ref.getPciDeviceUuid(), ref.getMdevSpecUuid()));
                    }
                }
            });

            // 4. delete garbage mdev specs
            cleanGarbageMdevSpecInDB();
        }

        void cleanGarbageMdevSpecInDB() {
            List<String> garbageMdevSpecUuids;
            List<String> mdevSpecUuids = Q.New(PciDeviceMdevSpecRefVO.class)
                    .notNull(PciDeviceMdevSpecRefVO_.mdevSpecUuid)
                    .select(PciDeviceMdevSpecRefVO_.mdevSpecUuid)
                    .groupBy(PciDeviceMdevSpecRefVO_.mdevSpecUuid)
                    .listValues();

            if (mdevSpecUuids.isEmpty()) {
                garbageMdevSpecUuids = Q.New(MdevDeviceSpecVO.class).select(MdevDeviceSpecVO_.uuid).listValues();
            } else {
                garbageMdevSpecUuids = Q.New(MdevDeviceSpecVO.class)
                        .select(MdevDeviceSpecVO_.uuid)
                        .notIn(MdevDeviceSpecVO_.uuid, mdevSpecUuids)
                        .listValues();
            }

            if (!garbageMdevSpecUuids.isEmpty()) {
                List<String> garbageMdevSpecNames = Q.New(MdevDeviceSpecVO.class)
                        .in(MdevDeviceSpecVO_.uuid, garbageMdevSpecUuids)
                        .select(MdevDeviceSpecVO_.name)
                        .listValues();
                SQL.New(AccountResourceRefVO.class).in(AccountResourceRefVO_.resourceType, garbageMdevSpecUuids).hardDelete();
                SQL.New(MdevDeviceSpecVO.class).in(MdevDeviceSpecVO_.uuid, garbageMdevSpecUuids).hardDelete();
                logger.debug(String.format("deleted mdev specs[uuids:%s, names:%s] because they are not existing any more",
                        garbageMdevSpecUuids, garbageMdevSpecNames));

                // delete VmInstanceMdevDeviceSpecRef
                SQL.New(VmInstanceMdevDeviceSpecRefVO.class).in(VmInstanceMdevDeviceSpecRefVO_.mdevSpecUuid, garbageMdevSpecUuids).hardDelete();
                logger.debug(String.format("clean up VmInstanceMdevDeviceSpecRefVO because mdev specs[uuids:%s] vanished", garbageMdevSpecUuids));
            }
        }

        void afterSyncPciWithDb() {
            List<AfterSyncPciDeviceExtensionPoint> exps = pluginRgty.getExtensionList(AfterSyncPciDeviceExtensionPoint.class);
            for (AfterSyncPciDeviceExtensionPoint exp : exps) {
                exp.afterSyncPciDeviceVO(this.hostUuid, this.tos);
            }
        }

        void syncPciWithDb(Completion completion) {
            if (CollectionUtils.isEmpty(tos)) {
                completion.success();
                return;
            }

            // order matters
            prepareSyncPicDevice();
            doCreateOrUpdatePciDeviceSpec();
            doSyncPciDevice();
            doDeleteGarbagePciDeviceSpec();
            doSyncMdevDeviceSpecs();
            afterSyncPciWithDb();
            completion.success();
        }

        void syncEthPciWithDb(Completion completion) {
            cleanPciWhenHostIommuDisabled();
            if (CollectionUtils.isEmpty(tos)) {
                completion.success();
                return;
            }

            tos.removeIf(to -> !PciDeviceType.Ethernet_Controller.isEqual(to.getType()));

            if (CollectionUtils.isEmpty(tos)) {
                completion.success();
                return;
            }

            syncPciWithDb(completion);
        }

        void cleanMdevDevices() {
            boolean mDevExists = Q.New(MdevDeviceVO.class)
                    .eq(MdevDeviceVO_.hostUuid, hostUuid)
                    .isExists();
            if (!mDevExists) {
                return;
            }

            SQL.New(MdevDeviceVO.class)
                    .eq(MdevDeviceVO_.hostUuid, hostUuid)
                    .hardDelete();
            logger.debug(String.format("delete all mdev devices in host[uuid:%s] due to iommu-disabled state", hostUuid));
            cleanGarbageMdevSpecInDB();
        }

        boolean anyPciDevicesExistsInDB() {
            return Q.New(PciDeviceVO.class)
                    .eq(PciDeviceVO_.hostUuid, hostUuid)
                    .isExists();
        }

        void cleanPciWhenHostIommuDisabled() {
            if (anyPciDevicesExistsInDB()) {
                cleanMdevDevices();
            }
        }
    }
}
