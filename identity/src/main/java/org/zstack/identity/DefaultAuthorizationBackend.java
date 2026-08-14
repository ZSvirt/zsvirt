package org.zstack.identity;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBusGson;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.identity.extension.AuthorizationBackend;
import org.zstack.header.message.APIMessage;
import org.zstack.identity.rbac.RBACResourceRequestChecker;
import org.zstack.identity.rbac.RBACAPIRequestChecker;
import org.zstack.identity.rbac.RBACManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

public class DefaultAuthorizationBackend implements AuthorizationBackend {
    private static final CLogger logger = Utils.getLogger(DefaultAuthorizationBackend.class);

    @Autowired
    private RBACManager rbacManager;

    @Override
    public boolean takeOverAuthorization(SessionInventory session) {
        return true;
    }

    @Override
    public APIMessage authorize(APIMessage msg) {
        List<APIRequestChecker> checkers = new ArrayList<>();
        checkers.add(new RBACAPIRequestChecker());
        checkers.add(new RBACResourceRequestChecker());
        checkers.add(new QuotaAPIRequestChecker());

        try {
            checkers.forEach(c -> {
                if (!c.bypass(msg)) {
                    c.check(msg);
                }
            });
        } catch (OperationFailureException e) {
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("%s, %s", e.getErrorCode(), CloudBusGson.toJson(msg)));
            }

            throw e;
        }

        return msg;
    }

    @Override
    public void validatePermission(List<Class> classes, SessionInventory session) {
        Map<String, Boolean> permissionResult = new RBACAPIRequestChecker().evalAPIPermission(classes, session);

        List<String> deniedApis = permissionResult.entrySet().stream().filter(entry -> !entry.getValue()).map(Map.Entry::getKey).collect(Collectors.toList());
        if (!deniedApis.isEmpty()) {
            throw new OperationFailureException(operr("the operations[%s] is denied", deniedApis));
        }
    }
}
