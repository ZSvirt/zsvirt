package org.zstack.guesttools.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.host.HostSystemTags;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.guesttools.GuestToolsAgentStatus;
import org.zstack.guesttools.GuestToolsAgentType;
import org.zstack.guesttools.GuestToolsConstant;
import org.zstack.guesttools.kvm.GuestToolsKvmCommands.GetVmGuestToolsInfoRsp;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostVO;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmPlatformChangedExtensionPoint;
import org.zstack.header.vm.cdrom.VmCdRomVO;
import org.zstack.header.vm.cdrom.VmCdRomVO_;
import org.zstack.mevoco.MevocoSystemTags;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;
import org.zstack.zwatch.namespace.VmNamespace;

import java.util.Map;

import static org.zstack.guesttools.GuestToolsAgentStatus.NOT_RUNNING;
import static org.zstack.guesttools.GuestToolsAgentStatus.RUNNING;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by GuoYi on 2019-09-18.
 */
public class GuestToolsForWindowsOnKvmBackend extends GuestToolsOnKvmBackend {
    private static final CLogger logger = Utils.getLogger(GuestToolsForWindowsOnKvmBackend.class);

    @Autowired
    protected PluginRegistry pluginRgty;

    @Override
    public GuestToolsAgentType getGuestToolsAgentType() {
        return GuestToolsAgentType.WindowsOnKvm;
    }

