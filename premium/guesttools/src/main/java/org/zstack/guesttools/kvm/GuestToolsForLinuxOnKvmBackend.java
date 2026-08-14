package org.zstack.guesttools.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.host.HostSystemTags;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.guesttools.GuestToolsAgentStatus;
import org.zstack.guesttools.GuestToolsAgentType;
import org.zstack.guesttools.GuestToolsConstant;
import org.zstack.guesttools.kvm.GuestToolsKvmCommands.GetVmGuestToolsInfoRsp;
import org.zstack.header.Component;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostVO;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.cdrom.VmCdRomVO;
import org.zstack.header.vm.cdrom.VmCdRomVO_;
import org.zstack.mevoco.MevocoSystemTags;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;
import org.zstack.zwatch.namespace.VmNamespace;

import java.util.*;

import static org.zstack.guesttools.GuestToolsAgentStatus.*;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by shixin on 2019-12-14.
 */
public class GuestToolsForLinuxOnKvmBackend extends GuestToolsOnKvmBackend implements Component {
    private static final CLogger logger = Utils.getLogger(GuestToolsForLinuxOnKvmBackend.class);

    @Autowired
    protected RESTFacade restf;
    @Autowired
    protected DatabaseFacade dbf;
    @Override
    public GuestToolsAgentType getGuestToolsAgentType() {
        return GuestToolsAgentType.LinuxOnKvm;
    }

    @Override
    public String getSrcGuestToolsIso(String archType, String HypervisorType, String version) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return "GuestTools_linux-1.0.0.iso";
        }

        return PathUtil.findFileOnClassPath(String.format("tools/guesttools/general/%s/GuestTools_linux-%s.iso",
                HypervisorType, version),true).getAbsolutePath();
    }

    @Override
    public String getDstGuestToolsIso() {
        return GuestToolsConstant.GUEST_TOOLS_LINUX_ISO_DST_PATH;
    }

    @Override
    public void updateHostGuestToolsTag(String hostUuid, String version) {
        SystemTagCreator creator = HostSystemTags.HOST_LINUX_GUEST_TOOLS.newSystemTagCreator(hostUuid);
        creator.inherent = false;
        creator.recreate = true;
        creator.setTagByTokens(map(e(HostSystemTags.HOST_LINUX_GUEST_TOOLS_VERSION_TOKEN, version)));
        creator.create();
        logger.info(String.format("successfully updated linux guest tools version tag of host[uuid:%s] to %s", hostUuid, version));
    }

    @Override
    public String getHostGuestToolsTag(String hostUuid) {
        return HostSystemTags.HOST_LINUX_GUEST_TOOLS.getTokenByResourceUuid(
                hostUuid, HostVO.class, HostSystemTags.HOST_LINUX_GUEST_TOOLS_VERSION_TOKEN);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void getVmGuestToolsInfo(VmInstanceInventory vm, boolean readMetricOnly, ReturnValueCompletion<GetVmGuestToolsInfoRsp> completion) {
        String versionTag = VmSystemTags.VM_GUEST_TOOLS.getTokenByResourceUuid(vm.getUuid(), VmSystemTags.VM_GUEST_TOOLS_VERSION_TOKEN);
        QueryGuestToolsInfoContext context = createContext(vm.getUuid());
        final GetVmGuestToolsInfoRsp rsp = context.getApiResponse();
        if (versionTag != null && versionTag.equals(GuestToolsKvmCommands.ZWATCH_INSTALL_FAILED)) {
            rsp.setVersion(versionTag);
            rsp.setStatus(GuestToolsAgentStatus.NOT_RUNNING.toString());
            completion.success(rsp);
            return;
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.disableDebugLog();
        chain.setName(String.format("get-linux-vm-%s-guest-info", vm.getUuid()));

        /*
         * ZSphere 4.0.0 ~
         *   From ZSTAC-49370
         *   ZWatchAgentVersion data found         => Guest Tools installed
         *   AgentLastResponseTimestampDelta > 0   => Guest Tools is Running
         */
        chain.then(new CheckGuestToolsStatusFlowBuilder()
                .name("check-guest-tools-last-response-timestamp")
                .context(context)
                .prometheusMetric(VmNamespace.AgentLastResponseTimestampDelta.getName())
                .skipIfMetricIsEmpty()
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
                return readMetricOnly || (context.getVersion() != null && context.getStatus() != null);
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
            public void handle(Map data1) {
                if (context.getStatus() != null) {
                    rsp.setStatus(context.getStatus().toString());
                }
                rsp.setVersion(context.getVersionString());
                completion.success(rsp);
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data1) {
                completion.fail(errCode);
            }
        }).start();
    }

    @Override
    public void checkGuestToolsInfoBeforeVmStop(VmInstanceInventory inv, String version) {
        String vmUuid = inv.getUuid();

        SQL.New(VmCdRomVO.class)
                .eq(VmCdRomVO_.vmInstanceUuid, vmUuid)
                .eq(VmCdRomVO_.occupant, VmInstanceConstant.VM_CDROM_OCCUPANT_GUEST_TOOLS)
                .set(VmCdRomVO_.occupant, null)
                .update();

        MevocoSystemTags.GUEST_TOOLS_HAS_ATTACHED.delete(vmUuid, VmInstanceVO.class);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
