package org.zstack.appliancevm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.vm.VmBeforeCreateOnHypervisorExtensionPoint;
import org.zstack.header.vm.VmBeforeStartOnHypervisorExtensionPoint;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.utils.*;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import static org.zstack.core.Platform.err;

/**
 */
public class ApplianceVmManagementIpChecker implements VmBeforeCreateOnHypervisorExtensionPoint, VmBeforeStartOnHypervisorExtensionPoint {
    private static final CLogger logger = Utils.getLogger(ApplianceVmManagementIpChecker.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    private void checkManagementIp(VmInstanceSpec spec, boolean isNewCreated) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        if (!ApplianceVmConstant.APPLIANCE_VM_TYPE.equals(spec.getVmInventory().getType())) {
            return;
        }

        VmNicInventory mgmtNic;
        if (isNewCreated) {
            final ApplianceVmSpec aspec = spec.getExtensionData(ApplianceVmConstant.Params.applianceVmSpec.toString(), ApplianceVmSpec.class);
            mgmtNic = CollectionUtils.findOneOrNull(spec.getDestNics(),
                    arg -> arg.getL3NetworkUuid().equals(aspec.getManagementNic().getL3NetworkUuid()));
        } else {
            ApplianceVmInventory apvm = ApplianceVmInventory.valueOf(dbf.findByUuid(spec.getVmInventory().getUuid(), ApplianceVmVO.class));
            mgmtNic = apvm.getManagementNic();
        }

        DebugUtils.Assert(mgmtNic!=null, String.format("cannot find management nic of appliance vm[uuid:%s, newCreated: %s]", spec.getVmInventory().getUuid(), isNewCreated));

        ShellResult ret = ShellUtils.runAndReturn(String.format("ping -c 1 -W 1 %s", mgmtNic.getIp()));
        if (ret.isReturnCode(0) || NetworkUtils.isReachable(mgmtNic.getIp(), 1000)) {
            throw new OperationFailureException(err(ApplianceVmErrors.MANAGEMENT_IP_OCCUPIED,
                    "the management nic IP[%s] has been occupied by another device in the data center, we can ping it", mgmtNic.getIp()
            ));
        }
    }

    @Override
    public void beforeCreateVmOnHypervisor(VmInstanceSpec spec) {
        checkManagementIp(spec, true);
    }

    @Override
    public void beforeStartVmOnHypervisor(VmInstanceSpec spec) {
        checkManagementIp(spec, false);
    }
}
