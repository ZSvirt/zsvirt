package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.ChangeVmPasswordMsg;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.image.ImageSystemTagOperator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;

import java.util.Map;

/**
 * Created by mingjian.deng on 16/10/18.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ChangeVmPasswordFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(ChangeVmPasswordFlow.class);
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private PluginRegistry pluginRgty;

    private boolean checkSystemTags(VmInstanceSpec spec) {
        if (spec.getAccountPerference() == null) {
            return false;
        }
        return new ImageSystemTagOperator().isQemuGASystemTagOnVm(spec.getAccountPerference().getVmUuid(), dbf);
    }

    @Override
    public void run(final FlowTrigger trigger, Map data) {
        logger.debug("change password while vm is running.");
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        for (SkipChangeVmPasswordOnHostExtensionPoint ext : pluginRgty.getExtensionList(SkipChangeVmPasswordOnHostExtensionPoint.class)) {
            if (!ext.skipChangeVmPasswordOnHost(spec)) {
                continue;
            }

            logger.debug(String.format("skip change vm[uuid:%s] password on host", spec.getVmInventory().getUuid()));
            trigger.next();
            return;
        }

        ErrorCode noHostErr = operr("not dest host found in db, can't send change password cmd to the host!");
        ErrorCode noAccountPerferenceErr = operr("not account preference found, " +
                " send change password cmd to the host!");


        ChangeVmPasswordMsg msg = new ChangeVmPasswordMsg();
        if(spec.getAccountPerference() == null) {
            trigger.fail(noAccountPerferenceErr);
            return;
        }
        if(spec.getDestHost() == null) {
            trigger.fail(noHostErr);
            return;
        }

        msg.setHostUuid(spec.getDestHost().getUuid());
        msg.setAccountPerference(spec.getAccountPerference());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, spec.getDestHost().getUuid());
        bus.send(msg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    trigger.fail(reply.getError());
                    return;
                }
                trigger.next();
            }
        });
    }
}
