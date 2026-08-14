package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.host.HostSystemTags;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigInitExtensionPoint;
import org.zstack.core.config.GlobalConfigVO;
import org.zstack.core.config.GlobalConfigVO_;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.CreateVmInstanceMsg;
import org.zstack.header.vm.VmInstanceCreateExtensionPoint;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.kvm.KVMStartVmExtensionPoint;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.resourceconfig.ResourceConfigVO;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.compute.vm.VmCpuVendor.AuthenticAMD;
import static org.zstack.kvm.KVMConstant.CPU_MODE_HOST_PASSTHROUGH;
import static org.zstack.kvm.KVMConstant.CPU_MODE_HYGON_CUSTOMIZED;

public class VmCpuVendorKvmStartVmExtension implements KVMStartVmExtensionPoint, GlobalConfigInitExtensionPoint {
    @Autowired
    ResourceConfigFacade rcf;

    @Autowired
    DatabaseFacade dbf;

    private static final String HYGON = "hygon";

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        String host_cpu_model_name = HostSystemTags.HOST_CPU_MODEL_NAME.getTokenByResourceUuid(host.getUuid(), HostSystemTags.HOST_CPU_MODEL_NAME_TOKEN);
        if (host_cpu_model_name == null) {
            return;
        }
        if (!host_cpu_model_name.toLowerCase().contains(HYGON)) {
            return;
        }

        ResourceConfig rc = rcf.getResourceConfig(VmGlobalConfig.VM_CPUID_VENDOR.getIdentity());
        String vmCpuIdVendor = rc.getResourceConfigValueByResourceType(spec.getVmInventory().getUuid(), VmInstanceVO.class.getSimpleName(), String.class);
        if (vmCpuIdVendor == null) {
            rc.updateValue(spec.getVmInventory().getUuid(), AuthenticAMD.toString());
        }

        cmd.setVmCpuVendorId(rcf.getResourceConfigValue(VmGlobalConfig.VM_CPUID_VENDOR, spec.getVmInventory().getUuid(), String.class));
    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {

    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {

    }


    @Override
    public List<GlobalConfig> getGenerationGlobalConfig() {
        // change 'Hygon_Customized' in db to 'Host_passthrough', otherwise the re-validation will fail.
        changeHygonCustomizedForGlobalConfigAndResourceConfig();
        return new ArrayList<>();
    }

    private void changeHygonCustomizedForGlobalConfigAndResourceConfig() {
        if (Q.New(GlobalConfigVO.class)
                .eq(GlobalConfigVO_.category, KVMGlobalConfig.CATEGORY)
                .eq(GlobalConfigVO_.name, "vm.cpuMode")
                .eq(GlobalConfigVO_.value, CPU_MODE_HYGON_CUSTOMIZED)
                .isExists()) {
            KVMGlobalConfig.NESTED_VIRTUALIZATION.updateValue(CPU_MODE_HOST_PASSTHROUGH);
        }

        String sql = "select rc from ResourceConfigVO rc where rc.name = :name " +
                "and rc.category = :category and rc.value = :value";
        List<ResourceConfigVO> rcs = SQL.New(sql).
                param("name", "vm.cpuMode").
                param("category", KVMGlobalConfig.CATEGORY).
                param("value", CPU_MODE_HYGON_CUSTOMIZED).list();

        if (!rcs.isEmpty()) {
            rcs.forEach(rc -> rc.setValue(CPU_MODE_HOST_PASSTHROUGH));
            dbf.updateCollection(rcs);
        }
    }
}
