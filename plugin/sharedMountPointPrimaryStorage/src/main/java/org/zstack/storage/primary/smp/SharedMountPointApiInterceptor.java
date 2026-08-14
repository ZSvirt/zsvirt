package org.zstack.storage.primary.smp;

import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.message.APIMessage;

import java.util.List;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.argerr;

/**
 * Created by camile on 2017/4/18.
 */
public class SharedMountPointApiInterceptor implements GlobalApiMessageInterceptor {
    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddSharedMountPointPrimaryStorageMsg) {
            validate((APIAddSharedMountPointPrimaryStorageMsg) msg);
        }

        return msg;
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return asList(APIAddSharedMountPointPrimaryStorageMsg.class);
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.DEFAULT;
    }

    private void validate(APIAddSharedMountPointPrimaryStorageMsg msg) {
        String url = msg.getUrl();
        if (url.startsWith("/dev") || url.startsWith("/proc") || url.startsWith("/sys")) {
            throw new ApiMessageInterceptionException(argerr(" the url contains an invalid folder[/dev or /proc or /sys]"));
        }
    }
}
