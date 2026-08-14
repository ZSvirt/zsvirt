package org.zstack.mevoco;

import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APISyncCallMessage;

import java.util.List;

import static org.zstack.core.Platform.argerr;

/**
 * Created by MaJin on 2020/4/9.
 */
public class PauseWorldApiInterceptor implements GlobalApiMessageInterceptor {
    @Override
    public List<Class> getMessageClassToIntercept() {
        return null;
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.SYSTEM;
    }

    @Override
    public int getPriority() {
        // Earliest interceptor
        return Integer.MIN_VALUE;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (!MevocoGlobalConfig.PAUSE_THE_WORLD.value(Boolean.class)) {
            return msg;
        }

        if (msg instanceof APISyncCallMessage) {
            return msg;
        }

        if (msg.getSystemTags() != null && msg.getSystemTags().stream().anyMatch(it -> MevocoSystemTags.CONFIRM_CALL_API.isMatch(it))) {
            return msg;
        }

        throw new ApiMessageInterceptionException(argerr("ZStack has been paused, reject all API which are not read only. " +
                "If you really want to call it and known the consequence, add '%s' into systemTags.", MevocoSystemTags.CONFIRM_CALL_API.getTagFormat()));
    }
}
