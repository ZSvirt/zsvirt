package org.zstack.portal.apimediator;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.ApiMessageValidator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.inerr;

/**
 * Created by Wenhao.Zhang on 22/11/22
 */
public class ApiParamValidator implements GlobalApiMessageInterceptor {
    private static CLogger logger = Utils.getLogger(ApiParamValidator.class);

    @Autowired
    private List<ApiMessageValidator> validators;

    @Override
    @SuppressWarnings("rawtypes")
    public List<Class> getMessageClassToIntercept() {
        return null; // all classes
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.SYSTEM;
    }

    @Override
    public int getPriority() {
        return -1;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        try {
            msg.validate(validators);
        } catch (ApiMessageInterceptionException | StopRoutingException ae) {
            if (logger.isTraceEnabled()) {
                logger.trace(ae.getMessage(), ae);
            }
            throw ae;
        } catch (APIMessage.InvalidApiMessageException ie) {
            if (logger.isTraceEnabled()) {
                logger.trace(ie.getMessage(), ie);
            }
            throw new ApiMessageInterceptionException(argerr(ie.getMessage(), ie.getArguments()));
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
            throw new ApiMessageInterceptionException(inerr(e.getMessage()));
        }
        return msg;
    }
}
