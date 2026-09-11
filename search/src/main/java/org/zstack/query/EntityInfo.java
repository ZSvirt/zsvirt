package org.zstack.query;

import org.objenesis.instantiator.ObjectInstantiator;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.ExpandedQueryStruct;
import org.zstack.header.query.Queryable;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.query.QueryOp;
import org.zstack.header.query.Unqueryable;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;
import org.zstack.header.search.TypeField;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.FieldUtils;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EntityInfo {
    EntityInfo parent;
    List<EntityInfo> children = new ArrayList<>();
    Inventory inventoryAnnotation;
    Class<?> entityClass;
    Class<?> jpaMetaClass;
    public Class<?> inventoryClass;
    String primaryKey;
    Field entityPrimaryKeyField;
    Field inventoryPrimaryKeyField;
    Field inventoryTypeField;
    Field entityTypeField;
    public final Map<String, ExpandedQueryStruct> expandedQueries = new HashMap<>();
    Map<String, EntityInfo> flatTypeEntityMap = new HashMap<>();
    Method inventoryValueOf;
    Method inventoryCollectionValueOf;
    ObjectInstantiator<?> objectInstantiator;
    public final Map<String, Field> allFieldsMap = new HashMap<>();
    public final Map<String, ExpandedQueryAliasInfo> aliases = new HashMap<>();
    List<String> primitiveFieldNames = new ArrayList<>();

    EntityInfo(InventoryCollector collector, Class<?> invClass) throws NoSuchMethodException {
        inventoryAnnotation = invClass.getAnnotation(Inventory.class);
        entityClass = inventoryAnnotation.mappingVOClass();
        if (!entityClass.isAnnotationPresent(Entity.class)) {
            throw new CloudRuntimeException(String.format("class[%s] is not annotated by @Entity, but it's stated as entity class by @Inventory of %s",
                    entityClass.getName(), invClass.getName()));

        }

        jpaMetaClass = collector.metaModelClasses.get(entityClass);
        if (jpaMetaClass == null) {
            throw new CloudRuntimeException(String.format("cannot find JPA meta model class for entity class[%s], the meta model class is expected as %s",
                    entityClass.getName(), entityClass.getName() + "_"));
        }
        inventoryClass = invClass;
        entityPrimaryKeyField = FieldUtils.getAnnotatedField(Id.class, entityClass);
        Objects.requireNonNull(entityPrimaryKeyField);
        primaryKey = entityPrimaryKeyField.getName();
        entityPrimaryKeyField.setAccessible(true);
        inventoryPrimaryKeyField = FieldUtils.getField(primaryKey, inventoryClass);
        if (inventoryPrimaryKeyField != null) {
            inventoryPrimaryKeyField.setAccessible(true);
        }

        inventoryTypeField = FieldUtils.getAnnotatedFieldOfThisClass(TypeField.class, invClass);
        if (inventoryTypeField != null) {
            inventoryTypeField.setAccessible(true);
            entityTypeField = FieldUtils.getField(inventoryTypeField.getName(), entityClass);
            DebugUtils.Assert(entityTypeField != null, String.format("the type field[%s] of inventory class[%s] is not on entity class[%s]", inventoryTypeField.getName(), inventoryClass.getName(), entityClass.getName()));
            entityTypeField.setAccessible(true);
        }


        String methodName = inventoryAnnotation.collectionValueOfMethod();
        if (methodName.isEmpty()) {
            methodName = "valueOf";
        }
        inventoryCollectionValueOf = invClass.getMethod(methodName, Collection.class);
        inventoryValueOf = invClass.getMethod("valueOf", entityClass);

        List<ExpandedQueryStruct> structs = collector.expandedQueryStructs.get(inventoryClass);
        if (structs != null) {
            for (ExpandedQueryStruct s : structs) {
                s.check();
                this.expandedQueries.put(s.getExpandedField(), s);
            }
        }

        ExpandedQueries expandedQueries = invClass.getAnnotation(ExpandedQueries.class);
        if (expandedQueries != null) {
            for (ExpandedQuery e : expandedQueries.value()) {
                Class<?> iclass = e.target() == Object.class ? inventoryClass : e.target();
                ExpandedQueryStruct s = ExpandedQueryStruct.fromExpandedQueryAnnotation(iclass, e);
                s.check();
                this.expandedQueries.put(s.getExpandedField(), s);
            }
        }

        objectInstantiator = collector.objenesis.getInstantiatorOf(inventoryClass);
        List<Field> allFields = FieldUtils.getAllFields(inventoryClass);
        for (Field f : allFields) {
            f.setAccessible(true);
            allFieldsMap.put(f.getName(), f);

            if (!f.isAnnotationPresent(Unqueryable.class) && !f.isAnnotationPresent(Queryable.class)) {
                primitiveFieldNames.add(f.getName());
            }
        }

        for (List<ExpandedQueryAliasInfo> aliasList : collector.aliasInfos.values()) {
            for (ExpandedQueryAliasInfo struct : aliasList) {
                if (struct.inventoryClassDefiningThisAlias == inventoryClass) {
                    aliases.put(struct.alias, struct);
                }
            }
        }
    }

    void buildFlatTypeEntityMap() {
        for (EntityInfo e : children) {
            Parent pat = e.inventoryAnnotation.parent()[0];
            flatTypeEntityMap.put(pat.type(), e);
        }
    }

    Object getPrimaryKeyValue(Object vo) {
        try {
            return entityPrimaryKeyField.get(vo);
        } catch (IllegalAccessException e) {
            throw new CloudRuntimeException(e);
        }
    }

    Class<?> selectInventoryClass(final APIQueryMessage msg) {
        if (inventoryTypeField == null) {
            return inventoryClass;
        }

        QueryCondition typeCondition = null;
        for (QueryCondition cond : msg.getConditions()) {
            if (QueryOp.EQ.toString().equals(cond.getOp()) && inventoryTypeField.getName().equals(cond.getName())) {
                typeCondition = cond;
                break;
            }
        }

        if (typeCondition != null) {
            EntityInfo child = flatTypeEntityMap.get(typeCondition.getValue());
            if (child != null) {
                return child.inventoryClass;
            }
        }

        return inventoryClass;
    }

    void addQueryAliases(List<ExpandedQueryAliasInfo> aliases) {
        for (ExpandedQueryAliasInfo info : aliases) {
            this.aliases.put(info.alias, info);
        }
    }
}
