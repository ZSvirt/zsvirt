package org.zstack.compute.vm.devices;

import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.tpm.api.APIGetTpmCapabilityMsg;
import org.zstack.header.tpm.api.APIRemoveTpmMsg;
import org.zstack.header.tpm.api.APIUpdateTpmMsg;
import org.zstack.header.tpm.api.TpmMessage;
import org.zstack.header.tpm.entity.TpmVO;
import org.zstack.header.tpm.entity.TpmVO_;

import java.util.List;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.err;
import static org.zstack.header.tpm.TpmErrors.TPM_NOT_FOUND;
import static org.zstack.utils.CollectionDSL.list;

public class TpmMessageAutoCompleter implements GlobalApiMessageInterceptor {
    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof TpmMessage) {
            validate((TpmMessage) msg);
        }
        return msg;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List<Class> getMessageClassToIntercept() {
        return list(APIGetTpmCapabilityMsg.class, APIRemoveTpmMsg.class, APIUpdateTpmMsg.class);
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.FRONT;
    }

    private void validate(TpmMessage msg) throws ApiMessageInterceptionException {
        String tpmUuid = msg.getTpmUuid();
        String vmUuid = msg.getVmInstanceUuid();

        if (tpmUuid == null && vmUuid == null) {
            throw new ApiMessageInterceptionException(argerr("tpmUuid and vmInstanceUuid cannot be null at the same time"));
        }

        if (tpmUuid != null && vmUuid != null) {
            boolean exists = Q.New(TpmVO.class)
                    .eq(TpmVO_.uuid, tpmUuid)
                    .eq(TpmVO_.vmInstanceUuid, vmUuid)
                    .isExists();
            if (!exists) {
                throw new ApiMessageInterceptionException(argerr("tpmUuid[%s] and vmInstanceUuid[%s] are not consistent", tpmUuid, vmUuid));
            }
        } else if (vmUuid != null) {
            tpmUuid = Q.New(TpmVO.class)
                    .select(TpmVO_.uuid)
                    .eq(TpmVO_.vmInstanceUuid, vmUuid)
                    .findValue();
            if (tpmUuid == null && (!(msg instanceof APIDeleteMessage))) {
                throw new ApiMessageInterceptionException(err(TPM_NOT_FOUND, "tpm for vm[%s] does not exist", vmUuid));
            } else {
                msg.setTpmUuid(tpmUuid);
            }
        } else {
            vmUuid = Q.New(TpmVO.class)
                    .select(TpmVO_.vmInstanceUuid)
                    .eq(TpmVO_.uuid, tpmUuid)
                    .findValue();
            msg.setVmInstanceUuid(vmUuid);
        }
    }
}
