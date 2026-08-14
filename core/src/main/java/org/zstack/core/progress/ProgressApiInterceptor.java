package org.zstack.core.progress;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.core.progress.APIGetTaskProgressMsg;
import org.zstack.header.message.APIMessage;

import static org.zstack.core.Platform.argerr;
import static org.zstack.utils.StringDSL.isApiId;
import static org.zstack.utils.StringDSL.isZStackUuid;

/**
 * Created by miao on 17-5-16.
 */
@InterceptorForService("core.progress")
public class ProgressApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIGetTaskProgressMsg) {
            validate((APIGetTaskProgressMsg) msg);
        }

        return msg;
    }

    private void validate(APIGetTaskProgressMsg msg) {
        if (!isApiId(msg.getApiId())) {
            throw new ApiMessageInterceptionException(argerr("parameter apiId[%s] is not a valid uuid.", msg.getApiId()));
        }
    }
}
