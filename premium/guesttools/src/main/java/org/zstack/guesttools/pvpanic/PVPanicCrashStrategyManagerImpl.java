package org.zstack.guesttools.pvpanic;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.CrashStrategy;
import org.zstack.compute.vm.ExecuteCrashStrategyMsg;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.guesttools.GuestToolsFeatureHelper;
import org.zstack.guesttools.kvm.AfterQueryGuestToolsInfoExtensionPoint;
import org.zstack.guesttools.kvm.GuestToolsKvmCommands;
import org.zstack.guesttools.kvm.QueryGuestToolsInfoContext;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmCanonicalEvents;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceType;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.kvm.KVMStartVmAddonExtensionPoint;
import org.zstack.network.service.flat.BeforeUpdateUserdataExtensionPoint;
import org.zstack.network.service.flat.FlatUserdataBackend;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.VersionComparator;
import org.zstack.utils.logging.CLogger;
import org.zstack.guesttools.kvm.GuestToolsKvmCommands.GetVmGuestToolsInfoRsp;

import java.util.Map;
import java.util.Optional;

import static org.zstack.core.Platform.*;
import static org.zstack.guesttools.pvpanic.PVPanicConstant.*;
import static org.zstack.zwatch.namespace.VmNamespace.*;

/**
 * Created by Wenhao.Zhang on 21/06/21
 */
