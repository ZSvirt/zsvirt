package org.zstack.zql

import org.zstack.header.configuration.PythonApiBindingWriter
import org.zstack.header.query.AutoQuery
import org.zstack.utils.BeanUtils
import org.zstack.zql.ast.ZQLMetadata

class ZQLPythonWriter implements PythonApiBindingWriter {

    private ZQLMetadata.InventoryMetadata findAlias(ZQLMetadata.InventoryMetadata current, Iterator<String> it) {
        String fieldname = it.next()
        def exp = current.expandQueries[fieldname]
        assert exp : "cannot find expanded field[${fieldname}] on class ${current.selfInventoryClass}"

        ZQLMetadata.InventoryMetadata metadata = ZQLMetadata.inventoryMetadata[exp.targetInventoryClass.name]
        assert metadata : "cannot find InventoryMetadata for ${exp.targetInventoryClass}"
        if (!it.hasNext()) {
            return metadata
        } else {
            return findAlias(metadata, it)
        }
    }

    @Override
    void writePython(StringBuilder sb) {
        ZQLMetadata.inventoryMetadata.values().each {
            def expandedFields = []
            it.expandQueries.each { name, metadata ->
                if (!metadata.hidden) {
                    expandedFields.add(name)
                }
            }
            expandedFields += it.expandQueryAliases.keySet()
            def expandedFieldsStr = expandedFields.collect { "'" + it + "'" }.join(",")

            def queryObjectMap = [:]
            it.expandQueries.each {name, metadata ->
                if (metadata.hidden) {
                    return
                }

                queryObjectMap[name] = "QueryObject${metadata.targetInventoryClass.simpleName}"
            }

            it.expandQueryAliases.each {name, alias ->
                List ss = alias.expandQueryText.split("\\.") as List
                def target = findAlias(it, ss.iterator())
                assert target != null : "cannot find alias[${it.selfInventoryClass.name}: ${alias.aliasName} to ${alias.expandQueryText}]"
                queryObjectMap[name] = "QueryObject${target.selfInventoryClass.simpleName}"
            }

            def queryObjectMapStrList = []
            queryObjectMap.each { k, v -> queryObjectMapStrList.add("        '${k}' : '${v}',") }
            def queryObjectMapStr = queryObjectMapStrList.join('\n')

            def primitiveFields = (it.selfInventoryFieldNames - expandedFields).collect {"'" + it + "'"}.join(",")
            sb.append("""class QueryObject${it.simpleInventoryName()}(object):
    PRIMITIVE_FIELDS = [${primitiveFields}, '${ZQLMetadata.SYS_TAG_NAME}', '${ZQLMetadata.USER_TAG_NAME}', '${ZQLMetadata.TAG_PATTERN_UUID}']
    EXPANDED_FIELDS = [${expandedFieldsStr}]
    QUERY_OBJECT_MAP = {
${queryObjectMapStr}
    }

""")
        }

        def queryMessageInventoryMapList = []
        BeanUtils.reflections.getTypesAnnotatedWith(AutoQuery.class).each { clz ->
            AutoQuery at = clz.getAnnotation(AutoQuery.class)
            if (at == null) {
                return
            }

            queryMessageInventoryMapList.add("    '${clz.simpleName}' : QueryObject${at.inventoryClass().simpleName},")
        }

        sb.append("""queryMessageInventoryMap = {
${queryMessageInventoryMapList.join("\n")}
}
""")
    }
}
