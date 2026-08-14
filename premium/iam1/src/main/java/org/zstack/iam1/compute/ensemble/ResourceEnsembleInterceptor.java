package org.zstack.iam1.compute.ensemble;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.identity.APIRevokeResourceSharingMsg;
import org.zstack.header.identity.APIShareResourceMsg;
import org.zstack.header.message.APIMessage;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by Wenhao.Zhang on 2024/08/07
 */
@InterceptorForService("iam1Ensemble")
public class ResourceEnsembleInterceptor implements GlobalApiMessageInterceptor {
    @Autowired
    private CloudBus bus;

    @Override
    @SuppressWarnings("rawtypes")
    public List<Class> getMessageClassToIntercept() {
        return list(
            APIShareResourceMsg.class,
            APIRevokeResourceSharingMsg.class
        );
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIShareResourceMsg) {
            validate(((APIShareResourceMsg) msg));
        } else if (msg instanceof APIRevokeResourceSharingMsg) {
            validate(((APIRevokeResourceSharingMsg) msg));
        }
        return msg;
    }

    private void validate(APIShareResourceMsg message) {
        bus.makeLocalServiceId(message, ResourceEnsembleConstant.SERVICE_ID);
    }

    private void validate(APIRevokeResourceSharingMsg message) {
        bus.makeLocalServiceId(message, ResourceEnsembleConstant.SERVICE_ID);
    }
}
