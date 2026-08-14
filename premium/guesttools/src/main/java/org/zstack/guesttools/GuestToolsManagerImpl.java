package org.zstack.guesttools;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.appliancevm.ApplianceVmConstant;
import org.zstack.compute.host.MevocoKVMAgentCommands;
import org.zstack.compute.host.MevocoKVMConstant;
import org.zstack.compute.sriov.VmVfNicHaStateChangeExtensionPoint;
import org.zstack.compute.vm.StaticIpOperator;
import org.zstack.compute.vm.VmConfigSyncHelper;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SingleFlightTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.guesttools.advanced.*;
import org.zstack.guesttools.kvm.GuestToolsKvmCommands;
import org.zstack.guesttools.kvm.GuestToolsKvmCommands.GetVmGuestToolsInfoRsp;
import org.zstack.header.AbstractService;
import org.zstack.header.configuration.VmCustomSpecificationStruct;
import org.zstack.header.core.*;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.IpAllocatedReason;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.sriov.*;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO;
import org.zstack.header.vm.*;
import org.zstack.identity.AccountManager;
import org.zstack.image.ImageSystemTags;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.kvm.KVMPingAgentNoFailureExtensionPoint;
import org.zstack.network.l3.IpRangeHelper;
import org.zstack.network.service.DnsUtils;
import org.zstack.network.service.HostRouteUtils;
import org.zstack.network.service.HostRouteUtils.HostRouteInfo;
import org.zstack.network.service.MtuGetter;
import org.zstack.network.service.NetworkServiceManager;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.vip.VipVO_;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.storage.snapshot.group.RevertVmFromSnapShotGroupExtension;
import org.zstack.storage.snapshot.group.VolumeSnapshotGroupConstant;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.utils.path.PathUtil;

import javax.persistence.TypedQuery;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.*;
import static org.zstack.guesttools.GuestToolsConstant.FLOW_CHAIN_KEY_GUEST_TOOLS_VERSION;
import static org.zstack.guesttools.GuestToolsConstant.GUEST_TOOLS_ARCH_GENERAL;
import static org.zstack.guesttools.header.GuestToolsErrors.NO_GUEST_TOOLS_FILES;
import static org.zstack.header.vm.VmInstanceConstant.USER_VM_TYPE;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by GuoYi on 2019-09-17.
 */
