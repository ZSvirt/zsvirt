package org.zstack.crypto.keyprovider;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.keyprovider.KeyProviderVO;
import org.zstack.header.keyprovider.KeyProviderVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.header.tpm.api.APIAddTpmMsg;
import org.zstack.header.vm.APICreateVmInstanceMsg;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

import static org.zstack.core.Platform.err;
import static org.zstack.utils.CollectionDSL.list;

public class KeyProviderTpmInterceptor implements GlobalApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(KeyProviderTpmInterceptor.class);

    @Override
    @SuppressWarnings("rawtypes")
    public List<Class> getMessageClassToIntercept() {
        return list(
            APICreateVmInstanceMsg.class,
            APIAddTpmMsg.class
        );
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateVmInstanceMsg) {
            validate((APICreateVmInstanceMsg) msg);
        } else if (msg instanceof APIAddTpmMsg) {
            validate((APIAddTpmMsg) msg);
        }

        return msg;
    }

    private void validate(APICreateVmInstanceMsg msg) {
        if (msg.getDevicesSpec() == null || msg.getDevicesSpec().getTpm() == null) {
            return;
        }

        final ErrorCode errorCode = checkKeyProviderUuid(
                msg.getDevicesSpec().getTpm().getKeyProviderUuid(), "devicesSpec.tpm.keyProviderUuid");
        if (errorCode != null) {
            throw new ApiMessageInterceptionException(errorCode);
        }
    }

    private void validate(APIAddTpmMsg msg) {
        final ErrorCode errorCode = checkKeyProviderUuid(msg.getKeyProviderUuid(), "keyProviderUuid");
        if (errorCode != null) {
            throw new ApiMessageInterceptionException(errorCode);
        }
    }

    private ErrorCode checkKeyProviderUuid(String keyProviderUuid, String path) {
        if (StringUtils.isEmpty(keyProviderUuid)) {
            logger.info("devicesSpec.tpm.keyProviderUuid is mandatory soon, but ignore for now only.");
            return null;
        }

        boolean keyProviderExists = Q.New(KeyProviderVO.class)
                .eq(KeyProviderVO_.uuid, keyProviderUuid)
                .isExists();
        if (!keyProviderExists) {
            return err(SysErrors.RESOURCE_NOT_FOUND,
                    "invalid field[keyProviderUuid] for %s, resource[uuid:%s, type:%s] not found",
                    path, keyProviderUuid, KeyProviderVO.class.getSimpleName()
            );
        }

        return null;
    }
}
