package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.Component;
import org.zstack.header.core.BypassWhenUnitTest;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.image.ImageBootMode;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.vm.*;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.kvm.KVMHostVO;
import org.zstack.kvm.KVMStartVmAddonExtensionPoint;
import org.zstack.mevoco.MevocoGlobalConfig;
import org.zstack.mevoco.VirtIODriverFormat;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

import static org.zstack.compute.vm.PremiumVmInstanceConstant.*;
import static org.zstack.core.Platform.*;
import static org.zstack.mevoco.VirtIODriverFormat.*;

/**
 * Created by Wenhao.Zhang on 21/08/19
 */
public class KvmUserVmVirtIODriverExtension implements Component,
        KVMStartVmAddonExtensionPoint,
        PreVmInstantiateResourceExtensionPoint {
    private static final CLogger logger = Utils.getLogger(KvmUserVmVirtIODriverExtension.class);
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected AnsibleFacade asf;

    @Override
    public VmInstanceType getVmTypeForAddonExtension() {
        return VmInstanceType.valueOf(VmInstanceConstant.USER_VM_TYPE);
    }

    @Override
    public void addAddon(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        VirtIODriverFormat driverFormat = queryFormatOfDefaultVirtioDriver(spec);
        logger.debug(String.format("auto attach VirtIO driver format: %s", driverFormat));
        if (!NONE.equals(driverFormat)) {
            cmd.getAddons().put(SYSTEM_VIRTIO_DRIVER_DEVICE_TYPE, driverFormat);
        }
    }

    /**
     * Return format of VirtIO driver the VM should attached.
     * If no need to attach VirtIO driver, return none.
     */
    private VirtIODriverFormat queryFormatOfDefaultVirtioDriver(VmInstanceSpec spec) {
        String attachFormatText = MevocoGlobalConfig.AUTO_ATTACH_VIRTIO_DRIVER.value();
        VirtIODriverFormat attachFormat = VirtIODriverFormat.valueOf(attachFormatText);

        if (NONE.equals(attachFormat)) {
            return NONE;
        }

        if (!VmInstanceConstant.VmOperation.NewCreate.equals(spec.getCurrentVmOperation())) {
            return NONE;
        }
        if (spec.getImageSpec() == null || spec.getImageSpec().getInventory() == null) {
            return NONE;
        }

        String bootMode = VmSystemTags.BOOT_MODE.getTokenByResourceUuid(
                spec.getVmInventory().getUuid(), VmSystemTags.BOOT_MODE_TOKEN);
        if (bootMode != null && !ImageBootMode.Legacy.toString().equals(bootMode)) {
            return NONE;
        }

        ImageInventory image = spec.getImageSpec().getInventory();
        boolean needAttachVirtIODriver =
                (ImageConstant.ISO_FORMAT_STRING.equals(image.getFormat()) &&
                (ImagePlatform.WindowsVirtio.name().equals(image.getPlatform()) ||
                ImagePlatform.Windows.name().equals(image.getPlatform()) && Boolean.TRUE.equals(image.getVirtio())));
        return needAttachVirtIODriver ? attachFormat : NONE;
    }

    @Override
    public void preBeforeInstantiateVmResource(VmInstanceSpec spec) throws VmInstantiateResourceException {}

    @Override
    @SuppressWarnings({"rawtypes"})
    public void preInstantiateVmResource(VmInstanceSpec spec, Completion completion) {
        final VirtIODriverFormat driverFormat = queryFormatOfDefaultVirtioDriver(spec);
        if (NONE.equals(driverFormat)) {
            logger.debug("auto attach VirtIO driver format is none, no need to instantiate VirtIO driver");
            completion.success();
            return;
        }

        Flow flow = createCheckAndSendVirtIODriverFlow(spec, driverFormat);
        FlowChain chain = new SimpleFlowChain();
        chain.setName(String.format("check-and-send-virtio-driver-%s-to-host-%s",
                driverFormat, spec.getDestHost().getUuid()));
        chain.then(flow);
        chain.done(new FlowDoneHandler(completion) {
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
    public void preReleaseVmResource(VmInstanceSpec spec, Completion completion) {
        completion.success();
    }

    private Flow createCheckAndSendVirtIODriverFlow(VmInstanceSpec spec, VirtIODriverFormat driverFormat) {
        switch (driverFormat) {
            case VFD: {
                CheckAndSendVirtIODriverFlow flow = new CheckAndSendVirtIODriverFlow();
                String hostUuid = spec.getDestHost().getUuid();
                flow.setKvm(dbf.findByUuid(hostUuid, KVMHostVO.class));
                flow.configWithVFDDriverPath();
                return flow;
            }
            case ISO: 
            default:
                throw new OperationFailureException(operr("invalid virtio driver device format: %s", driverFormat));
        }
    }

    @Override
    public boolean start() {
        deployAnsible();
        return true;
    }

    @BypassWhenUnitTest
    private void deployAnsible() {
        asf.deployModule(ANSIBLE_MODULE_PATH, ANSIBLE_PLAYBOOK_NAME);
    }

    @Override
    public boolean stop() {
        return true;
    }
}
