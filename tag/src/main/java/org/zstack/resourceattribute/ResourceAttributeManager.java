package org.zstack.resourceattribute;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.AbstractService;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.Message;
import org.zstack.header.resourceattribute.AttributeConstant;
import org.zstack.header.resourceattribute.AttributeErrors;
import org.zstack.header.resourceattribute.ResourceAttributeMessage;
import org.zstack.header.resourceattribute.api.APICreateResourceAttributeKeyEvent;
import org.zstack.header.resourceattribute.api.APICreateResourceAttributeKeyMsg;
import org.zstack.header.resourceattribute.entity.ResourceAttributeConstraintParam;
import org.zstack.header.resourceattribute.entity.ResourceAttributeConstraintVO;
import org.zstack.header.resourceattribute.entity.ResourceAttributeConstraintVO_;
import org.zstack.header.resourceattribute.entity.ResourceAttributeKeyInventory;
import org.zstack.header.resourceattribute.entity.ResourceAttributeKeyResourceTypeVO;
import org.zstack.header.resourceattribute.entity.ResourceAttributeKeyVO;
import org.zstack.header.resourceattribute.entity.ResourceAttributeKeyVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.zstack.core.Platform.err;
import static org.zstack.header.resourceattribute.AttributeConstant.*;
import static org.zstack.utils.CollectionUtils.isEmpty;
import static org.zstack.utils.CollectionUtils.transform;

public class ResourceAttributeManager extends AbstractService {
    private static final CLogger logger = Utils.getLogger(ResourceAttributeManager.class);

    private final Object createLock = new Object();

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade databaseFacade;

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof ResourceAttributeMessage) {
            passThrough((ResourceAttributeMessage) msg);
        } else if (msg instanceof APICreateResourceAttributeKeyMsg) {
            handle((APICreateResourceAttributeKeyMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void passThrough(ResourceAttributeMessage msg) {
        ResourceAttributeKeyVO vo = databaseFacade.findByUuid(msg.getKeyUuid(), ResourceAttributeKeyVO.class);

        if (vo == null) {
            String err = String.format("Cannot find ResourceAttributeKeyVO[uuid:%s], it may have been deleted",
                    msg.getKeyUuid());
            bus.replyErrorByMessageType((Message) msg, err);
            return;
        }

        ResourceAttributeBase base = new ResourceAttributeBase(vo);
        base.handleMessage((Message) msg);
    }

    private void handle(APICreateResourceAttributeKeyMsg msg) {
        APICreateResourceAttributeKeyEvent event = new APICreateResourceAttributeKeyEvent(msg.getId());

        ResourceAttributeKeyVO key = new ResourceAttributeKeyVO();
        key.setUuid(msg.getResourceUuid());
        key.setName(msg.getName());
        key.setDescription(msg.getDescription());

        List<ResourceAttributeKeyResourceTypeVO> relatedTypes = new ArrayList<>();
        for (String resourceType : msg.getResourceTypes()) {
            ResourceAttributeKeyResourceTypeVO type = new ResourceAttributeKeyResourceTypeVO();
            type.setKeyUuid(key.getUuid());
            type.setResourceType(resourceType);
            relatedTypes.add(type);
        }

        List<ResourceAttributeConstraintVO> constraints = createConstraints(key.getUuid(), msg.getConstraints());

        boolean duplicate;
        synchronized (createLock) {
            duplicate = Q.New(ResourceAttributeKeyVO.class)
                    .eq(ResourceAttributeKeyVO_.name, msg.getName())
                    .isExists();
            if (!duplicate) {
                databaseFacade.persist(key);
                databaseFacade.persistCollection(relatedTypes);
                databaseFacade.persistCollection(constraints);

                key = databaseFacade.reload(key);
            }
        }

        if (duplicate) {
            event.setError(err(AttributeErrors.DUPLICATED_ATTRIBUTE,
                    "duplicate resource attribute key name[%s]", msg.getName()));
        } else {
            event.setInventory(ResourceAttributeKeyInventory.valueOf(key));
        }
        bus.publish(event);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(AttributeConstant.SERVICE_ID);
    }

    public static ErrorCode checkResourceAttributeConstraints(List<ResourceAttributeConstraintParam> constraints) {
        if (isEmpty(constraints)) {
            return null;
        }

        String optionType = null;
        Set<String> options = new HashSet<>();

        for (ResourceAttributeConstraintParam constraint : constraints) {
            if (constraint == null) {
                return err(AttributeErrors.UNSUPPORTED_CONSTRAINTS, "constraint can not be null");
            }

            String type = constraint.type;
            if (!VALID_CONSTRAINTS_OPTIONS.contains(type)) {
                return err(AttributeErrors.UNSUPPORTED_CONSTRAINTS, "unsupported constraints type[%s]", type)
                        .withOpaque("constraint.type", type);
            }

            if (CONSTRAINTS_OPTION.equals(type) || CONSTRAINTS_ENUM.equals(type)) {
                if (optionType == null) {
                    optionType = type;
                } else if (!optionType.equals(type)) {
                    return err(AttributeErrors.UNSUPPORTED_CONSTRAINTS,
                            "unsupported constraints type[%s]: 'option' and 'enum' can not exist simultaneously", type)
                        .withOpaque("constraint.type", type);
                }

                String parameter = constraint.parameter;
                if (parameter == null || parameter.isEmpty()) {
                    return err(AttributeErrors.UNSUPPORTED_CONSTRAINTS,
                            "unsupported constraints type[%s]: 'option' or 'enum' parameter can not be empty", type)
                        .withOpaque("constraint.type", type)
                        .withOpaque("constraint.parameter", parameter);
                }

                if (options.contains(parameter)) {
                    return err(AttributeErrors.UNSUPPORTED_CONSTRAINTS,
                            "unsupported constraints type[%s]: 'option' or 'enum' parameter[%s] is duplicated", type, parameter)
                        .withOpaque("constraint.type", type)
                        .withOpaque("constraint.parameter", parameter);
                }
                options.add(parameter);
            }
        }

        return null;
    }

    public static List<ResourceAttributeConstraintVO> createConstraints(
            String keyUuid,
            Collection<ResourceAttributeConstraintParam> constraints) {
        if (constraints == null) {
            return new ArrayList<>();
        }
        return transform(constraints, c -> createConstraint(keyUuid, c));
    }

    public static ResourceAttributeConstraintVO createConstraint(String keyUuid, ResourceAttributeConstraintParam constraint) {
        ResourceAttributeConstraintVO vo = new ResourceAttributeConstraintVO();
        vo.setKeyUuid(keyUuid);
        vo.setType(constraint.type);
        vo.setParameter(constraint.parameter);
        return vo;
    }

    public static Set<String> enumOptionsForKeyUuid(String keyUuid) {
        List<String> options = Q.New(ResourceAttributeConstraintVO.class)
                .eq(ResourceAttributeConstraintVO_.keyUuid, keyUuid)
                .eq(ResourceAttributeConstraintVO_.type, CONSTRAINTS_ENUM)
                .select(ResourceAttributeConstraintVO_.parameter)
                .listValues();
        return options.isEmpty() ? null : new HashSet<>(options);
    }
}
