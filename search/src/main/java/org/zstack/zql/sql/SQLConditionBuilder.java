package org.zstack.zql.sql;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.db.EntityMetadata;
import org.zstack.header.core.StaticInit;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.query.Queryable;
import org.zstack.header.search.Inventory;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.UserTagVO;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.FieldUtils;
import org.zstack.utils.StringDSL;
import org.zstack.zql.ast.ZQLMetadata;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SQLConditionBuilder {
    private String template;
    private Field conditionField;
    private String operator;
    private String value;

    private enum ConditionType {
        QueryableField,
        Tag,
        TagPattern,
        Normal
    }

    private static class QueryableField {
        Queryable annotation;
        Class inventoryClass;
        Field field;

        String toSQL(String operator, String value) {
            ZQLMetadata.InventoryMetadata metadata = ZQLMetadata.getInventoryMetadataByName(inventoryClass.getName());
            if (metadata == null) {
                throw new CloudRuntimeException(String.format("cannot find InventoryMetadata for class[%s]", inventoryClass));
            }

            Field primaryKey = EntityMetadata.getPrimaryKeyField(metadata.inventoryAnnotation.mappingVOClass());
            Class mappingInventoryClass = annotation.mappingClass();
            ZQLMetadata.InventoryMetadata mappingInventoryMetadata = ZQLMetadata.getInventoryMetadataByName(mappingInventoryClass.getName());
            String mappingEntityName = mappingInventoryClass.getSimpleName();

            if (operator.contains("has")) {
                return String.format("%s.%s IN (%s)", inventoryClass.getSimpleName(), primaryKey.getName(),
                        generateHasSQL(mappingInventoryMetadata.inventoryAnnotation.mappingVOClass().getSimpleName(), mappingEntityName,
                                annotation.joinColumn().name(), annotation.joinColumn().referencedColumnName(), operator, value));
            } else {
                return String.format("%s.%s IN (SELECT %s.%s FROM %s %s WHERE %s.%s %%s %%s)",
                        inventoryClass.getSimpleName(), primaryKey.getName(), mappingEntityName,
                        annotation.joinColumn().name(), mappingInventoryMetadata.inventoryAnnotation.mappingVOClass().getSimpleName(),
                        mappingEntityName, mappingEntityName, annotation.joinColumn().referencedColumnName());
            }
        }
    }

    private static Map<String, QueryableField> queryableFields = new HashMap<>();
    private static Map<String, String> TAG_FALSE_OP = new HashMap();

    @StaticInit
    static void staticInit() {
        BeanUtils.reflections.getFieldsAnnotatedWith(Queryable.class).forEach(f->{
            QueryableField qf = new QueryableField();
            qf.annotation = f.getAnnotation(Queryable.class);
            qf.field = f;
            qf.inventoryClass = f.getDeclaringClass();
            if (!qf.annotation.mappingClass().isAnnotationPresent(Inventory.class)) {
                throw new CloudRuntimeException(String.format("field[%s] is annotated of @Queryable, however it's mapping class[%s] is " +
                        " not annotated by @Inventory", f.getName(), qf.annotation.mappingClass()));
            }
            queryableFields.put(String.format("%s.%s", qf.inventoryClass.getName(), f.getName()), qf);
        });

        TAG_FALSE_OP.put("!=", "=");
        TAG_FALSE_OP.put("not in", "in");
        TAG_FALSE_OP.put("not like", "like");
        TAG_FALSE_OP.put("not has", "in"); // not has for tag query is same as not in
    }

    private void setConditionField(Class clz, String fname) {
        conditionField = FieldUtils.getField(fname, clz);
        if (conditionField == null) {
            throw new CloudRuntimeException(String.format("inventory class[%s] has no field[%s]", clz, fname));
        }
    }

    private QueryableField getIfConditionFieldQueryableField() {
        return queryableFields.get(String.format("%s.%s", conditionField.getDeclaringClass().getName(), conditionField.getName()));
    }

    private ConditionType getConditionType(Class invClass, String fname) {
        if (fname.equals(ZQLMetadata.SYS_TAG_NAME) || fname.equals(ZQLMetadata.USER_TAG_NAME)) {
            return ConditionType.Tag;
        } else if (fname.equals(ZQLMetadata.TAG_PATTERN_UUID)) {
            return ConditionType.TagPattern;
        } else if (queryableFields.containsKey(String.format("%s.%s", invClass.getName(), fname))) {
            return ConditionType.QueryableField;
        } else {
            return ConditionType.Normal;
        }
    }

    public SQLConditionBuilder(String queryTargetInventoryName, List<String> conditionNames, String operator, String value) {
        this.operator = operator;
        this.value = value;

        List<ZQLMetadata.ChainQueryStruct> chainQueries = ZQLMetadata.createChainQuery(queryTargetInventoryName, conditionNames);
        if (chainQueries.size() == 1) {
            ZQLMetadata.FieldChainQuery fc = (ZQLMetadata.FieldChainQuery) chainQueries.get(0);

            ConditionType ctype = getConditionType(fc.self.selfInventoryClass, fc.fieldName);
            if (ctype == ConditionType.Tag) {
                template = createTagSQL(fc.self.selfInventoryClass, fc.fieldName);
            } else if (ctype == ConditionType.TagPattern) {
                template = createTagPatternSQL(fc.self.selfInventoryClass);
            } else {
                setConditionField(fc.self.selfInventoryClass, fc.fieldName);
                QueryableField qf = getIfConditionFieldQueryableField();

                if (qf == null) {
                    template = String.format("%s.%s %%s %%s", fc.self.selfInventoryClass.getSimpleName(), fc.fieldName);
                } else {
                    // self may be children class
                    qf.inventoryClass = fc.self.selfInventoryClass;
                    template = qf.toSQL(operator, value);
                }
            }
        } else {
            ZQLMetadata.ExpandChainQuery first = (ZQLMetadata.ExpandChainQuery) chainQueries.get(0);
            template = String.format("%s.%s IN %s",
                    first.self.simpleInventoryName(), first.right.selfKeyName, makeTemplate(chainQueries.subList(1, chainQueries.size()).iterator()));
        }

        if (template.contains("%s")) {
            // unresolved SQL, resolve it
            template = String.format(template, operator, normalizeValue(value));
        }
    }

    private String createTagSQL(Class invClz, String fieldName) {
        class TagSQLMaker {
            private String make() {
                ZQLMetadata.InventoryMetadata src = ZQLMetadata.getInventoryMetadataByName(invClz.getName());

                String primaryKey = EntityMetadata.getPrimaryKeyField(src.inventoryAnnotation.mappingVOClass()).getName();
                String tableName = fieldName.equals(ZQLMetadata.USER_TAG_NAME) ? UserTagVO.class.getSimpleName() : SystemTagVO.class.getSimpleName();

                String subCondition;
                if (TAG_FALSE_OP.containsKey(operator)) {
                    String reserveOp = TAG_FALSE_OP.get(operator);
                    subCondition = String.format("tagvo.uuid IN (SELECT tagvo_.uuid FROM %s tagvo_ WHERE tagvo_.tag %s %s)",
                            tableName, reserveOp, value);

                    return String.format("(%s.%s NOT IN (SELECT tagvo.resourceUuid FROM %s tagvo WHERE %s))",
                            src.simpleInventoryName(), primaryKey, tableName, subCondition);
                } else {
                    if (value == null) {
                        subCondition = String.format("tagvo.tag %s", operator);
                    } else {
                        subCondition = String.format("tagvo.tag %s %s", operator, value);
                    }

                    String filterResourceUuidSQL = operator.equals("has") ?
                            generateHasSQL(tableName, "tagvo", "resourceUuid", "tag", operator, value) :
                            String.format("SELECT tagvo.resourceUuid FROM %s tagvo WHERE %s", tableName, subCondition);

                    return String.format("(%s.%s IN (%s))",
                            src.simpleInventoryName(), primaryKey, filterResourceUuidSQL);
                }
            }
        }

        return new TagSQLMaker().make();
    }

    private String createTagPatternSQL(Class invClz) {
        ZQLMetadata.InventoryMetadata src = ZQLMetadata.getInventoryMetadataByName(invClz.getName());

        String primaryKey = EntityMetadata.getPrimaryKeyField(src.inventoryAnnotation.mappingVOClass()).getName();

        boolean reserve = TAG_FALSE_OP.containsKey(operator);

        String op = reserve ? TAG_FALSE_OP.get(operator) : operator;
        String subCondition = String.format("tagPatternUuid %s ", op + (value == null ? "" : value));
        String filterResourceUuidSQL = operator.equals("has") ?
                generateHasSQL("UserTagVO", "tagvo", "resourceUuid", "tagPatternUuid", operator, value) :
                String.format("SELECT distinct resourceUuid FROM UserTagVO WHERE %s", subCondition);

        String wetherNotIn = reserve ? "NOT" : "";
        return String.format("(%s.%s %s IN (%s))",
                    src.simpleInventoryName(), primaryKey, wetherNotIn, filterResourceUuidSQL);
    }

    private String makeTemplate(Iterator<ZQLMetadata.ChainQueryStruct> iterator) {
        ZQLMetadata.ChainQueryStruct current = iterator.next();

        String value = iterator.hasNext() ? makeTemplate(iterator) : null;

        if (value == null) {
            if (!(current instanceof ZQLMetadata.FieldChainQuery)) {
                throw new CloudRuntimeException("the last pair is not a FieldChainQuery");
            }

            ZQLMetadata.FieldChainQuery fc = (ZQLMetadata.FieldChainQuery) current;
            ZQLMetadata.ExpandQueryMetadata right = fc.right;
            String entityName = right.targetInventoryClass.getSimpleName();

            ConditionType ctype = getConditionType(right.targetInventoryClass, fc.fieldName);
            String filterSqlFormat = String.format("(SELECT %s.%s FROM %s %s WHERE %%s)",
                    entityName, right.targetKeyName, right.targetVOClass.getSimpleName(), entityName);

            if (ctype == ConditionType.Tag) {
                return String.format(filterSqlFormat, createTagSQL(right.targetInventoryClass, fc.fieldName));
            } else if (ctype == ConditionType.TagPattern) {
                return String.format(filterSqlFormat, createTagPatternSQL(right.targetInventoryClass));
            } else {
                setConditionField(right.targetInventoryClass, fc.fieldName);
                QueryableField qf = getIfConditionFieldQueryableField();

                if (qf != null) {
                    return String.format(filterSqlFormat, qf.toSQL(operator, this.value));
                } else if (operator.contains("has")) {
                    return String.format("(%s)", generateHasSQL(right.targetVOClass.getSimpleName(), entityName,
                        right.targetKeyName, fc.fieldName, operator, this.value));
                } else {
                    return String.format(filterSqlFormat, String.format("%s.%s %%s %%s", entityName, fc.fieldName));
                }
            }
        }

        ZQLMetadata.ExpandChainQuery ec = (ZQLMetadata.ExpandChainQuery) current;

        ZQLMetadata.ExpandQueryMetadata right = ec.right;
        String entityName = ec.self.selfInventoryClass.getSimpleName();
        return String.format("(SELECT %s.%s FROM %s %s" +
                " WHERE %s.%s IN %s)",
                entityName, ec.selfKey, right.selfVOClass.getSimpleName(), entityName,
                entityName, right.selfKeyName, value);
    }

    private String normalizeValue(String value) {
        if (Boolean.class.isAssignableFrom(conditionField.getType()) || boolean.class.isAssignableFrom(conditionField.getType())) {
            return StringUtils.strip(value, "'");
        } else {
            return value;
        }
    }

    public String build() {
        return template;
    }

    private static String generateHasSQL(String entityClass, String entityName, String targetKeyName, String fieldName, String operator, String value) {
        if (operator.equals("has")) {
            int count = value.replaceAll("\\s", "").split("','").length;
            return StringDSL.s("SELECT {3}.{0} FROM {2} {3}" +
                    " WHERE {3}.{1} in {4}" +
                    " GROUP BY {3}.{0}" +
                    " HAVING COUNT(distinct {3}.{1}) = {5}")
                    .format(targetKeyName, fieldName, entityClass, entityName, value, count);
        } else {
            return StringDSL.s("SELECT {3}.{0} FROM {2} {3}" +
                    " WHERE {3}.{0} NOT IN" +
                    " ( " +
                    " SELECT {3}_.{0} FROM {2} {3}_" +
                    " WHERE {3}_.{1} IN {4}" +
                    " )")
                    .format(targetKeyName, fieldName, entityClass, entityName, value);
        }

    }
}