public class GuestToolsManagerImpl extends AbstractService implements GuestToolsManager, VmPreMigrationExtensionPoint,
        BeforeVmInstanceStopExtensionPoint, VmInstanceStartNewCreatedVmExtensionPoint, SetVmHostnameFlowInterface,
        KVMPingAgentNoFailureExtensionPoint, VmVfNicHaStateChangeExtensionPoint, L3NetworkDnsUpdateExtensionPoint,
        VmDetachNicExtensionPoint, RevertVmFromSnapShotGroupExtension, PreVmInstantiateResourceExtensionPoint,
        VmJustBeforeDeleteFromDbExtensionPoint {
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected AnsibleFacade asf;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    private NetworkServiceManager nwServiceMgr;
    @Autowired
    protected RESTFacade restf;
    @Autowired
    private EventFacade evf;
    @Autowired
    private ResourceConfigFacade rcf;
    @Autowired
    protected AccountManager acntMgr;

    private static final CLogger logger = Utils.getLogger(GuestToolsManagerImpl.class);
    private static final StaticIpOperator ipOperator = new StaticIpOperator();
    private static final VmConfigSyncHelper vmConfigSyncHelper = new VmConfigSyncHelper();
    private Map<String, GuestToolsHypervisorBackend> guestToolsHypervisorBackends = new HashMap<>();
    private List<String> GUEST_OS_SUPPORT_QGA_GUEST_TOOLS = new ArrayList<>();

    private String GUEST_TOOL_STATE_WITHOUT_HOST_QUEUE = "guestToolStateWithoutHostQueue";

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage)msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof DownloadGuestToolsIsoToHostMsg) {
            handle((DownloadGuestToolsIsoToHostMsg) msg);
        } else if (msg instanceof AttachGuestToolsIsoToVmMsg) {
            handle((AttachGuestToolsIsoToVmMsg) msg);
        } else if (msg instanceof DetachGuestToolsIsoFromVmMsg) {
            handle((DetachGuestToolsIsoFromVmMsg) msg);
        } else if (msg instanceof UpdateVmNetworkConfigMsg) {
            handle((UpdateVmNetworkConfigMsg) msg);
        } else if (msg instanceof GetVmGuestToolsInfoMsg) {
            handle((GetVmGuestToolsInfoMsg) msg);
        }  else if (msg instanceof UpdateGuestToolsStateMsg) {
            handle((UpdateGuestToolsStateMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIGetLatestGuestToolsForVmMsg) {
            handle((APIGetLatestGuestToolsForVmMsg) msg);
        } else if (msg instanceof APIAttachGuestToolsIsoToVmMsg) {
            handle((APIAttachGuestToolsIsoToVmMsg) msg);
        } else if (msg instanceof APIGetVmGuestToolsInfoMsg) {
            handle((APIGetVmGuestToolsInfoMsg) msg);
        } else if (msg instanceof APIUpdateVmNetworkConfigMsg) {
            handle((APIUpdateVmNetworkConfigMsg) msg);
        } else if (msg instanceof APIDetachGuestToolsIsoFromVmMsg) {
            handle((APIDetachGuestToolsIsoFromVmMsg) msg);
        } else if (msg instanceof APIUpdateGuestToolsStateMsg) {
            handle((APIUpdateGuestToolsStateMsg) msg);
        } else if (msg instanceof APICreateVmCustomSpecificationMsg) {
            handle((APICreateVmCustomSpecificationMsg) msg);
        } else if (msg instanceof APIDeleteVmCustomSpecificationMsg) {
            handle((APIDeleteVmCustomSpecificationMsg) msg);
        } else if (msg instanceof APIUpdateVmCustomSpecificationMsg) {
            handle((APIUpdateVmCustomSpecificationMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private boolean isGuestOsSupportQga(String guestOsType) {
        if (guestOsType == null || guestOsType.equals("")) {
            return false;
        }

        boolean supportQga = false;
        for (String osType : GUEST_OS_SUPPORT_QGA_GUEST_TOOLS) {
            if (guestOsType.toLowerCase().contains(osType)) {
                supportQga = true;
                break;
            }
        }

        return supportQga;
    }

    private GuestToolsStateVO qgaStateChange(String vmUuid, GuestToolsZWatchStateEvent zevent, GuestToolsQgaStateEvent qevent,
                                String osType, String platform, String zversion, String qversion, Boolean noversion) {
        if (zevent == null) {
            zevent = GuestToolsZWatchStateEvent.noOperation;
        }
        if (qevent == null) {
            qevent = GuestToolsQgaStateEvent.noOperation;
        }
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("get vm[uuid:%s] qga event %s, zwatch event: %s, osType: %s, platform: %s, qga version %s, zwatch version: %s",
                    vmUuid, qevent.toString(), zevent, osType, platform, qversion, zversion));
        }
        GuestToolsStateVO stateVO = dbf.findByUuid(vmUuid, GuestToolsStateVO.class);
        if (stateVO == null) {
            logger.debug(String.format("get guest tools state failed for vm[uuid:%s]", vmUuid));
            return null;
        }

        if (logger.isTraceEnabled()) {
            logger.trace(String.format("guest tools state: %s", JSONObjectUtil.toJsonString(stateVO)));
        }

        if (qevent == GuestToolsQgaStateEvent.noOperation && zevent == GuestToolsZWatchStateEvent.noOperation) {
            return stateVO;
        }

        boolean changed = false;
        GuestToolsStateInventory inv = GuestToolsStateInventory.valueOf(stateVO);
        if (!StringUtils.isEmpty(osType) && !osType.equals(stateVO.getOsType())) {
            stateVO.setOsType(osType);
            changed = true;
        }

        if (!StringUtils.isEmpty(platform) && !platform.equals(stateVO.getPlatform())) {
            stateVO.setPlatform(platform);
            changed = true;
        }

        GuestToolsZWatchState zstate = stateVO.getZwatchState();
        GuestToolsQgaState qstate = stateVO.getQgaState();

        GuestToolsZWatchState nz = zstate.nextState(zevent);
        if (!zstate.equals(nz)) {
            stateVO.setZwatchState(nz);
            changed = true;
        }

        GuestToolsQgaState nq = qstate.nextState(qevent);
        osType = stateVO.getOsType();
        boolean supportQga = noversion || isGuestOsSupportQga(osType);
        if (supportQga) {
            /* zwatch is installed, but qga is not installed, set qga state to NotUpgraded */
            if (qstate.equals(GuestToolsQgaState.NotInstalled)
                    && nq.equals(GuestToolsQgaState.NotInstalled)
                    && nz.equals(GuestToolsZWatchState.Running)) {
                nq = GuestToolsQgaState.NotUpgraded;
            }

            if (!stateVO.getQgaState().equals(nq)) {
                stateVO.setQgaState(nq);
                changed = true;
            }

            if (!StringUtils.isEmpty(qversion) && !qversion.equals(stateVO.getVersion())) {
                stateVO.setVersion(qversion);
                changed = true;
            } else if (!StringUtils.isEmpty(zversion) && !zversion.equals(stateVO.getVersion())) {
                stateVO.setVersion(zversion);
                changed = true;
            }
        } else {
            if (!StringUtils.isEmpty(zversion) && !zversion.equals(stateVO.getVersion())) {
                stateVO.setVersion(zversion);
                changed = true;
            }
            /* when upgrade to 4.6.11, if vm has zwatch installed, qga state will be set as NotUpgraded
             * if guest os is not supported, set qga state to not running, to avoid jira:
             * http://jira.zstack.io/browse/ZSTAC-55772
             * */
            if (qstate == GuestToolsQgaState.NotUpgraded && nq == GuestToolsQgaState.NotUpgraded
                    && !StringUtils.isEmpty(osType) && !isGuestOsSupportQga(osType)) {
                stateVO.setQgaState(GuestToolsQgaState.NotRunning);
                changed = true;
            }
        }


        if (changed) {
            stateVO = dbf.updateAndRefresh(stateVO);
            logger.debug(String.format("vm[uuid:%s] guest tools state changed from %s to %s",
                    vmUuid, JSONObjectUtil.toJsonString(inv),
                    JSONObjectUtil.toJsonString(GuestToolsStateInventory.valueOf(stateVO))));

            if (!GuestToolsQgaState.Running.equals(qstate) &&
                    GuestToolsQgaState.Running.equals(stateVO.getQgaState())) {
                logger.debug(String.format("set sync dns tag for vm[uuid:%s]", vmUuid));
                vmConfigSyncHelper.setVmSyncDns(vmUuid);
            }
        }

        return stateVO;
    }

    // TODO: replace kvm ping hardcode
    @Override
    public void kvmPingAgentNoFailure(KVMHostInventory host, NoErrorCompletion completion) {
        if (!HostStatus.Connected.toString().equals(host.getStatus())) {
            completion.done();
            return;
        }

        if (Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.type, USER_VM_TYPE)
                .eq(VmInstanceVO_.hostUuid, host.getUuid())
                .eq(VmInstanceVO_.state, VmInstanceState.Running)
                .count() == 0) {
            completion.done();
            return;
        }

        UpdateGuestToolsStateMsg umsg = new UpdateGuestToolsStateMsg();
        umsg.setHostUuid(host.getUuid());
        //QGA version info embed in zwatch metric, QGA state info use self-report update instead mn actively obtain
        umsg.getServiceTypeList().remove(GuestToolsServiceType.QGA);
        bus.makeTargetServiceIdByResourceUuid(umsg, GuestToolsConstant.SERVICE_ID, host.getUuid());
        bus.send(umsg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to update guest tools state for kvm host[uuid:%s], %s",
                            host.getUuid(), reply.getError()));
                }
            }
        });

        // do not block the ping operation
        completion.done();
    }

    @Override
    public void afterDnsUpdated(String l3NetworkUuid, Integer ipVersion) {
        List<String> vmUuidsWithL3 = Q.New(VmNicVO.class)
                .eq(VmNicVO_.l3NetworkUuid, l3NetworkUuid)
                .select(VmNicVO_.vmInstanceUuid)
                .listValues();

        if (vmUuidsWithL3.isEmpty()) {
            return;
        }

        List<String> vmUuids = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.uuid)
                .eq(VmInstanceVO_.platform, ImagePlatform.Windows.toString())
                .in(VmInstanceVO_.uuid, vmUuidsWithL3)
                .listValues();

        vmUuids.addAll(Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.uuid)
                .notEq(VmInstanceVO_.platform, ImagePlatform.Windows.toString())
                .in(VmInstanceVO_.uuid, vmUuidsWithL3)
                .eq(VmInstanceVO_.defaultL3NetworkUuid, l3NetworkUuid)
                .listValues());

        if (vmUuids.isEmpty()) {
            return;
        }

        List<String> vmUuidsWithoutTools = Q.New(GuestToolsStateVO.class)
                .select(GuestToolsStateVO_.vmInstanceUuid)
                .eq(GuestToolsStateVO_.qgaState, GuestToolsQgaState.NotInstalled)
                .eq(GuestToolsStateVO_.zwatchState, GuestToolsZWatchState.NotInstalled)
                .in(GuestToolsStateVO_.vmInstanceUuid, vmUuids)
                .listValues();

        vmUuids.removeAll(vmUuidsWithoutTools);
        vmUuids.removeAll(DnsUtils.getVmDnsListOnL3(vmUuids, ipVersion, l3NetworkUuid)
                .stream()
                .map(VmDnsVO::getVmInstanceUuid)
                .collect(Collectors.toSet()));

        for (String uuid: vmUuids) {
            vmConfigSyncHelper.setVmSyncPorts(uuid);
        }
    }

    @Override
    public void preDetachNic(VmNicInventory nic) {

    }

    @Override
    public void beforeDetachNic(VmNicInventory nic) {
        VmInstanceVO vm = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, nic.getVmInstanceUuid())
                .find();

        if (vm == null || vm.getState() == VmInstanceState.Running) {
            return;
        }

        String defaultNicUuid = getVmDefaultNicUuid(vm.getVmNics(), vm.getDefaultL3NetworkUuid());

        if (Objects.equals(nic.getUuid(), defaultNicUuid)) {
            logger.debug(String.format("default nic is detached, set vm[uuid: %s] sync ports", vm.getUuid()));
            vmConfigSyncHelper.setVmSyncPorts(vm.getUuid());
        }
    }

    @Override
    public void afterDetachNic(VmNicInventory nic) {

    }

    @Override
    public void failedToDetachNic(VmNicInventory nic, ErrorCode error) {

    }

    @Override
    public boolean needRunExtension() {
        return true;
    }

    @Override
    public Flow getBeforeRevertFlow() {
        return new NoRollbackFlow() {
            String __name__ = "set-sync-ports-tag";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                String sgUuid = (String) data.get(VolumeSnapshotGroupConstant.Parmas.SnapshotGroupUuid.toString());
                VolumeSnapshotGroupVO vsg = dbf.findByUuid(sgUuid, VolumeSnapshotGroupVO.class);
                if (vsg == null) {
                    trigger.next();
                    return;
                }

                vmConfigSyncHelper.setVmSyncPorts(vsg.getVmInstanceUuid());
                trigger.next();
            }
        };
    }

    static class GuestToolsInfoBundle {
        GuestToolsQgaStateEvent qgaEvent;
        GuestToolsZWatchStateEvent zEvent;
        String osType;
        String platform;
        String qgaVersion;
        String zwatchVerion;

        static public GuestToolsInfoBundle makeNoQgaOperationBundle(GuestToolsInfoBundle bundle) {
            bundle.qgaEvent = GuestToolsQgaStateEvent.noOperation;
            bundle.osType = "";
            bundle.platform = "";
            bundle.qgaVersion = "";
            return bundle;
        }
    }

    private void updateGuestToolsState(UpdateGuestToolsStateMsg msg, ReturnValueCompletion<List<GuestToolsStateInventory>> completion) {
        final List<GuestToolsStateInventory> stateInventories = new ArrayList<>();
        if (msg.getVmInstanceUuid() != null) {
            VmInstanceVO vmVo = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
            if (msg.getHostUuid() == null || vmVo.getState() != VmInstanceState.Running) {
                GuestToolsQgaStateEvent qgaEvent = GuestToolsQgaStateEvent.qgaNotRunning;
                GuestToolsZWatchStateEvent zEvent = GuestToolsZWatchStateEvent.stopped;
                String osType = "";
                String platform = "";
                String qgaVersion = "";
                String zwatchVerion = "";

                GuestToolsStateVO stateVO = qgaStateChange(vmVo.getUuid(), zEvent, qgaEvent, osType, platform,
                        zwatchVerion, qgaVersion, Boolean.FALSE);
                if (stateVO == null) {
                    completion.fail(operr("qgaStateChange failed for vm[uuid:%s]", vmVo.getUuid()));
                    return;
                }

                stateInventories.add(GuestToolsStateInventory.valueOf(stateVO));
                completion.success(stateInventories);
                return;
            }

            msg.setHostUuid(vmVo.getHostUuid());
        }

        final List<String> vmInstanceUuidList = new ArrayList<>();
        if (msg.getVmInstanceUuid() != null) {
            vmInstanceUuidList.add(msg.getVmInstanceUuid());
        } else if (msg.getHostUuid() != null) {
            vmInstanceUuidList.addAll(Q.New(VmInstanceVO.class)
                    .eq(VmInstanceVO_.state, VmInstanceState.Running)
                    .eq(VmInstanceVO_.hostUuid, msg.getHostUuid())
                    .eq(VmInstanceVO_.type, USER_VM_TYPE)
                    .select(VmInstanceVO_.uuid)
                    .listValues());
        }

        if (vmInstanceUuidList.isEmpty()) {
            logger.debug(String.format("no vm running on host[uuid:%s]", msg.getHostUuid()));
            completion.success(null);
            return;
        }

        FlowChain chain = new SimpleFlowChain();
        chain.disableDebugLog();
        chain.setName(String.format("update-guest-tools-state-%s", msg.getVmInstanceUuid()));
        final Map<String, GuestToolsInfoBundle> bundles = new HashMap<>();
        chain.then(new NoRollbackFlow() {
            String __name__ = String.format("get-guest-tools-info-from-metric-%s", msg.getVmInstanceUuid());

            @Override
            public void run(FlowTrigger trigger, Map data) {
                new While<>(vmInstanceUuidList).each((vmUuid, whileCompletion) -> {
                    if (!msg.getServiceTypeList().contains(GuestToolsServiceType.ZWatch)) {
                        whileCompletion.done();
                        return;
                    }
                    getVmGuestToolsInfo(bundles, vmUuid, new Completion(whileCompletion) {
                        @Override
                        public void success() {
                            whileCompletion.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            whileCompletion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errorCodeList.getCauses().size() != 0) {
                            logger.debug(String.format("get vm guest tools info failed, %s", errorCodeList.getCauses()));
                        }

                        // keep this code to keep backward compatibility
                        trigger.next();
                    }
                });
            }

            private void getVmGuestToolsInfo(final Map<String, GuestToolsInfoBundle> bundles, String vmInstanceUuid, Completion getInfoCompletion) {
                GetVmGuestToolsInfoMsg infoMsg = new GetVmGuestToolsInfoMsg();
                infoMsg.setUuid(vmInstanceUuid);
                infoMsg.setDebug(new HashSet<>());
                infoMsg.setReadMetricOnly(true);
                bus.makeTargetServiceIdByResourceUuid(infoMsg, GuestToolsConstant.SERVICE_ID, infoMsg.getVmInstanceUuid());
                bus.send(infoMsg, new CloudBusCallBack(getInfoCompletion) {
                    @Override
                    public void run(MessageReply reply) {
                        GuestToolsZWatchStateEvent event = GuestToolsZWatchStateEvent.noOperation;
                        String version = "";

                        GuestToolsInfoBundle bundle = new GuestToolsInfoBundle();
                        bundles.put(vmInstanceUuid, bundle);
                        if (reply.isSuccess()) {
                            GetVmGuestToolsInfoReply reply1 = reply.castReply();
                            logger.debug(String.format("get vm[uuid:%s] zwatch state Status[%s] Version[%s]",
                                    vmInstanceUuid, reply1.getStatus(), reply1.getVersion()));
                            if (reply1.getStatus() != null) {
                                if (reply1.getStatus().equals(GuestToolsAgentStatus.RUNNING.toString())) {
                                    event = GuestToolsZWatchStateEvent.started;
                                } else if (reply1.getStatus().equals(GuestToolsAgentStatus.NOT_RUNNING.toString())) {
                                    event = GuestToolsZWatchStateEvent.stopped;
                                } else if (reply1.getStatus().equals(GuestToolsAgentStatus.NOT_CONNECTED.toString())) {
                                    event = GuestToolsZWatchStateEvent.stopped;
                                }
                            }
                            if (reply1.getVersion() != null) {
                                version = reply1.getVersion();
                            }

                        } else {
                            logger.debug(String.format("get vm[uuid:%s] zwatch state failed %s",
                                    vmInstanceUuid, reply.getMessageName()));
                            getInfoCompletion.fail(reply.getError());
                            return;
                        }

                        bundle.zEvent = event;
                        bundle.zwatchVerion = version;

                        getInfoCompletion.success();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = String.format("get-guest-tools-info-from-host-agent-%s", msg.getVmInstanceUuid());

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (!msg.getServiceTypeList().contains(GuestToolsServiceType.QGA)) {
                    trigger.next();
                    return;
                }
                KVMHostAsyncHttpCallMsg hmsg = new KVMHostAsyncHttpCallMsg();
                MevocoKVMAgentCommands.GetGuestToolsStateCmd cmd = new MevocoKVMAgentCommands.GetGuestToolsStateCmd();
                cmd.setVmInstanceUuids(vmInstanceUuidList);
                hmsg.setCommand(cmd);
                hmsg.setNoStatusCheck(true);
                hmsg.setHostUuid(msg.getHostUuid());
                hmsg.setPath(MevocoKVMConstant.KVM_VM_GUESTTOOLS_STATE_PATH);
                bus.makeTargetServiceIdByResourceUuid(hmsg, HostConstant.SERVICE_ID, msg.getHostUuid());
                bus.send(hmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            for (String vmUuid : vmInstanceUuidList) {
                                GuestToolsInfoBundle.makeNoQgaOperationBundle(bundles.get(vmUuid));
                                logger.debug(String.format("get vm[uuid:%s] qga state failed %s",
                                        msg.getVmInstanceUuid(), reply.getMessageName()));
                            }

                            trigger.next();
                            return;
                        }

                        KVMHostAsyncHttpCallReply r = reply.castReply();
                        MevocoKVMAgentCommands.GuestToolsStateResp ret = r.toResponse(MevocoKVMAgentCommands.GuestToolsStateResp.class);
                        if (!ret.isSuccess()) {
                            for (String vmUuid : vmInstanceUuidList) {
                                GuestToolsInfoBundle.makeNoQgaOperationBundle(bundles.get(vmUuid));
                                logger.debug(String.format("get vm[uuid:%s] qga state failed %s",
                                        msg.getVmInstanceUuid(), reply.getMessageName()));
                            }
                            trigger.next();
                            return;
                        }

                        vmInstanceUuidList.forEach(vmUuid -> {
                            MevocoKVMAgentCommands.GuestToolsState state = ret.getStates().get(vmUuid);

                            if (state == null) {
                                logger.debug(String.format("get vm[uuid:%s] qga state failed, state is null",
                                        vmUuid));
                                GuestToolsInfoBundle.makeNoQgaOperationBundle(bundles.get(vmUuid));
                                return;
                            }

                            GuestToolsInfoBundle bundle = bundles.get(vmUuid);
                            bundle.qgaEvent = getGuestToolsEventByState(vmUuid, ret.getStates().get(vmUuid));
                            bundle.osType = state.getOsType();
                            bundle.platform = state.getPlatForm();
                            bundle.qgaVersion = state.getVersion();
                        });

                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = String.format("update-guest-tools-save-db-%s", msg.getVmInstanceUuid());

            @Override
            public void run(FlowTrigger trigger, Map data) {
                logger.debug(String.format("dump bundles for guest tools state update: %s",
                        JSONObjectUtil.toJsonString(bundles)));

                bundles.forEach((vmUuid, bundle) -> {
                    if (skipUpdateGuestToolsState(vmUuid)) {
                        logger.debug(String.format("skip update guest tools state for vm[uuid:%s]", vmUuid));
                        return;
                    }

                    GuestToolsStateVO stateVo = qgaStateChange(
                            vmUuid,
                            bundle.zEvent,
                            bundle.qgaEvent,
                            bundle.osType,
                            bundle.platform,
                            bundle.zwatchVerion,
                            bundle.qgaVersion,
                            Boolean.FALSE
                    );
                    if (stateVo == null) {
                        return;
                    }
                    stateInventories.add(GuestToolsStateInventory.valueOf(stateVo));
                });
                trigger.next();
            }

            private boolean skipUpdateGuestToolsState(String vmUuid) {
                List<UpdateGuestToolsState> exts = pluginRgty.getExtensionList(UpdateGuestToolsState.class);
                for (UpdateGuestToolsState extState : exts) {
                    if (extState.skipUpdate(vmUuid)) {
                        return true;
                    }
                }

                return false;
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data1) {
                completion.success(stateInventories);
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data1) {
                completion.fail(errCode);
            }
        }).start();
    }

    private String guestToolsStateUpdateSync(String vmUuid) {
        return String.format("sync-guest-tools-state-vm-%s", vmUuid);
    }

    private void queueStateUpdateForVm(UpdateGuestToolsStateMsg msg, ReturnValueCompletion<List<GuestToolsStateInventory>> completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return guestToolsStateUpdateSync(msg.getVmInstanceUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                UpdateGuestToolsStateReply areply = new UpdateGuestToolsStateReply();

                updateGuestToolsState(msg, new ReturnValueCompletion<List<GuestToolsStateInventory>>(chain) {
                    @Override
                    public void success(List<GuestToolsStateInventory> guestToolsStateInventories) {
                        completion.success(guestToolsStateInventories);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        areply.setError(errorCode);
                        bus.reply(msg, areply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return GUEST_TOOL_STATE_WITHOUT_HOST_QUEUE;
            }
        });
    }

    private void singleFlightUpdateGuestToolsStateByHost(UpdateGuestToolsStateMsg msg, ReturnValueCompletion<List<GuestToolsStateInventory>> completion) {
        thdf.singleFlightSubmit(new SingleFlightTask(completion)
                .setSyncSignature(String.format("update-guest-tools-state-host-%s-single-flight", msg.getHostUuid()))
                .run((c) -> updateGuestToolsState(msg, new ReturnValueCompletion<List<GuestToolsStateInventory>>(c) {
                    @Override
                    public void success(List<GuestToolsStateInventory> returnValue) {
                        c.success(returnValue);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        c.fail(errorCode);
                    }
                }))
                .done(((result) -> {
                    if (!result.isSuccess()) {
                        completion.fail(result.getErrorCode());
                        return;
                    }

                    completion.success((List<GuestToolsStateInventory>) result.getResult());
                })));
    }

    private void handle(UpdateGuestToolsStateMsg msg) {
        UpdateGuestToolsStateReply reply = new UpdateGuestToolsStateReply();
        ReturnValueCompletion<List<GuestToolsStateInventory>> completion = new ReturnValueCompletion<List<GuestToolsStateInventory>>(msg) {
            @Override
            public void success(List<GuestToolsStateInventory> guestToolsStateInventories) {
                if (guestToolsStateInventories != null && !guestToolsStateInventories.isEmpty()) {
                    reply.setInventory(guestToolsStateInventories.get(0));
                    reply.setInventories(guestToolsStateInventories);
                }

                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        };

        if (msg.getHostUuid() == null || msg.getVmInstanceUuid() != null) {
            queueStateUpdateForVm(msg, completion);
        } else {
            singleFlightUpdateGuestToolsStateByHost(msg, completion);
        }
    }

    private void handle(APIUpdateGuestToolsStateMsg msg) {
        APIUpdateGuestToolsStateReply areply = new APIUpdateGuestToolsStateReply();

        VmInstanceVO vmvo = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);

        UpdateGuestToolsStateMsg umsg = new UpdateGuestToolsStateMsg();
        umsg.setVmInstanceUuid(msg.getVmInstanceUuid());
        umsg.setHostUuid(vmvo.getHostUuid());

        if (umsg.getHostUuid() == null) {
            bus.makeTargetServiceIdByResourceUuid(umsg, GuestToolsConstant.SERVICE_ID, msg.getVmInstanceUuid());
        } else {
            bus.makeTargetServiceIdByResourceUuid(umsg, GuestToolsConstant.SERVICE_ID, umsg.getHostUuid());
        }

        bus.makeTargetServiceIdByResourceUuid(umsg, GuestToolsConstant.SERVICE_ID, msg.getVmInstanceUuid());
        bus.send(umsg, new CloudBusCallBack(umsg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    areply.setError(reply.getError());
                } else {
                    areply.setInventory(((UpdateGuestToolsStateReply)reply).getInventory());
                }
                bus.reply(msg, areply);
            }
        });
    }

    private void handle(APIUpdateVmNetworkConfigMsg msg) {
        APIUpdateVmNetworkConfigEvent evt = new APIUpdateVmNetworkConfigEvent(msg.getId());
        UpdateVmNetworkConfigMsg umsg = new UpdateVmNetworkConfigMsg();
        umsg.setVmNicUuids(msg.getVmNicUuids());
        umsg.setVmInstanceUuid(msg.getVmInstanceUuid());
        bus.makeTargetServiceIdByResourceUuid(umsg, GuestToolsConstant.SERVICE_ID, msg.getVmInstanceUuid());
        bus.send(umsg, new CloudBusCallBack(evt) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    evt.setError(reply.getError());
                }
                bus.publish(evt);
            }
        });
    }

    private void handle(APIGetLatestGuestToolsForVmMsg msg) {
        APIGetLatestGuestToolsForVmReply reply = new APIGetLatestGuestToolsForVmReply();

        VmInstanceVO vm = dbf.findByUuid(msg.getUuid(), VmInstanceVO.class);
        GuestToolsAgentType agentType = GuestToolsAgentType.fromImagePlatformAndHypervisorType(
                ImagePlatform.valueOf(vm.getPlatform()), vm.getHypervisorType());
        if (agentType == null) {
            reply.setError(Platform.argerr(
                    "cannot get latest guest-tools for vm[uuid:%s] because it's hypervisor type is not supported",
                    msg.getUuid()
            ));
            bus.reply(msg, reply);
            return;
        }

        if (vm.getState() != VmInstanceState.Running && vm.getState() != VmInstanceState.VolumeRecovering) {
            reply.setError(Platform.argerr(
                    "cannot get latest guest-tools for vm[uuid:%s] because it's not running or volume recovering.",
                    msg.getUuid()
            ));
            bus.reply(msg, reply);
            return;
        }

        if (!VmInstanceConstant.USER_VM_TYPE.equals(vm.getType())) {
            reply.setError(Platform.argerr(
                    "cannot get latest guest-tools for vm[uuid:%s] because it's not user vm",
                    msg.getUuid()
            ));
            bus.reply(msg, reply);
            return;
        }

        GuestToolsVO gt = new SQLBatchWithReturn<GuestToolsVO>() {
            @Override
            protected GuestToolsVO scripts() {
                VmInstanceVO vm = q(VmInstanceVO.class)
                        .eq(VmInstanceVO_.uuid, msg.getUuid())
                        .find();
                String hype = q(HostVO.class)
                        .eq(HostVO_.uuid, vm.getHostUuid())
                        .select(HostVO_.hypervisorType)
                        .findValue();

                return q(GuestToolsVO.class)
                        .eq(GuestToolsVO_.managementNodeUuid, Platform.getManagementServerId())
                        .in(GuestToolsVO_.architecture, Arrays.asList(vm.getArchitecture(), GUEST_TOOLS_ARCH_GENERAL))
                        .eq(GuestToolsVO_.hypervisorType, hype)
                        .eq(GuestToolsVO_.agentType, agentType.toString())
                        .orderBy(GuestToolsVO_.createDate, SimpleQuery.Od.DESC)
                        .limit(1)
                        .find();
            }
        }.execute();

        if (gt == null) {
            logger.debug(String.format("no proper guest tools iso found in management node[uuid:%s] for vm[uuid:%s]",
                    Platform.getManagementServerId(), msg.getUuid()));
            bus.reply(msg, reply);
            return;
        }

        GuestToolsStateVO stateVO = dbf.findByUuid(msg.getUuid(), GuestToolsStateVO.class);
        if (stateVO != null && !stateVO.getQgaState().equals(GuestToolsQgaState.NotInstalled)) {
            if (!gt.getVersion().equals(stateVO.getVersion())) {
                reply.setInventory(gt.toInventory());
            }
        } else {
            String vmToolsVersion = VmSystemTags.VM_GUEST_TOOLS.getTokenByResourceUuid(
                    msg.getUuid(), VmInstanceVO.class, VmSystemTags.VM_GUEST_TOOLS_VERSION_TOKEN);
            if (!gt.getVersion().equals(vmToolsVersion)) {
                reply.setInventory(gt.toInventory());
            }
        }
        bus.reply(msg, reply);
    }

    private void handle(APIAttachGuestToolsIsoToVmMsg msg) {
        APIAttachGuestToolsIsoToVmEvent evt = new APIAttachGuestToolsIsoToVmEvent(msg.getId());
        AttachGuestToolsIsoToVmMsg amsg = new AttachGuestToolsIsoToVmMsg();
        amsg.setVmInstanceUuid(msg.getUuid());
        bus.makeTargetServiceIdByResourceUuid(amsg, GuestToolsConstant.SERVICE_ID, msg.getUuid());
        bus.send(amsg, new CloudBusCallBack(evt) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    evt.setError(reply.getError());
                }
                bus.publish(evt);
            }
        });
    }

    private void handle(APIDetachGuestToolsIsoFromVmMsg msg) {
        APIDetachGuestToolsIsoFromVmEvent evt = new APIDetachGuestToolsIsoFromVmEvent(msg.getId());
        DetachGuestToolsIsoFromVmMsg amsg = new DetachGuestToolsIsoFromVmMsg();
        amsg.setVmInstanceUuid(msg.getUuid());
        bus.makeTargetServiceIdByResourceUuid(amsg, GuestToolsConstant.SERVICE_ID, msg.getUuid());
        bus.send(amsg, new CloudBusCallBack(evt) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    evt.setError(reply.getError());
                }
                bus.publish(evt);
            }
        });
    }

    private void handle(APIGetVmGuestToolsInfoMsg amsg) {
        APIGetVmGuestToolsInfoReply areply = new APIGetVmGuestToolsInfoReply();

        GetVmGuestToolsInfoMsg msg = new GetVmGuestToolsInfoMsg();
        msg.setUuid(amsg.getUuid());
        msg.setDebug(amsg.getDebug());
        bus.makeTargetServiceIdByResourceUuid(msg, GuestToolsConstant.SERVICE_ID, msg.getVmInstanceUuid());
        bus.send(msg, new CloudBusCallBack(amsg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    GetVmGuestToolsInfoReply reply1 = reply.castReply();
                    areply.setVersion(reply1.getVersion());
                    areply.setStatus(reply1.getStatus());
                    areply.setFeatures(reply1.getFeatures());
                } else {
                    areply.setError(reply.getError());
                }
                bus.reply(amsg, areply);
            }
        });
    }

    private void handle(APICreateVmCustomSpecificationMsg msg) {
        APICreateVmCustomSpecificationEvent evt = new APICreateVmCustomSpecificationEvent(msg.getId());

        VmCustomSpecificationVO vo = new VmCustomSpecificationVO(msg.getVmCustomSpecification());
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setAccountUuid(msg.getSession().getAccountUuid());
        dbf.persistAndRefresh(vo);

        evt.setInventory(vo.toInventory());
        bus.publish(evt);
    }

    private void handle(APIDeleteVmCustomSpecificationMsg msg) {
        APIDeleteVmCustomSpecificationEvent evt = new APIDeleteVmCustomSpecificationEvent(msg.getId());

        dbf.removeByPrimaryKey(msg.getUuid(), VmCustomSpecificationVO.class);
        bus.publish(evt);
    }

    private void handle(APIUpdateVmCustomSpecificationMsg msg) {
        APIUpdateVmCustomSpecificationEvent evt = new APIUpdateVmCustomSpecificationEvent(msg.getId());

        VmCustomSpecificationVO vo = dbf.findByUuid(msg.getUuid(), VmCustomSpecificationVO.class);
        vo.updateVOByVmCustomSpec(msg.getVmCustomSpecification());

        if (msg.getName() != null) {
            vo.setName(msg.getName());
        }

        if (msg.getDescription() != null) {
            vo.setDescription(msg.getDescription());
        }

        vo = dbf.updateAndRefresh(vo);
        evt.setInventory(vo.toInventory());
        bus.publish(evt);
    }

    private void handle(GetVmGuestToolsInfoMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "get-guest-tools-info-for-vm-" + msg.getUuid();
            }

            @Override
            public void run(SyncTaskChain chain) {
                VmInstanceVO vm = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
                GuestToolsAgentType agentType = GuestToolsAgentType.fromImagePlatformAndHypervisorType(
                        ImagePlatform.valueOf(vm.getPlatform()), vm.getHypervisorType());

                if (agentType == null) {
                    logger.debug(String.format("cannot get guest-tools info from vm[uuid:%s] because it's hypervisor type is not supported", msg.getUuid()));

                    GetVmGuestToolsInfoReply reply = new GetVmGuestToolsInfoReply();
                    reply.setStatus(GuestToolsAgentStatus.NOT_SUPPORTED.toString());
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                GuestToolsHypervisorBackend bkd = getGuestToolsHypervisorBackend(agentType);

                getVmGuestToolsInfoInnerFlow(vm, bkd, msg.getDebug(), msg.isReadMetricOnly(),
                        new ReturnValueCompletion<GetVmGuestToolsInfoReply>(chain) {
                    @Override
                    public void success(GetVmGuestToolsInfoReply reply) {
                        bus.reply(msg, reply);
                        chain.next();
                    }
                    
                    @Override
                    public void fail(ErrorCode errorCode) {
                        GetVmGuestToolsInfoReply reply = new GetVmGuestToolsInfoReply();
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    @SuppressWarnings("rawtypes")
    private void getVmGuestToolsInfoInnerFlow(VmInstanceVO vm,
                                              GuestToolsHypervisorBackend bkd,
                                              Set<String> debugItems,
                                              boolean readMetricOnly,
                                              ReturnValueCompletion<GetVmGuestToolsInfoReply> completion) {
        GetVmGuestToolsInfoReply reply = new GetVmGuestToolsInfoReply();
        reply.setFeatures(new HashMap<>());

        FlowChain chain = new SimpleFlowChain();
        chain.setName(String.format("get-vm-%s-guest-tools-info", vm.getUuid()));
        chain.disableDebugLog();

        chain.then(new NoRollbackFlow() {
            String __name__ = "debug-for-getting-vm-metrics-routing-status";

            @Override
            public boolean skip(Map data) {
                return CollectionUtils.isEmpty(debugItems);
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                bkd.getVmMetricsRoutingStatus(VmInstanceInventory.valueOf(vm), debugItems,
                        new ReturnValueCompletion<Map<String, String>>(trigger) {
                    @Override
                    public void success(Map<String, String> debugResults) {
                        printVmMetricsRoutingStatus(debugResults);
                        reply.getFeatures().putAll(debugResults);
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.warn(String.format("failed to get vm metrics routing status from vm[uuid:%s]: %s",
                                vm.getUuid(), errorCode));
                        trigger.next(); // skip anyway
                    }
                });
            }

            void printVmMetricsRoutingStatus(Map<String, String> values) {
                StringBuilder builder = new StringBuilder(1024);
                builder.append("vm[uuid:").append(vm.getUuid()).append("] metrics routing status list below:\n");
                values.forEach((k, v) -> builder.append(k).append(": ").append(v).append('\n'));
                logger.info(builder.toString());
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "get-vm-guest-tools-info";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                final VmInstanceInventory vmInventory = VmInstanceInventory.valueOf(vm);
                bkd.getVmGuestToolsInfo(vmInventory, readMetricOnly, new ReturnValueCompletion<GetVmGuestToolsInfoRsp>(trigger) {
                    @Override
                    public void success(GetVmGuestToolsInfoRsp rsp) {
                        reply.setVersion(rsp.getVersion());
                        reply.setStatus(rsp.getStatus());
                        reply.getFeatures().putAll(rsp.getFeatures());

                        if (rsp.getVersion() != null) {
                            updateGuestToolsTag(vm.getUuid(), rsp.getVersion());
                        }

                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        if (errorCode.getDetails().contains(GuestToolsConstant.GUEST_TOOLS_NOT_CONNNETED)) {
                            reply.setStatus(GuestToolsAgentStatus.NOT_CONNECTED.toString());
                        } else {
                            reply.setError(errorCode);
                        }
                        // skip anyway
                        trigger.next();
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success(reply);
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void handle(final UpdateVmNetworkConfigMsg msg) {
        UpdateVmNetworkConfigReply reply = new UpdateVmNetworkConfigReply();
        String vmUuid = msg.getVmInstanceUuid();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                VmInstanceVO vm = dbf.findByUuid(vmUuid, VmInstanceVO.class);
                List<VmConfigSyncStruct.VmPortConfig> ports = getVmPortsConfig(msg.getVmInstanceUuid(), msg.getVmNicUuids(), msg.getHaState());
                logger.debug(String.format("get vm[uuid:%s] ports config: %s", vmUuid, ports));
                VmPortsConfigSyncOnHypervisorMsg syncMsg = new VmPortsConfigSyncOnHypervisorMsg();
                syncMsg.setPorts(ports);
                syncMsg.setVmInstanceUuid(vmUuid);
                if (rcf.getResourceConfigValue(GuestToolsGlobalConfig.CONFIG_IPADDRESS_WITH_HOSTNAME, vmUuid, Boolean.class)) {
                    syncMsg.setHostname(VmSystemTags.HOSTNAME.getTokenByResourceUuid(vmUuid, VmSystemTags.HOSTNAME_TOKEN));
                    syncMsg.setDefaultIP(getVmDefaultIp(vm));
                }
                syncMsg.setHostUuid(vm.getHostUuid());
                bus.makeTargetServiceIdByResourceUuid(syncMsg, HostConstant.SERVICE_ID, vm.getHostUuid());
                bus.send(syncMsg, new CloudBusCallBack(chain) {
                    @Override
                    public void run(MessageReply agentReply) {
                        if (!agentReply.isSuccess()) {
                            reply.setError(agentReply.getError());
                        }
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return String.format("port-config-sync-for-vm-%s", vmUuid);
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private List<VmConfigSyncStruct.VmPortConfig> getVmPortsConfig(String vmUuid, List<String> vmNicUuids, String haState) {
        ArrayList<VmConfigSyncStruct.VmPortConfig> ports = new ArrayList<>();
        final VmInstanceVO vm = dbf.findByUuid(vmUuid, VmInstanceVO.class);
        final Set<VmNicVO> nicList = vm.getVmNics().stream().filter(nic -> vmNicUuids.contains(nic.getUuid())).collect(Collectors.toSet());
        final String defaultNicUuid = nicList.stream().filter(nic -> Objects.equals(nic.getL3NetworkUuid(), vm.getDefaultL3NetworkUuid()))
                .sorted(Comparator.comparingInt(VmNicVO::getDeviceId))
                .map(VmNicVO::getUuid).findFirst().orElse(null);
        final boolean isWindowsVm = ImagePlatform.Windows.toString().equals(vm.getPlatform());
        final List<VmGuestNetworkInfoVO> guestInfoList = Q.New(VmGuestNetworkInfoVO.class)
                .eq(VmGuestNetworkInfoVO_.vmInstanceUuid, vmUuid).list();
        for (VmNicVO nic : nicList) {
            VmGuestNetworkInfoVO guestInfo = guestInfoList.stream()
                    .filter(info -> Objects.equals(info.getVmNicUuid(), nic.getUuid()))
                    .findFirst().orElse(new VmGuestNetworkInfoVO());

            ArrayList<VmConfigSyncStruct.VmIpConfig> ips = new ArrayList<>();
            VmConfigSyncStruct.VmIpConfig ipConfig = new VmConfigSyncStruct.VmIpConfig();
            ipConfig.setVersion(IPv6Constants.IPv4);
            ipConfig.setProto(NetworkUtils.PROTO_STATIC);
            UsedIpVO usedIpVO = nic.getUsedIps().stream()
                    .filter(usedIp -> usedIp.getIpVersion() == IPv6Constants.IPv4)
                    .findFirst().orElse(new UsedIpVO());
            if (StringUtils.isEmpty(usedIpVO.getIp())) {
                usedIpVO = new UsedIpVO();
                usedIpVO.setIp(NetworkUtils.NETWORK_CFG_EMPTY);
                usedIpVO.setNetmask(NetworkUtils.NETWORK_CFG_EMPTY);
                usedIpVO.setGateway(NetworkUtils.NETWORK_CFG_EMPTY);
            } else if (StringUtils.isEmpty(usedIpVO.getGateway()) && guestInfo.getGateway() != null) {
                usedIpVO.setGateway(NetworkUtils.NETWORK_CFG_EMPTY);
            }
            ipConfig.setIp(usedIpVO.getIp());
            ipConfig.setNetmask(usedIpVO.getNetmask());
            if (Objects.equals(nic.getUuid(), defaultNicUuid)) {
                ipConfig.setGateway(usedIpVO.getGateway());
            }
            List<String> dnsList = new ArrayList<>();
            if (isWindowsVm) {
                dnsList.addAll(nwServiceMgr.getVmNicDns(nic.getUuid(), IPv6Constants.IPv4, nic.getL3NetworkUuid()));
            } else if (Objects.equals(nic.getUuid(), defaultNicUuid)) {
                dnsList.addAll(nwServiceMgr.getVmDns(vmUuid, nic.getL3NetworkUuid()));
            }
            if (!CollectionUtils.isEmpty(dnsList) || !DnsUtils.getDnsListFromString(guestInfo.getDnsList()).isEmpty()) {
                ipConfig.setDns(dnsList);
            }
            List<HostRouteInfo> routeList = HostRouteUtils.getL3NetworkHostRoute(nic.getL3NetworkUuid(), IPv6Constants.IPv4);
            if (!CollectionUtils.isEmpty(routeList) ||
                    !HostRouteUtils.getHostRouteFromString(guestInfo.getRouteList()).isEmpty()) {
                ipConfig.setRoutes(routeList);
            }

            // Windows vm with APIPA
            if (isWindowsVm && NetworkUtils.isInternalAddress(usedIpVO.getIp())) {
                ipConfig.setProto(NetworkUtils.PROTO_DHCP);
            }
            ips.add(ipConfig);

            VmConfigSyncStruct.VmIpConfig ipConfigV6 = new VmConfigSyncStruct.VmIpConfig();
            ipConfigV6.setVersion(IPv6Constants.IPv6);
            ipConfigV6.setProto(NetworkUtils.PROTO_STATIC);
            UsedIpVO usedIpV6VO = nic.getUsedIps().stream()
                    .filter(usedIp -> usedIp.getIpVersion() == IPv6Constants.IPv6)
                    .findFirst().orElse(null);
            if (usedIpV6VO != null) {
                ipConfigV6.setIp(usedIpV6VO.getIp());
                ipConfigV6.setNetmask(usedIpV6VO.getNetmask() != null ?
                        NetworkUtils.getPrefixLengthFromNetmask(usedIpV6VO.getNetmask()).toString() : null);
                if (Objects.equals(nic.getUuid(), defaultNicUuid)) {
                    if (StringUtils.isEmpty(usedIpV6VO.getGateway()) && guestInfo.getIpv6Gateway() != null) {
                        ipConfigV6.setGateway(NetworkUtils.NETWORK_CFG_EMPTY);
                    } else {
                        ipConfigV6.setGateway(usedIpV6VO.getGateway());
                    }
                }
            } else {
                ipConfigV6.setIp(NetworkUtils.NETWORK_CFG_EMPTY);
                ipConfigV6.setNetmask(NetworkUtils.NETWORK_CFG_EMPTY);
                ipConfigV6.setGateway(NetworkUtils.NETWORK_CFG_EMPTY);
            }
            if (isWindowsVm) {
                List<String> dns6List = nwServiceMgr.getVmNicDns(nic.getUuid(), IPv6Constants.IPv6, nic.getL3NetworkUuid());
                if (!CollectionUtils.isEmpty(dns6List) || !DnsUtils.getDnsListFromString(guestInfo.getDns6List()).isEmpty()) {
                    ipConfigV6.setDns(dns6List);
                }
            }
            List<HostRouteInfo> route6List = HostRouteUtils.getL3NetworkHostRoute(nic.getL3NetworkUuid(), IPv6Constants.IPv6);
            if (!CollectionUtils.isEmpty(route6List) ||
                    !HostRouteUtils.getHostRouteFromString(guestInfo.getRoute6List()).isEmpty()) {
                ipConfigV6.setRoutes(route6List);
            }
            ips.add(ipConfigV6);

            VmConfigSyncStruct.VmPortConfig portConfig = new VmConfigSyncStruct.VmPortConfig();
            portConfig.setMac(nic.getMac());
            portConfig.setVmIps(ips);
            portConfig.setMtu(new MtuGetter().getMtu(nic.getL3NetworkUuid()));
            portConfig.setDefault(Objects.equals(nic.getUuid(), defaultNicUuid));
            portConfig.setHaState(VmVfNicHaState.Disabled.toString());
            if (VmVfNicConstant.VIRTUAL_FUNCTION_TYPE.equals(nic.getType())) {
                if (haState != null) {
                    portConfig.setHaState(haState);
                } else {
                    VmVfNicVO vfNic = Q.New(VmVfNicVO.class).eq(VmVfNicVO_.uuid, nic.getUuid()).find();
                    portConfig.setHaState(vfNic.getHaState().toString());
                }
            }
            ports.add(portConfig);
        }

        return ports;
    }


    private void handle(DownloadGuestToolsIsoToHostMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "download-guest-tools-iso-to-host-" + msg.getHostUuid();
            }

            @Override
            public void run(SyncTaskChain chain) {
                // other vm may have already downloaded latest iso to the the same host and updated the system tag
                DownloadGuestToolsIsoToHostReply reply = new DownloadGuestToolsIsoToHostReply();

                HostVO host = dbf.findByUuid(msg.getHostUuid(), HostVO.class);
                GuestToolsAgentType agentType = GuestToolsAgentType.fromImagePlatformAndHypervisorType(
                        ImagePlatform.valueOf(msg.getPlatform()), msg.getHypervisorType());

                GuestToolsVO gt = Q.New(GuestToolsVO.class)
                        .eq(GuestToolsVO_.managementNodeUuid, Platform.getManagementServerId())
                        .in(GuestToolsVO_.architecture, Arrays.asList(host.getArchitecture(), GUEST_TOOLS_ARCH_GENERAL))
                        .eq(GuestToolsVO_.hypervisorType, host.getHypervisorType())
                        .eq(GuestToolsVO_.agentType, agentType.toString())
                        .orderBy(GuestToolsVO_.createDate, SimpleQuery.Od.DESC)
                        .limit(1)
                        .find();
                if (gt == null) {
                    reply.setError(err(NO_GUEST_TOOLS_FILES,
                            "no proper guest tools iso found in management node[uuid:%s] for host[uuid:%s]",
                            Platform.getManagementServerId(), msg.getHostUuid()));
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                String mnToolsVersion = gt.getVersion();
                GuestToolsHypervisorBackend bkd = getGuestToolsHypervisorBackend(agentType);
                String hostToolsVersion = bkd.getHostGuestToolsTag(msg.getHostUuid());

                if (mnToolsVersion.equals(hostToolsVersion)) {
                    CheckFileOnHostMsg checkMsg = new CheckFileOnHostMsg();
                    checkMsg.setHostUuid(msg.getHostUuid());
                    checkMsg.setPaths(Collections.singletonList(bkd.getDstGuestToolsIso()));
                    bus.makeTargetServiceIdByResourceUuid(checkMsg, HostConstant.SERVICE_ID, host.getUuid());
                    bus.send(checkMsg, new CloudBusCallBack(msg, chain) {
                        @Override
                        public void run(MessageReply kReply) {
                            if (!kReply.isSuccess()) {
                                reply.setError(err(NO_GUEST_TOOLS_FILES,
                                        "failed to check guest tools on host[uuid:%s]", host.getUuid())
                                        .withCause(kReply.getError()));
                                bus.reply(msg, reply);
                                chain.next();
                                return;
                            }

                            CheckFileOnHostReply rly = kReply.castReply();
                            if (rly.getExistPaths().isEmpty()) {
                                String iso = bkd.getSrcGuestToolsIso(host.getArchitecture(), host.getHypervisorType(), mnToolsVersion);
                                logger.info(String.format("guest tools iso not exists ,download GuestToolsIso[%s] to host[%s].", iso, host.getUuid()));
                                bkd.downloadGuestToolsIsoToHost(HostInventory.valueOf(host), mnToolsVersion, new Completion(msg, chain) {
                                    @Override
                                    public void success() {
                                        bkd.updateHostGuestToolsTag(msg.getHostUuid(), mnToolsVersion);
                                        bus.reply(msg, reply);
                                        chain.next();
                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        reply.setError(errorCode);
                                        bus.reply(msg, reply);
                                        chain.next();
                                    }
                                });
                                return;
                            }
                            logger.info(String.format("no need to download guest tools iso to host[uuid:%s]", host.getUuid()));
                            bus.reply(msg, reply);
                            chain.next();
                        }
                    });
                    return;
                }

                bkd.downloadGuestToolsIsoToHost(HostInventory.valueOf(host), mnToolsVersion, new Completion(reply) {
                    @Override
                    public void success() {
                        bkd.updateHostGuestToolsTag(msg.getHostUuid(), mnToolsVersion);
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(AttachGuestToolsIsoToVmMsg msg) {
        AttachGuestToolsIsoToVmReply reply = new AttachGuestToolsIsoToVmReply();
        VmInstanceVO vm = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);

        FlowChain chain = new SimpleFlowChain();
        chain.setName("attach-guest-tools-iso-to-vm");
        chain.then(new NoRollbackFlow() {
            String __name__ = "download guest tools iso from mn to host if need to";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                DownloadGuestToolsIsoToHostMsg dmsg = new DownloadGuestToolsIsoToHostMsg();
                dmsg.setHostUuid(vm.getHostUuid());
                dmsg.setPlatform(vm.getPlatform());
                dmsg.setHypervisorType(vm.getHypervisorType());
                bus.makeTargetServiceIdByResourceUuid(dmsg, GuestToolsConstant.SERVICE_ID, vm.getUuid());
                bus.send(dmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply rly) {
                        if (!rly.isSuccess()) {
                            trigger.fail(rly.getError());
                            return;
                        } else {
                            DownloadGuestToolsIsoToHostReply drly = rly.castReply();
                            if (!drly.isSuccess()) {
                                trigger.fail(rly.getError());
                                return;
                            }
                        }
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "attach guest tools iso to vm";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                thdf.chainSubmit(new ChainTask(trigger) {
                    @Override
                    public String getSyncSignature() {
                        return "attach-guest-tools-iso-on-vm-" + msg.getVmInstanceUuid();
                    }

                    @Override
                    public void run(SyncTaskChain chain) {
                        GuestToolsAgentType agentType = GuestToolsAgentType.fromImagePlatform(ImagePlatform.valueOf(vm.getPlatform()));
                        GuestToolsHypervisorBackend bkd = getGuestToolsHypervisorBackend(agentType);
                        bkd.attachGuestToolsIsoToVm(VmInstanceInventory.valueOf(vm), new Completion(reply) {
                            @Override
                            public void success() {
                                trigger.next();
                                chain.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                                chain.next();
                            }
                        });
                    }

                    @Override
                    public String getName() {
                        return getSyncSignature();
                    }
                });
            }
        }).done(new FlowDoneHandler(reply) {
            @Override
            public void handle(Map data) {
                bus.reply(msg, reply);
            }
        }).error(new FlowErrorHandler(reply) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                reply.setError(errCode);
                bus.reply(msg, reply);
            }
        }).start();
    }

    private void handle(DetachGuestToolsIsoFromVmMsg msg) {
        DetachGuestToolsIsoFromVmReply reply = new DetachGuestToolsIsoFromVmReply();
        VmInstanceVO vm = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);

        thdf.chainSubmit(new ChainTask(reply) {
            @Override
            public String getSyncSignature() {
                return "detach-guest-tools-iso-on-vm-" + msg.getVmInstanceUuid();
            }

            @Override
            public void run(SyncTaskChain chain) {
                GuestToolsAgentType agentType = GuestToolsAgentType.fromImagePlatform(ImagePlatform.valueOf(vm.getPlatform()));
                GuestToolsHypervisorBackend bkd = getGuestToolsHypervisorBackend(agentType);
                bkd.detachGuestToolsIsoFromVm(VmInstanceInventory.valueOf(vm), new Completion(reply) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    @Override
    public GuestToolsHypervisorBackend getGuestToolsHypervisorBackend(GuestToolsAgentType type) {
        GuestToolsHypervisorBackend bkd = guestToolsHypervisorBackends.get(type.toString());
        if (bkd == null) {
            throw new CloudRuntimeException(String.format("cannot find GuestToolsHypervisorBackend[type:%s]", type.toString()));
        }
        return bkd;
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(GuestToolsConstant.SERVICE_ID);
    }

    private GuestToolsStateVO initGuestToolsStateByGuestToolsVersion(String vmUuid, String version) {
        GuestToolsZWatchState zState;
        GuestToolsQgaState qState;
        if (!StringUtils.isEmpty(version)) {
            zState = GuestToolsZWatchState.NotRunning;
            qState = GuestToolsQgaState.NotUpgraded;
        } else {
            zState = GuestToolsZWatchState.NotInstalled;
            qState = GuestToolsQgaState.NotInstalled;
        }

        GuestToolsStateVO vo = new GuestToolsStateVO();
        vo.setVmInstanceUuid(vmUuid);
        vo.setZwatchState(zState);
        vo.setQgaState(qState);
        vo.setVersion(version);
        return vo;
    }

    private boolean guestToolsStateRectify(GuestToolsStateVO stateVO) {
        boolean changed = false;

        if ("".equals(stateVO.getVersion())) {
            stateVO.setVersion(null);
            if (stateVO.getZwatchState() == GuestToolsZWatchState.NotInstalled) {
                stateVO.setQgaState(GuestToolsQgaState.NotInstalled);
            } else {
                stateVO.setQgaState(GuestToolsQgaState.NotUpgraded);
            }
            changed = true;
        }

        if ("".equals(stateVO.getPlatform())) {
            stateVO.setPlatform(null);
            changed = true;
        }

        if ("".equals(stateVO.getOsType())
                || (stateVO.getOsType() != null && stateVO.getOsType().contains("None"))) {
            stateVO.setOsType(null);
            changed = true;
        }
        return changed;
    }

    private void upgradeGuestToolsStateVO() {
        String sql = "select state from GuestToolsStateVO state where state.version = :nullStr" +
                " or state.platform = :nullStr or state.osType in (:invalidOsType)";
        TypedQuery<GuestToolsStateVO> qGuestToolsState = dbf.getEntityManager().createQuery(sql, GuestToolsStateVO.class);
        qGuestToolsState.setParameter("nullStr", "");
        qGuestToolsState.setParameter("invalidOsType", asList("", "None None"));

        List<GuestToolsStateVO> stateVos = qGuestToolsState.getResultList();
        for (GuestToolsStateVO state : stateVos) {
            if (guestToolsStateRectify(state)) {
                dbf.update(state);
            }
        }

        sql = "select vm.uuid from VmInstanceVO vm where vm.type = :type" +
                " and vm.uuid not in (select vmInstanceUuid from GuestToolsStateVO)";
        TypedQuery<String> qVm = dbf.getEntityManager().createQuery(sql, String.class);
        qVm.setParameter("type", USER_VM_TYPE);
        List<String> vmUuids = qVm.getResultList();

        List<GuestToolsStateVO> stateVOs = new ArrayList<>();

        for (String vmUuid : vmUuids) {
            String toolsVersion = VmSystemTags.VM_GUEST_TOOLS.getTokenByResourceUuid(vmUuid, VmSystemTags.VM_GUEST_TOOLS_VERSION_TOKEN);
            GuestToolsStateVO stateVO = initGuestToolsStateByGuestToolsVersion(vmUuid, toolsVersion);
            stateVOs.add(stateVO);
        }

        if (!stateVOs.isEmpty()) {
            dbf.persistCollection(stateVOs);
        }
    }

    private void updateGuestToolsVmOsInfo(String vmUuid, String osType, String platform) {
        thdf.chainSubmit(new ChainTask(null) {
            @Override
            public String getSyncSignature() {
                return guestToolsStateUpdateSync(vmUuid);
            }

            @Override
            public void run(SyncTaskChain chain) {
                GuestToolsStateVO stateVO = dbf.findByUuid(vmUuid, GuestToolsStateVO.class);
                boolean isOsTypeInValid = stateVO.getOsType() == null || stateVO.getOsType().equals("") || stateVO.getOsType().contains("None");
                boolean isPlatformInValid = stateVO.getPlatform() == null || stateVO.getPlatform().equals("") || stateVO.getPlatform().contains("None");

                if (isOsTypeInValid || isPlatformInValid) {
                    stateVO.setOsType(osType);
                    stateVO.setPlatform(platform);
                    dbf.update(stateVO);
                }
                chain.next();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void reportVmQgaState(GuestToolsKvmCommands.QGAVmStateResultCmd result) {
        if (result.qgaState == null || result.qgaState.isEmpty()
                || result.toolsState == null || result.toolsState.isEmpty()) {
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, Boolean> qgaStateMap =
                (Map<String, Boolean>) (Map<?, ?>) JSONObjectUtil.toObject(result.qgaState, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Boolean> toolsStateMap =
                (Map<String, Boolean>) (Map<?, ?>) JSONObjectUtil.toObject(result.toolsState, Map.class);

        Set<String> vmUuidSet = new HashSet<>(qgaStateMap.keySet());
        vmUuidSet.retainAll(toolsStateMap.keySet());

        vmUuidSet.forEach( vmUuid -> {
            // VPC ApplianceVm not support qga and tools
            if (Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid)
                    .eq(VmInstanceVO_.type, ApplianceVmConstant.APPLIANCE_VM_TYPE).isExists()) {
                return;
            }
            Boolean qgaState = qgaStateMap.get(vmUuid) != null ? qgaStateMap.get(vmUuid) : Boolean.FALSE;
            Boolean toolsState = toolsStateMap.get(vmUuid) != null ? toolsStateMap.get(vmUuid) : Boolean.FALSE;
            Boolean supportQga = qgaState || toolsState;
            GuestToolsQgaStateEvent qgaEvent;
            if (toolsState) {
                qgaEvent = GuestToolsQgaStateEvent.zsToolFound;
            } else {
                qgaEvent = GuestToolsQgaStateEvent.zsToolNotFound;
            }
            if (!qgaState) {
                qgaEvent = GuestToolsQgaStateEvent.qgaNotRunning;
            }
            GuestToolsZWatchStateEvent zEvent = GuestToolsZWatchStateEvent.noOperation;

            qgaStateChange(vmUuid, zEvent, qgaEvent, null, null,
                    null, null, Boolean.TRUE);
            if (qgaState && toolsState) {
                syncPortsToVm(vmUuid, new ArrayList<>(), new ReturnValueCompletion<Boolean>(null) {
                    @Override
                    public void success(Boolean returnValue) {
                        if (Boolean.TRUE.equals(returnValue)) {
                            // join domain need vm to set ip and dns first
                            syncCustomSpecificationToVm(vmUuid);
                        } else {
                            syncHostnameToVm(vmUuid);
                            syncDnsToVm(vmUuid);
                        }
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.warn(errorCode.getDetails());
                    }
                });
            }
        });
    }

    private void syncPortsToVm(String vmUuid, List<String> nicList, ReturnValueCompletion<Boolean> completion) {
        if (vmConfigSyncHelper.vmNeedSyncPorts(vmUuid)) {
            if (!rcf.getResourceConfigValue(GuestToolsGlobalConfig.PUSH_NETWORK_CONFIG_VIA_QGA, vmUuid, Boolean.class)) {
                logger.debug(String.format("skip syncing ports to vm[uuid:%s] because the global config is set to false", vmUuid));
                vmConfigSyncHelper.afterVmSyncPorts(vmUuid);
                completion.success(false);
                return;
            }

            if (nicList == null) {
                completion.success(false);
                return;
            }

            if (CollectionUtils.isEmpty(nicList)) {
                nicList.addAll(Q.New(VmNicVO.class).select(VmNicVO_.uuid)
                        .eq(VmNicVO_.vmInstanceUuid, vmUuid).listValues());
            }

            if (CollectionUtils.isEmpty(nicList)) {
                logger.info(String.format("QGA vm[uuid:%s] sync ports without nic", vmUuid));
                completion.success(false);
                return;
            }

            UpdateVmNetworkConfigMsg umsg = new UpdateVmNetworkConfigMsg();
            umsg.setVmNicUuids(nicList);
            umsg.setVmInstanceUuid(vmUuid);
            bus.makeTargetServiceIdByResourceUuid(umsg, GuestToolsConstant.SERVICE_ID, vmUuid);
            bus.send(umsg, new CloudBusCallBack(null) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.error(String.format("QGA vm[uuid:%s] sync nic list [%s] failed", vmUuid, nicList));
                        completion.success(false);
                    } else {
                        vmConfigSyncHelper.afterVmSyncPorts(vmUuid);
                        completion.success(true);
                    }
                }
            });
        } else {
            completion.success(false);
        }
    }

    private void syncHostnameToVm(String vmUuid) {
        if (vmConfigSyncHelper.vmNeedSyncHostname(vmUuid)) {
            VmInstanceVO vm = dbf.findByUuid(vmUuid, VmInstanceVO.class);
            String defaultIP = getVmDefaultIp(vm);

            SetVmHostnameOnHypervisorMsg msg = new SetVmHostnameOnHypervisorMsg();
            msg.setHostUuid(vm.getHostUuid());
            msg.setVmInstanceUuid(vmUuid);
            msg.setHostname(VmSystemTags.HOSTNAME.getTokenByResourceUuid(vmUuid, VmSystemTags.HOSTNAME_TOKEN));
            msg.setDefaultIP(defaultIP);

            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, vm.getHostUuid());
            bus.send(msg, new CloudBusCallBack(null) {
                @Override
                public void run(MessageReply rly) {
                    if (!rly.isSuccess()) {
                        logger.warn(String.format("vm [uuid:%s, name: %s] set hostname failed, error :%s",
                                vm.getUuid(), vm.getName(), rly.getError().getDetails()));
                    } else {
                        vmConfigSyncHelper.afterVmSyncHostname(vmUuid);
                    }
                }
            });
        }
    }

    private void syncDnsToVm(String vmUuid) {
        if (vmConfigSyncHelper.vmNeedSyncDns(vmUuid)) {
            VmInstanceVO vm = dbf.findByUuid(vmUuid, VmInstanceVO.class);
            if (ImagePlatform.Windows.toString().equals(vm.getPlatform())) {
                logger.debug(String.format("Windows vm[uuid:%s] will sync dns by ports", vmUuid));
                return;
            }

            vmConfigSyncHelper.afterVmSyncDns(vmUuid);
            SetVmDnsOnHypervisorMsg msg = new SetVmDnsOnHypervisorMsg();
            msg.setHostUuid(vm.getHostUuid());
            msg.setVmInstanceUuid(vmUuid);
            List<String> dnsList = nwServiceMgr.getVmDns(vmUuid, vm.getDefaultL3NetworkUuid());
            if (CollectionUtils.isEmpty(dnsList) && DnsUtils.getDnsListFromString(
                    Q.New(VmGuestNetworkInfoVO.class).select(VmGuestNetworkInfoVO_.dnsList)
                    .eq(VmGuestNetworkInfoVO_.vmInstanceUuid, vmUuid).findValue()).isEmpty()) {
                logger.debug(String.format("skip setting DNS to empty for vm[uuid:%s], " +
                        "because DNS has not yet been obtained from within the VM", vmUuid));
                return;
            }
            msg.setDns(dnsList);

            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, vm.getHostUuid());
            bus.send(msg, new CloudBusCallBack(null) {
                @Override
                public void run(MessageReply rly) {
                    if (!rly.isSuccess()) {
                        logger.warn(String.format("vm [uuid:%s, name: %s] set dns failed, error :%s",
                                vm.getUuid(), vm.getName(), rly.getError().getDetails()));
                    }
                }
            });
        }
    }

    private void syncCustomSpecificationToVm(String vmUuid) {
        List<VmCustomSpecificationVO> vos = Q.New(VmCustomSpecificationVO.class)
                .eq(VmCustomSpecificationVO_.vmInstanceUuid, vmUuid).list();
        if (vos.isEmpty()) {
            return;
        }

        String hostUuid = Q.New(VmInstanceVO.class).select(VmInstanceVO_.hostUuid)
                .eq(VmInstanceVO_.uuid, vmUuid).findValue();
        if (StringUtils.isEmpty(hostUuid)) {
            logger.warn(String.format("QGA vm[uuid:%s] has no hostUuid, skip syncing custom specification", vmUuid));
            return;
        }

        VmConfigSyncStruct.VmSpecificationConfig spec = VmCustomSpecificationUtils.getVmSpecificationConfig(vos.get(0));
        logger.debug(String.format("get vm[uuid:%s] specification config: %s", vmUuid, spec));
        VmSpecificationConfigSyncOnHypervisorMsg syncMsg = new VmSpecificationConfigSyncOnHypervisorMsg();
        syncMsg.setSpec(spec);
        syncMsg.setVmInstanceUuid(vmUuid);
        syncMsg.setHostUuid(hostUuid);
        bus.makeTargetServiceIdByResourceUuid(syncMsg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(syncMsg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.error(String.format("QGA vm[uuid:%s] sync custom specification failed", vmUuid));
                }

                // delete the record no matter success or not
                dbf.removeCollection(vos, VmCustomSpecificationVO.class);
            }
        });
    }

    private void syncVmNicInfo(GuestToolsKvmCommands.QGANicInfoResultCmd result) {
        String vmUuid  = result.vmUuid;
        String nicInfo = result.nicInfo;
        if (vmUuid == null || vmUuid.isEmpty() || nicInfo == null || nicInfo.isEmpty()) {
            return;
        }
        logger.debug(String.format("QGA receive vm[uuid:%s] sync nic info [%s] ",
                vmUuid, nicInfo));

        @SuppressWarnings("unchecked")
        Map<String, Object> rawMap = JSONObjectUtil.toObject(nicInfo, Map.class);
        Map<String, List<String>> nicMacIpMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
            if (entry.getValue() instanceof String) {
                nicMacIpMap.put(entry.getKey(), Collections.singletonList((String) entry.getValue()));
            } else if (entry.getValue() instanceof List) {
                List<?> rawList = (List<?>) entry.getValue();
                List<String> stringList = new ArrayList<>();
                for (Object item : rawList) {
                    if (item instanceof String) {
                        stringList.add((String) item);
                    }
                }
                nicMacIpMap.put(entry.getKey(), stringList);
            }
        }
        if (nicMacIpMap.isEmpty()) {
            return;
        }
        for (Map.Entry<String, List<String>> entry : nicMacIpMap.entrySet()) {
            String mac = entry.getKey();
            List<String> ipAddresses = entry.getValue();

            if (!IPv6NetworkUtils.isValidMac(mac)) {
                logger.warn(String.format("QGA check vm[uuid:%s] nic info with mac[%s] is not valid", vmUuid, mac));
                return;
            }

            for (String ipAddress : ipAddresses) {
                if (!StringUtils.isEmpty(ipAddress) &&
                        (ipAddress.startsWith(GuestToolsConstant.QGA_NIC_INFO_IPV4_GATEWAY_PREFIX) ||
                                ipAddress.startsWith(GuestToolsConstant.QGA_NIC_INFO_IPV6_GATEWAY_PREFIX))) {
                    continue;
                }

                if (!StringUtils.isEmpty(ipAddress) && !IPv6NetworkUtils.isValidIpv4(ipAddress) && !IPv6NetworkUtils.isValidIpv6(ipAddress)) {
                    logger.warn(String.format("QGA check vm[uuid:%s] nic info with mac[%s] contains invalid ip[%s]",
                            vmUuid, mac, ipAddress));
                    return;
                }
            }
        }

        Boolean ipConflict = Boolean.FALSE;
        Set<String> ipSet = new HashSet<>();
        for (List<String> ipAddresses : nicMacIpMap.values()) {
            for (String ip : ipAddresses) {
                if (ipSet.contains(ip) && !ip.isEmpty()) {
                    ipConflict = Boolean.TRUE;
                }
                ipSet.add(ip);
            }
        }

        List<VmNicVO> nicVOList = Q.New(VmNicVO.class)
                .eq(VmNicVO_.vmInstanceUuid, vmUuid)
                .in(VmNicVO_.mac, nicMacIpMap.keySet()).list();
        Map<String, Set<UsedIpVO>> nicUuidUsedIpMap = new HashMap<>();
        nicVOList.forEach(nicVO ->
                nicUuidUsedIpMap.computeIfAbsent(nicVO.getUuid(), k -> new HashSet<>())
                        .addAll(nicVO.getUsedIps())
        );

        Map<String, VmGuestNetworkInfoVO> oldInfoMap = new HashMap<>();
        Map<String, VmGuestNetworkInfoVO> newInfoMap = new HashMap<>();
        List<VmGuestNetworkInfoVO> oldInfoList = Q.New(VmGuestNetworkInfoVO.class)
                .eq(VmGuestNetworkInfoVO_.vmInstanceUuid, vmUuid).list();
        for (VmGuestNetworkInfoVO oldInfo : oldInfoList) {
            oldInfoMap.putIfAbsent(oldInfo.getVmNicUuid() != null ? oldInfo.getVmNicUuid() : oldInfo.getVmInstanceUuid(), oldInfo);
        }

        Set<VmNicVO> updatedNicSet = new HashSet<>();
        Set<VmNicVO> nicVOUpdateSet = new HashSet<>();
        Set<VmNicVO> nicNeedSyncToVmSet = new HashSet<>();
        Set<UsedIpVO> usedIpVONewSet = new HashSet<>();
        Set<UsedIpVO> usedIpVOUpdateSet = new HashSet<>();
        Set<UsedIpVO> usedIpVODeleteSet = new HashSet<>();

        class GuestNetworkInfoProcessor {
            private final VmNicVO nicVO;
            private final VmGuestNetworkInfoVO oldInfo;
            private final VmGuestNetworkInfoVO newInfo;
            private final UsedIpVO usedIpVO;
            private final UsedIpVO usedIpv6VO;

            public GuestNetworkInfoProcessor(VmNicVO nicVO) {
                this.nicVO = nicVO;
                this.oldInfo = oldInfoMap.computeIfAbsent(nicVO.getUuid(), k -> new VmGuestNetworkInfoVO());
                List<String> ipAddresses = nicMacIpMap.get(nicVO.getMac());
                if (CollectionUtils.isEmpty(ipAddresses)) {
                    this.newInfo = null;
                    this.usedIpVO = null;
                    this.usedIpv6VO = null;
                    return;
                }

                this.newInfo = newInfoMap.computeIfAbsent(nicVO.getUuid(), k ->
                        processGuestNetworkInfo(ipAddresses, nicVO));

                usedIpVO = nicUuidUsedIpMap.getOrDefault(nicVO.getUuid(), Collections.emptySet()).stream()
                        .filter(vo -> Objects.equals(vo.getIpVersion(), IPv6Constants.IPv4))
                        .findFirst().orElse(new UsedIpVO());

                usedIpv6VO = nicUuidUsedIpMap.getOrDefault(nicVO.getUuid(), Collections.emptySet()).stream()
                        .filter(vo -> Objects.equals(vo.getIpVersion(), IPv6Constants.IPv6))
                        .findFirst().orElse(new UsedIpVO());
            }

            private VmGuestNetworkInfoVO processGuestNetworkInfo(List<String> ipAddresses, VmNicVO nicVO) {
                if (CollectionUtils.isEmpty(ipAddresses)) {
                    return null;
                }
                VmGuestNetworkInfoVO info = new VmGuestNetworkInfoVO(nicVO.getVmInstanceUuid(), nicVO.getUuid());

                for (String ip : ipAddresses) {
                    if ((IPv6NetworkUtils.isValidIpv4(ip) || ip.isEmpty()) && info.getIpAddress() == null) {
                        if (ip.isEmpty() || ip.contains(NetworkUtils.DEFAULT_IPV4_PREFIX_SPLIT)) {
                            info.setIpAddress(ip);
                        } else {
                            info.setIpAddress(ip + NetworkUtils.DEFAULT_IPV4_PREFIX_SPLIT + NetworkUtils.DEFAULT_IPV4_PREFIX);
                        }
                    } else if (IPv6NetworkUtils.isValidGlobalIpv6(ip) && info.getIpv6Address() == null) {
                        if (ip.contains(NetworkUtils.DEFAULT_IPV6_PREFIX_SPLIT)) {
                            info.setIpv6Address(ip);
                        } else {
                            info.setIpv6Address(ip + NetworkUtils.DEFAULT_IPV6_PREFIX_SPLIT + NetworkUtils.DEFAULT_IPV6_PREFIX);
                        }
                    } else if (ip.startsWith(GuestToolsConstant.QGA_NIC_INFO_IPV4_GATEWAY_PREFIX) && info.getGateway() == null) {
                        info.setGateway(ip.substring(GuestToolsConstant.QGA_NIC_INFO_IPV4_GATEWAY_PREFIX.length()));
                    } else if (ip.startsWith(GuestToolsConstant.QGA_NIC_INFO_IPV6_GATEWAY_PREFIX) && info.getIpv6Gateway() == null) {
                        info.setIpv6Gateway(ip.substring(GuestToolsConstant.QGA_NIC_INFO_IPV6_GATEWAY_PREFIX.length()));
                    }
                }

                return info;
            }

            public void getNicToBeUpdated() {
                if (oldInfo == null || newInfo == null) {
                    return;
                }

                getFromIpv4();
                getFromIpv6();
                getFromGateway();
                getFromIpv6Gateway();
            }

            public void getFromIpv4() {
                if (Objects.equals(oldInfo.getIpAddress(), newInfo.getIpAddress())) {
                    return;
                }

                // Windows vm with APIPA
                if (oldInfo.getIpAddress() == null && usedIpVO.getIp() != null && !NetworkUtils.isInternalAddress(usedIpVO.getIp())
                        && NetworkUtils.isInternalAddress(NetworkUtils.getIp(newInfo.getIpAddress()))) {
                    logger.debug(String.format("QGA find vm[uuid:%s] internal ip[%s] for nic[uuid:%s] which is assigned by APIPA and has not been synced yet",
                            vmUuid, newInfo.getIpAddress(), nicVO.getUuid()));
                    newInfo.setIpAddress(null);
                    nicNeedSyncToVmSet.add(nicVO);
                    return;
                }
                if (!StringUtils.isEmpty(newInfo.getIpAddress())) {
                    updatedNicSet.add(nicVO);
                    logger.debug(String.format("QGA find vm[uuid:%s] internal ip changed from [%s] to [%s]",
                            vmUuid, oldInfo.getIpAddress(), newInfo.getIpAddress()));

                    // only Windows vm will have sync dns tag here, sync dns by ports
                    if (oldInfo.getIpAddress() == null && vmConfigSyncHelper.vmNeedSyncDns(vmUuid) &&
                            !DnsUtils.getVmNicDnsList(nicVO.getUuid(), IPv6Constants.IPv4).isEmpty()) {
                        nicNeedSyncToVmSet.add(nicVO);
                    }
                    return;
                }
                // new ip address can be empty
                newInfo.setIpAddress(null);
                if (oldInfo.getIpAddress() == null && usedIpVO.getIp() != null) {
                    logger.debug(String.format("QGA find vm[uuid:%s] internal ip for nic[uuid:%s] is empty and has not been synced yet",
                            vmUuid, nicVO.getUuid()));


                    nicNeedSyncToVmSet.add(nicVO);
                    return;
                }
                if (oldInfo.getIpAddress() == null) {
                    return;
                }
                updatedNicSet.add(nicVO);
                logger.debug(String.format("QGA find vm[uuid:%s] internal ip [%s] deleted",
                        vmUuid, oldInfo.getIpAddress()));
            }

            public void getFromIpv6() {
                if (oldInfo.getIpv6Address() != null && newInfo.getIpv6Address() == null) {
                    updatedNicSet.add(nicVO);
                    logger.debug(String.format("QGA find vm[uuid:%s] internal ipv6 [%s] deleted",
                            vmUuid, oldInfo.getIpv6Address()));
                } else if (!Objects.equals(oldInfo.getIpv6Address(), newInfo.getIpv6Address())) {
                    // new ipv6 address is not null
                    updatedNicSet.add(nicVO);
                    logger.debug(String.format("QGA find vm[uuid:%s] internal ipv6 changed from [%s] to [%s]",
                            vmUuid, oldInfo.getIpv6Address(), newInfo.getIpv6Address()));

                    // only Windows vm will have sync dns tag here, sync dns by ports
                    if (oldInfo.getIpv6Address() == null && vmConfigSyncHelper.vmNeedSyncDns(vmUuid) &&
                            !DnsUtils.getVmNicDnsList(nicVO.getUuid(), IPv6Constants.IPv6).isEmpty()) {
                        nicNeedSyncToVmSet.add(nicVO);
                    }
                } else if (oldInfo.getIpv6Address() == null && usedIpv6VO.getIp() != null) {
                    // new ipv6 address is null
                    logger.debug(String.format("QGA find vm[uuid:%s] internal ipv6 for nic[uuid:%s] is empty and has not been synced yet",
                            vmUuid, nicVO.getUuid()));
                    nicNeedSyncToVmSet.add(nicVO);
                }
            }

            public void getFromGateway() {
                // ip does not exist, so no need to sync gateway
                if (usedIpVO.getUuid() == null && StringUtils.isEmpty(newInfo.getIpAddress())) {
                    newInfo.setGateway(null);
                    return;
                }

                // gateway will be updated when the ip address is updated
                boolean newIpSameWithUsedIp = Objects.equals(NetworkUtils.getIp(newInfo.getIpAddress()), usedIpVO.getIp()) &&
                        Objects.equals(NetworkUtils.getNetmask(newInfo.getIpAddress()), usedIpVO.getNetmask());
                if (!Objects.equals(oldInfo.getIpAddress(), newInfo.getIpAddress()) && !newIpSameWithUsedIp) {
                    return;
                }

                if (oldInfo.getGateway() != null && newInfo.getGateway() == null) {
                    updatedNicSet.add(nicVO);
                    logger.debug(String.format("QGA find vm[uuid:%s] internal gateway [%s] deleted",
                            vmUuid, oldInfo.getGateway()));
                } else if (!Objects.equals(oldInfo.getGateway(), newInfo.getGateway())) {
                    updatedNicSet.add(nicVO);
                    logger.debug(String.format("QGA find vm[uuid:%s] internal gateway changed from [%s] to [%s]",
                            vmUuid, oldInfo.getGateway(), newInfo.getGateway()));
                } else if (oldInfo.getGateway() == null && usedIpVO.getGateway() != null) {
                    logger.debug(String.format("QGA find vm[uuid:%s] internal gateway for nic[uuid:%s] is empty and has not been synced yet",
                            vmUuid, nicVO.getUuid()));
                    nicNeedSyncToVmSet.add(nicVO);
                }
            }

            public void getFromIpv6Gateway() {
                // ip does not exist, so no need to sync gateway
                if (usedIpv6VO.getUuid() == null && StringUtils.isEmpty(newInfo.getIpv6Address())) {
                    newInfo.setIpv6Gateway(null);
                    return;
                }

                // ipv6 gateway will be updated when the ipv6 address is updated
                boolean newIpSameWithUsedIp = Objects.equals(NetworkUtils.getIp(newInfo.getIpv6Address()), usedIpv6VO.getIp()) &&
                        Objects.equals(NetworkUtils.getNetmask(newInfo.getIpv6Address()), usedIpv6VO.getNetmask());
                if (!Objects.equals(oldInfo.getIpv6Address(), newInfo.getIpv6Address()) && !newIpSameWithUsedIp) {
                    return;
                }

                if (oldInfo.getIpv6Gateway() != null && newInfo.getIpv6Gateway() == null) {
                    updatedNicSet.add(nicVO);
                    logger.debug(String.format("QGA find vm[uuid:%s] internal ipv6 gateway [%s] deleted",
                            vmUuid, oldInfo.getIpv6Gateway()));
                } else if (!Objects.equals(oldInfo.getIpv6Gateway(), newInfo.getIpv6Gateway())) {
                    updatedNicSet.add(nicVO);
                    logger.debug(String.format("QGA find vm[uuid:%s] internal ipv6 gateway changed from [%s] to [%s]",
                            vmUuid, oldInfo.getIpv6Gateway(), newInfo.getIpv6Gateway()));
                } else if (oldInfo.getIpv6Gateway() == null && usedIpv6VO.getGateway() != null) {
                    logger.debug(String.format("QGA find vm[uuid:%s] internal ipv6 gateway for nic[uuid:%s] is empty and has not been synced yet",
                            vmUuid, nicVO.getUuid()));
                    nicNeedSyncToVmSet.add(nicVO);
                }
            }
        }
        class GuestNetworkInfoSynchronizer {
            private final VmNicVO nicVO;
            private final VmGuestNetworkInfoVO oldInfo;
            private final VmGuestNetworkInfoVO newInfo;
            private final Set<UsedIpVO> oldIpv4Set;
            private final Set<UsedIpVO> oldIpv6Set;
            private final boolean enableIPAM;

            public GuestNetworkInfoSynchronizer(VmNicVO nicVO, boolean enableIPAM) {
                this.nicVO = nicVO;
                this.oldInfo = oldInfoMap.get(nicVO.getUuid());
                this.newInfo = newInfoMap.get(nicVO.getUuid());
                this.oldIpv4Set = nicUuidUsedIpMap.getOrDefault(nicVO.getUuid(), Collections.emptySet()).stream()
                        .filter(vo -> Objects.equals(vo.getIpVersion(), IPv6Constants.IPv4))
                        .collect(Collectors.toSet());
                this.oldIpv6Set = nicUuidUsedIpMap.getOrDefault(nicVO.getUuid(), Collections.emptySet()).stream()
                        .filter(vo -> Objects.equals(vo.getIpVersion(), IPv6Constants.IPv6))
                        .collect(Collectors.toSet());
                this.enableIPAM = enableIPAM;
            }

            public void syncGuestNetworkInfo() {
                if (oldInfo == null || newInfo == null) {
                    return;
                }

                syncIpv4();
                syncIpv6();
                syncGateway();
                syncIpv6Gateway();
            }

            private void syncIpv4() {
                if (Objects.equals(oldInfo.getIpAddress(), newInfo.getIpAddress())) {
                    return;
                }

                boolean skip = sendInternalIpChanged(oldInfo.getIpAddress(), newInfo.getIpAddress(), vmUuid, nicVO);
                // vm ip is null or empty, delete usedIp and nic ip
                if (StringUtils.isEmpty(newInfo.getIpAddress())) {
                    newInfo.setIpAddress(null);
                    if (enableIPAM) {
                        logger.debug(String.format("l3 network[uuid:%s] enable IPAM, skip deleting ip address of nic[uuid:%s]",
                                nicVO.getL3NetworkUuid(), nicVO.getUuid()));
                        return;
                    }

                    if (StringUtils.isEmpty(newInfo.getIpv6Address())) {
                        nicVO.removeIp();
                        nicVOUpdateSet.add(nicVO);
                    }
                    usedIpVODeleteSet.addAll(oldIpv4Set);
                    return;
                }

                String ipv4 = NetworkUtils.getIp(newInfo.getIpAddress());
                String netmask = NetworkUtils.getNetmask(newInfo.getIpAddress());
                if (skip || oldIpv4Set.stream().anyMatch(vo ->
                        Objects.equals(vo.getIp(), ipv4) && Objects.equals(vo.getNetmask(), netmask))) {
                    return;
                }

                // read vm ip to usedIp and nic
                UsedIpVO oldUsedIpVO = oldIpv4Set.stream().findFirst().orElse(new UsedIpVO());
                if (oldUsedIpVO.getUuid() != null &&
                        !VmGlobalConfig.ENABLE_VM_INTERNAL_IP_OVERWRITE.value(Boolean.class)) {
                    return;
                }

                String ipRangeUuid = IpRangeHelper.getIpRangeUuid(nicVO.getL3NetworkUuid(), ipv4);
                UsedIpVO usedIpVO = UsedIpHelper.getOrRecreateForIpChange(oldUsedIpVO, ipv4, ipRangeUuid, usedIpVODeleteSet);
                if (usedIpVO.getUuid() == null) {
                    usedIpVO.setUuid(Platform.getUuid());
                    usedIpVO.setIpVersion(IPv6Constants.IPv4);
                    usedIpVO.setVmNicUuid(nicVO.getUuid());
                    usedIpVO.setL3NetworkUuid(nicVO.getL3NetworkUuid());
                    usedIpVONewSet.add(usedIpVO);
                } else {
                    usedIpVOUpdateSet.add(usedIpVO);
                }
                usedIpVO.setIp(ipv4);
                usedIpVO.setIpInLong(NetworkUtils.ipv4StringToLong(usedIpVO.getIp()));
                usedIpVO.setIpInBinary(NetworkUtils.ipStringToBytes(usedIpVO.getIp()));
                usedIpVO.setNetmask(netmask);
                // only overwrite gateway if tools support obtaining it
                if (oldInfo.getGateway() != null || newInfo.getGateway() != null) {
                    usedIpVO.setGateway(newInfo.getGateway());
                }
                usedIpVO.setIpRangeUuid(ipRangeUuid);
                if (enableIPAM && IpRangeHelper.checkIpRangeConflict(usedIpVO)) {
                    sendInternalIpRangeConflict(nicVO, usedIpVO.getIp());
                    usedIpVONewSet.remove(usedIpVO);
                    usedIpVOUpdateSet.remove(usedIpVO);
                    if (usedIpVO != oldUsedIpVO) {
                        usedIpVODeleteSet.remove(oldUsedIpVO);
                    }
                    return;
                }

                nicVO.setUsedIpUuid(usedIpVO.getUuid());
                nicVO.setIp(usedIpVO.getIp());
                nicVO.setNetmask(usedIpVO.getNetmask());
                nicVO.setIpVersion(usedIpVO.getIpVersion());
                nicVO.setGateway(usedIpVO.getGateway());
                nicVOUpdateSet.add(nicVO);
            }

            private void syncIpv6() {
                if (Objects.equals(oldInfo.getIpv6Address(), newInfo.getIpv6Address())) {
                    return;
                }

                boolean skip = sendInternalIpChanged(oldInfo.getIpv6Address(), newInfo.getIpv6Address(), vmUuid, nicVO);
                if (StringUtils.isEmpty(newInfo.getIpv6Address())) {
                    if (enableIPAM) {
                        logger.debug(String.format("l3 network[uuid:%s] enable IPAM, skip deleting ip address of nic[uuid:%s]",
                                nicVO.getL3NetworkUuid(), nicVO.getUuid()));
                        return;
                    }

                    if (StringUtils.isEmpty(newInfo.getIpAddress())) {
                        nicVO.removeIp();
                        nicVOUpdateSet.add(nicVO);
                    }
                    usedIpVODeleteSet.addAll(oldIpv6Set);
                    return;
                }

                String ipv6 = NetworkUtils.getIp(newInfo.getIpv6Address());
                String netmask = NetworkUtils.getNetmask(newInfo.getIpv6Address());
                if (skip || oldIpv6Set.stream().anyMatch(vo ->
                        Objects.equals(vo.getIp(), ipv6) && Objects.equals(vo.getNetmask(), netmask))) {
                    return;
                }

                if (NetworkUtils.isInternalAddress(ipv6)) {
                    return;
                }

                // read vm ip to usedIp and nic
                UsedIpVO oldUsedIpVO = oldIpv6Set.stream().findFirst().orElse(new UsedIpVO());
                if (oldUsedIpVO.getUuid() != null &&
                        !VmGlobalConfig.ENABLE_VM_INTERNAL_IP_OVERWRITE.value(Boolean.class)) {
                    return;
                }

                String ipRangeUuid = IpRangeHelper.getIpRangeUuid(nicVO.getL3NetworkUuid(), ipv6);
                UsedIpVO usedIpVO = UsedIpHelper.getOrRecreateForIpChange(oldUsedIpVO, ipv6, ipRangeUuid, usedIpVODeleteSet);
                if (usedIpVO.getUuid() == null) {
                    usedIpVO.setUuid(Platform.getUuid());
                    usedIpVO.setIpVersion(IPv6Constants.IPv6);
                    usedIpVO.setVmNicUuid(nicVO.getUuid());
                    usedIpVO.setL3NetworkUuid(nicVO.getL3NetworkUuid());
                    usedIpVONewSet.add(usedIpVO);
                } else {
                    usedIpVOUpdateSet.add(usedIpVO);
                }
                usedIpVO.setIp(ipv6);
                usedIpVO.setIpInBinary(NetworkUtils.ipStringToBytes(usedIpVO.getIp()));
                usedIpVO.setNetmask(netmask);
                if (oldInfo.getIpv6Gateway() != null || newInfo.getIpv6Gateway() != null) {
                    usedIpVO.setGateway(newInfo.getIpv6Gateway());
                }
                usedIpVO.setIpRangeUuid(ipRangeUuid);
                if (enableIPAM && IpRangeHelper.checkIpRangeConflict(usedIpVO)) {
                    sendInternalIpRangeConflict(nicVO, usedIpVO.getIp());
                    usedIpVONewSet.remove(usedIpVO);
                    usedIpVOUpdateSet.remove(usedIpVO);
                    if (usedIpVO != oldUsedIpVO) {
                        usedIpVODeleteSet.remove(oldUsedIpVO);
                    }
                    return;
                }

                // not the first time to sync an empty ip to delete the IPv4,
                // or the first time to sync an empty IPv4 whose nic does not have IPv4
                if ((oldInfo.getIpAddress() != null || oldIpv4Set.isEmpty()) &&
                        StringUtils.isEmpty(newInfo.getIpAddress())) {
                    nicVO.setUsedIpUuid(usedIpVO.getUuid());
                    nicVO.setIp(usedIpVO.getIp());
                    nicVO.setNetmask(usedIpVO.getNetmask());
                    nicVO.setIpVersion(usedIpVO.getIpVersion());
                    nicVO.setGateway(usedIpVO.getGateway());
                    nicVOUpdateSet.add(nicVO);
                }
            }

            private void syncGateway() {
                if (!Objects.equals(oldInfo.getIpAddress(), newInfo.getIpAddress()) ||
                        Objects.equals(oldInfo.getGateway(), newInfo.getGateway())) {
                    return;
                }

                UsedIpVO usedIpVO = oldIpv4Set.stream().findFirst().orElse(null);
                if (usedIpVO == null || Objects.equals(usedIpVO.getGateway(), newInfo.getGateway()) ||
                        !VmGlobalConfig.ENABLE_VM_INTERNAL_IP_OVERWRITE.value(Boolean.class)) {
                    return;
                }

                if (enableIPAM) {
                    logger.debug(String.format("l3 network[uuid:%s] enable IPAM, skip overwriting gateway of nic[uuid:%s]",
                            nicVO.getL3NetworkUuid(), nicVO.getUuid()));
                    return;
                }

                usedIpVO.setGateway(newInfo.getGateway());
                usedIpVOUpdateSet.add(usedIpVO);
                nicVO.setGateway(usedIpVO.getGateway());
                nicVOUpdateSet.add(nicVO);
            }

            private void syncIpv6Gateway() {
                if (!Objects.equals(oldInfo.getIpv6Address(), newInfo.getIpv6Address()) ||
                        Objects.equals(oldInfo.getIpv6Gateway(), newInfo.getIpv6Gateway())) {
                    return;
                }

                UsedIpVO usedIpVO = oldIpv6Set.stream().findFirst().orElse(null);
                if (usedIpVO == null || Objects.equals(usedIpVO.getGateway(), newInfo.getIpv6Gateway()) ||
                        !VmGlobalConfig.ENABLE_VM_INTERNAL_IP_OVERWRITE.value(Boolean.class)) {
                    return;
                }

                if (enableIPAM) {
                    logger.debug(String.format("l3 network[uuid:%s] enable IPAM, skip overwriting ipv6 gateway of nic[uuid:%s]",
                            nicVO.getL3NetworkUuid(), nicVO.getUuid()));
                    return;
                }

                usedIpVO.setGateway(newInfo.getIpv6Gateway());
                usedIpVOUpdateSet.add(usedIpVO);
                if ((oldInfo.getIpAddress() != null || oldIpv4Set.isEmpty()) &&
                        StringUtils.isEmpty(newInfo.getIpAddress())) {
                    nicVO.setGateway(usedIpVO.getGateway());
                    nicVOUpdateSet.add(nicVO);
                }
            }
        }

        for (VmNicVO nicVO : nicVOList) {
            new GuestNetworkInfoProcessor(nicVO).getNicToBeUpdated();
        }

        if (!updatedNicSet.isEmpty()) {
            List<L3NetworkVO> l3s = Q.New(L3NetworkVO.class).in(L3NetworkVO_.uuid,
                    updatedNicSet.stream().map(VmNicVO::getL3NetworkUuid).collect(Collectors.toList())).list();
            // Send alarm if nic use l3 without dhcp
            updatedNicSet.forEach(nicVO -> {
                // only support l3 without dhcp
                if (l3s.stream().noneMatch(l3 -> l3.getUuid().equals(nicVO.getL3NetworkUuid()) && !l3.enableIpAllocation())) {
                    return;
                }

                new GuestNetworkInfoSynchronizer(nicVO, l3s.stream().anyMatch(l3 ->
                        l3.getUuid().equals(nicVO.getL3NetworkUuid()) && l3.getEnableIPAM()))
                        .syncGuestNetworkInfo();
            });
        }

        if(!updatedNicSet.isEmpty() && !ipConflict) {
            Stream.concat(usedIpVONewSet.stream(), usedIpVOUpdateSet.stream())
                    .forEach(usedIpVO ->
                            ipOperator.setStaticIp(vmUuid, usedIpVO.getL3NetworkUuid(), usedIpVO.getIp())
                    );
            dbf.persistCollection(usedIpVONewSet);
            dbf.updateCollection(usedIpVOUpdateSet);
            if (!nicVOUpdateSet.isEmpty()) {
                logger.debug(String.format("QGA vm[uuid:%s] internal ip overwrite nic used ip",
                        vmUuid));
                dbf.updateCollection(nicVOUpdateSet);
            }
            for (UsedIpVO ipVO : usedIpVODeleteSet) {
                ipOperator.deleteStaticIpByVmUuidAndL3Uuid(vmUuid, ipVO.getL3NetworkUuid(),
                        IPv6NetworkUtils.ipv6AddessToTagValue(ipVO.getIp()));
            }
            dbf.removeCollection(usedIpVODeleteSet, UsedIpVO.class);
        }

        for (VmGuestNetworkInfoVO info : newInfoMap.values()) {
            VmGuestNetworkInfoVO oldInfo = oldInfoMap.get(info.getVmNicUuid());
            if (oldInfo.getId() != null) {
                oldInfo.setIpAddress(info.getIpAddress());
                oldInfo.setGateway(info.getGateway());
                oldInfo.setIpv6Address(info.getIpv6Address());
                oldInfo.setIpv6Gateway(info.getIpv6Gateway());
                dbf.update(oldInfo);
            } else if (info.getIpAddress() != null || info.getGateway() != null ||
                    info.getIpv6Address() != null || info.getIpv6Gateway() != null) {
                dbf.persist(info);
            }
        }

        if (!nicNeedSyncToVmSet.isEmpty()) {
            vmConfigSyncHelper.setVmSyncPorts(vmUuid);
            syncPortsToVm(vmUuid, nicNeedSyncToVmSet.stream().map(VmNicVO::getUuid).collect(Collectors.toList()), new NopeReturnValueCompletion());
        }

        // delete sync dns tag for Windows vm
        if (vmConfigSyncHelper.vmNeedSyncDns(vmUuid)) {
            vmConfigSyncHelper.afterVmSyncDns(vmUuid);
        }
    }

    private boolean sendInternalIpChanged(String oldInternalIpAddress, String newInternalIpAddress, String vmUuid, VmNicVO nicVO) {
        boolean skip = false;
        VmCanonicalEvents.VmInternalIpChangedData cData = new VmCanonicalEvents.VmInternalIpChangedData();
        cData.setVmUuid(vmUuid);
        cData.setL3NetworkUuid(nicVO.getL3NetworkUuid());
        if (StringUtils.isEmpty(oldInternalIpAddress)){
            cData.setOldInternalIp("");
        } else {
            cData.setOldInternalIp(oldInternalIpAddress);
        }
        cData.setRelateResourceUuid("");
        if (StringUtils.isEmpty(newInternalIpAddress)){
            cData.setNewInternalIp("");
            evf.fire(VmCanonicalEvents.VM_NIC_INFO_CHANGED_PATH, cData);
            logger.debug(String.format("QGA send vm[uuid:%s] internal ip changed and send ALARM",
                    vmUuid));
            return false;
        } else {
            cData.setNewInternalIp(newInternalIpAddress);
        }
        // simple L3 has no vip anymore, it is prepared for other l3
        VipVO existVipVO = Q.New(VipVO.class).eq(VipVO_.l3NetworkUuid, nicVO.getL3NetworkUuid())
                .eq(VipVO_.ip, NetworkUtils.getIp(newInternalIpAddress)).limit(1).find();
        if (existVipVO != null) {
            cData.setRelateResourceUuid(existVipVO.getUuid());
        }
        VmNicVO existNicVO = Q.New(VmNicVO.class).notEq(VmNicVO_.uuid, nicVO.getUuid())
                .eq(VmNicVO_.l3NetworkUuid, nicVO.getL3NetworkUuid())
                .eq(VmNicVO_.ip, NetworkUtils.getIp(newInternalIpAddress)).limit(1).find();
        if (existNicVO != null) {
            cData.setRelateResourceUuid(existNicVO.getVmInstanceUuid());
        }
        UsedIpVO existUsedIpVO = Q.New(UsedIpVO.class)
                .eq(UsedIpVO_.l3NetworkUuid, nicVO.getL3NetworkUuid())
                .eq(UsedIpVO_.ip, NetworkUtils.getIp(newInternalIpAddress)).limit(1).find();
        if (existUsedIpVO != null && Objects.equals(existUsedIpVO.getUsedFor(), IpAllocatedReason.Reserved.toString())) {
            cData.setRelateResourceUuid(vmUuid);
            skip = true;
        } else if (existUsedIpVO != null && !Objects.equals(existUsedIpVO.getVmNicUuid(), nicVO.getUuid())) {
            cData.setRelateResourceUuid(Q.New(VmNicVO.class).select(VmNicVO_.vmInstanceUuid)
                    .eq(VmNicVO_.uuid, existUsedIpVO.getVmNicUuid()).findValue());
        }
        evf.fire(VmCanonicalEvents.VM_NIC_INFO_CHANGED_PATH, cData);
        logger.debug(String.format("QGA send vm[uuid:%s] internal ip changed and send ALARM",
                vmUuid));
        if (cData.getRelateResourceUuid() != null && !cData.getRelateResourceUuid().isEmpty()) {
            VmCanonicalEvents.VmInternalIpDuplicateData dData = new VmCanonicalEvents.VmInternalIpDuplicateData();
            dData.setVmUuid(cData.getVmUuid());
            dData.setL3NetworkUuid(cData.getL3NetworkUuid());
            dData.setInternalIp(cData.getNewInternalIp());
            dData.setRelateResourceUuid(cData.getRelateResourceUuid());
            evf.fire(VmCanonicalEvents.VM_NIC_INFO_DUPLICATE_PATH, dData);
        }
        return skip;
    }

    private void sendInternalIpRangeConflict(VmNicVO nicVO, String ip) {
        logger.debug(String.format("QGA send vm[uuid:%s] internal ip range conflict", nicVO.getVmInstanceUuid()));
        VmCanonicalEvents.VmInternalIpRangeConflictData cData = new VmCanonicalEvents.VmInternalIpRangeConflictData();
        cData.setVmUuid(nicVO.getVmInstanceUuid());
        cData.setL3NetworkUuid(nicVO.getL3NetworkUuid());
        cData.setInternalIp(ip);
        evf.fire(VmCanonicalEvents.VM_NIC_INFO_IPRANGE_CONFLICT_PATH, cData);
    }

    private void zwatchInstallResult(GuestToolsKvmCommands.ZwatchInstallResultCmd result) {
        logger.debug(String.format("zwatch version: %s is installed to vm [uuid:%s]",
                result.version, result.vmInstanceUuid));

        // new format: "version=x.x.x,os_type=centos 7,platform=linux"
        // old format: "x.x.x"
        if (result.version.contains("version")) {
            String[] resultInfos = result.version.split(",");
            Map<String, String> resultMaps = new HashMap<String, String>();
            for (String ret : resultInfos) {
                String[] info = ret.split("=");
                if (info.length != 2) {
                    logger.error(String.format("vm %s zwatch installed result format error: %s", result.vmInstanceUuid, result.version));
                    return;
                }
                resultMaps.put(info[0], info[1]);
            }

            if (resultMaps.get("version") != null) {
                SystemTagCreator creator = VmSystemTags.VM_GUEST_TOOLS.newSystemTagCreator(result.vmInstanceUuid);
                creator.setTagByTokens(Collections.singletonMap(VmSystemTags.VM_GUEST_TOOLS_VERSION_TOKEN, resultMaps.get("version")));
                creator.inherent = false;
                creator.recreate = true;
                creator.create();
            } else {
                logger.error(String.format("vm %s zwatch installed result format error: %s", result.vmInstanceUuid, result.version));
                return;
            }
            String osType = resultMaps.get("os_type");
            String platform = resultMaps.get("platform");
            if  (osType != null && !osType.equals("") && platform != null && !platform.equals("")) {
                updateGuestToolsVmOsInfo(result.vmInstanceUuid, osType, platform);
            }
        } else {
            SystemTagCreator creator = VmSystemTags.VM_GUEST_TOOLS.newSystemTagCreator(result.vmInstanceUuid);
            creator.setTagByTokens(Collections.singletonMap(VmSystemTags.VM_GUEST_TOOLS_VERSION_TOKEN, result.version));
            creator.inherent = false;
            creator.recreate = true;
            creator.create();
        }

    }

    @Override
    public boolean start() {
        upgradeGuestToolsStateVO();
        populateExtensions();
        reloadGuestTools();
        deployAnsible();
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("uos 20"); /* use lower letter */
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("kylin v10");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("kylin v11");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("neokylin v7");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("neokylin v7update6");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("kylin 4");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("centos 6");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("centos 7");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("centos 8");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("centos 9");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("centos 10");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("opensuse-leap 11");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("sles 11");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("sled 11");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("opensuse-leap 12");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("sles 12");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("sled 12");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("opensuse-leap 15");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("sles 15");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("sled 15");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("ol 7");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("ol 8");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("ol 9");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("ol 10");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("rhel 6");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("rhel 7");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("rhel 8");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("rhel 9");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("rhel 10");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("anolis 7");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("anolis 8");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("ubuntu 14");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("ubuntu 16");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("ubuntu 18");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("ubuntu 20");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("ubuntu 22");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("ubuntu 24");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("debian 9");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("debian 10");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("debian 11");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("debian 12");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("debian 13");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("ubuntu 10");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("fedora 30");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("fedora 31");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("fedora 42");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("openEuler 20");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("openEuler 22");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("openEuler 24");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("rocky 8");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("rocky 9");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("rocky 10");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("almalinux 9");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("almalinux 10");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("amzn 2023");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("mswindows 7");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("mswindows 8");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("mswindows 10");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("mswindows 11");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("mswindows 2008r2");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("mswindows 2012");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("mswindows 2012r2");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("mswindows 2016");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("mswindows 2019");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("mswindows 2022");
        GUEST_OS_SUPPORT_QGA_GUEST_TOOLS.add("mswindows 2025");

        restf.registerSyncHttpCallHandler(GuestToolsKvmCommands.ZWATCH_INSTALL_RESULT_PATH, GuestToolsKvmCommands.ZwatchInstallResultCmd.class, cmd -> {
            zwatchInstallResult(cmd);
            return null;
        });

        restf.registerSyncHttpCallHandler(GuestToolsKvmCommands.QGA_VM_SYNC_NETWORK_INFO_PATH, GuestToolsKvmCommands.QGANicInfoResultCmd.class, cmd ->{
            syncVmNicInfo(cmd);
            return null;
        });

        restf.registerSyncHttpCallHandler(GuestToolsKvmCommands.QGA_VM_STATE_REPORT_PATH, GuestToolsKvmCommands.QGAVmStateResultCmd.class, cmd ->{
            reportVmQgaState(cmd);
            return null;
        });
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private void deployAnsible() {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        asf.deployModule(GuestToolsConstant.ANSIBLE_MODULE_PATH, GuestToolsConstant.ANSIBLE_PLAYBOOK_NAME);
    }

    private void populateExtensions() {
        for (GuestToolsHypervisorBackend bkd : pluginRgty.getExtensionList(GuestToolsHypervisorBackend.class)) {
            String type = bkd.getGuestToolsAgentType().toString();
            GuestToolsHypervisorBackend old = guestToolsHypervisorBackends.get(type);
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate GuestToolsHypervisorBackend[%s, %s] for type[%s]",
                        bkd.getClass().getName(), old.getClass().getName(), type));
            }
            guestToolsHypervisorBackends.put(type, bkd);
        }
    }

    private void reloadGuestTools() {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        /*
         tools/guesttools/
            aarch64
                ESX
                KVM
            x86_64
                ESX
                KVM
                    GuestTools-x.y.z.iso
         */

        new SQLBatch() {
            @Override
            protected void scripts() {
                List<GuestToolsVO> baremetalFromFs = scanBaremetalVmGuestTools();
                List<GuestToolsVO> voFromFS = scanGuestTools();
                voFromFS.addAll(baremetalFromFs);

                List<GuestToolsVO> voFromDB = q(GuestToolsVO.class)
                        .eq(GuestToolsVO_.managementNodeUuid, Platform.getManagementServerId())
                        .list();
                List<GuestToolsVO> voFromDBCopy = new ArrayList<>(voFromDB);

                // remove garbage guest tools vo
                voFromDB.removeAll(voFromFS);
                if (!voFromDB.isEmpty()) {
                    sql(GuestToolsVO.class)
                            .in(GuestToolsVO_.uuid, voFromDB.stream().map(GuestToolsVO::getUuid).collect(Collectors.toList()))
                            .delete();
                }

                // create new guest tools vo
                voFromFS.removeAll(voFromDBCopy);
                for (GuestToolsVO newVO : voFromFS) {
                    persist(newVO);
                }
            }
        }.execute();
    }

    private List<GuestToolsVO> scanBaremetalVmGuestTools() {
        List<GuestToolsVO> vos = new ArrayList<>();

        File kvmAgentFile = PathUtil.findFileOnClassPath("ansible/baremetalpxeserver/agent_version");
        if (kvmAgentFile == null) {
            return vos;
        }

        try {
            /* agent file format:
             * zwatch-vm-agent.linux-amd64=1.1.0
             * md5-zwatch-vm-agent.linux-amd64=aa59c70ade124b05125339e46f59a5c6 */
            String conent = FileUtils.readFileToString(kvmAgentFile);
            String[] lines = conent.split("\n");
            String[] columns = lines[0].split("=");

            GuestToolsVO vo = new GuestToolsVO();
            vo.setUuid(Platform.getUuid());
            vo.setManagementNodeUuid(Platform.getManagementServerId());
            /* TODO: only X86 and kvm is supported, hard code here */
            vo.setArchitecture("x86_64");
            vo.setHypervisorType("KVM");
            vo.setVersion(columns[1].trim());
            vo.setAgentType(GuestToolsAgentType.BaremetalForLinux.toString());
            vos.add(vo);

        } catch (IOException e) {
            logger.debug(String.format("read file: %s failed, because err: %s", kvmAgentFile.getAbsolutePath(), e.getMessage()));
        }

        return vos;
    }

    private List<GuestToolsVO> scanGuestTools() {
        List<GuestToolsVO> vos = new ArrayList<>();

        File toolsFolder = PathUtil.findFolderOnClassPath("tools/guesttools/", true);
        if (!toolsFolder.isDirectory()) {
            logger.warn(String.format("cannot find %s, its' either not existing or not a directory", toolsFolder));
            return vos;
        }

        File[] archFolders = toolsFolder.listFiles();
        if (archFolders == null) {
            logger.warn(String.format("cannot find architecture folder under %s", toolsFolder));
            return vos;
        }
        for (File archFolder : archFolders) {
            if (archFolder.isFile()) {
                continue;
            }

            String arch = archFolder.getName();
            File[] hypeFolders = archFolder.listFiles();
            if (hypeFolders == null) {
                logger.warn(String.format("cannot find hypervisor folder under %s", archFolder));
                continue;
            }

            for (File hypeFolder : hypeFolders) {
                if (hypeFolder.isFile()) {
                    continue;
                }
                String hype = hypeFolder.getName();
                File[] isos = hypeFolder.listFiles();
                if (isos == null) {
                    logger.warn(String.format("cannot find guest tools iso file under %s", hypeFolder));
                    continue;
                }
                for (File iso : isos) {
                    if (iso.isDirectory()) {
                        continue;
                    }
                    String agentType;
                    if (Pattern.matches("GuestTools_linux-.+\\.iso", iso.getName())) {
                        agentType = GuestToolsAgentType.LinuxOnKvm.toString();
                    } else if (Pattern.matches("GuestTools-.+\\.iso", iso.getName())) {
                        agentType = GuestToolsAgentType.WindowsOnKvm.toString();
                    }else {
                        continue;
                    }

                    String[] parts = iso.getName().split("-");
                    String version = parts[parts.length - 1].replace(".iso", "").trim();
                    GuestToolsVO vo = new GuestToolsVO();
                    vo.setUuid(Platform.getUuid());
                    vo.setManagementNodeUuid(Platform.getManagementServerId());
                    vo.setArchitecture(arch);
                    vo.setHypervisorType(hype);
                    vo.setVersion(version);
                    vo.setAgentType(agentType);
                    vos.add(vo);
                }
            }
        }
        return vos;
    }

    // non-guest-tools-subcases need to fail this, otherwise vm platform and qga tag will be changed
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void getVmGuestToolsInfo(VmInstanceInventory inv, Completion completion) {
        // only non-community version of zstack has the ability of guest tools
        GuestToolsAgentType agentType = GuestToolsAgentType.fromImagePlatformAndHypervisorType(
                ImagePlatform.valueOf(inv.getPlatform()), inv.getHypervisorType());
        if (agentType == null) {
            completion.success();
            return;
        }

        GuestToolsHypervisorBackend bkd = getGuestToolsHypervisorBackend(agentType);

        FlowChain chain = new SimpleFlowChain();
        chain.setName(String.format("get-vm-%s-guest-tools-info", inv.getUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "get-vm-guest-tools-info-from-hypervisor";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                bkd.getVmGuestToolsInfo(inv, false, new ReturnValueCompletion<GuestToolsKvmCommands.GetVmGuestToolsInfoRsp>(completion) {
                    @Override
                    public void success(GuestToolsKvmCommands.GetVmGuestToolsInfoRsp rsp) {
                        data.put(FLOW_CHAIN_KEY_GUEST_TOOLS_VERSION, rsp.getVersion());
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.warn(String.format("failed to get guest tools info from vm[uuid:%s] before it's stopped", inv.getUuid()));

                        // continue stopping the vm even failed to get vm guest tools info
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "check-guest-tools-info";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                // version may be null
                String version = (String) data.get(FLOW_CHAIN_KEY_GUEST_TOOLS_VERSION);
                
                if (version != null) {
                    updateGuestToolsTag(inv.getUuid(), version);
                    logger.debug(String.format("re-create GuestTools tag for vm[uuid:%s] after receiving guest tools info",
                            inv.getUuid()));
                }

                // Even if we don't get guest tools info, there are still some tasks to be processed,
                // so we can't skip here
                bkd.checkGuestToolsInfoBeforeVmStop(inv, version);
                trigger.next();
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    @Override
    public void beforeVmInstanceStop(VmInstanceInventory inv, Completion completion) {
        /* change guest tool state */
        GuestToolsStateVO stateVO = dbf.findByUuid(inv.getUuid(), GuestToolsStateVO.class);
        if (stateVO != null) {
            GuestToolsQgaStateEvent qgaEvent = GuestToolsQgaStateEvent.qgaNotRunning;
            GuestToolsZWatchStateEvent zEvent = GuestToolsZWatchStateEvent.stopped;
            String osType = "";
            String platform = "";
            String qgaVersion = "";
            String zwatchVerion = "";
            qgaStateChange(inv.getUuid(), zEvent, qgaEvent, osType, platform, zwatchVerion, qgaVersion, Boolean.FALSE);
        }

        getVmGuestToolsInfo(inv, completion);
    }

    @Override
    public void preVmMigration(VmInstanceInventory vm, VmMigrationType vmMigrationType, String dstHostUuid, Completion completion) {
        GuestToolsAgentType agentType = GuestToolsAgentType.fromImagePlatformAndHypervisorType(
                ImagePlatform.valueOf(vm.getPlatform()), vm.getHypervisorType());
        if (agentType == null) {
            completion.success();
            return;
        }

        if (VmMigrationType.PrimaryStorageMigration == vmMigrationType &&
                Objects.equals(VmInstanceState.Stopped.toString(), vm.getState())) {
            completion.success();
            return;
        }

        GuestToolsHypervisorBackend bkd = getGuestToolsHypervisorBackend(agentType);
        bkd.beforeVmMigrate(vm, completion);
    }

    private void updateGuestToolsTag(String vmUuid, String version) {
        SystemTagCreator creator = VmSystemTags.VM_GUEST_TOOLS.newSystemTagCreator(vmUuid);
        creator.setTagByTokens(Collections.singletonMap(VmSystemTags.VM_GUEST_TOOLS_VERSION_TOKEN, version));
        creator.inherent = false;
        creator.recreate = true;
        creator.create();
    }

    private GuestToolsQgaStateEvent getGuestToolsEventByState(String vmUuid, MevocoKVMAgentCommands.GuestToolsState state) {
        if (!state.getQgaRunning()) {
            return GuestToolsQgaStateEvent.qgaNotRunning;
        }

        if (!state.getZsToolsFound()) {
            return GuestToolsQgaStateEvent.zsToolNotFound;
        }

        return GuestToolsQgaStateEvent.zsToolFound;
    }

    @Override
    public String preStartNewCreatedVm(VmInstanceInventory inv) {
        return null;
    }

    @Override
    public void beforeStartNewCreatedVm(VmInstanceInventory inv) {
    }

    @Override
    public void afterStartNewCreatedVm(VmInstanceInventory inv) {
        if (!inv.getType().equals(USER_VM_TYPE)) {
            return;
        }

        String toolsVersion = VmSystemTags.VM_GUEST_TOOLS.getTokenByResourceUuid(inv.getUuid(), VmSystemTags.VM_GUEST_TOOLS_VERSION_TOKEN);
        if (toolsVersion == null) {
            toolsVersion = ImageSystemTags.IMAGE_GUEST_TOOLS.getTokenByResourceUuid(inv.getImageUuid(), ImageSystemTags.IMAGE_GUEST_TOOLS_VERSION_TOKEN);
        }

        GuestToolsStateVO vo = initGuestToolsStateByGuestToolsVersion(inv.getUuid(), toolsVersion);
        dbf.persist(vo);

        logger.debug(String.format("create GuestToolsStateVO after vm create: %s",
                JSONObjectUtil.toJsonString(vo)));
    }

    @Override
    public void failedToStartNewCreatedVm(VmInstanceInventory inv, ErrorCode reason) {
    }


    @Override
    public void preBeforeInstantiateVmResource(VmInstanceSpec spec) {

    }

    @Override
    public void preInstantiateVmResource(VmInstanceSpec spec, Completion completion) {
        VmCustomSpecificationStruct struct = spec.getVmCustomSpecification();
        if (struct == null) {
            completion.success();
            return;
        }

        VmCustomSpecificationVO vo = new VmCustomSpecificationVO(struct);
        vo.setUuid(Platform.getUuid());
        vo.setVmInstanceUuid(spec.getVmInventory().getUuid());
        vo.setName(GuestToolsConstant.VM_CUSTOM_SPECIFICATION_NAME_PREFIX + spec.getVmInventory().getName());
        vo.setAccountUuid(acntMgr.getOwnerAccountUuidOfResource(spec.getVmInventory().getUuid()));
        dbf.persistAndRefresh(vo);
        struct.setUuid(vo.getUuid());
        String hostname = vo.getHostname() != null ? vo.getHostname() : spec.getVmInventory().getName();
        SystemTagCreator creator = VmSystemTags.HOSTNAME.newSystemTagCreator(spec.getVmInventory().getUuid());
        creator.setTagByTokens(map(
                e(VmSystemTags.HOSTNAME_TOKEN, hostname)
        ));
        creator.create();

        logger.debug(String.format("create VmCustomSpecificationVO for vm[uuid:%s]",
                spec.getVmInventory().getUuid()));
        completion.success();
    }

    @Override
    public void preReleaseVmResource(VmInstanceSpec spec, Completion completion) {
        VmCustomSpecificationStruct struct = spec.getVmCustomSpecification();
        if (struct == null) {
            completion.success();
            return;
        }

        SQL.New(VmCustomSpecificationVO.class)
                .eq(VmCustomSpecificationVO_.uuid, struct.getUuid())
                .delete();
        completion.success();
    }

    @Override
    public void vmJustBeforeDeleteFromDb(VmInstanceInventory inv) {
        SQL.New(VmCustomSpecificationVO.class)
                .eq(VmCustomSpecificationVO_.vmInstanceUuid, inv.getUuid())
                .delete();
    }

    private String getVmDefaultNicUuid(Collection<VmNicVO> vmNicList, String defaultL3Uuid) {
        if (CollectionUtils.isEmpty(vmNicList)) {
            return null;
        }

        return vmNicList.stream().filter(it -> Objects.equals(it.getL3NetworkUuid(), defaultL3Uuid))
                .sorted(Comparator.comparingInt(VmNicVO::getDeviceId))
                .map(VmNicVO::getUuid).findFirst().orElse(null);
    }

    private String getVmDefaultIp(VmInstanceVO vm) {
        String defaultNicUuid = getVmDefaultNicUuid(vm.getVmNics(), vm.getDefaultL3NetworkUuid());

        for (VmNicVO nic : vm.getVmNics()) {
            if (Objects.equals(nic.getUuid(), defaultNicUuid)) {
                List<String> addresses = VmNicHelper.getIpAddresses(nic);
                if (!addresses.isEmpty()) {
                    return addresses.get(0);
                }
            }
        }
        return "";
    }

    @Override
    public Flow getSetVmHostnameFlow() {
        return new Flow() {
            String __name__ = "set-hostname-by-qga";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                String vmInstanceUuid = (String) data.get(VmInstanceConstant.Params.VmInstanceUuid.toString());
                VmInstanceVO vm = dbf.findByUuid(vmInstanceUuid, VmInstanceVO.class);

                String defaultIP= getVmDefaultIp(vm);

                if (!Q.New(GuestToolsStateVO.class).eq(GuestToolsStateVO_.vmInstanceUuid, vmInstanceUuid)
                        .eq(GuestToolsStateVO_.qgaState, GuestToolsQgaState.Running).isExists() ||
                        !Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmInstanceUuid)
                        .eq(VmInstanceVO_.state, VmInstanceState.Running).isExists()) {
                    L3NetworkVO defaultL3 = dbf.findByUuid(vm.getDefaultL3NetworkUuid(), L3NetworkVO.class);
                    boolean enableDhcp = defaultL3.getNetworkServices().stream().map(NetworkServiceL3NetworkRefVO::getNetworkServiceType)
                                            .collect(Collectors.toList()).contains(NetworkServiceType.DHCP.toString());
                    if (enableDhcp) {
                        logger.debug(String.format("vm[uuid: %s, name: %s] qga is not running, but dhcp service is enable, will set hostname by dhcp", vm.getUuid(), vm.getName()));
                        vmConfigSyncHelper.setVmSyncHostname(vm.getUuid());
                        trigger.next();
                        return;
                    }

                    trigger.fail(operr("failed to set vm[uuid: %s, name: %s] hostname, because qga state is not running and there is no dhcp service", vm.getUuid(), vm.getName())
                            .withOpaque("vm.uuid", vm.getUuid())
                            .withOpaque("vm.name", vm.getName()));
                    return;
                }

                SetVmHostnameOnHypervisorMsg msg = new SetVmHostnameOnHypervisorMsg();
                msg.setHostUuid(vm.getHostUuid());
                msg.setVmInstanceUuid(vmInstanceUuid);
                msg.setHostname(VmSystemTags.HOSTNAME.getTokenByResourceUuid(vm.getUuid(), VmSystemTags.HOSTNAME_TOKEN));
                msg.setDefaultIP(defaultIP);

                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, vm.getHostUuid());
                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply rly) {
                        if (!rly.isSuccess()) {
                            logger.warn(String.format("vm [uuid:%s, name: %s] set hostname failed, error :%s",
                                    vm.getUuid(), vm.getName(), rly.getError().getDetails()));
                            trigger.fail(rly.getError());
                        } else {
                            trigger.next();
                        }

                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                trigger.rollback();
            }
        };
    }

    @Override
    public void afterVmVfNicHaStateChange(VmVfNicInventory inventory, String haState, Completion completion) {
        if (inventory == null || inventory.getVmInstanceUuid() == null) {
            completion.success();
            return;
        }

        UpdateVmNetworkConfigMsg umsg = new UpdateVmNetworkConfigMsg();
        umsg.setVmInstanceUuid(inventory.getVmInstanceUuid());
        umsg.setVmNicUuids(asList(inventory.getUuid()));
        umsg.setHaState(haState == null ? VmVfNicHaState.Disabled.toString() : haState);

        bus.makeTargetServiceIdByResourceUuid(umsg, GuestToolsConstant.SERVICE_ID, inventory.getVmInstanceUuid());
        bus.send(umsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to update vm nic[uuid:%s] network config, %s", inventory.getUuid(), reply.getError()));
                    completion.fail(reply.getError());
                } else {
                    completion.success();
                }
            }
        });
    }
}
