package org.zstack.search;

import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIMessage;
import org.zstack.header.search.APISearchMessage;
import org.zstack.header.search.APISearchMessage.NOLTriple;
import org.zstack.header.search.APISearchMessage.NOVTriple;
import org.zstack.header.search.SearchOp;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

public class SearchMsgValidator implements GlobalApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(SearchMsgValidator.class);
    
    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException{
        if (msg instanceof APISearchMessage) {
            APISearchMessage smsg = (APISearchMessage)msg;
            try {
                for (NOLTriple t : smsg.getNameOpListTriples()) {
                    SearchOp.valueOf(t.getOp());
                }
                for (NOVTriple t : smsg.getNameOpValueTriples()) {
                    SearchOp.valueOf(t.getOp());
                }
            } catch (IllegalArgumentException e) {
                logger.warn("", e);
                //ErrorCode err = ErrorCodeFacade.generateErrorCode(ErrorCodeFacade.BuiltinErrors.INVALID_ARGRUMENT.toString(), e.getMessage());
                throw new ApiMessageInterceptionException(new ErrorCode());
            }
        }
        return msg;
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return null;
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.FRONT;
    }

}
