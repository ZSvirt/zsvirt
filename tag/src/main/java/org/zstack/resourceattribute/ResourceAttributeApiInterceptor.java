package org.zstack.resourceattribute;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.identity.rbac.RBAC;
import org.zstack.header.message.APIMessage;
import org.zstack.header.resourceattribute.AttributeConstant;
import org.zstack.header.resourceattribute.ResourceAttributeMessage;
import org.zstack.header.resourceattribute.api.APICreateResourceAttributeKeyMsg;
import org.zstack.header.resourceattribute.api.APIUpdateResourceAttributeKeyMsg;
import org.zstack.header.resourceattribute.entity.ResourceAttributeConstraintParam;
import org.zstack.header.resourceattribute.entity.ResourceAttributeConstraintVO;
import org.zstack.header.resourceattribute.entity.ResourceAttributeConstraintVO_;
import org.zstack.header.resourceattribute.entity.ResourceAttributeKeyVO;
import org.zstack.header.resourceattribute.entity.ResourceAttributeKeyVO_;
import org.zstack.utils.CollectionUtils;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import static org.zstack.core.Platform.err;
import static org.zstack.header.resourceattribute.AttributeErrors.*;
import static org.zstack.utils.CollectionUtils.*;

@InterceptorForService("resourceAttribute")
public class ResourceAttributeApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    CloudBus bus;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof ResourceAttributeMessage) {
            ResourceAttributeMessage castMessage = (ResourceAttributeMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, AttributeConstant.SERVICE_ID, castMessage.getKeyUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateResourceAttributeKeyMsg) {
            validate((APICreateResourceAttributeKeyMsg) msg);
        } else if (msg instanceof APIUpdateResourceAttributeKeyMsg) {
            validate((APIUpdateResourceAttributeKeyMsg) msg);
        }

