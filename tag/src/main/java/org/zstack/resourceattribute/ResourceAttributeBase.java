package org.zstack.resourceattribute;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.db.UpdateQuery;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.resourceattribute.api.APICreateResourceAttributeValueEvent;
import org.zstack.header.resourceattribute.api.APICreateResourceAttributeValueMsg;
import org.zstack.header.resourceattribute.api.APIDeleteResourceAttributeKeyEvent;
import org.zstack.header.resourceattribute.api.APIDeleteResourceAttributeKeyMsg;
import org.zstack.header.resourceattribute.api.APIDeleteResourceAttributeValueEvent;
import org.zstack.header.resourceattribute.api.APIDeleteResourceAttributeValueMsg;
import org.zstack.header.resourceattribute.api.APIUpdateResourceAttributeKeyEvent;
import org.zstack.header.resourceattribute.api.APIUpdateResourceAttributeKeyMsg;
import org.zstack.header.resourceattribute.entity.CreateResourceAttributeResult;
import org.zstack.header.resourceattribute.entity.ResourceAttributeConstraintParam;
import org.zstack.header.resourceattribute.entity.ResourceAttributeConstraintVO;
import org.zstack.header.resourceattribute.entity.ResourceAttributeConstraintVO_;
import org.zstack.header.resourceattribute.entity.ResourceAttributeKeyInventory;
import org.zstack.header.resourceattribute.entity.ResourceAttributeKeyResourceTypeVO;
import org.zstack.header.resourceattribute.entity.ResourceAttributeKeyResourceTypeVO_;
import org.zstack.header.resourceattribute.entity.ResourceAttributeKeyVO;
import org.zstack.header.resourceattribute.entity.ResourceAttributeKeyVO_;
import org.zstack.header.resourceattribute.entity.ResourceAttributeValueVO;
import org.zstack.header.resourceattribute.entity.ResourceAttributeValueVO_;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.ObjectUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static org.zstack.core.Platform.err;
import static org.zstack.header.resourceattribute.AttributeErrors.*;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ResourceAttributeBase {
    private static final CLogger logger = Utils.getLogger(ResourceAttributeBase.class);

    private ResourceAttributeKeyVO self;
    @Autowired
    private CloudBus bus;
    @Autowired
    protected ThreadFacade threadFacade;
    @Autowired
    private DatabaseFacade databaseFacade;

    private String syncThreadName;

    ResourceAttributeBase(ResourceAttributeKeyVO vo) {
        self = vo;
        syncThreadName = "resource-attribute-" + self.getUuid();
    }

    void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateResourceAttributeValueMsg) {
            handle((APICreateResourceAttributeValueMsg) msg);
        } else if (msg instanceof APIDeleteResourceAttributeValueMsg){
            handle((APIDeleteResourceAttributeValueMsg) msg);
        } else if (msg instanceof APIUpdateResourceAttributeKeyMsg) {
            handle((APIUpdateResourceAttributeKeyMsg) msg);
        } else if (msg instanceof APIDeleteResourceAttributeKeyMsg) {
            handle((APIDeleteResourceAttributeKeyMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APICreateResourceAttributeValueMsg msg) {
        threadFacade.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return String.format("create-resource-attribute-value-with-key-%s", self.getUuid());
            }

            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                CreateResourceAttributeValueContext context = new CreateResourceAttributeValueContext();
                context.value = msg.getValue();
                context.resourceUuids = msg.getResourceUuids();
                createResourceAttributeValue(context);

                APICreateResourceAttributeValueEvent event = new APICreateResourceAttributeValueEvent(msg.getId());
                boolean allFail = context.values.stream().allMatch(value -> !value.isSuccess());
                if (allFail) {
                    ErrorCode error = err(GENERIC_ERROR, "failed to create resource attribute value");
                    ErrorCode errors = ObjectUtils.newAndCopy(error, ErrorCode.class);
                    errors.setCauses(transform(context.values, value -> value.error));
                    event.setError(errors);
                } else {
                    event.setInventories(CreateResourceAttributeResult.valueOf(context.values));
                }

                bus.publish(event);

                chain.next();
            }
        });
    }

    private void handle(APIDeleteResourceAttributeValueMsg msg) {
        threadFacade.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return String.format("delete-resource-attribute-value-with-key-%s", self.getUuid());
            }

            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                DeleteResourceAttributeValueContext context = new DeleteResourceAttributeValueContext();
                context.resourceUuids = msg.getResourceUuids();
                deleteResourceAttributeValue(context);

                APIDeleteResourceAttributeValueEvent event = new APIDeleteResourceAttributeValueEvent(msg.getId());
                bus.publish(event);

                chain.next();
            }
        });
    }

    private void handle(APIUpdateResourceAttributeKeyMsg msg) {
        threadFacade.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return String.format("update-resource-attribute-key-%s", self.getUuid());
            }

            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIUpdateResourceAttributeKeyEvent event = new APIUpdateResourceAttributeKeyEvent(msg.getId());

                ErrorCode error = checkConstraints(
                        msg.getCreateConstraints() == null ? emptyList() : msg.getCreateConstraints(),
                        msg.getUpdateConstraints() == null ? emptyList() : msg.getUpdateConstraints(),
                        msg.getDeleteConstraintIds() == null ? emptyList() : msg.getDeleteConstraintIds());
                if (error != null) {
                    event.setError(error);
                    bus.publish(event);
                    chain.next();
                    return;
                }

                if (!CollectionUtils.isEmpty(msg.getResourceTypes())) {
                    error = updateTypes();
                    if (error != null) {
                        event.setError(error);
                        bus.publish(event);
                        chain.next();
                        return;
                    }
                }

                updateConstraints(
                        msg.getCreateConstraints() == null ? emptyList() : msg.getCreateConstraints(),
                        msg.getUpdateConstraints() == null ? emptyList() : msg.getUpdateConstraints(),
                        msg.getDeleteConstraintIds() == null ? emptyList() : msg.getDeleteConstraintIds());

                boolean update = false;
                final UpdateQuery sql = SQL.New(ResourceAttributeKeyVO.class)
                        .eq(ResourceAttributeKeyVO_.uuid, self.getUuid());

                if (msg.getName() != null) {
                    sql.set(ResourceAttributeKeyVO_.name, msg.getName());
                    update = true;
                }
                if (msg.getDescription() != null) {
                    sql.set(ResourceAttributeKeyVO_.description, msg.getDescription());
                    update = true;
                }

                if (update) {
                    sql.update();
                }
                self = databaseFacade.reload(self);
                event.setInventory(ResourceAttributeKeyInventory.valueOf(self));
                bus.publish(event);
                chain.next();
            }

            private ErrorCode checkConstraints(
                    List<ResourceAttributeConstraintParam> constraintsNeedAdd,
                    List<ResourceAttributeConstraintParam> constraintsNeedUpdate,
                    List<Long> constraintsNeedDelete) {
                if (constraintsNeedAdd.isEmpty() && constraintsNeedUpdate.isEmpty() && constraintsNeedDelete.isEmpty()) {
                    return null;
                }

                self = databaseFacade.updateAndRefresh(self);

                final Map<Long, ResourceAttributeConstraintParam> idConstraintMap = toMap(self.getConstraints(),
                        ResourceAttributeConstraintVO::getId, ResourceAttributeConstraintParam::valueOf);
                for (Long id : constraintsNeedDelete) {
                    idConstraintMap.remove(id);
                }
                for (ResourceAttributeConstraintParam updateItem : constraintsNeedUpdate) {
                    Long id = updateItem.id;
                    ResourceAttributeConstraintParam constraint = idConstraintMap.get(id);
                    if (constraint == null) {
                        return err(INVALID_CONSTRAINTS_ID, "invalid constraint id[%s]", id)
                                .withOpaque("constraint.id", id);
                    }
                    constraint.parameter = updateItem.parameter;
                }

                List<ResourceAttributeConstraintParam> list = new ArrayList<>(idConstraintMap.values());
                list.addAll(constraintsNeedAdd);
                return ResourceAttributeManager.checkResourceAttributeConstraints(list);
            }

            @Transactional
            private void updateConstraints(
                    List<ResourceAttributeConstraintParam> constraintsNeedAdd,
                    List<ResourceAttributeConstraintParam> constraintsNeedUpdate,
                    List<Long> constraintsNeedDelete) {
                if (constraintsNeedAdd.isEmpty() && constraintsNeedUpdate.isEmpty() && constraintsNeedDelete.isEmpty()) {
                    return;
                }

                if (!constraintsNeedDelete.isEmpty()) {
                    SQL.New(ResourceAttributeConstraintVO.class)
                            .in(ResourceAttributeConstraintVO_.id, constraintsNeedDelete)
                            .delete();
                }

                Map<Long, ResourceAttributeConstraintVO> idConstraintMap = toMap(self.getConstraints(),
                        ResourceAttributeConstraintVO::getId, Function.identity());
                if (!constraintsNeedUpdate.isEmpty()) {
                    for (ResourceAttributeConstraintParam param : constraintsNeedUpdate) {
                        String oldValue = idConstraintMap.get(param.id).getParameter();

                        SQL.New(ResourceAttributeConstraintVO.class)
                                .eq(ResourceAttributeConstraintVO_.id, param.id)
                                .set(ResourceAttributeConstraintVO_.parameter, param.parameter)
                                .update();
                        // related value also been updated
                        SQL.New(ResourceAttributeValueVO.class)
                                .eq(ResourceAttributeValueVO_.keyUuid, self.getUuid())
                                .eq(ResourceAttributeValueVO_.value, oldValue)
                                .set(ResourceAttributeValueVO_.value, param.parameter)
                                .update();
                    }
                }

                if (!constraintsNeedAdd.isEmpty()) {
                    List<ResourceAttributeConstraintVO> needPersists = new ArrayList<>();
                    for (ResourceAttributeConstraintParam param : constraintsNeedAdd) {
                        ResourceAttributeConstraintVO vo = new ResourceAttributeConstraintVO();
                        vo.setKeyUuid(self.getUuid());
                        vo.setType(param.type);
                        vo.setParameter(param.parameter);
                        needPersists.add(vo);
                    }

                    databaseFacade.persistCollection(needPersists);
                }
            }

            @Transactional
            private ErrorCode updateTypes() {
                Set<String> originalTypes = transformToSet(self.getTypes(), ResourceAttributeKeyResourceTypeVO::getResourceType);

                Set<String> needDeletes = new HashSet<>(originalTypes);
                needDeletes.removeAll(msg.getResourceTypes());

                Set<String> needPersists = new HashSet<>(msg.getResourceTypes());
                needPersists.removeAll(originalTypes);

                if (!CollectionUtils.isEmpty(needDeletes)) {
                    List<String> resourceRelatedTypes = Q.New(ResourceAttributeValueVO.class)
                            .eq(ResourceAttributeValueVO_.keyUuid, self.getUuid())
                            .in(ResourceAttributeValueVO_.resourceType, needDeletes)
                            .select(ResourceAttributeValueVO_.resourceType)
                            .listValues();
                    if (!resourceRelatedTypes.isEmpty()) {
                        return err(REMOVE_RESOURCE_TYPE_NOT_ALLOWED,
                                "failed to remove resource type %s in attribute key[%s]: resources already attached",
                                resourceRelatedTypes, self.getUuid())
                                .withOpaque("related.resource.types", resourceRelatedTypes)
                                .withOpaque("attribute.key", self.getUuid());
                    }

                    SQL.New(ResourceAttributeKeyResourceTypeVO.class)
                            .eq(ResourceAttributeKeyResourceTypeVO_.keyUuid, self.getUuid())
                            .in(ResourceAttributeKeyResourceTypeVO_.resourceType, needDeletes)
                            .delete();
                }

                if (!CollectionUtils.isEmpty(needPersists)) {
                    List<ResourceAttributeKeyResourceTypeVO> types = new ArrayList<>();
                    for (String resourceType : needPersists) {
                        ResourceAttributeKeyResourceTypeVO vo = new ResourceAttributeKeyResourceTypeVO();
                        vo.setKeyUuid(self.getUuid());
                        vo.setResourceType(resourceType);
                        types.add(vo);
                    }
                    databaseFacade.persistCollection(types);
                }

                return null;
            }
        });
    }

    private void handle(APIDeleteResourceAttributeKeyMsg msg) {
        threadFacade.chainSubmit(new ChainTask(msg) {
            @Override
            public String getName() {
                return String.format("delete-resource-attribute-key-%s", self.getUuid());
            }

            @Override
            public String getSyncSignature() {
                return syncThreadName;
            }

            @Override
            public void run(SyncTaskChain chain) {
                SQL.New(ResourceAttributeKeyVO.class)
                        .eq(ResourceAttributeKeyVO_.uuid, msg.getUuid())
                        .delete();
                APIDeleteResourceAttributeKeyEvent event = new APIDeleteResourceAttributeKeyEvent(msg.getId());
                bus.publish(event);
                chain.next();
            }
        });
    }

    static class CreateResourceAttributeValueContext {
        String value;
        List<String> resourceUuids;

        // results
        List<ErrorableValue<ResourceAttributeValueVO>> values;
    }

    @SuppressWarnings("unchecked")
    private void createResourceAttributeValue(CreateResourceAttributeValueContext context) {
        final Set<String> optionsMayNull = ResourceAttributeManager.enumOptionsForKeyUuid(self.getUuid());
        if (optionsMayNull != null && !optionsMayNull.contains(context.value)) {
            context.values = list(ErrorableValue.ofErrorCode(
                    err(INVALID_VALUE, "invalid value[%s]: enum constraint", context.value)
                            .withOpaque("expect.options", optionsMayNull)
                            .withOpaque("attribute.key", self.getUuid())));
            return;
        }

        context.values = new ArrayList<>(context.resourceUuids.size());
        Map<String, String> resourceUuidTypeMap = buildResourceUuidTypeMap(context.resourceUuids);

        List<ResourceAttributeValueVO> values = new ArrayList<>();
        for (String resourceUuid : context.resourceUuids) {
            ResourceAttributeValueVO value = new ResourceAttributeValueVO();
            value.setKeyUuid(self.getUuid());
            value.setResourceUuid(resourceUuid);
            value.setResourceType(resourceUuidTypeMap.get(resourceUuid));
            value.setValue(context.value);
            values.add(value);
        }

        Set<String> supportTypes = transformToSet(self.getTypes(), ResourceAttributeKeyResourceTypeVO::getResourceType);
        new SQLBatch() {
            @Override
            protected void scripts() {
                for (List<String> sub : Lists.partition(context.resourceUuids, 100)) { // equals to update value
                    sql(ResourceAttributeValueVO.class)
                            .eq(ResourceAttributeValueVO_.keyUuid, self.getUuid())
                            .in(ResourceAttributeValueVO_.resourceUuid, sub)
                            .delete();
                }

                for (ResourceAttributeValueVO item : values) {
                    if (supportTypes.contains(item.getResourceType())) {
                        persist(item);
                        context.values.add(ErrorableValue.of(reload(item)));
                        continue;
                    }

                    context.values.add(ErrorableValue.ofErrorCode(
                            err(UNSUPPORTED_RESOURCE_TYPE, "unsupported resource type[%s]", item.getResourceType())
                                    .withOpaque("actual.resource.type", item.getResourceType())
                                    .withOpaque("attribute.key", self.getUuid())));
                }
            }
        }.execute();
    }

    static class DeleteResourceAttributeValueContext {
        List<String> resourceUuids;
    }

    private void deleteResourceAttributeValue(DeleteResourceAttributeValueContext context) {
        for (List<String> sub : Lists.partition(context.resourceUuids, 100)) { // equals to update value
            SQL.New(ResourceAttributeValueVO.class)
                    .eq(ResourceAttributeValueVO_.keyUuid, self.getUuid())
                    .in(ResourceAttributeValueVO_.resourceUuid, sub)
                    .delete();
        }
    }

    // utils

    @Transactional(readOnly = true)
    protected Map<String, String> buildResourceUuidTypeMap(List<String> resourceUuids) {
        Map<String, String> resourceType = new HashMap<>();

        for (List<String> sub : Lists.partition(resourceUuids, 100)) {
            List<Tuple> types = Q.New(ResourceVO.class).select(ResourceVO_.uuid, ResourceVO_.resourceType).in(ResourceVO_.uuid, sub).listTuple();
            types.forEach(t -> resourceType.put((String) t.get(0), (String) t.get(1)));
        }

        return resourceType;
    }
}
