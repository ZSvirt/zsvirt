package scripts

import com.fasterxml.jackson.core.type.TypeReference
import org.zstack.core.Platform
import org.zstack.header.message.APIParam
import org.zstack.header.message.OverriddenApiParam
import org.zstack.header.message.OverriddenApiParams
import org.zstack.header.rest.APINoSee
import org.zstack.header.rest.RestResponse
import org.zstack.header.search.Inventory
import org.zstack.rest.sdk.SdkFile
import org.zstack.rest.sdk.SdkTemplate
import org.zstack.utils.FieldUtils
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.util.stream.Collectors

class GoInventory implements SdkTemplate {
    private static final CLogger logger = Utils.getLogger(GoInventory.class)
    private Set<Class> inventories = new ArrayList<>()
    private Set<Class> markedInventories = new ArrayList<>()


    @Override
    List<SdkFile> generate() {
        def file = []
        def sdkFile = new SdkFile()
        sdkFile.subPath = "/api/"
        sdkFile.fileName = "Inventory.go"
        sdkFile.content = "package api\n\n"
        inventories.addAll(Platform.reflections.getTypesAnnotatedWith(Inventory.class))
        while (inventories.size() != 0) {
            def inventoriesCopy = new TreeSet<Class>(Comparator.comparing({it.simpleName}))
            sdkFile.content += inventoriesCopy.stream().filter({ it -> it.simpleName.contains("Inventory") }).map({ it -> generateStruct(it, it.simpleName) }).collect(Collectors.toList()).join("")
            markedInventories.addAll(inventoriesCopy)
            inventories.removeAll(markedInventories)
        }
        file.add(sdkFile)
        return file
    }

    String generateStruct(Class<?> inventoryClazz, String structName) {
        if (inventoryClazz == null) {
            return ""
        }

        def apiParamMap = new HashMap<String, APIParam>()
        if (inventoryClazz.isAnnotationPresent(OverriddenApiParams.class)) {
            for (OverriddenApiParam overriddenApiParam :inventoryClazz.getAnnotation(OverriddenApiParams.class).value()) {
                apiParamMap.put(overriddenApiParam.field(), overriddenApiParam.param())
            }
        }
        def fieldMap = new HashMap<String, Field>()
        FieldUtils.getAllFields(inventoryClazz).forEach({ it -> fieldMap.put(it.name, it) })
        if (inventoryClazz.isAnnotationPresent(RestResponse.class)) {
            //allTo
            def at = inventoryClazz.getAnnotation(RestResponse.class)
            if (at.allTo() != "") {
                fieldMap = new HashMap<String, Field>()
                fieldMap.put(at.allTo(), inventoryClazz.getDeclaredField(at.allTo()))
            } else if (at.fieldsTo().size() != 0 && at.fieldsTo()[0] != "all") {
                fieldMap = new HashMap<String, Field>()
                for (String fieldsTo : at.fieldsTo()) {
                    def spilt = fieldsTo.split("=")
                    if (spilt.size() == 1) {
                        fieldMap.put(spilt[0], inventoryClazz.getDeclaredField(spilt[0]))
                    } else {
                        fieldMap.put(spilt[0], inventoryClazz.getDeclaredField(spilt[1]))
                    }
                }
            }
        }
        def inventoryBuilder = new StringBuilder()
        inventoryBuilder.append("type ${structName} struct {\n")
        fieldMap.forEach { k, v ->
            if (v.isAnnotationPresent(APINoSee.class)) {
                return
            }
            if ('error' == k && structName.endsWith('Rsp')) {
                return
            }
            //name
            String name = k.substring(0, 1).toUpperCase() + k.substring(1)
            inventoryBuilder.append("\t" + name)
            //type
            inventoryBuilder.append(" " + generateFieldGeneric(v))
            //json tag
            inventoryBuilder.append(' `json:"' + k + '" ' + generateValidatorString(v, apiParamMap) + '`\n')
        }
        inventoryBuilder.append("}\n\n")
        return inventoryBuilder.toString()
    }

    private String generateFieldGeneric(Field field) {
        if (!(field.getGenericType() instanceof ParameterizedType)) {
            return generateFieldType(field, null)
        }
        String typeName = ""
        if (Collection.class.isAssignableFrom(field.type)) {
            Type type = ((ParameterizedType) field.getGenericType()).actualTypeArguments[0]
            typeName = "[]" + generateFieldType(null, type)
        }
        if (Map.class.isAssignableFrom(field.type)) {
            Type value = ((ParameterizedType) field.getGenericType()).actualTypeArguments[1]
            typeName = "map[string]" + generateFieldType(null, value)
        }
        return typeName
    }

    private String generateFieldType(Field field, Type type) {
        def value = "interface{}"
        if (field != null && field.type.name.contains("Inventory")) {
            value = field.type.simpleName
            inventories.add(field.type)
        }
        if (type != null && type.typeName.contains("Inventory") && !type.typeName.contains("<")) {
            def split = type.typeName.split('\\.')
            split = split[split.length - 1].split('\\$')
            value = split[split.length - 1]
            Class clz;
            if (type instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) type
                clz = ((Class) pt.rawType)
            } else if (type instanceof TypeVariable) {
                TypeVariable tType = (TypeVariable) type
                clz = tType.genericDeclaration.getClass()
            } else {
                clz = (Class) type
            }
            inventories.add(clz)
        }
        return value
    }

    private static String generateValidatorString(Field field, Map<String, APIParam> overriden) {
        def value = new StringBuilder("")
        if (!field.isAnnotationPresent(APIParam.class)) {
            return value
        }
        APIParam annotation = overriden.containsKey(field.name) ? overriden[field.name] : field.getAnnotation(APIParam.class)
        if (!annotation.required()) {
            return value
        }
        value.append('validate:"')
        def strings = new ArrayList<String>()
        if (annotation.required()) {
            strings.add('required')
        }
        value.append(strings.join(","))
        value.append('"')
        return value
    }
}
