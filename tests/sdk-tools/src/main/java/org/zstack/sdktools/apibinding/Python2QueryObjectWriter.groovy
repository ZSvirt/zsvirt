package org.zstack.sdktools.apibinding

import org.apache.commons.lang.StringUtils
import org.zstack.header.query.ExpandedQueryStruct
import org.zstack.header.query.Unqueryable
import org.zstack.header.rest.APINoSee
import org.zstack.header.search.Inventory
import org.zstack.query.EntityInfo
import org.zstack.query.ExpandedQueryAliasInfo
import org.zstack.query.InventoryCollector
import org.zstack.utils.FieldUtils
import org.zstack.utils.TypeUtils

import java.lang.reflect.Field
import java.util.stream.Collectors

/**
 * Offline copy of {@code MysqlQueryBuilderImpl3.writePython}.
 * Uses {@link InventoryCollector}; does not instantiate the Spring query bean.
 */
class Python2QueryObjectWriter {
    static void writePython(StringBuilder sb) {
        InventoryCollector collector = new InventoryCollector()
        collector.collect()
        write(sb, collector.entityInfos, collector.inventoryQueryMessageMap)
    }

    private static String makeClassName(Class<?> clazz) {
        return String.format("QueryObject%s", clazz.getSimpleName())
    }

    private static void write(StringBuilder sb,
                              Map<Class<?>, EntityInfo> entityInfos,
                              Map<Class<?>, Class<?>> inventoryQueryMessageMap) {
        sb.append("\n#QueryObjectInventory")
        Set<String> objectNameHavingWritten = new HashSet<>()
        for (EntityInfo info : entityInfos.values()
                .stream()
                .sorted({ i1, i2 ->
                    i1.inventoryClass.getSimpleName() <=> i2.inventoryClass.getSimpleName()
                })
                .collect(Collectors.toList())) {
            if (objectNameHavingWritten.contains(info.inventoryClass.getName())) {
                continue
            }
            write(sb, info)
            objectNameHavingWritten.add(info.inventoryClass.getName())
        }

        sb.append("\n\n").append("#QueryMessageInventoryMap").append("\nqueryMessageInventoryMap = {")
        for (Map.Entry<Class<?>, Class<?>> e : inventoryQueryMessageMap.entrySet()
                .stream()
                .sorted({ e1, e2 ->
                    e1.getValue().getSimpleName() <=> e2.getValue().getSimpleName()
                })
                .collect(Collectors.toList())) {
            sb.append(String.format("\n%s '%s' : %s,", StringUtils.repeat(" ", 4), e.getValue().getSimpleName(), makeClassName(e.getKey())))
        }
        sb.append("\n}\n")
    }

    private static void write(StringBuilder sb, EntityInfo info) {
        sb.append(String.format("\nclass %s(object):", makeClassName(info.inventoryClass)))
        List<String> primitiveFields = new ArrayList<>()
        List<String> expandedFields = new ArrayList<>()

        Map<String, Class<?>> nestedAndExpandedFields = new HashMap<>()

        for (Field f : info.allFieldsMap.values()) {
            if (f.isAnnotationPresent(Unqueryable.class)) {
                continue
            }
            if (f.isAnnotationPresent(APINoSee.class)) {
                continue
            }

            if (Collection.class.isAssignableFrom(f.getType())) {
                Class invClass = FieldUtils.getGenericType(f)
                if (!TypeUtils.isZstackBeanPrimitive(invClass)) {
                    if (invClass.isAnnotationPresent(Inventory.class)) {
                        expandedFields.add(String.format("'%s'", f.getName()))
                        nestedAndExpandedFields.put(f.getName(), invClass)
                    }
                }
            } else {
                primitiveFields.add(String.format("'%s'", f.getName()))
            }
        }

        primitiveFields.add("'__userTag__'")
        primitiveFields.add("'__systemTag__'")

        sb.append(String.format("\n%s PRIMITIVE_FIELDS = [%s]", StringUtils.repeat(" ", 4), StringUtils.join(primitiveFields, ",")))

        for (ExpandedQueryStruct s : info.expandedQueries.values()) {
            if (s.isHidden()) {
                continue
            }
            expandedFields.add(String.format("'%s'", s.getExpandedField()))
            nestedAndExpandedFields.put(s.getExpandedField(), s.getInventoryClass())
        }
        for (ExpandedQueryAliasInfo i : info.aliases.values()) {
            expandedFields.add(String.format("'%s'", i.alias))
            nestedAndExpandedFields.put(i.alias, i.inventoryClass)
        }
        sb.append(String.format("\n%s EXPANDED_FIELDS = [%s]", StringUtils.repeat(" ", 4), StringUtils.join(expandedFields, ",")))

        sb.append(String.format("\n%s QUERY_OBJECT_MAP = {", StringUtils.repeat(" ", 4)))
        for (Map.Entry<String, Class<?>> e : nestedAndExpandedFields.entrySet()) {
            sb.append(String.format("\n%s'%s' : '%s',", StringUtils.repeat(" ", 8), e.getKey(), makeClassName(e.getValue())))
        }
        sb.append(String.format("\n%s}\n", StringUtils.repeat(" ", 5)))
    }
}
