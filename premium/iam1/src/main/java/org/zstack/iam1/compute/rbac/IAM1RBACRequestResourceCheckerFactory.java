package org.zstack.iam1.compute.rbac;

import org.zstack.core.db.Q;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.vo.ResourceTypeMetadata;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.iam1.compute.ensemble.ResourceEnsembleHelper;
import org.zstack.iam1.header.ensemble.ResourceEnsembleInfo;
import org.zstack.identity.rbac.ResourcePolicyChecker;
import org.zstack.identity.rbac.ResourcePolicyCheckerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class IAM1RBACRequestResourceCheckerFactory implements ResourcePolicyCheckerFactory {
    @Override
    public ResourcePolicyChecker build(Collection<APIMessage.FieldParam> fieldParams, APIMessage message) {
        Set<String> uuidSet = new HashSet<>();

        for (APIMessage.FieldParam fieldParam : fieldParams) {
            Object object = getFromField(fieldParam, message);

            if (fieldParam.param.resourceType().length == 0) {
                continue;
            }

            if (object instanceof Collection) {
                ((Collection<?>) object).forEach(uuid -> {
                    if (uuid == null) {
                        return;
                    }
                    uuidSet.add(uuid.toString());
                });
            } else if (object instanceof String) {
                uuidSet.add(object.toString());
            }
        }

        IAM1RBACRequestResourceChecker checker = new IAM1RBACRequestResourceChecker();
        if (uuidSet.isEmpty()) {
            return checker;
        }

        final Map<String, String> uuidTypeMap = Q.New(ResourceVO.class)
                .in(ResourceVO_.uuid, uuidSet)
                .select(ResourceVO_.uuid, ResourceVO_.resourceType)
                .listTuple()
                .stream()
                .collect(Collectors.toMap(tuple -> tuple.get(0, String.class), tuple -> tuple.get(1, String.class)));

        for (String uuid : uuidSet) {
            final Class<?> typeClass = ResourceTypeMetadata.resourceTypeForName(uuidTypeMap.get(uuid));

            if (!ResourceEnsembleHelper.inEnsemble(typeClass)) {
                continue;
            }

            ResourceEnsembleInfo ensemble = ResourceEnsembleHelper.findResourceEnsemble(uuid, typeClass);
            if (ensemble == null) {
                continue;
            }

            checker.uuidTypeMapNeedCheck.put(ensemble.uuid, ensemble.resourceType());
        }

        return checker;
    }

    private Object getFromField(APIMessage.FieldParam fieldParam, APIMessage message) {
        try {
            return fieldParam.field.get(message);
        } catch (IllegalAccessException e) {
            throw new CloudRuntimeException(e);
        }
    }
}