public class PVPanicCrashStrategyManagerImpl implements Component, PVPanicCrashStrategyManager,
        KVMStartVmAddonExtensionPoint, BeforeUpdateUserdataExtensionPoint, AfterQueryGuestToolsInfoExtensionPoint {

    private static final CLogger logger = Utils.getLogger(PVPanicCrashStrategyManagerImpl.class);
    @Autowired
    protected CloudBus bus;
    @Autowired
    private ResourceConfigFacade rcf;
    @Autowired
    private EventFacade evtf;

    @Override
    public VmInstanceType getVmTypeForAddonExtension() {
        return VmInstanceType.valueOf(VmInstanceConstant.USER_VM_TYPE);
    }

    @Override
    public void addAddon(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        if (!isCrashStrategyEnable(spec.getVmInventory().getUuid())) {
            return;
        }

        /*
         * Windows Guest:            <panic model='hyperv'> is on;   <panic model='isa'> is on;
         * Linux / Other Guest:      <panic model='hyperv'> is off;  <panic model='isa'> is on;
         * No operating system:      <panic model='hyperv'> is off;  <panic model='isa'> is on;
         */
        cmd.getAddons().putIfAbsent(PANIC_ISA, Boolean.TRUE);

        final ImageInventory imageInventory = spec.getImageSpec().getInventory();
        final String platform = imageInventory == null ? null : imageInventory.getPlatform();
        if (ImagePlatform.Windows.toString().equals(platform) || ImagePlatform.WindowsVirtio.toString().equals(platform)) {
            cmd.getAddons().putIfAbsent(PANIC_HYPERV, Boolean.TRUE);
        } else {
            cmd.getAddons().putIfAbsent(PANIC_HYPERV, Boolean.FALSE);
        }
        cmd.getAddons().putIfAbsent(VM_BOOT_PARAM_ON_CRASH, CrashStrategy.Preserve.name().toLowerCase());
    }

    @Override
    public void beforeApplyUserdata(String vmUuid, FlatUserdataBackend.UserdataTO to) {
        if (isCrashStrategyEnable(vmUuid)) {
            to.agentConfig.put(PVPANIC, RESULT_ENABLE);
        }
    }

    @Override
    public boolean isCrashStrategyEnable(String vmUuid) {
        return CrashStrategy.valueOf(rcf.getResourceConfigValue(VmGlobalConfig.VM_CRASH_STRATEGY, vmUuid, String.class))
                .isCrashStrategyEnable();
    }

    @Override
    public boolean start() {
        installVmCrashEventListener();
        return true;
    }

    @Override
    public boolean stop() {
        return false;
    }

    @SuppressWarnings("rawtypes")
    private void installVmCrashEventListener() {
        evtf.on(VmCanonicalEvents.VM_LIBVIRT_REPORT_CRASH, new EventCallback<VmCanonicalEvents.VmCrashReportData>() {
            @Override
            protected void run(Map tokens, VmCanonicalEvents.VmCrashReportData data) {
                new VmCrashRecorder().record(data.getTime(), data.getVmUuid());
                ExecuteCrashStrategyMsg rmsg = new ExecuteCrashStrategyMsg();
                rmsg.setVmInstanceUuid(data.getVmUuid());
                if (new VmCrashRecorder().hasReachedThreshold(data.getTime(), data.getVmUuid())) {
                    rmsg.setSkipReboot(false);
                }
                bus.makeTargetServiceIdByResourceUuid(rmsg, VmInstanceConstant.SERVICE_ID, rmsg.getVmInstanceUuid());
                bus.send(rmsg, new CloudBusCallBack(rmsg) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.warn(String.format("Unable to operate vm[%s] according to the strategy, %s,", data.getVmUuid(), reply.getError()));
                            return;
                        }
                        logger.warn("Successfully operate vm according to the strategy");
                    }
                });
            }
        });
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void afterQueryGuestToolsInfo(QueryGuestToolsInfoContext context, Completion completion) {
        final GetVmGuestToolsInfoRsp rsp = context.getApiResponse();

        FlowChain flowChain = FlowChainBuilder.newSimpleFlowChain();
        flowChain.setName(String.format("put-pvpanic-param-after-query-guest-tools-for-vm-%s", context.getVmUuid()));
        flowChain.then(new NoRollbackFlow() {
            String __name__ = "get-pvpanic-guest-state-from-prometheus";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                context.queryFromPrometheus(ZWatchAgentFeaturePvpanic.getName(), null,
                        new ReturnValueCompletion<Double>(trigger) {
                    @Override
                    public void success(Double value) {
                        putPVPanicGuestState(value, context);
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.warn(String.format("failed to query PVPanic guest state of agent tools [vm:%s] from Prometheus, because %s",
                                context.getVmUuid(), errorCode.getDetails()));
                        putPVPanicGuestState(null, context);
                        trigger.next(); // but continue anyway
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "get-pvpanic-host-state-from-prometheus";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                context.queryFromPrometheus(PVPanicEnableInDomainXML.getName(), null, new ReturnValueCompletion<Double>(trigger) {
                    @Override
                    public void success(Double value) {
                        putPVPanicHostState(value, context);
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.warn(String.format("failed to query PVPanic host state of agent tools [vm:%s] from Prometheus, because %s",
                                context.getVmUuid(), errorCode.getDetails()));
                        putPVPanicHostState(null, context);
                        trigger.next(); // but continue anyway
                    }
                });
            }
        });

        flowChain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                boolean hostEnable = HOST_ENABLE.equals(rsp.getFeatures().get(PVPANIC_HOST));
                boolean guestToolEnable = GUEST_TOOL_ENABLE.equals(rsp.getFeatures().get(PVPANIC_GUEST_TOOL));
                boolean kernelSupported = ImagePlatform.Windows.equals(context.getPlatform()) ||
                        KERNEL_SUPPORTED.equals(rsp.getFeatures().get(PVPANIC_KERNEL));

                rsp.getFeatures().put(PVPANIC,
                        (hostEnable && guestToolEnable && kernelSupported) ? RESULT_ENABLE : RESULT_DISABLE);
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                // can not be here
                completion.fail(operr("can not be here", errCode));
            }
        }).start();
    }

    /**
     * Return value: (convert to integer)
     * -  1st bit (0x01)  1 => guest tools PVPanic module is enable;   0 => guest tools PVPanic module is disable.
     * -  2nd bit (0x02)  1 => guest kernel support PVPanic;           0 => guest kernel do not support PVPanic.
     * @param valueFromPrometheus may be null
     */
    private static void putPVPanicGuestState(Double valueFromPrometheus, QueryGuestToolsInfoContext context) {
        GetVmGuestToolsInfoRsp info = context.getApiResponse();
        if (valueFromPrometheus == null) {
            // maybe version of guest tools is too low, and do not support PVPanic
            // or platform of guest is Windows
            if (ImagePlatform.Windows.equals(context.getPlatform())) {
                info.getFeatures().put(PVPANIC_GUEST_TOOL,
                        isWindowsVMPVPanicGuestToolsEnable(context) ? GUEST_TOOL_ENABLE : GUEST_TOOL_NOT_SUPPORT);
            } else {
                info.getFeatures().put(PVPANIC_GUEST_TOOL, GUEST_TOOL_NOT_SUPPORT);
            }
            info.getFeatures().put(PVPANIC_KERNEL, KERNEL_UNKNOWN);
        } else {
            int number = valueFromPrometheus.intValue(); // The last one is the newest
            info.getFeatures().put(PVPANIC_GUEST_TOOL, (number & 1) == 0 ? GUEST_TOOL_DISABLE : GUEST_TOOL_ENABLE);
            info.getFeatures().put(PVPANIC_KERNEL, (number & 2) == 0 ? KERNEL_NOT_SUPPORT : KERNEL_SUPPORTED);
        }
    }

    /**
     * @param valueFromPrometheus may be null
     */
    private static void putPVPanicHostState(Double valueFromPrometheus, QueryGuestToolsInfoContext context) {
        GuestToolsKvmCommands.GetVmGuestToolsInfoRsp info = context.getApiResponse();
        if (valueFromPrometheus == null) {
            info.getFeatures().put(PVPANIC_HOST, KERNEL_UNKNOWN);
        } else {
            info.getFeatures().put(PVPANIC_HOST, (valueFromPrometheus.intValue() & 1) == 0 ? HOST_DISABLE : HOST_ENABLE);
        }
    }

    private static boolean isWindowsVMPVPanicGuestToolsEnable(QueryGuestToolsInfoContext context) {
        if (context.getVersionString() == null) {
            return false;
        }

        Optional<GuestToolsFeatureHelper.FeatureStruct> optional = GuestToolsFeatureHelper.getFeatureVersionMap()
                .stream()
                .filter(struct -> String.format("%s-%s", PVPANIC, ImagePlatform.Windows).equals(struct.name))
                .findAny();

        if (!optional.isPresent()) {
            return false;
        }

        VersionComparator guestToolsVersion = new VersionComparator(context.getVersionString());
        VersionComparator pvpanicBaseVersion = new VersionComparator(optional.get().version);
        return guestToolsVersion.compare(pvpanicBaseVersion) >= 0; // assert guestToolsVersion >= pvpanicBaseVersion
    }
}
