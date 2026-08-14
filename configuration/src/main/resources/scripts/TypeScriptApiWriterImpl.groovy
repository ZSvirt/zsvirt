package scripts

import org.apache.commons.io.FileUtils
import org.apache.commons.lang.StringUtils
import org.zstack.configuration.TypeScriptApiWriter
import org.zstack.header.exception.CloudRuntimeException
import org.zstack.header.identity.AccountConstant
import org.zstack.header.message.APIMessage
import org.zstack.header.message.Message
import org.zstack.header.query.Unqueryable
import org.zstack.header.rest.APINoSee
import org.zstack.header.search.Inventory
import org.zstack.utils.BeanUtils
import org.zstack.utils.FieldUtils
import org.zstack.utils.TypeUtils
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

import java.lang.reflect.Field
import java.lang.reflect.Type
import java.sql.Timestamp

/**
 */

public class TypeScriptApiWriterImpl implements TypeScriptApiWriter {
    private static CLogger logger = Utils.getLogger(TypeScriptApiWriterImpl.class);
    private List<String> contents = [];
    private Set<String> generatedClassName = [];

    private void addContent(String content, String typeName) {
        contents.add(content);
        generatedClassName.add(typeName);
    }

    private String buildObject(Class clz) {
        if (generatedClassName.contains(clz.getSimpleName())) {
            return clz.getSimpleName();
        }

        logger.debug(String.format("building %s ...", clz))
        generatedClassName.add(clz.getSimpleName())
        List<Field> fs = FieldUtils.getAllFields(clz)
        List<String> fields = []
        for (Field it : fs) {
            if (it.isAnnotationPresent(APINoSee.class)) {
                continue;
            }

            logger.debug(String.format("building field[%s] of class[%s]", it.getName(), clz.getName()))
            fields.add(String.format("%s : %s;", it.getName(), toTypeScriptType(it.getType(), it)))
        }

        def str;
        if (APIMessage.class.isAssignableFrom(clz)) {
            str = """\
export class ${clz.getSimpleName()} implements APIMessage {
    toApiMap() : any {
        var msg = {
            '${clz.getName()}': this
        };
        return msg;
    }
${StringUtils.join(fields, "\n")}
}
"""
        } else {
            str = """\
export class ${clz.getSimpleName()} {
${StringUtils.join(fields, "\n")}
}
"""
        }

        contents.add(str)
        return clz.getSimpleName()
    }

    private String buildGenericCollection(FieldUtils.CollectionGenericType genericType) {
        if (genericType.nestedGenericValue != null) {
            return "any[]"
        } else {
            return String.format("Array<%s>", toTypeScriptType(genericType.valueType, null))
        }
    }

    private String toTypeScriptType(Type type, Field f) {
        logger.debug(String.format("building type[%s] on field[%s]", type.class.getName(), f == null ? "null": String.format("%s.%s", f.getDeclaringClass().getName(), f.getName())))
        if (TypeUtils.isTypeOf(type, Timestamp.class, Date.class, String.class)) {
            return "string";
        } else if (TypeUtils.isTypeOf(type, Long.class, Long.TYPE, Integer.class, Integer.TYPE, Float.class, Float.TYPE, Short.TYPE, Short.class)) {
            return "number";
        } else if (TypeUtils.isTypeOf(type, Boolean.class, Boolean.TYPE)) {
            return "boolean";
        } else if (Enum.class.isAssignableFrom((Class)type)) {
            if (generatedClassName.contains(type.getClass().getSimpleName())) {
                return type.getClass().getSimpleName();
            }

            List enums = new ArrayList();
            Collections.addAll(enums, ((Class)type).enumConstants)
            def str = """\
export enum ${f.type.getSimpleName()} {
${StringUtils.join(enums, ",\n")}
}
"""
            addContent(str, type.getClass().getSimpleName())
            return type.getClass().getSimpleName()
        } else if (Collection.isAssignableFrom((Class)type)) {
            FieldUtils.CollectionGenericType genericType = FieldUtils.inferGenericTypeOnMapOrCollectionField(f);
            if (!genericType.isInferred()) {
                // no generic info
                return "any[]";
            } else {
                return buildGenericCollection(genericType);
            }
        } else if (Map.class.isAssignableFrom((Class)type)) {
            FieldUtils.MapGenericType genericType = FieldUtils.inferGenericTypeOnMapOrCollectionField(f);
            if (!genericType.isInferred()) {
                return "any";
            } else {
                return buildGenericMap(genericType)
            }
        } else {
            return buildObject((Class)type)
        }
    }

    String buildGenericMap(FieldUtils.MapGenericType genericType) {
        if (genericType.nestedGenericValue != null || genericType.nestedGenericKey != null) {
            return "any"
        } else {
            if (!String.isAssignableFrom(genericType.keyType)) {
                return "any";
            }

            return String.format("[value : string] : %s", toTypeScriptType(genericType.valueType, null))
        }
    }

    private void buildQueryable(Class invClz, String parent, List ret) {
        def fs = FieldUtils.getAllFields(invClz);
        fs.each { f ->
            if (f.isAnnotationPresent(APINoSee.class) || f.isAnnotationPresent(Unqueryable.class)) {
                return;
            }

            if (Collection.isAssignableFrom(f.getType())) {
                FieldUtils.CollectionGenericType gtype = FieldUtils.inferGenericTypeOnMapOrCollectionField(f);
                if (!gtype.isInferred()) {
                    throw new CloudRuntimeException(String.format("unable infer generic type of %s.%s", f.getDeclaringClass(), f.getName()))
                }
                if (gtype.nestedGenericValue != null) {
                    throw new CloudRuntimeException(String.format("%s.%s is nested Collection, not support", f.getDeclaringClass(), f.getName()))
                }

                if (!TypeUtils.isPrimitiveOrWrapper(gtype.valueType) && !TypeUtils.isTypeOf(gtype.valueType, Timestamp.class, Enum.class)) {
                    buildQueryable(gtype.valueType, f.getName(), ret);
                    return
                }
            }

            if (parent != null) {
                ret.add(String.format("'%s.%s'", parent, f.getName()))
            } else {
                ret.add(String.format("'%s'", f.getName()));
            }
        }
    }
    private void buildQueryable(Class inventoryClass) {
        def ret = [];
        buildQueryable(inventoryClass, null, ret);
        def str = """\
export var ${inventoryClass.getSimpleName()}Queryable = ${ret};
"""
        contents.add(str)
    }

    @Override
    void write(String outPath, List<Class> apiClass, List<Class> apiResultClass, List<Class> inventoryClass) {
        apiClass.each {
            buildObject(it)
        }
        apiResultClass.each {
            buildObject(it)
        }
        inventoryClass.each {
            buildObject(it)
        }

        File out = new File(outPath);
        File dir = out.getParentFile();
        if (!dir.exists()) {
            dir.mkdirs()
        }

        List<Class> inventories = BeanUtils.reflections.getTypesAnnotatedWith(Inventory.class);
        inventories.each {
            buildQueryable(it);
            Inventory inventory = it.getAnnotation(Inventory.class);
            Class entityClass = inventory.mappingVOClass();
            contents.add(String.format("export var TagResourceType%s = '%s';", entityClass.getSimpleName(), entityClass.getSimpleName()))
        }

        def str = """\
module ApiHeader {
export interface APIMessage {
    session? : SessionInventory;
    toApiMap() : any;
}

${contents.join("\n\n")}
}
"""
        FileUtils.writeStringToFile(out, str)
    }
}