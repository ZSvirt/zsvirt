package org.zstack.portal.apimediator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.ApiMessageValidator;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;

import javax.persistence.Tuple;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.zstack.core.Platform.err;

public class PortApiValidator implements ApiMessageValidator, Ordered {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    @Override
    public void validate(APIMessage msg, Field f, Object value, APIParam at) {
        if (value == null || at.resourceType().length == 0) {
            return;
        }

        List<Class<?>> resourceTypes = Arrays.asList(at.resourceType());
        Set<String> uuidList = new HashSet<>();
        boolean successIfResourceNotExisting;

        if (value instanceof Collection) {
            uuidList.addAll(CollectionUtils.transform((Collection<?>) value, Object::toString));
            successIfResourceNotExisting = false;
        } else {
            DebugUtils.Assert(String.class.isAssignableFrom(f.getType()),
                    String.format("field[%s] of message[%s] has APIParam.resourceType specified, then the field must be uuid which is a String, but actual is %s",
                            f.getName(), msg.getClass().getName(), f.getType()));
            uuidList.add(value.toString());
            successIfResourceNotExisting = at.successIfResourceNotExisting();
        }

        if (uuidList.isEmpty()) {
            return;
        }

        for (String uuid : uuidList) {
            Pattern p = Pattern.compile("[0-9a-f]{8}[0-9a-f]{4}[1-5][0-9a-f]{3}[89ab][0-9a-f]{3}[0-9a-f]{12}");
            Matcher mt = p.matcher(uuid);
            if (!mt.matches()){
                throw new ApiMessageInterceptionException(err(SysErrors.RESOURCE_NOT_FOUND,
                        "invalid value[%s] of field[%s]", uuid, f.getName()));
            }
        }

        if (resourceTypes.size() == 1) {
            Class<?> resourceType = resourceTypes.get(0);
            if (uuidList.size() == 1) {
                String uuid = uuidList.iterator().next();
                boolean exists = dbf.isExist(uuid, resourceType);
                if (!exists && successIfResourceNotExisting) {
                    stopRoutingForNoneExistsResource(msg);
                } else if (!exists) {
                    throw new ApiMessageInterceptionException(err(SysErrors.RESOURCE_NOT_FOUND,
                            "invalid field[%s] for %s, resource[uuid:%s, type:%s] not found",
                            f.getName(), msg.getClass().getSimpleName(), uuid, resourceType.getSimpleName()));
                }
                return;
            }

            List<String> uuids = SQL.New("select e.uuid from " + resourceType.getSimpleName() + " e where e.uuid in (:uuids)", String.class)
                    .param("uuids", uuidList)
                    .list();
            if (uuids.size() != uuidList.size()) {
                uuidList.removeAll(uuids);
                throw new ApiMessageInterceptionException(err(SysErrors.RESOURCE_NOT_FOUND,
                        "invalid field[%s] for %s, resource[uuids:%s, type:%s] not found",
                        f.getName(), msg.getClass().getSimpleName(), uuidList, resourceType.getSimpleName()));
            }
            return;
        }

        final List<Tuple> tuples = Q.New(ResourceVO.class)
                .in(ResourceVO_.uuid, uuidList)
                .select(ResourceVO_.uuid, ResourceVO_.resourceType, ResourceVO_.concreteResourceType)
                .listTuple();
        if (tuples.size() != uuidList.size()) {
            if (successIfResourceNotExisting) {
                stopRoutingForNoneExistsResource(msg);
            }

            for (Tuple tuple : tuples) {
                uuidList.remove(tuple.get(0, String.class));
            }
            throw new ApiMessageInterceptionException(err(SysErrors.RESOURCE_NOT_FOUND,
                    "invalid field[%s] for %s, resource[uuids:%s, type:%s] not found",
                    f.getName(), msg.getClass().getSimpleName(), uuidList,
                    CollectionUtils.transform(resourceTypes, Class::getSimpleName)));
        }

        for (Tuple tuple : tuples) {
            String resourceType = tuple.get(1, String.class);
            if (resourceTypes.stream().anyMatch(type -> type.getSimpleName().equals(resourceType))) {
                continue;
            }

            try {
                final Class<?> aClass = Class.forName(tuple.get(2, String.class));
                if (resourceTypes.stream().noneMatch(type -> type.isAssignableFrom(aClass))) {
                    throw new ApiMessageInterceptionException(err(SysErrors.RESOURCE_NOT_FOUND,
                            "invalid value %s of field[%s]", tuple.get(0, String.class), f.getName()));
                }
            } catch (ClassNotFoundException e) {
                throw new CloudRuntimeException(String.format(
                        "invalid concreteResourceType[%s] for resource[uuid=%s]",
                        tuple.get(2, String.class), tuple.get(0, String.class)), e);
            }
        }
    }

    private void stopRoutingForNoneExistsResource(APIMessage msg) {
        RestRequest rat = msg.getClass().getAnnotation(RestRequest.class);
        if (rat == null) {
            throw new CloudRuntimeException(String.format("the API class[%s] does not have @RestRequest but it uses a successIfResourceNotExisting helper", msg.getClass()));
        }

        APIEvent evt;
        try {
            evt = (APIEvent) rat.responseClass().getConstructor(String.class).newInstance(msg.getId());
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }

        bus.publish(evt);
        throw new StopRoutingException();
    }
    
    @Override
    public int getOrder() {
        return -1;
    }
}
