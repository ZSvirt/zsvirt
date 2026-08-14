package org.zstack.network.service.virtualrouter.vyos;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.appliancevm.ApplianceVmConstant;
import org.zstack.appliancevm.ApplianceVmInventory;
import org.zstack.appliancevm.ApplianceVmSpec;
import org.zstack.appliancevm.ApplianceVmVO;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

/**
 * Created by shixin.ruan on 2018/05/22.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VyosGetVersionFlow extends NoRollbackFlow {
    private final static CLogger logger = Utils.getLogger(VyosGetVersionFlow.class);
    @Autowired
    private VyosVersionManager vyosVersionManager;
    @Autowired
    protected RESTFacade restf;
    @Autowired
    private VirtualRouterManager vrMgr;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void run(FlowTrigger flowTrigger, Map flowData) {
        final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) flowData.get(VirtualRouterConstant.Param.VR.toString());
        String vrUuid;
        VmNicInventory mgmtNic;

        if (vr != null) {
            mgmtNic = vr.getManagementNic();
            vrUuid = vr.getUuid();
        } else {
            final VmInstanceSpec spec = (VmInstanceSpec) flowData.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
            vrUuid = spec.getVmInventory().getUuid();
            if (spec.getCurrentVmOperation() == VmInstanceConstant.VmOperation.NewCreate) {
                final ApplianceVmSpec aspec = spec.getExtensionData(ApplianceVmConstant.Params.applianceVmSpec.toString(), ApplianceVmSpec.class);
                mgmtNic = CollectionUtils.findOneOrNull(spec.getDestNics(),
                        arg -> arg.getL3NetworkUuid().equals(aspec.getManagementNic().getL3NetworkUuid()));
            } else {
                ApplianceVmVO avo = dbf.findByUuid(vrUuid, ApplianceVmVO.class);
                ApplianceVmInventory ainv = ApplianceVmInventory.valueOf(avo);
                mgmtNic = ainv.getManagementNic();
            }
        }

        flowData.put(VmInstanceConstant.Params.vmInstanceUuid.toString(), vrUuid);
        vyosVersionManager.vyosRouterVersionCheck(vrUuid, new ReturnValueCompletion<VyosVersionCheckResult>(flowTrigger) {
            @Override
            public void fail(ErrorCode errorCode) {
                flowTrigger.next();
            }

            @Override
            public void success(VyosVersionCheckResult returnValue) {
                if (returnValue.isNeedReconnect()) {
                    logger.warn(String.format("virtual router [uuid:%s] need to be reconnect: %s", vrUuid, JSONObjectUtil.toJsonString(returnValue)));
                    flowData.put(ApplianceVmConstant.Params.isReconnect.toString(), Boolean.TRUE.toString());
                    flowData.put(ApplianceVmConstant.Params.managementNicIp.toString(), mgmtNic.getIp());
                    flowData.put(ApplianceVmConstant.Params.rebuildSnat.toString(), returnValue.isRebuildSnat());
                }
                flowTrigger.next();
            }
        });
    }
}
