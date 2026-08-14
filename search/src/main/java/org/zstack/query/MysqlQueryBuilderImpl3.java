package org.zstack.query;

import org.apache.commons.lang.StringUtils;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import org.objenesis.instantiator.ObjectInstantiator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.Component;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.configuration.PythonApiBindingWriter;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.query.*;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;
import org.zstack.header.search.TypeField;
import org.zstack.utils.*;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.argerr;

import javax.persistence.*;
import javax.persistence.metamodel.StaticMetamodel;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;
import static org.zstack.utils.StringDSL.s;

/**
 */
public class MysqlQueryBuilderImpl3 implements Component, QueryBuilder, GlobalApiMessageInterceptor, PythonApiBindingWriter {
    private static final CLogger logger = Utils.getLogger(MysqlQueryBuilderImpl3.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private PluginRegistry pluginRgty;

    private Objenesis objenesis = new ObjenesisStd();

    private static final String USER_TAG = "__userTag__";
    private static final String SYSTEM_TAG = "__systemTag__";

    @Override
    public List<Class> getMessageClassToIntercept() {
        List<Class> clz = new ArrayList<>();
        clz.add(APIQueryMessage.class);
        return clz;
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.FRONT;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        List<ExpandedQueryAliasInfo> infos = aliasInfos.get(msg.getClass());
        if (infos != null) {
            APIQueryMessage qmsg = (APIQueryMessage) msg;
            for (QueryCondition qcond : qmsg.getConditions()) {
                for (ExpandedQueryAliasInfo info : infos) {
                    if (qcond.getName().startsWith(String.format("%s.", info.alias))) {
                        qcond.setName(qcond.getName().replaceFirst(info.alias, info.expandField));
                    }
                }
            }
        }

        return msg;
    }

    private class ExpandedQueryAliasInfo {
        Class queryMessageClass;
        Class inventoryClassDefiningThisAlias;
        Class inventoryClass;
        String expandField;
        String alias;
        boolean isFromAnnotation;

        void check() {
            String[] slices = expandField.split("\\.");
            String firstExpand = slices[0];

            if (isFromAnnotation) {
                ExpandedQueries eqs = (ExpandedQueries) inventoryClassDefiningThisAlias.getAnnotation(ExpandedQueries.class);
                DebugUtils.Assert(eqs != null, String.format("inventory[%s] having annotation[ExpandedQueryAliases] must also have annotation[ExpandedQueries]",
                        inventoryClassDefiningThisAlias.getName()));

                for (ExpandedQuery at : eqs.value()) {
                    if (at.expandedField().equals(firstExpand)) {
                        return;
                    }
                }

                throw new CloudRuntimeException(String.format("inventory[%s] has an expanded query alias[%s]," +
                                " but it doesn't have an expand query that has expandedField[%s]",
                        inventoryClassDefiningThisAlias.getName(), alias, firstExpand));
            } else {
                List<ExpandedQueryStruct> expds = expandedQueryStructs.get(inventoryClassDefiningThisAlias);
                for (ExpandedQueryStruct s : expds) {
                    if (s.getExpandedField().equals(firstExpand)) {
                        return;
                    }
                }

                throw new CloudRuntimeException(String.format("inventory[%s] has an expanded query alias[%s](added by AddExpandedQueryExtensionPoint]," +
                                " but the extension doesn't declare any expanded query having expandedField[%s]",
                        inventoryClassDefiningThisAlias.getClass(), alias, firstExpand));
            }
        }
    }

    private class EntityInfo {
        EntityInfo parent;
        List<EntityInfo> children = new ArrayList<EntityInfo>();
        Inventory inventoryAnnotation;
        Class entityClass;
        Class jpaMetaClass;
        Class inventoryClass;
        String primaryKey;
        Field entityPrimaryKeyField;
        Field inventoryPrimaryKeyField;
        Field inventoryTypeField;
        Field entityTypeField;
        Map<String, ExpandedQueryStruct> expandedQueries = new HashMap<String, ExpandedQueryStruct>();
        Map<String, EntityInfo> flatTypeEntityMap = new HashMap<String, EntityInfo>();
        Method inventoryValueOf;
        Method inventoryCollectionValueOf;
        ObjectInstantiator objectInstantiator;
        Map<String, Field> allFieldsMap = new HashMap<String, Field>();
        Map<String, ExpandedQueryAliasInfo> aliases = new HashMap<String, ExpandedQueryAliasInfo>();
        List<String> premitiveFieldNames = new ArrayList<String>();

        EntityInfo(Class invClass) throws NoSuchMethodException {
            inventoryAnnotation = (Inventory) invClass.getAnnotation(Inventory.class);
            entityClass = inventoryAnnotation.mappingVOClass();
            if (!entityClass.isAnnotationPresent(Entity.class)) {
                throw new CloudRuntimeException(String.format("class[%s] is not annotated by @Entity, but it's stated as entity class by @Inventory of %s",
                        entityClass.getName(), invClass.getName()));

            }

            jpaMetaClass = metaModelClasses.get(entityClass);
            if (jpaMetaClass == null) {
                throw new CloudRuntimeException(String.format("cannot find JPA meta model class for entity class[%s], the meta model class is expected as %s",
                        entityClass.getName(), entityClass.getName() + "_"));
            }
            inventoryClass = invClass;
            entityPrimaryKeyField = FieldUtils.getAnnotatedField(Id.class, entityClass);
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
            if (methodName.equals("")) {
                methodName = "valueOf";
            }
            inventoryCollectionValueOf = invClass.getMethod(methodName, Collection.class);
            inventoryValueOf = invClass.getMethod("valueOf", entityClass);

            List<ExpandedQueryStruct> structs = expandedQueryStructs.get(inventoryClass);
            if (structs != null) {
                for (ExpandedQueryStruct s : structs) {
                    s.check();
                    this.expandedQueries.put(s.getExpandedField(), s);
                }
            }

            ExpandedQueries expandedQueries = (ExpandedQueries) invClass.getAnnotation(ExpandedQueries.class);
            if (expandedQueries != null) {
                for (ExpandedQuery e : expandedQueries.value()) {
                    Class iclass = e.target() == Object.class ? inventoryClass : e.target();
                    ExpandedQueryStruct s = ExpandedQueryStruct.fromExpandedQueryAnnotation(iclass, e);
                    s.check();
                    this.expandedQueries.put(s.getExpandedField(), s);
                }
            }

            objectInstantiator = objenesis.getInstantiatorOf(inventoryClass);
            List<Field> allFields = FieldUtils.getAllFields(inventoryClass);
            for (Field f : allFields) {
                f.setAccessible(true);
                allFieldsMap.put(f.getName(), f);

                if (!f.isAnnotationPresent(Unqueryable.class) && !f.isAnnotationPresent(Queryable.class)) {
                    premitiveFieldNames.add(f.getName());
                }
            }

            for (List<ExpandedQueryAliasInfo> aliasList : aliasInfos.values()) {
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

        Class selectInventoryClass(final APIQueryMessage msg) {
            if (inventoryTypeField == null) {
                return inventoryClass;
            }

            QueryCondition typeCondition = null;
            for (QueryCondition cond : msg.getConditions()) {
                if (QueryOp.EQ.equals(cond.getOp()) && inventoryTypeField.getName().equals(cond.getName())) {
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

    private class SubQueryInfo {
        Class joinInventoryClass;
    }

    private class ExpandedSubQuery extends SubQueryInfo {
        ExpandedQueryStruct struct;
    }

    private class InherentSubQuery extends SubQueryInfo {
        Queryable at;
        Field parentField;
    }

    private Map<Class, EntityInfo> entityInfos = new HashMap<Class, EntityInfo>();
    private Map<Class, Class> metaModelClasses = new HashMap<Class, Class>();
    private List<String> escapeConditionNames = new ArrayList<String>();
    private List<MysqlQuerySubQueryExtension> subQueryExtensions = new ArrayList<MysqlQuerySubQueryExtension>();
    private Map<Class, List<ExpandedQueryStruct>> expandedQueryStructs = new HashMap<Class, List<ExpandedQueryStruct>>();
    private Map<Class, List<AddExtraConditionToQueryExtensionPoint>> extraConditionsExts = new HashMap<Class, List<AddExtraConditionToQueryExtensionPoint>>();
    private Map<Class, List<ExpandedQueryAliasInfo>> aliasInfos = new HashMap<Class, List<ExpandedQueryAliasInfo>>();
    private Map<Class, Class> inventoryQueryMessageMap = new HashMap<Class, Class>();

    private EntityInfo buildEntityInfo(Class invClass) throws NoSuchMethodException {
        EntityInfo info = entityInfos.get(invClass);
        if (info != null) {
            return info;
        }

        info = new EntityInfo(invClass);
        entityInfos.put(invClass, info);

        return info;
    }

    private void populateEntityInfo() throws NoSuchMethodException {
        Set<Class<?>> metaClasses = BeanUtils.reflections.getTypesAnnotatedWith(StaticMetamodel.class);
        for (Class it : metaClasses) {
            StaticMetamodel at = (StaticMetamodel) it.getAnnotation(StaticMetamodel.class);
            metaModelClasses.put(at.value(), it);
        }

        Set<Class<?>> invClasses = BeanUtils.reflections.getTypesAnnotatedWith(Inventory.class);

        for (Class invClass : invClasses) {
            EntityInfo info = buildEntityInfo(invClass);

            if (info.inventoryAnnotation.parent().length > 0) {
                Parent pat = info.inventoryAnnotation.parent()[0];
                Class pinvClass = pat.inventoryClass();
                DebugUtils.Assert(pinvClass.isAnnotationPresent(Inventory.class), String.format("inventory[%s]'s parent inventory class[%s] is not annotated by @Inventory", info.inventoryClass.getName(), pinvClass.getName()));
                EntityInfo pinfo = buildEntityInfo(pinvClass);
                info.parent = pinfo;
                pinfo.children.add(info);
            }

        }

        for (EntityInfo e : entityInfos.values()) {
            e.buildFlatTypeEntityMap();
        }
    }

    private class MetaCondition {
        String attr;
        String op;
        String value;
        Class inventoryClass;
        String attrValueName;
        boolean skipInventoryCheck;
        int index;

        private Field entityField;

        QueryCondition toQueryCondtion() {
            QueryCondition qcond = new QueryCondition();
            qcond.setName(attr);
            qcond.setOp(op);
            qcond.setValue(value);
            return qcond;
        }

        private Class getEntityFieldType() {
            if (Collection.class.isAssignableFrom(entityField.getType())) {
                return FieldUtils.getGenericType(entityField);
            } else if (Map.class.isAssignableFrom(entityField.getType())) {
                throw new CloudRuntimeException(String.format("query cannot support Map type. %s.%s",
                        entityField.getDeclaringClass(), entityField.getName()));
            } else {
                return entityField.getType();
            }
        }

        private Object doNormalizeValue(String value) {
            try {
                Class entityType = getEntityFieldType();
                if (Timestamp.class.isAssignableFrom(entityType)) {
                    return Timestamp.valueOf(value);
                } else if (Enum.class.isAssignableFrom(entityType)) {
                    Method valueOf = entityType.getMethod("valueOf", String.class);
                    return valueOf.invoke(entityType, value);
                } else if (Boolean.class.isAssignableFrom(entityType) || Boolean.TYPE.isAssignableFrom(entityType)) {
                    return Boolean.valueOf(value);
                } else {
                    return TypeUtils.stringToValue(value, entityType);
                }
            } catch (Exception e) {
                throw new CloudRuntimeException(String.format("failed to parse value[%s], error: [%s]", value, e.getMessage()), e);
            }
        }

        Object normalizeValue() {
            if (QueryOp.IS_NULL.equals(op) || QueryOp.NOT_NULL.equals(op)) {
                return null;
            }

            if (QueryOp.IN.equals(op) || QueryOp.NOT_IN.equals(op)) {
                List<Object> ret = new ArrayList();
                for (String it : value.split(",")) {
                    ret.add(doNormalizeValue(it.trim()));
                }

                if (ret.isEmpty()) {
                    // the query value is like ",,,,",
                    // in this case, compliment an empty string
                    ret.add("");
                }
                return ret;
            } else {
                return doNormalizeValue(value);
            }
        }

        private String formatSql(String entityName, String attr, String op) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%s.%s", entityName, attr));
            if (QueryOp.IN.equals(op)) {
                sb.append(String.format(" in (:%s)", attrValueName));
            } else if (QueryOp.NOT_IN.equals(op)) {
                sb.append(String.format(" not in (:%s)", attrValueName));
            } else if (QueryOp.IS_NULL.equals(op)) {
                sb.append(" is null");
                attrValueName = null;
            } else if (QueryOp.NOT_NULL.equals(op)) {
                sb.append(" is not null");
                attrValueName = null;
            } else {
                sb.append(String.format(" %s :%s", op, attrValueName));
            }
            return sb.toString();
        }

        String toJpql() {
            String entityName = inventoryClass.getSimpleName().toLowerCase();
            attrValueName = entityName + "_" + attr + "_" + "value" + index;
            Field inventoryField = FieldUtils.getField(attr, inventoryClass);

            if (!skipInventoryCheck) {
                if (inventoryField == null || inventoryField.isAnnotationPresent(APINoSee.class)) {
                    throw new OperationFailureException(argerr("condition name[%s] is invalid, no such field on inventory class[%s]",
                                    attr, inventoryClass.getName()));
                }

                if (inventoryField.isAnnotationPresent(Unqueryable.class)) {
                    throw new OperationFailureException(argerr("condition name[%s] is invalid, field[%s] of inventory[%s] is annotated as @Unqueryable field",
                                    attr, attr, inventoryClass.getName()));
                }
            }

            Queryable at = inventoryField.getAnnotation(Queryable.class);
            EntityInfo info = entityInfos.get(inventoryClass);

            if (at == null) {
                Field metaField = FieldUtils.getField(attr, info.jpaMetaClass);
                if (metaField == null) {
                    throw new OperationFailureException(argerr("entity meta class[%s] has no field[%s]",
                            info.jpaMetaClass.getName(), attr));
                }

                entityField = FieldUtils.getField(attr, info.entityClass);
                DebugUtils.Assert(entityField != null, String.format("mismatching between inventory[%s] and entity[%s], field[%s] is not present on entity",
                        inventoryClass.getName(), info.entityClass.getName(), attr));

                return formatSql(entityName, attr, op);
            } else {
                entityField = inventoryField;
                JoinColumn jc = at.joinColumn();
                String refName = jc.referencedColumnName();
                DebugUtils.Assert(!"".equals(refName), String.format("referencedColumnName of JoinColumn of field[%s] on inventory class[%s] cannot be empty string",
                        inventoryField.getName(), inventoryClass.getName()));
                String foreignKey = jc.name();
                DebugUtils.Assert(!"".equals(foreignKey), String.format("name of JoinColumn of field[%s] on inventory class[%s] cannot be empty string",
                        inventoryField.getName(), inventoryClass.getName()));
                Class mappingInvClass = at.mappingClass();
                Inventory mappingInvAt = (Inventory) mappingInvClass.getAnnotation(Inventory.class);
                DebugUtils.Assert(mappingInvAt != null, String.format("Mapping inventory class[%s] of inventory class[%s] is not annotated by @Inventory", mappingInvClass.getName(), inventoryClass.getName()));
                Class foreignVOClass = mappingInvAt.mappingVOClass();
                DebugUtils.Assert(FieldUtils.hasField(refName, foreignVOClass), String.format("referencedColumnName of JoinColumn of field[%s] on inventory class[%s] is invalid, class[%s] doesn't have field[%s]",
                        inventoryField.getName(), inventoryClass.getName(), foreignVOClass.getName(), refName));
                DebugUtils.Assert(FieldUtils.hasField(foreignKey, foreignVOClass), String.format("name of JoinColumn of field[%s] on inventory class[%s] is invalid, class[%s] doesn't have field[%s]",
                        inventoryField.getName(), inventoryClass.getName(), foreignVOClass.getName(), foreignKey));

                Map<String, String> var = new HashMap();
                var.put("entity", entityName);
                var.put("primaryKey", info.primaryKey);
                var.put("subEntity", foreignVOClass.getSimpleName().toLowerCase());
                var.put("foreignKey", foreignKey);
                var.put("foreignVO", foreignVOClass.getSimpleName());

                if (QueryOp.NOT_IN.equals(op)) {
                    // NOT_IN needs special handle
                    op = QueryOp.IN.toString();
                    var.put("condition", formatSql(foreignVOClass.getSimpleName().toLowerCase(), refName, op));
                    return s("{entity}.{primaryKey} not in (select {subEntity}.{foreignKey} from {foreignVO} {subEntity} where {condition})").formatByMap(var);
                } else {
                    var.put("condition", formatSql(foreignVOClass.getSimpleName().toLowerCase(), refName, op));
                    return s("{entity}.{primaryKey} in (select {subEntity}.{foreignKey} from {foreignVO} {subEntity} where {condition})").formatByMap(var);
                }
            }
        }
    }

    private class QueryObject {
        EntityInfo info;
        List<MetaCondition> conditions = new ArrayList<MetaCondition>();
        QueryObject parent;
        List<QueryObject> children = new ArrayList<QueryObject>();
        SubQueryInfo subQueryInfo;
        APIQueryMessage msg;

        // NOTE: we hard code tag specific logic here because we think current query model is not sustainable,
        // it worth nothing to waste effort on making this as extension point; we will switch the entire
        // query framework to ANTLR based DSL in next version.
        class TagSqlBuilder {
            List<String> IN_CONDITIONS;
            List<String> NOT_IN_CONDITIONS;

            {
                IN_CONDITIONS = CollectionDSL.list(
                        QueryOp.EQ.toString(),
                        QueryOp.GT.toString(),
                        QueryOp.GT_AND_EQ.toString(),
                        QueryOp.LT.toString(),
                        QueryOp.LT_AND_EQ.toString(),
                        QueryOp.IN.toString(),
                        QueryOp.LIKE.toString(),
                        QueryOp.NOT_NULL.toString()
                );

                NOT_IN_CONDITIONS = CollectionDSL.list(
                        QueryOp.NOT_EQ.toString(),
                        QueryOp.NOT_IN.toString(),
                        QueryOp.IS_NULL.toString(),
                        QueryOp.NOT_LIKE.toString()
                );
            }

            private String reverseOpIfNeed(QueryCondition cond) {
                if (QueryOp.NOT_EQ.equals(cond.getOp())) {
                    return QueryOp.EQ.toString();
                } else if (QueryOp.NOT_IN.equals(cond.getOp())) {
                    return QueryOp.IN.toString();
                } else if (QueryOp.IS_NULL.equals(cond.getOp())) {
                    return QueryOp.NOT_NULL.toString();
                } else if (QueryOp.NOT_LIKE.equals(cond.getOp())) {
                    return QueryOp.LIKE.toString();
                } else {
                    return cond.getOp();
                }
            }

            private String buildCondition(String field, QueryCondition cond) {
                if (QueryOp.IN.equals(cond.getOp()) || QueryOp.NOT_IN.equals(cond.getOp())) {
                    String[] values = cond.getValue().split(",");
                    List<String> vals = new ArrayList<String>();
                    for (String val : values) {
                        vals.add(String.format("'%s'", val));
                    }

                    return String.format("%s %s (%s)", field, reverseOpIfNeed(cond), StringUtils.join(vals, ","));
                } else if (QueryOp.IS_NULL.equals(cond.getOp()) || QueryOp.NOT_NULL.equals(cond.getOp())) {
                    return String.format("%s %s", field, reverseOpIfNeed(cond));
                } else {
                    return String.format("%s %s '%s'", field, reverseOpIfNeed(cond), cond.getValue());
                }
            }

            private String chooseOp(QueryCondition cond) {
                if (IN_CONDITIONS.contains(cond.getOp())) {
                    return "in";
                }

                if (NOT_IN_CONDITIONS.contains(cond.getOp())) {
                    return "not in";
                }

                throw new CloudRuntimeException(String.format("invalid comparison operator[%s]; %s", cond.getOp(), JSONObjectUtil.toJsonString(cond)));
            }

            private List<String> getAllResourceTypesForTag() {
                List<String> types = new ArrayList<String>();
                Class c = info.entityClass;
                while (c != Object.class) {
                    types.add(String.format("'%s'", c.getSimpleName()));
                    c = c.getSuperclass();
                }
                return types;
            }

            String toJpql() {
                List<String> resultQuery = new ArrayList<>();
                List<String> rtypes = getAllResourceTypesForTag();
                String primaryKey = info.primaryKey;
                String invname = info.inventoryClass.getSimpleName().toLowerCase();

                List<QueryCondition> conds = transformAndRemoveNull(conditions,
                        arg -> USER_TAG.equals(arg.attr) || SYSTEM_TAG.equals(arg.attr) ? arg.toQueryCondtion() : null);

                String typeString = StringUtils.join(rtypes, ",");
                for (QueryCondition cond : conds) {
                    if (cond.getName().equals(USER_TAG)) {
                        List<String> condStrs = new ArrayList<String>();
                        condStrs.add(buildCondition("user.tag", cond));
                        condStrs.add(String.format("user.resourceType in (%s)", typeString));
                        resultQuery.add(String.format("%s.%s %s (select user.resourceUuid from UserTagVO user where %s)",
                                invname, primaryKey, chooseOp(cond), StringUtils.join(condStrs, " and ")));
                    } else if (cond.getName().equals(SYSTEM_TAG)) {
                        List<String> condStrs = new ArrayList<String>();
                        condStrs.add(buildCondition("sys.tag", cond));
                        condStrs.add(String.format("sys.resourceType in (%s)", typeString));
                        resultQuery.add(String.format("%s.%s %s (select sys.resourceUuid from SystemTagVO sys where %s)",
                                invname, primaryKey, chooseOp(cond), StringUtils.join(condStrs, " and ")));
                    }
                }

                if (resultQuery.isEmpty()) {
                    return null;
                } else {
                    return StringUtils.join(resultQuery, " and ");
                }
            }
        }

        String toJpql(boolean isCount) {
            List<String> where = new ArrayList<String>();

            boolean hasTag = false;

            int index = 0;
            for (MetaCondition it : conditions) {
                if (USER_TAG.equals(it.attr) || SYSTEM_TAG.equals(it.attr)) {
                    hasTag = true;
                    continue;
                }

                // use an index to differentiate multiple conditions with the same name
                it.index = index++;
                where.add(it.toJpql());
            }

            //conditions = tmpConditions;
            if (hasTag) {
                where.add(new TagSqlBuilder().toJpql());
            }

            for (QueryObject it : children) {
                where.add(it.toJpql(false));
            }

            if (parent != null) {
                // this is a sub query
                if (subQueryInfo instanceof InherentSubQuery) {
                    InherentSubQuery isub = (InherentSubQuery) subQueryInfo;
                    JoinColumn jc = isub.at.joinColumn();
                    String foreignKey = jc.name();
                    DebugUtils.Assert(!"".equals(foreignKey), String.format("name of JoinColumn of field[%s] on inventory class[%s] cannot be empty string",
                            isub.parentField.getName(), parent.info.inventoryClass.getName()));
                    Class foreignVOClass = info.entityClass;
                    DebugUtils.Assert(FieldUtils.hasField(foreignKey, foreignVOClass), String.format("name of JoinColumn of field[%s] on inventory class[%s] is invalid, class[%s] doesn't have field[%s]",
                            isub.parentField.getName(), parent.info.inventoryClass.getName(), foreignVOClass.getName(), foreignKey));

                    Map<String, String> var = new HashMap<String, String>();
                    var.put("entity", parent.info.inventoryClass.getSimpleName().toLowerCase());
                    var.put("primaryKey", parent.info.primaryKey);
                    var.put("subEntity", info.inventoryClass.getSimpleName().toLowerCase());
                    var.put("foreignKey", foreignKey);
                    var.put("foreignVO", foreignVOClass.getSimpleName());
                    if (where.isEmpty()) {
                        return s("{entity}.{primaryKey} in (select {subEntity}.{foreignKey} from {foreignVO} {subEntity})").formatByMap(var);
                    } else {
                        var.put("condition", StringUtils.join(where, " and ").trim());
                        return s("{entity}.{primaryKey} in (select {subEntity}.{foreignKey} from {foreignVO} {subEntity} where {condition})").formatByMap(var);
                    }
                } else if (subQueryInfo instanceof ExpandedSubQuery) {
                    ExpandedSubQuery esub = (ExpandedSubQuery) subQueryInfo;
                    Inventory joinAt = (Inventory) esub.struct.getInventoryClass().getAnnotation(Inventory.class);
                    Class joinVO = joinAt.mappingVOClass();
                    Map<String, String> var = new HashMap<String, String>();
                    var.put("entity", parent.info.inventoryClass.getSimpleName().toLowerCase());
                    var.put("foreignKey", esub.struct.getForeignKey());
                    var.put("expandedEntity", esub.struct.getInventoryClass().getSimpleName().toLowerCase());
                    var.put("expandedVO", joinVO.getSimpleName());
                    var.put("expandedKey", esub.struct.getExpandedInventoryKey());

                    if (where.isEmpty()) {
                        return s("{entity}.{foreignKey} in (select {expandedEntity}.{expandedKey} from {expandedVO} {expandedEntity})").formatByMap(var);
                    } else {
                        var.put("condition", StringUtils.join(where, " and ").trim());
                        return s("{entity}.{foreignKey} in (select {expandedEntity}.{expandedKey} from {expandedVO} {expandedEntity} where {condition})").formatByMap(var);
                    }
                }

                throw new CloudRuntimeException("cannot be here");
            } else {
                // this is root query
                for (MysqlQuerySubQueryExtension ext : subQueryExtensions) {
                    String sub = ext.makeSubquery(msg, info.inventoryClass);
                    if (sub != null) {
                        where.add(sub);
                    }
                }

                String entityName = info.inventoryClass.getSimpleName().toLowerCase();
                String entity = info.entityClass.getSimpleName();
                String condition = StringUtils.join(where, " and ").trim();
                if (isCount) {
                    if (where.isEmpty()) {
                        return String.format("select count(%s) from %s %s", entityName, entity, entityName);
                    } else {
                        return String.format("select count(%s) from %s %s where %s", entityName, entity, entityName, condition);
                    }
                } else {
                    String ret = null;
                    String selector = null;
                    if (msg.isFieldQuery()) {
                        List<String> ss = new ArrayList<String>();
                        for (String f : msg.getFields()) {
                            ss.add(String.format("%s.%s", entityName, f));
                        }
                        selector = StringUtils.join(ss, ",");
                    } else {
                        selector = entityName;
                    }

                    if (where.isEmpty()) {
                        ret = String.format("select %s from %s %s", selector, entity, entityName);
                    } else {
                        ret = String.format("select %s from %s %s where %s", selector, entity, entityName, condition);
                    }

                    if (msg.getSortBy() != null) {
                        if (!FieldUtils.hasField(msg.getSortBy(), info.entityClass)) {
                            throw new IllegalArgumentException(String.format("illegal sortBy[%s], entity[%s] doesn't have this field", msg.getSortBy(), info.entityClass.getName()));
                        }

                        ret = String.format("%s order by %s.%s %s", ret, entityName, msg.getSortBy(), msg.getSortDirection().toUpperCase());
                    }

                    if (msg.getGroupBy() != null) {
                        if (!FieldUtils.hasField(msg.getGroupBy(), info.entityClass)) {
                            throw new IllegalArgumentException(String.format("illegal groupBy[%s], entity[%s] doesn't have this field", msg.getGroupBy(), info.entityClass.getName()));
                        }

                        ret = String.format("%s group by %s", ret, msg.getGroupBy());
                    }

                    return ret;
                }
            }
        }
    }

    private class QueryContext {
        private APIQueryMessage msg;
        private Class inventoryClass;
        private QueryObject root;
        private Map<Class, QueryObject> tmpMap = new HashMap<Class, QueryObject>();

        private MetaCondition buildCondition(QueryCondition qcond, EntityInfo info) {
            MetaCondition mcond = new MetaCondition();
            mcond.attr = qcond.getName();
            mcond.op = qcond.getOp();
            mcond.inventoryClass = info.inventoryClass;
            mcond.value = qcond.getValue();
            return mcond;
        }

        private void buildSubQuery(QueryCondition qcond, QueryObject parent) {
            String[] slices = qcond.getName().split("\\.");
            String currentFieldName = slices[0];
            Class parentInvClass = parent.info.inventoryClass;
            DebugUtils.Assert(parentInvClass != null, String.format("parent inventory class cannot be null. Parent entity class[%s]", parent.info.entityClass.getName()));

            SubQueryInfo subQueryInfo = null;
            ExpandedQueryStruct expandedQuery = parent.info.expandedQueries.get(currentFieldName);
            if (expandedQuery == null) {
                // try finding alias
                ExpandedQueryAliasInfo alias = parent.info.aliases.get(currentFieldName);
                if (alias != null) {
                    QueryCondition ncond = new QueryCondition();
                    ncond.setName(qcond.getName().replaceFirst(alias.alias, alias.expandField));
                    ncond.setOp(qcond.getOp());
                    ncond.setValue(qcond.getValue());
                    buildSubQuery(ncond, parent);
                    return;
                }
            }

            if (expandedQuery != null) {
                // an expanded query
                ExpandedSubQuery esub = new ExpandedSubQuery();
                esub.struct = expandedQuery;
                esub.joinInventoryClass = expandedQuery.getInventoryClass();
                subQueryInfo = esub;
            } else {
                Field currentField = FieldUtils.getField(currentFieldName, parentInvClass);

                DebugUtils.Assert(currentField != null, String.format("cannot find field[%s] on class[%s], wrong subquery name[%s]",
                        currentFieldName, parentInvClass.getName(), qcond.getName()));
                InherentSubQuery isub = new InherentSubQuery();
                Queryable at = currentField.getAnnotation(Queryable.class);
                DebugUtils.Assert(at != null, String.format("nested query field[%s] on inventory[%s] must be annotated as @Queryable", currentFieldName, parentInvClass.getName()));
                isub.at = at;
                isub.joinInventoryClass = at.mappingClass();
                isub.parentField = currentField;
                DebugUtils.Assert(isub.joinInventoryClass.isAnnotationPresent(Inventory.class),
                        String.format("field[%s] of inventory[%s] can only be type of Collection whose generic type is inventory class or a object whose type is inventory class. Current class is %s which is not annotated by @Inventory",
                                currentField.getName(), parentInvClass.getName(), isub.joinInventoryClass.getName())
                );
                subQueryInfo = isub;
            }

            EntityInfo info = entityInfos.get(subQueryInfo.joinInventoryClass);
            QueryObject qobj = tmpMap.get(info.entityClass);
            if (qobj == null) {
                qobj = new QueryObject();
                qobj.info = info;
                qobj.parent = parent;
                qobj.subQueryInfo = subQueryInfo;
                qobj.msg = msg;
                parent.children.add(qobj);
                tmpMap.put(info.entityClass, qobj);
            }

            slices = Arrays.copyOfRange(slices, 1, slices.length);
            String subFieldName = StringUtils.join(slices, ".");

            QueryCondition ncond = new QueryCondition();
            ncond.setName(subFieldName);
            ncond.setOp(qcond.getOp());
            ncond.setValue(qcond.getValue());

            if (!subFieldName.contains(".")) {
                qobj.conditions.add(buildCondition(ncond, info));
            } else {
                buildSubQuery(ncond, qobj);
            }
        }

        private void buildMetaCondition(QueryCondition qcond, EntityInfo info, boolean skipInventoryCheck) {
            QueryObject qobj = tmpMap.get(info.entityClass);
            if (qobj == null) {
                qobj = new QueryObject();
                qobj.info = info;
                tmpMap.put(info.entityClass, qobj);
            }

            MetaCondition mcond = buildCondition(qcond, info);
            mcond.skipInventoryCheck = skipInventoryCheck;
            qobj.conditions.add(mcond);
        }

        private void buildMetaCondition(QueryCondition qcond, EntityInfo info) {
            buildMetaCondition(qcond, info, false);
        }

        private String build(boolean isCount) {
            root = new QueryObject();
            root.msg = msg;
            root.info = entityInfos.get(inventoryClass);
            DebugUtils.Assert(root.info != null, String.format("class[%s] is not annotated by @Inventory", inventoryClass.getName()));
            tmpMap.put(root.info.entityClass, root);

            for (QueryCondition qcond : msg.getConditions()) {
                if (escapeConditionNames.contains(qcond.getName())) {
                    continue;
                }

                if (!qcond.getName().contains(".")) {
                    buildMetaCondition(qcond, root.info);
                } else {
                    buildSubQuery(qcond, root);
                }
            }

            List<AddExtraConditionToQueryExtensionPoint> exts = extraConditionsExts.get(msg.getClass());
            if (exts != null) {
                for (AddExtraConditionToQueryExtensionPoint ext : exts) {
                    try {
                        for (QueryCondition cond : ext.getExtraQueryConditionForMessage(msg)) {
                            buildMetaCondition(cond, root.info, true);
                        }
                    } catch (Throwable t) {
                        logger.warn(String.format("unhandled exception when calling %s", ext.getClass().getName()), t);
                    }
                }
            }

            return root.toJpql(isCount);
        }

        private void setQueryValue(Query q, QueryObject qobj) {
            for (MetaCondition mcond : qobj.conditions) {
                if (USER_TAG.equals(mcond.attr) || SYSTEM_TAG.equals(mcond.attr)) {
                    continue;
                }

                Object val = mcond.normalizeValue();
                if (val != null) {
                    q.setParameter(mcond.attrValueName, val);
                }
            }
            for (QueryObject child : qobj.children) {
                setQueryValue(q, child);
            }
        }


        public List convertVOsToInventories(final List vos) {
            try {
                if (vos.isEmpty()) {
                    return new ArrayList();
                }

                if (root.info.children.isEmpty()) {
                    return (List) root.info.inventoryCollectionValueOf.invoke(inventoryClass, vos);
                }

                final LinkedHashMap flatMap = new LinkedHashMap();
                final List primaryKeysNeedResolve = new ArrayList();
                for (Object vo : vos) {
                    String type = (String) root.info.entityTypeField.get(vo);
                    Object priKey = root.info.getPrimaryKeyValue(vo);
                    if (!root.info.flatTypeEntityMap.containsKey(type)) {
                        flatMap.put(priKey, root.info.inventoryValueOf.invoke(inventoryClass, vo));
                    } else {
                        flatMap.put(priKey, null);
                        primaryKeysNeedResolve.add(priKey);
                    }

                }

                if (primaryKeysNeedResolve.isEmpty()) {
                    return (List) root.info.inventoryCollectionValueOf.invoke(inventoryClass, vos);
                }

                // the inventory has child inventory inheriting it, we have to find out all child inventory and
                // reload them from DB and keep them in order.
                class SubInventoryResolver {

                    class SQL {
                        String sql;
                        EntityInfo entityInfo;
                    }

                    List<SQL> subInventoryQuerySQL = new ArrayList<SQL>();

                    List resolve() throws InvocationTargetException, IllegalAccessException {
                        buildSubInventoryQuerySQL(root.info.children);
                        querySubInventory();

                        List result = new ArrayList(flatMap.values().size());
                        result.addAll(flatMap.values());
                        return result;
                    }

                    @Transactional(readOnly = true)
                    private void querySubInventory() throws InvocationTargetException, IllegalAccessException {
                        for (SQL sql : subInventoryQuerySQL) {
                            if (primaryKeysNeedResolve.isEmpty()) {
                                break;
                            }

                            TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql.sql, Tuple.class);
                            q.setParameter("ids", primaryKeysNeedResolve);
                            List<Tuple> res = q.getResultList();

                            for (Tuple t : res) {
                                Object priKey = t.get(0);
                                Object vo = t.get(1);
                                flatMap.put(priKey, sql.entityInfo.inventoryValueOf.invoke(sql.entityInfo.inventoryClass, vo));
                                primaryKeysNeedResolve.remove(priKey);
                            }
                        }
                    }

                    private void buildSubInventoryQuerySQL(List<EntityInfo> infos) {
                        // child queries execute first
                        for (EntityInfo info : infos) {
                            if (!info.children.isEmpty()) {
                                buildSubInventoryQuerySQL(info.children);
                            }
                        }

                        for (EntityInfo info : infos) {
                            SQL sql = new SQL();
                            sql.entityInfo = info;
                            sql.sql = String.format("select e.%s, e from %s e where e.%s in (:ids)", info.primaryKey, info.entityClass.getSimpleName(), info.primaryKey);
                            subInventoryQuerySQL.add(sql);
                        }
                    }
                }

                return new SubInventoryResolver().resolve();
            } catch (Exception e) {
                throw new CloudRuntimeException(e);
            }
        }

        private void validateFields() {
            EntityInfo info = entityInfos.get(inventoryClass);
            for (String f : msg.getFields()) {
                if (!info.premitiveFieldNames.contains(f)) {
                    throw new OperationFailureException(argerr("field[%s] is not a primitive of the inventory %s; you cannot specify it in the parameter 'fields';" +
                                    "valid fields are %s", f, info.inventoryClass.getSimpleName(), info.premitiveFieldNames));
                }
            }
        }

        private List convertFieldsTOPartialInventories(List fieldTuple) {
            if (fieldTuple.isEmpty()) {
                return new ArrayList();
            }

            EntityInfo info = entityInfos.get(inventoryClass);
            List ret = new ArrayList(fieldTuple.size());
            for (Object t : fieldTuple) {
                Tuple tuple = (Tuple) t;
                Object inv = info.objectInstantiator.newInstance();
                for (int i = 0; i < msg.getFields().size(); i++) {
                    String fname = msg.getFields().get(i);
                    Object value = tuple.get(i);
                    Field f = info.allFieldsMap.get(fname);
                    try {
                        if (value != null && String.class.isAssignableFrom(f.getType())) {
                            value = value.toString();
                        }
                        f.set(inv, value);
                    } catch (IllegalAccessException e) {
                        throw new CloudRuntimeException(e);
                    }
                }

                ret.add(inv);
            }

            return ret;
        }

        @Transactional(readOnly = true)
        List query() {
            if (msg.isFieldQuery()) {
                validateFields();
            }

            String jpql = build(false);
            Query q = msg.isFieldQuery() ? dbf.getEntityManager().createQuery(jpql, Tuple.class) : dbf.getEntityManager().createQuery(jpql);

            if (logger.isTraceEnabled()) {
                org.hibernate.Query hq = q.unwrap(org.hibernate.Query.class);
                logger.trace(hq.getQueryString());
            }
            setQueryValue(q, root);
            if (msg.getLimit() != null) {
                q.setMaxResults(msg.getLimit());
            }
            if (msg.getStart() != null) {
                q.setFirstResult(msg.getStart());
            }

            List vos = q.getResultList();

            if (msg.isFieldQuery()) {
                return convertFieldsTOPartialInventories(vos);
            } else {
                return convertVOsToInventories(vos);
            }
        }

        @Transactional(readOnly = true)
        long count() {
            String jpql = build(true);
            Query q = dbf.getEntityManager().createQuery(jpql);
            if (logger.isTraceEnabled()) {
                org.hibernate.Query hq = q.unwrap(org.hibernate.Query.class);
                logger.trace(hq.getQueryString());
            }
            setQueryValue(q, root);
            return (Long) q.getSingleResult();
        }
    }

    private void populateExtensions() {
        subQueryExtensions.addAll(pluginRgty.getExtensionList(MysqlQuerySubQueryExtension.class));
        for (MysqlQuerySubQueryExtension ext : subQueryExtensions) {
            if (ext.getEscapeConditionNames() != null) {
                escapeConditionNames.addAll(ext.getEscapeConditionNames());
            }
        }

        for (AddExpandedQueryExtensionPoint ext : pluginRgty.getExtensionList(AddExpandedQueryExtensionPoint.class)) {
            List<ExpandedQueryStruct> expandedQueries = ext.getExpandedQueryStructs();
            if (expandedQueries != null) {
                for (ExpandedQueryStruct s : expandedQueries) {
                    List<ExpandedQueryStruct> exts = expandedQueryStructs.get(s.getInventoryClassToExpand());
                    if (exts == null) {
                        exts = new ArrayList<ExpandedQueryStruct>();
                        expandedQueryStructs.put(s.getInventoryClassToExpand(), exts);
                    }

                    exts.add(s);
                }
            }

            List<ExpandedQueryAliasStruct> aliases = ext.getExpandedQueryAliasesStructs();
            if (aliases != null) {
                for (ExpandedQueryAliasStruct as : aliases) {
                    ExpandedQueryAliasInfo info = new ExpandedQueryAliasInfo();
                    info.isFromAnnotation = false;
                    info.inventoryClassDefiningThisAlias = as.getInventoryClass();
                    info.queryMessageClass = inventoryQueryMessageMap.get(info.inventoryClassDefiningThisAlias);
                    DebugUtils.Assert(info.queryMessageClass != null, String.format("AddExpandedQueryExtensionPoint[%s] defines an expanded query alias[%s], but no query message declares inventory class[%s] to which the alias maps",
                            ext.getClass().getName(), as.getAlias(), as.getInventoryClass()));
                    info.expandField = as.getExpandedField();
                    info.alias = as.getAlias();

                    List<ExpandedQueryAliasInfo> infos = aliasInfos.get(info.queryMessageClass);
                    if (infos == null) {
                        infos = new ArrayList<ExpandedQueryAliasInfo>();
                        aliasInfos.put(info.queryMessageClass, infos);
                    }

                    infos.add(info);
                }
            }
        }

        for (AddExtraConditionToQueryExtensionPoint ext : pluginRgty.getExtensionList(AddExtraConditionToQueryExtensionPoint.class)) {
            for (Class clz : ext.getMessageClassesForAddExtraConditionToQueryExtensionPoint()) {
                List<AddExtraConditionToQueryExtensionPoint> exts = extraConditionsExts.get(clz);
                if (exts == null) {
                    exts = new ArrayList<AddExtraConditionToQueryExtensionPoint>();
                    extraConditionsExts.put(clz, exts);
                }
                exts.add(ext);
            }
        }
    }

    private void buildExpandedQueryAliasInfo() {
        Set<Class<?>> invClasses = BeanUtils.reflections.getTypesAnnotatedWith(Inventory.class);

        for (Class invClass : invClasses) {
            ExpandedQueryAliases aliases = (ExpandedQueryAliases) invClass.getAnnotation(ExpandedQueryAliases.class);
            if (aliases == null) {
                continue;
            }

            for (ExpandedQueryAlias alias : aliases.value()) {
                Class aliasInventoryClass = alias.target() == Object.class ? invClass : alias.target();

                ExpandedQueryAliasInfo info = new ExpandedQueryAliasInfo();
                info.alias = alias.alias();
                info.expandField = alias.expandedField();
                info.queryMessageClass = inventoryQueryMessageMap.get(aliasInventoryClass);
                info.inventoryClassDefiningThisAlias = aliasInventoryClass;
                info.isFromAnnotation = true;
                if (info.queryMessageClass == null) {
                    throw new CloudRuntimeException(String.format("inventory[%s] declares expanded query alias, but not query message declare this inventory class in AutoQuery annotation",
                            aliasInventoryClass.getName()));
                }

                List<ExpandedQueryAliasInfo> lst = aliasInfos.computeIfAbsent(info.queryMessageClass, k -> new ArrayList<>());
                lst.add(info);
            }
        }
    }

    @Override
    public boolean start() {
        try {
            BeanUtils.reflections.getSubTypesOf(APIQueryMessage.class).forEach(msgClass -> {
                AutoQuery at = msgClass.getAnnotation(AutoQuery.class);
                if (at == null) {
                    logger.warn(String.format("query message[%s] doesn't have AutoQuery annotation, expanded query alias would not take effect", msgClass.getName()));
                    return;
                }

                inventoryQueryMessageMap.put(at.inventoryClass(), msgClass);
            });

            // NOTE: don't change the order
            populateExtensions();
            buildExpandedQueryAliasInfo();
            populateEntityInfo();
            completeAliasInfo();
            inheritExpandedQueryAndAliases();
            removeSuppressedExpandedQuery();
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }

        return true;
    }

    private void removeSuppressedExpandedQuery() {
        for (EntityInfo info : entityInfos.values()) {
            Map<String, ExpandedQueryStruct> ess = new HashMap<String, ExpandedQueryStruct>();
            ess.putAll(info.expandedQueries);
            for (Entry<String, ExpandedQueryStruct> e : ess.entrySet()) {
                if (e.getValue().getSuppressedInventoryClass() != null) {
                    ExpandedQueryStruct toSuppressed = null;
                    for (ExpandedQueryStruct s : info.expandedQueries.values()) {
                        if (s.getInventoryClass() == e.getValue().getSuppressedInventoryClass()) {
                            toSuppressed = s;
                            break;
                        }
                    }

                    DebugUtils.Assert(toSuppressed != null, String.format("ExpandedQuery[%s] of %s is going to suppress a undefined ExpandedQuery that has inventory class[%s]",
                            e.getValue().getExpandedField(), info.inventoryClass, e.getValue().getSuppressedInventoryClass()));

                    info.expandedQueries.remove(toSuppressed.getExpandedField());
                    logger.debug(String.format("ExpandedQuery[%s] of %s suppresses ExpandedQuery[%s]",
                            e.getValue().getExpandedField(), info.inventoryClass, toSuppressed.getExpandedField()));
                }
            }
        }
    }

    private void completeAliasInfo() {
        class ExpandedQueryAliasInfoCompletion {
            ExpandedQueryAliasInfo alias;

            ExpandedQueryAliasInfoCompletion(ExpandedQueryAliasInfo alias) {
                this.alias = alias;
            }

            private ExpandedQueryStruct findTargetExpandedQueryStruct(String[] expandedFields, EntityInfo entityInfo) {
                ExpandedQueryStruct struct = null;
                for (String exf : expandedFields) {
                    struct = entityInfo.expandedQueries.get(exf);
                    if (struct == null) {
                        ExpandedQueryAliasInfo exalias = entityInfo.aliases.get(exf);
                        DebugUtils.Assert(exalias != null, String.format("cannot find expanded query or alias[%s] on %s", exf, entityInfo.inventoryClass));
                        struct = findTargetExpandedQueryStruct(exalias.expandField.split("\\."), entityInfo);
                    }
                    entityInfo = entityInfos.get(struct.getInventoryClass());
                }
                return struct;
            }

            void complete() {
                EntityInfo entityInfo = entityInfos.get(alias.inventoryClassDefiningThisAlias);
                String[] expandedFields = alias.expandField.split("\\.");
                DebugUtils.Assert(expandedFields.length != 0, String.format("alias[%s] defined in %s is invalid", alias.expandField, alias.inventoryClassDefiningThisAlias));
                ExpandedQueryStruct struct = findTargetExpandedQueryStruct(expandedFields, entityInfo);
                alias.inventoryClass = struct.getInventoryClass();
                alias.check();
            }
        }

        for (List<ExpandedQueryAliasInfo> infos : aliasInfos.values()) {
            for (ExpandedQueryAliasInfo alias : infos) {
                new ExpandedQueryAliasInfoCompletion(alias).complete();
            }
        }
    }

    private void inheritExpandedQueryAndAliases(EntityInfo current, EntityInfo ancestor) {
        if (ancestor == null) {
            return;
        }

        if (!ancestor.aliases.isEmpty()) {
            Class msgClz = inventoryQueryMessageMap.get(current.inventoryClass);
            List<ExpandedQueryAliasInfo> aliases = aliasInfos.get(msgClz);
            if (aliases == null) {
                aliases = new ArrayList<ExpandedQueryAliasInfo>();
            }
            aliases.addAll(ancestor.aliases.values());

            current.addQueryAliases(aliases);
        }

        if (!ancestor.expandedQueries.isEmpty()) {
            current.expandedQueries.putAll(ancestor.expandedQueries);
        }

        inheritExpandedQueryAndAliases(current, ancestor.parent);
    }

    private void inheritExpandedQueryAndAliases() {
        for (EntityInfo info : entityInfos.values()) {
            inheritExpandedQueryAndAliases(info, info.parent);
        }
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public <T> List<T> query(APIQueryMessage msg, Class<T> inventoryClass) {
        QueryContext context = new QueryContext();
        context.msg = msg;
        context.inventoryClass = selectInventoryClass(msg, inventoryClass);

        return context.query();
    }

    private Class selectInventoryClass(APIQueryMessage msg, Class inventoryClass) {
        EntityInfo info = entityInfos.get(inventoryClass);
        return info.selectInventoryClass(msg);
    }

    @Override
    public long count(APIQueryMessage msg, Class inventoryClass) {
        QueryContext context = new QueryContext();
        context.msg = msg;
        context.inventoryClass = selectInventoryClass(msg, inventoryClass);
        return context.count();
    }

    @Override
    public Map<String, List<String>> populateQueryableFields() {
        //throw new CloudRuntimeException("it's impossible enumerate all combinations");
        Map<String, List<String>> ret = new HashMap<>();

        class QueryableBuilder {
            private Class inventoryClass;
            private List<String> queryableFields = new ArrayList<>();
            private EntityInfo info;

            private Stack<Class> visitedPath = new Stack<>();

            private QueryableBuilder() {
            }

            private QueryableBuilder(Stack<Class> path) {
                visitedPath = path;
            }

            private List<String> build() {
                info = entityInfos.get(inventoryClass);
                if (!visitedPath.contains(inventoryClass)) {
                    visitedPath.push(inventoryClass);
                    buildExpandedQueryableFields();
                    buildInherentQueryableFields();
                    normalizeToAliases();
                    visitedPath.pop();
                }

                return queryableFields;
            }

            private String normalizeToAlias(String fieldName) {
                for (ExpandedQueryAliasInfo alias : info.aliases.values()) {
                    if (fieldName.startsWith(String.format("%s.", alias.expandField))) {
                        return fieldName.replaceFirst(alias.expandField, alias.alias);
                    }
                }

                return fieldName;
            }

            private void normalizeToAliases() {
                Set<String> set = new HashSet<>();
                for (String ret : queryableFields) {
                    set.add(normalizeToAlias(ret));
                }

                queryableFields.clear();
                queryableFields.addAll(set);
            }

            private void buildExpandedQueryableFields() {
                for (ExpandedQueryStruct struct : info.expandedQueries.values()) {
                    //QueryableBuilder nbuilder = new QueryableBuilder(inherentPath, expandedPath);
                    QueryableBuilder nbuilder = new QueryableBuilder(visitedPath);
                    nbuilder.inventoryClass = struct.getInventoryClass();
                    List<String> expandedFields = nbuilder.build();
                    for (String ef : expandedFields) {
                        String ff = String.format("%s.%s", struct.getExpandedField(), ef);
                        //logger.debug(ff);
                        queryableFields.add(ff);
                    }
                }
            }

            private void buildInherentQueryableFields() {
                for (Field field : info.allFieldsMap.values()) {
                    if (field.isAnnotationPresent(APINoSee.class)) {
                        continue;
                    }
                    if (field.isAnnotationPresent(Unqueryable.class)) {
                        continue;
                    }
                    if (TypeUtils.isZstackBeanPrimitive(field.getType())) {
                        queryableFields.add(field.getName());
                        continue;
                    }
                    if (Map.class.isAssignableFrom(field.getType())) {
                        logger.warn(String.format("%s.%s is Map type, not support", field.getDeclaringClass(), field.getName()));
                        continue;
                    }

                    if (Collection.class.isAssignableFrom(field.getType()) && field.isAnnotationPresent(Queryable.class)) {
                        FieldUtils.CollectionGenericType gtype = (FieldUtils.CollectionGenericType) FieldUtils.inferGenericTypeOnMapOrCollectionField(field);
                        if (!gtype.isInferred()) {
                            throw new CloudRuntimeException(String.format("unable infer generic type of %s.%s", field.getDeclaringClass(), field.getName()));
                        }
                        if (gtype.getNestedGenericValue() != null) {
                            throw new CloudRuntimeException(String.format("%s.%s is nested Collection, not support", field.getDeclaringClass(), field.getName()));
                        }
                        if (TypeUtils.isZstackBeanPrimitive(gtype.getValueType())) {
                            queryableFields.add(field.getName());
                            continue;
                        }
                        Class nestedInventory = gtype.getValueType();
                        if (!nestedInventory.isAnnotationPresent(Inventory.class)) {
                            throw new CloudRuntimeException(String.format("field[%s] on inventory class[%s] is collection type with @Queryable, but its generic type[%s] is not an inventory class",
                                    field.getName(), inventoryClass.getName(), nestedInventory.getName()));
                        }

                        QueryableBuilder nbuilder = new QueryableBuilder(visitedPath);
                        nbuilder.inventoryClass = nestedInventory;
                        List<String> nestedFields = nbuilder.build();
                        for (String nf : nestedFields) {
                            queryableFields.add(String.format("%s.%s", field.getName(), nf));
                        }
                    }
                }
            }
        }

        for (Map.Entry<Class, Class> e : inventoryQueryMessageMap.entrySet()) {
            QueryableBuilder builder = new QueryableBuilder();
            builder.inventoryClass = e.getKey();
            List<String> queryables = builder.build();
            ret.put(e.getValue().getName(), queryables);
        }


        LinkedHashMap<String, List<String>> orderedRet = new LinkedHashMap<>();
        // order
        List<String> keys = new ArrayList<>();
        keys.addAll(ret.keySet());
        Collections.sort(keys);
        for (String k : keys) {
            List<String> lst = ret.get(k);
            Collections.sort(lst);
            orderedRet.put(k, lst);
        }

        return orderedRet;
    }


    @Override
    public void writePython(final StringBuilder sb) {
        class PythonQueryObjectWriter {

            private String makeClassName(Class clazz) {
                return String.format("QueryObject%s", clazz.getSimpleName());
            }

            private void write() {
                sb.append("\n#QueryObjectInventory");
                Set<String> objectNameHavingWritten = new HashSet<>();
                for (EntityInfo info : entityInfos.values()
                        .stream()
                        .sorted((i1, i2) -> {
                            return i1.inventoryClass.getSimpleName().compareTo(i2.inventoryClass.getSimpleName());
                        })
                        .collect(Collectors.toList())) {
                    if (objectNameHavingWritten.contains(info.inventoryClass.getName())) {
                        continue;
                    }
                    write(info);
                    objectNameHavingWritten.add(info.inventoryClass.getName());
                }

                sb.append("\n\n").append("#QueryMessageInventoryMap").append("\nqueryMessageInventoryMap = {");
                for (Map.Entry<Class, Class> e : inventoryQueryMessageMap.entrySet()
                        .stream()
                        .sorted((e1, e2) ->
                        {
                            return e1.getValue().getSimpleName().compareTo(e2.getValue().getSimpleName());
                        })
                        .collect(Collectors.toList())) {
                    sb.append(String.format("\n%s '%s' : %s,", StringUtils.repeat(" ", 4), e.getValue().getSimpleName(), makeClassName(e.getKey())));
                }
                sb.append("\n}\n");
            }

            private void write(EntityInfo info) {
                sb.append(String.format("\nclass %s(object):", makeClassName(info.inventoryClass)));
                List<String> primitiveFields = new ArrayList<>();
                List<String> expandedFields = new ArrayList<>();

                Map<String, Class> nestedAndExpandedFields = new HashMap<>();

                for (Field f : info.allFieldsMap.values()) {
                    if (f.isAnnotationPresent(Unqueryable.class)) {
                        continue;
                    }
                    if (f.isAnnotationPresent(APINoSee.class)) {
                        continue;
                    }

                    if (Collection.class.isAssignableFrom(f.getType())) {
                        Class invClass = FieldUtils.getGenericType(f);
                        if (!TypeUtils.isZstackBeanPrimitive(invClass)) {
                            if (invClass.isAnnotationPresent(Inventory.class)) {
                                expandedFields.add(String.format("'%s'", f.getName()));
                                nestedAndExpandedFields.put(f.getName(), invClass);
                            }
                        }
                    } else {
                        primitiveFields.add(String.format("'%s'", f.getName()));
                    }

                }

                primitiveFields.add("'__userTag__'");
                primitiveFields.add("'__systemTag__'");

                sb.append(String.format("\n%s PRIMITIVE_FIELDS = [%s]", StringUtils.repeat(" ", 4), StringUtils.join(primitiveFields, ",")));

                for (ExpandedQueryStruct s : info.expandedQueries.values()) {
                    if (s.isHidden()) {
                        continue;
                    }
                    expandedFields.add(String.format("'%s'", s.getExpandedField()));
                    nestedAndExpandedFields.put(s.getExpandedField(), s.getInventoryClass());
                }
                for (ExpandedQueryAliasInfo i : info.aliases.values()) {
                    expandedFields.add(String.format("'%s'", i.alias));
                    nestedAndExpandedFields.put(i.alias, i.inventoryClass);
                }
                sb.append(String.format("\n%s EXPANDED_FIELDS = [%s]", StringUtils.repeat(" ", 4), StringUtils.join(expandedFields, ",")));

                sb.append(String.format("\n%s QUERY_OBJECT_MAP = {", StringUtils.repeat(" ", 4)));
                for (Map.Entry<String, Class> e : nestedAndExpandedFields.entrySet()) {
                    sb.append(String.format("\n%s'%s' : '%s',", StringUtils.repeat(" ", 8), e.getKey(), makeClassName(e.getValue())));
                }
                sb.append(String.format("\n%s}\n", StringUtils.repeat(" ", 5)));
            }
        }

        new PythonQueryObjectWriter().write();
    }
}
