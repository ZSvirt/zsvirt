package org.zstack.compute.vm.vmfiles;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.header.vm.APICloneVmInstanceMsg;
import org.zstack.header.vm.APICreateVmInstanceFromTemplatedVmInstanceMsg;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

public class MevocoVmHostFileAutoCompleter implements GlobalApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(MevocoVmHostFileAutoCompleter.class);

    @Autowired
    private ResourceConfigFacade resourceConfigFacade;

    @Override
    @SuppressWarnings("rawtypes")
    public List<Class> getMessageClassToIntercept() {
        return list(
                APICloneVmInstanceMsg.class,
                APICreateVmInstanceFromTemplatedVmInstanceMsg.class
        );
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICloneVmInstanceMsg) {
            validate((APICloneVmInstanceMsg) msg);
        } else if (msg instanceof APICreateVmInstanceFromTemplatedVmInstanceMsg) {
            validate((APICreateVmInstanceFromTemplatedVmInstanceMsg) msg);
        }
        return msg;
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.FRONT;
    }

    private void validate(APICloneVmInstanceMsg msg) {
        if (msg.getResetTpm() != null) {
            return;
        }

        String srcVmUuid = msg.getVmInstanceUuid();
        if (srcVmUuid == null) {
            return;
        }

        Boolean resolved = resourceConfigFacade.getResourceConfigValue(
                org.zstack.compute.vm.VmGlobalConfig.RESET_TPM_AFTER_VM_CLONE, srcVmUuid, Boolean.class);
        msg.setResetTpm(resolved);
    }

    private void validate(APICreateVmInstanceFromTemplatedVmInstanceMsg msg) {
        if (msg.getResetTpm() != null) {
            return;
        }

        // template VM is also a VM  ->  templateUuid is vmUuid
        String templateUuid = msg.getTemplatedVmInstanceUuid();
        if (templateUuid == null) {
            return;
        }

        Boolean resolved = resourceConfigFacade.getResourceConfigValue(
                org.zstack.compute.vm.VmGlobalConfig.RESET_TPM_AFTER_VM_CLONE, templateUuid, Boolean.class);
        msg.setResetTpm(resolved);
    }
}
