package org.zstack.ipsec.vyos;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.network.service.VirtualRouterHaTask;
import org.zstack.ipsec.IPsecConnectionInventory;
import org.zstack.ipsec.vyos.VyosIPsecConstants.Param;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import org.zstack.network.service.virtualrouter.ha.VirtualRouterHaBackend;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.Map;

import static org.zstack.core.Platform.operr;


/**
 * Created by xing5 on 2016/11/10.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VyosDeleteIPsecFlow extends NoRollbackFlow {

    @Autowired
    private VyosIPsecBackend bkd;
    @Autowired
    private VirtualRouterHaBackend haBackend;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        VirtualRouterVmInventory vr = (VirtualRouterVmInventory)data.get(Param.VR);
        IPsecConnectionInventory inv = (IPsecConnectionInventory)data.get(Param.IPSEC_STRUCT);

        if (vr == null) {
            /* can not delete */
            trigger.next();
            return;
        }

        bkd.deleteIpSecconnection(vr, inv, new Completion(trigger) {
            @Override
            public void success() {
                submitDeleteIpsecToHaRouter(vr, inv, new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(operr("delete ipsec from ha group failed because %s", errorCode.getDescription()));
                    }
                });
            }

            @Override
            public void fail(ErrorCode errorCode) {
                trigger.fail(errorCode);
            }
        });
    }

    private void submitDeleteIpsecToHaRouter(VirtualRouterVmInventory vrInv, IPsecConnectionInventory inv, Completion completion) {
        VirtualRouterHaTask task = new VirtualRouterHaTask();
        task.setTaskName(VyosIPsecBackend.DELETE_IPSEC_TASK);
        task.setOriginRouterUuid(vrInv.getUuid());
        task.setJsonData(JSONObjectUtil.toJsonString(inv));
        haBackend.submitVirtualRouterHaTask(task, completion);
    }
}
