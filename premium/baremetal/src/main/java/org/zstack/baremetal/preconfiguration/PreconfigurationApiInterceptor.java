package org.zstack.baremetal.preconfiguration;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.baremetal.preconfiguration.*;
import org.zstack.header.message.APIMessage;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by GuoYi on 2018-12-26.
 */
@InterceptorForService("baremetal.preconfiguration")
public class PreconfigurationApiInterceptor implements ApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(PreconfigurationApiInterceptor.class);

    @Autowired
    protected TemplateParamExtractor extractor;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddPreconfigurationTemplateMsg) {
            validate((APIAddPreconfigurationTemplateMsg) msg);
        } else if (msg instanceof APIUpdatePreconfigurationTemplateMsg) {
            validate((APIUpdatePreconfigurationTemplateMsg) msg);
        } else if (msg instanceof APIDeletePreconfigurationTemplateMsg) {
            validate((APIDeletePreconfigurationTemplateMsg) msg);
        } else if (msg instanceof APIChangePreconfigurationTemplateStateMsg) {
            validate((APIChangePreconfigurationTemplateStateMsg) msg);
        }

        return msg;
    }

    private void validate(APIAddPreconfigurationTemplateMsg msg) {
        validate(msg.getContent());
    }

    private void validate(APIUpdatePreconfigurationTemplateMsg msg) {
        if (isPredefined(msg.getUuid())) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "cannot update predefined preconfiguration templates"
            ));
        }

        if (msg.getContent() != null) {
            validate(msg.getContent());
        }
    }

    private void validate(APIDeletePreconfigurationTemplateMsg msg) {
        if (isPredefined(msg.getUuid())) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "cannot delete predefined preconfiguration templates"
            ));
        }
    }

    private void validate(APIChangePreconfigurationTemplateStateMsg msg) {
        if (isPredefined(msg.getUuid())) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "cannot change state of predefined preconfiguration templates"
            ));
        }
    }

    private boolean isPredefined(String uuid) {
        return Q.New(PreconfigurationTemplateVO.class)
                .select(PreconfigurationTemplateVO_.isPredefined)
                .eq(PreconfigurationTemplateVO_.uuid, uuid)
                .findValue();
    }

    private void validate(String templateContent) {
        TemplateParamExtractor.Result result = extractor.extractParams(templateContent);
        if (!result.isSuccess()) {
            throw new ApiMessageInterceptionException(Platform.argerr(result.getError()));
        }
    }
}
