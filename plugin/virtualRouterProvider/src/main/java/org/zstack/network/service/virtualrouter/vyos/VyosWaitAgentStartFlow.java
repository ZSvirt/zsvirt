package org.zstack.network.service.virtualrouter.vyos;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.appliancevm.ApplianceVmInventory;
import org.zstack.appliancevm.ApplianceVmVO;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by shixin on 2022/06/15.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VyosWaitAgentStartFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(VyosWaitAgentStartFlow.class);

    @Autowired
    private VirtualRouterManager vrMgr;
    @Autowired
    private RESTFacade restf;
    @Autowired
    private DatabaseFacade dbf;

    public void updateVrLoginUser(String vrUuid, boolean isVyos){
        SystemTagCreator creator = VirtualRouterSystemTags.VIRTUAL_ROUTER_LOGIN_USER.newSystemTagCreator(vrUuid);
        creator.setTagByTokens(map(
                e(VirtualRouterSystemTags.VIRTUAL_ROUTER_LOGIN_USER_TOKEN, isVyos ? "vyos" : "zstack")
        ));
        creator.inherent = true;
        creator.recreate = true;
        creator.create();
        logger.debug("creator done");
    }

    /* this flow is called before VyosDeployAgentFlow, because:
    *  1. vyos vm startup, zvrboot will reboot vyos agent
    *  2. VyosDeployAgentFlow may also reboot vyos agent,
    *  3. we must keep #1 finished before #2
    *  */
    @Override
    public void run(FlowTrigger trigger, Map data) {
        final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());
        String vrUuid;
        VmNicInventory mgmtNic;
        if (vr != null) {
            mgmtNic = vr.getManagementNic();
            vrUuid = vr.getUuid();
        } else {
            final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
            vrUuid = spec.getVmInventory().getUuid();
            ApplianceVmInventory applianceVm = ApplianceVmInventory.valueOf(dbf.findByUuid(vrUuid, ApplianceVmVO.class));
            mgmtNic = applianceVm.getManagementNic();
            DebugUtils.Assert(mgmtNic!=null, String.format("cannot find management nic for virtual router[uuid:%s, name:%s]", spec.getVmInventory().getUuid(), spec.getVmInventory().getName()));
        }

        String url = vrMgr.buildUrl(mgmtNic.getIp(), VirtualRouterConstant.VR_ECHO_PATH);
        restf.echo(url, new Completion(trigger) {
            @Override
            public void success() {
                String url = vrMgr.buildUrl(mgmtNic.getIp(), VirtualRouterConstant.VR_GET_TYPE_PATH);
                VirtualRouterCommands.GetTypeCommand cmd = new VirtualRouterCommands.GetTypeCommand();
                cmd.setUuid(vrUuid);
                try {
                    VirtualRouterCommands.GetTypeRsp rsp = restf.syncJsonPost(url, cmd, VirtualRouterCommands.GetTypeRsp.class, TimeUnit.SECONDS, 10);
                    if (rsp.isSuccess()) {
                        updateVrLoginUser(vrUuid, rsp.isVyos());
                    }
                }catch (OperationFailureException e){
                    // old zvr will return 404
                    updateVrLoginUser(vrUuid, true);
                }
                trigger.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                trigger.next();
            }
        }, TimeUnit.SECONDS.toMillis(1), TimeUnit.SECONDS.toMillis(Long.parseLong(VirtualRouterGlobalConfig.VYOS_ECHO_TIMEOUT.value())));
    }
}