        setServiceId(msg);
        return msg;
    }

    private void validate(APICreateResourceAttributeKeyMsg msg) {
        boolean duplicateName = Q.New(ResourceAttributeKeyVO.class)
                .eq(ResourceAttributeKeyVO_.name, msg.getName())
                .isExists();
        if (duplicateName) {
            throw new ApiMessageInterceptionException(err(DUPLICATED_ATTRIBUTE,
                    "duplicate resource attribute key name[%s]", msg.getName()));
        }

        if (CollectionUtils.isEmpty(msg.getResourceTypes())) {
            msg.setResourceTypes(transform(RBAC.attributeSupportResources, Class::getSimpleName));
        } else {
            msg.setResourceTypes(checkAttributeResourceTypeOrThrow(msg.getResourceTypes()));
        }

        final ErrorCode errorCode = ResourceAttributeManager.checkResourceAttributeConstraints(msg.getConstraints());
        if (errorCode != null) {
            throw new ApiMessageInterceptionException(errorCode);
        }

        if (msg.getResourceUuid() == null) {
            msg.setResourceUuid(Platform.getUuid());
        }
        bus.makeTargetServiceIdByResourceUuid(msg, AttributeConstant.SERVICE_ID, ResourceAttributeKeyVO.class.getName());
    }

    private void validate(APIUpdateResourceAttributeKeyMsg msg) {
        if (!CollectionUtils.isEmpty(msg.getResourceTypes())) {
            msg.setResourceTypes(checkAttributeResourceTypeOrThrow(msg.getResourceTypes()));
        }

        if (msg.getName() != null) {
            boolean duplicateName = Q.New(ResourceAttributeKeyVO.class)
                    .eq(ResourceAttributeKeyVO_.name, msg.getName())
                    .notEq(ResourceAttributeKeyVO_.uuid, msg.getKeyUuid())
                    .isExists();
            if (duplicateName) {
                throw new ApiMessageInterceptionException(err(DUPLICATED_ATTRIBUTE,
                        "duplicate resource attribute key name[%s]", msg.getName()));
            }
        }

        // update constraints: only support to update parameter
        // ResourceAttributeConstraintParam.type will be set
        if (!CollectionUtils.isEmpty(msg.getUpdateConstraints())) {
            List<Tuple> tuples = Q.New(ResourceAttributeConstraintVO.class)
                    .in(ResourceAttributeConstraintVO_.id, transform(msg.getUpdateConstraints(), c -> c.id))
                    .select(ResourceAttributeConstraintVO_.id, ResourceAttributeConstraintVO_.type)
                    .listTuple();
            Map<Long, String> idTypeMap = toMap(tuples, t -> t.get(0, Long.class), t -> t.get(1, String.class));

            for (ResourceAttributeConstraintParam constraint : msg.getUpdateConstraints()) {
                if (constraint.parameter == null) {
                    throw new ApiMessageInterceptionException(err(UNSUPPORTED_CONSTRAINTS,
                            "unsupported constraint parameter: can not be null")
                        .withOpaque("constraint", constraint));
                }
                constraint.type = idTypeMap.get(constraint.id);
            }
        }

        // id checker
        Set<Long> constraintIds = new HashSet<>();
        if (msg.getUpdateConstraints() != null) {
            for (ResourceAttributeConstraintParam constraint : msg.getUpdateConstraints()) {
                long id = constraint.id;
                if (constraintIds.contains(id)) {
                    throw new ApiMessageInterceptionException(err(INVALID_CONSTRAINTS_ID,
                            "duplicated constraint id[%s]", id)
                            .withOpaque("constraint.id", id));
                }
                constraintIds.add(id);
            }
        }
        if (msg.getDeleteConstraintIds() != null) {
            for (Long id : msg.getDeleteConstraintIds()) {
                if (id == null) {
                    throw new ApiMessageInterceptionException(err(INVALID_CONSTRAINTS_ID,
                            "field[deleteConstraints[x]] is mandatory, can not be null"));
                }
                if (constraintIds.contains(id)) {
                    throw new ApiMessageInterceptionException(err(INVALID_CONSTRAINTS_ID,
                            "duplicated constraint id[%s]", id)
                            .withOpaque("constraint.id", id));
                }
                constraintIds.add(id);
            }
        }
        if (!isEmpty(constraintIds)) {
            List<Tuple> tuples = Q.New(ResourceAttributeConstraintVO.class)
                    .in(ResourceAttributeConstraintVO_.id, constraintIds)
                    .select(ResourceAttributeConstraintVO_.id, ResourceAttributeConstraintVO_.keyUuid)
                    .listTuple();
            final Set<Long> ids = transformToSet(tuples, tuple -> tuple.get(0, Long.class));
            Set<Long> notFoundIds = new HashSet<>(constraintIds);
            notFoundIds.removeAll(ids);

            if (!notFoundIds.isEmpty()) {
                throw new ApiMessageInterceptionException(err(INVALID_CONSTRAINTS_ID,
                        "invalid constraint id%s", notFoundIds)
                        .withOpaque("constraint.id.list", notFoundIds));
            }

            for (Tuple tuple : tuples) {
                String keyUuid = tuple.get(1, String.class);
                if (!Objects.equals(keyUuid, msg.getKeyUuid())) {
                    throw new ApiMessageInterceptionException(err(INVALID_CONSTRAINTS_ID,
                        "constraint id %s is not belong to resource attribute key %s", tuple.get(0, Integer.class), keyUuid)
                        .withOpaque("constraint.id", notFoundIds));
                }
            }
        }

        // basic checker
        List<ResourceAttributeConstraintParam> constraints = new ArrayList<>();
        if (msg.getUpdateConstraints() != null) {
            constraints.addAll(msg.getUpdateConstraints());
        }
        if (msg.getCreateConstraints() != null) {
            constraints.addAll(msg.getCreateConstraints());
        }
        final ErrorCode errorCode = ResourceAttributeManager.checkResourceAttributeConstraints(constraints);
        if (errorCode != null) {
            throw new ApiMessageInterceptionException(errorCode);
        }
    }

    private static List<String> checkAttributeResourceTypeOrThrow(List<String> types) {
        Set<String> supportResourceType = transformToSet(RBAC.attributeSupportResources, Class::getSimpleName);
        final Set<String> requiredTypes = new TreeSet<>(types);
        requiredTypes.removeAll(supportResourceType);

        if (!CollectionUtils.isEmpty(requiredTypes)) {
            throw new ApiMessageInterceptionException(err(UNSUPPORTED_RESOURCE_TYPE,
                    "invalid resource type[%s]", requiredTypes));
        }

        // duplicate removal
        requiredTypes.addAll(types);
        return new ArrayList<>(requiredTypes);
    }
}
