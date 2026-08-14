package org.zstack.image;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.image.APIGetImageQgaMsg;
import org.zstack.header.image.APISetImageQgaMsg;
import org.zstack.header.image.APISetImageSecurityLevelMsg;
import org.zstack.header.image.ImageMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.securityLevel.SecurityLevel;
import org.zstack.mevoco.MevocoConstants;
import org.zstack.mevoco.MevocoGlobalConfig;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.argerr;

/**
 * Created by mingjian.deng on 17/1/5.
 */
public class ImageMevocoApiInterceptor implements GlobalApiMessageInterceptor {
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private CloudBus bus;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APISetImageSecurityLevelMsg) {
            validate((APISetImageSecurityLevelMsg) msg);
        }

        setServiceId(msg);
        return msg;
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return asList(
                APIGetImageQgaMsg.class,
                APISetImageQgaMsg.class,
                APISetImageSecurityLevelMsg.class
        );
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.DEFAULT;
    }

    private void setServiceId(APIMessage msg) {
        if (msg instanceof ImageMessage) {
            ImageMessage imsg = (ImageMessage)msg;
            bus.makeTargetServiceIdByResourceUuid(msg, MevocoConstants.SERVICE_ID, imsg.getImageUuid());
        }
    }

    private void validate(APISetImageSecurityLevelMsg msg) {
        if (!MevocoGlobalConfig.ENABLE_SECURITY_LEVEL.value(Boolean.class)) {
            throw new ApiMessageInterceptionException(argerr("Failed to set security level, because security level is disabled."));
        }

        if (msg.getSecurityLevel() == null) {
            return;
        }

        SecurityLevel level = SecurityLevel.fromCode(msg.getSecurityLevel());

        if (level == null) {
            throw new ApiMessageInterceptionException(argerr("Unknown security level code[%s], supported values are %s",
                    msg.getSecurityLevel(), Arrays.stream(SecurityLevel.values()).map(SecurityLevel::getCode).collect(Collectors.toList())));
        }
    }
}
