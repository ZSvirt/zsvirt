package org.zstack.crypto.keyprovider;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.header.tpm.api.APIAddTpmMsg;
import org.zstack.header.vm.APICreateVmInstanceMsg;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

import static org.zstack.header.keyprovider.KeyProviderGlobalConfig.DEFAULT_KEY_PROVIDER_UUID;
import static org.zstack.utils.CollectionDSL.list;

public class KeyProviderTpmAutoCompleter implements GlobalApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(KeyProviderTpmAutoCompleter.class);

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateVmInstanceMsg) {
            complete((APICreateVmInstanceMsg) msg);
        } else if (msg instanceof APIAddTpmMsg) {
            complete((APIAddTpmMsg) msg);
        }
        return msg;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List<Class> getMessageClassToIntercept() {
        return list(
            APICreateVmInstanceMsg.class,
            APIAddTpmMsg.class
        );
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.FRONT;
    }

    private void complete(APICreateVmInstanceMsg msg) {
        if (msg.getDevicesSpec() == null || msg.getDevicesSpec().getTpm() == null || !msg.getDevicesSpec().getTpm().isEnable()) {
            return;
        }

        if (msg.getDevicesSpec().getTpm().getKeyProviderUuid() == null) {
            msg.getDevicesSpec().getTpm().setKeyProviderUuid(getDefaultKeyProviderUuid());
        }
    }

    private void complete(APIAddTpmMsg msg) {
        if (msg.getKeyProviderUuid() == null) {
            msg.setKeyProviderUuid(getDefaultKeyProviderUuid());
        }
    }

    private String getDefaultKeyProviderUuid() {
        String uuid = DEFAULT_KEY_PROVIDER_UUID.value(String.class);
        return StringUtils.isEmpty(uuid) || "None".equals(uuid) ? null : uuid;
    }
}