    @Override
    public String getSrcGuestToolsIso(String archType, String HypervisorType, String version) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return "GuestTools-1.0.0.iso";
        }

        return PathUtil.findFileOnClassPath(String.format("tools/guesttools/%s/%s/GuestTools-%s.iso",
                archType, HypervisorType, version),true).getAbsolutePath();
    }

    @Override
    public String getDstGuestToolsIso() {
        return GuestToolsConstant.GUEST_TOOLS_ISO_DST_PATH;
    }

    @Override
    public void updateHostGuestToolsTag(String hostUuid, String version) {
        SystemTagCreator creator = HostSystemTags.HOST_GUEST_TOOLS.newSystemTagCreator(hostUuid);
        creator.inherent = false;
        creator.recreate = true;
        creator.setTagByTokens(map(e(HostSystemTags.HOST_GUEST_TOOLS_VERSION_TOKEN, version)));
        creator.create();
        logger.info(String.format("successfully updated windows guest tools version tag of host[uuid:%s] to %s", hostUuid, version));
    }

    @Override
    public String getHostGuestToolsTag(String hostUuid) {
        return HostSystemTags.HOST_GUEST_TOOLS.getTokenByResourceUuid(
                hostUuid, HostVO.class, HostSystemTags.HOST_GUEST_TOOLS_VERSION_TOKEN);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void getVmGuestToolsInfo(VmInstanceInventory vm, boolean readMetricOnly, ReturnValueCompletion<GetVmGuestToolsInfoRsp> completion) {
        QueryGuestToolsInfoContext context = createContext(vm.getUuid());

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.disableDebugLog();
        chain.setName(String.format("get-windows-vm-%s-guest-info", vm.getUuid()));

        /*
         * Windows guest tools do not support automatic upgrade, so compatibility must be considered
         * 
         * before Cloud 4.6.0, zwatch-vm-guest (windows) version < 1.3.2
         *   get guest tools state from host by sending command
         * 
         * Cloud 4.6.0+ ~ , zwatch-vm-guest (windows) version 1.3.2 ~
         *   From ZSTAC-40993
         *   ZWatchAgentVersion data found         => Guest Tools installed
         *   AgentLastResponseTimestampDelta > 0   => Guest Tools is Running
         *   AgentLastResponseTimestampDelta = 0   => Guest Tools is Not Running
         */
        chain.then(new CheckGuestToolsStatusFlowBuilder() // for Cloud 4.6.0+ ~ , zwatch-vm-guest (windows) version 1.3.2 ~
                .name("check-guest-tools-last-response-timestamp")
                .context(context)
                .prometheusMetric(VmNamespace.AgentLastResponseTimestampDelta.getName())
                .skipIfMetricIsEmpty()
                .skipWhenQueryFail()
                .checkStatusByLastMetricValue(value -> value > 0 ? RUNNING : NOT_RUNNING)
                .create()
        ).then(new CheckGuestToolsStatusFlowBuilder()
                .name("check-guest-tools-version")
                .context(context)
                .prometheusMetric(VmNamespace.ZWatchAgentVersion.getName())
                .skipIfMetricIsEmpty()
                .skipWhenQueryFail()
                .version(value -> new GuestToolsVersion(value.intValue()))
                .create()
        ).then(new NoRollbackFlow() {
            String __name__ = "get-guest-tools-info-from-host";

            @Override
            public boolean skip(Map data) {
                // If both version and status are null, don't skip.
                if (context.getVersion() == null && context.getStatus() == null) {
                    return false;
                } else if  (context.getVersion() != null && context.getStatus() != null) {
                    return true;
                }

                return readMetricOnly;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                getVmGuestToolsInfoFromHost(vm.getUuid(), vm.getHostUuid(), new ReturnValueCompletion<GetVmGuestToolsInfoRsp>(trigger) {
                    @Override
                    public void success(GetVmGuestToolsInfoRsp hostRsp) {
                        context.setVersion(hostRsp.getVersion());
                        context.setStatus(GuestToolsAgentStatus.of(hostRsp.getStatus()));
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "after-query-guest-tools-info";

            @Override
            public boolean skip(Map data) {
                return readMetricOnly;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                emitAfterQueryGuestToolsInfoExtension(context, new NoErrorCompletion(trigger) {
                    @Override
                    public void done() {
                        trigger.next();
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                if (context.getStatus() != null) {
                    context.getApiResponse().setStatus(context.getStatus().toString());
                }
                context.getApiResponse().setVersion(context.getVersionString());
                completion.success(context.getApiResponse());
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    @Override
    public void checkGuestToolsInfoBeforeVmStop(VmInstanceInventory inv, String version) {
        String vmUuid = inv.getUuid();

        boolean guestToolsJustAttached = Q.New(VmCdRomVO.class)
                .eq(VmCdRomVO_.vmInstanceUuid, vmUuid)
                .eq(VmCdRomVO_.occupant, VmInstanceConstant.VM_CDROM_OCCUPANT_GUEST_TOOLS)
                .isExists();

        if (guestToolsJustAttached) {
            MevocoSystemTags.GUEST_TOOLS_HAS_ATTACHED.delete(vmUuid, VmInstanceVO.class);

            SQL.New(VmCdRomVO.class)
                    .eq(VmCdRomVO_.vmInstanceUuid, vmUuid)
                    .eq(VmCdRomVO_.occupant, VmInstanceConstant.VM_CDROM_OCCUPANT_GUEST_TOOLS)
                    .set(VmCdRomVO_.occupant, null)
                    .update();
        }

        if (version == null) {
            return;
        }
        if (guestToolsJustAttached) {
            createVirtIOSystemTag(inv);
        }

        // re-create vm inject qga tag (only for Windows)
        SystemTagCreator creator = VmSystemTags.VM_INJECT_QEMUGA.newSystemTagCreator(vmUuid);
        creator.inherent = false;
        creator.recreate = true;
        creator.create();
        logger.info(String.format("re-create QGA tag for vm[uuid:%s] after receiving guest tools info", vmUuid));
    }

    private void createVirtIOSystemTag(VmInstanceInventory vm) {
        // if vm platform is 'Windows', change to 'Windows' with VirtIO
        // WindowsVirtio is split into Windows and virtio, not change platform, only change virtio
        for (VmPlatformChangedExtensionPoint ext : pluginRgty.getExtensionList(VmPlatformChangedExtensionPoint.class)) {
            if (ext.skipPlatformChange(vm, vm.getPlatform(), ImagePlatform.WindowsVirtio.toString())) {
                return;
            }
        }

        SystemTagCreator creator = VmSystemTags.VIRTIO.newSystemTagCreator(vm.getUuid());
        creator.inherent = false;
        creator.recreate = true;
        creator.create();
        logger.info(String.format("change vm[uuid:%s] rootVolume driver after receiving guest tools info", vm.getUuid()));

        for (VmPlatformChangedExtensionPoint ext : pluginRgty.getExtensionList(VmPlatformChangedExtensionPoint.class)) {
            ext.vmPlatformChange(vm, vm.getPlatform(), ImagePlatform.WindowsVirtio.toString());
        }
    }
}
