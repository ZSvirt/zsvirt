package org.zstack.testlib.tool

import org.zstack.utils.StringTemplateUtils

import javax.persistence.*
import java.lang.reflect.Field

/**
 * Code generator for zstack to generate inventory, mysql schema and rest api
 * from jpa vo entity class.
 *
 * @author kayo
 */
class CodeGenerator {
    private static String PACKAGE_NAME = "PACKAGE_NAME"
    private static String RESOURCE_NAME = "RESOURCE_NAME"
    private static String LOWER_CASE_RESOURCE_NAME = "LOWER_CASE_RESOURCE_NAME"
    private static String PRIVATE_FIELDS = "PRIVATE_FIELDS"
    private static String GETTER_SETTER = "GETTER_SETTER"
    private static String IMPORT_PACKAGE = "IMPORT_PACKAGE"
    private static String INVENTORY_NAME = "INVENTORY_NAME"
    private static String ENTITY_CLASS = "ENTITY_CLASS"
    /**
     * Generate inventory, mysql schema and rest api from jpa vo entity class.
     *
     * @param entityClass the jpa vo entity class
     */
    static void generateCodeFromJpaEntity(Class<?> entityClass) {
        if (!entityClass.isAnnotationPresent(Entity.class) && !entityClass.isAnnotationPresent(MappedSuperclass.class)) {
            System.out.printf("%s is not a JPA entity class", entityClass.getName())
            return
        }

        List<Field> fields = new ArrayList<>()
        // collection column fields
        for (Field field : entityClass.getDeclaredFields()) {
            if (!field.isAnnotationPresent(Column.class)) {
                continue
            }

            fields.add(field)
        }

        // transfer to pojo field
        List<PojoField> pojoFields = new ArrayList<>()
        for (Field field : fields) {
            PojoField pojoField = new PojoField()
            pojoField.fieldName = field.getName()
            pojoField.fieldType = getJavaTypeFromFieldType(field.getType())
            pojoField.columnName = convert(field.getType())
            pojoField.field = field
            pojoFields.add(pojoField)
        }

        PojoClass pojoClass = createInventory(pojoFields, entityClass)
        pojoClass.pojoFields = pojoFields
        createSQL(fields, entityClass)
        createRestAPI(pojoClass)
    }

    private static String generateImportPackage(String packageName) {
        return String.format("import %s;", packageName)
    }

    private static String generateGetterAndSetter(PojoField field) {
        String methodName = field.fieldName.substring(0, 1).toUpperCase() + field.fieldName.substring(1)
        String getterName = String.format("get%s", methodName)
        String setterName = String.format("set%s", methodName)

        return String.format("    public %s %s() {%n" +
                "        return %s;%n" +
                "    }%n" +
                "%n" +
                "    public void %s(%s %s) {%n" +
                "        this.%s = %s;%n" +
                "    }%n", field.fieldType,
                getterName,
                field.fieldName,
                setterName,
                field.fieldType,
                field.fieldName,
                field.fieldName,
                field.fieldName)
    }

    static class PojoField {
        String fieldName
        String fieldType
        String columnName
        Field field
    }

    static class PojoClass {
        String className
        String packageName
        String inventoryClassName

        // transient fields contains field class of jpa entity
        List<PojoField> pojoFields = new ArrayList<>()

        // fields for inventory
        List<String> privateFields = new ArrayList<>()
        Set<String> importPackages = new HashSet<>()

        // fields for rest api and inventory
        List<String> gettersAndSetters = new ArrayList<>()
    }

    private static PojoClass createInventory(List<PojoField> fields, Class<?> entityClass) {
        PojoClass pojoClass = new PojoClass()
        pojoClass.packageName = entityClass.getPackage().getName().replace("vo", "inventory")
        pojoClass.className = entityClass.getName()
        // if class extends ResourceVO add more fields
        if (entityClass.getSuperclass().getName() == "org.zstack.header.vo.ResourceVO") {
            pojoClass.privateFields.add("private String uuid;")
            pojoClass.gettersAndSetters.add("public String getUuid() {\n" +
                    "        return uuid;\n" +
                    "    }\n" +
                    "\n" +
                    "    public void setUuid(String uuid) {\n" +
                    "        this.uuid = uuid;\n" +
                    "    }")
        }

        pojoClass.importPackages.add("import java.io.Serializable;")
        pojoClass.importPackages.add("import org.zstack.header.configuration.PythonClassInventory;")
        pojoClass.importPackages.add("import org.zstack.header.search.Inventory;")
        pojoClass.importPackages.add("import java.sql.Timestamp;")


        for (PojoField field : fields) {
            if (!field.field.getClass().isPrimitive()) {
                pojoClass.importPackages.add(generateImportPackage(field.field.getClass().getCanonicalName()))
            }
            pojoClass.gettersAndSetters.add(generateGetterAndSetter(field))
            pojoClass.privateFields.add(String.format("    private %s %s;", field.fieldType, field.fieldName))
        }

        if (entityClass.getSimpleName().endsWith("VO")) {
            pojoClass.inventoryClassName = entityClass.getSimpleName().replace("VO", "Inventory")
        } else if (entityClass.getSimpleName().endsWith("AO")) {
            pojoClass.inventoryClassName = entityClass.getSimpleName().replace("AO", "Inventory")
        } else {
            pojoClass.inventoryClassName = entityClass.getSimpleName() + "Inventory"
        }

        Map<String, Object> bindings = new HashMap<>()
        bindings.put(PACKAGE_NAME, pojoClass.packageName)
        bindings.put(PRIVATE_FIELDS, pojoClass.privateFields.join("\n"))
        bindings.put(GETTER_SETTER, pojoClass.gettersAndSetters.join("\n"))
        bindings.put(IMPORT_PACKAGE, pojoClass.importPackages.join("\n"))
        bindings.put(INVENTORY_NAME, pojoClass.inventoryClassName)
        bindings.put(ENTITY_CLASS, entityClass.getSimpleName())

        writeToFile(String.format("%s.java", pojoClass.inventoryClassName),
                StringTemplateUtils.createStringFromTemplate(INVENTORY_TEMPLATE, bindings))

        return pojoClass
    }

    static class ColumnDefinition {
        String columnName
        String columnType
        boolean isPrimaryKey = false
        boolean needIndex = false
    }

    private static String getJavaTypeFromFieldType(Class<?> fieldTypeClass) {
        if (fieldTypeClass.isEnum()) {
            return "String"
        } else {
            return fieldTypeClass.getSimpleName()
        }
    }

    private static String convert(Class<?> fieldType) {
        String javaType = getJavaTypeFromFieldType(fieldType)

        if (javaType == "String") {
            return "varchar(255)"
        } else if (javaType == "Integer" || javaType == "int") {
            return "int"
        } else if (javaType == "Long" || javaType == "long") {
            return "bigint"
        } else if (javaType == "Boolean" || javaType == "boolean") {
            return "boolean"
        } else if (javaType == "Timestamp") {
            return "timestamp"
        } else if (javaType == "Date") {
            return "date"
        } else if (javaType == "byte[]") {
            return "blob"
        } else {
            throw new RuntimeException(String.format("unknown java type[%s]", javaType))
        }
    }

    private static void createSQL(List<Field> fields, Class<?> entityClass) {
        List<ColumnDefinition> columnDefinitions = new ArrayList<>()

        // if class extends ResourceVO add more fields
        if (entityClass.getSuperclass().getName() == "org.zstack.header.vo.ResourceVO") {
            ColumnDefinition columnDefinition = new ColumnDefinition()
            columnDefinition.columnName = "uuid"
            columnDefinition.columnType = "varchar(32)"
            columnDefinition.isPrimaryKey = true
            columnDefinition.needIndex = true
            columnDefinitions.add(columnDefinition)
        }

        for (Field field : fields) {
            String columnName = field.getName()
            boolean needIndex = false
            boolean isPrimaryKey = false

            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class)
                if (!column.name().isEmpty()) {
                    columnName = column.name()
                } else {
                    columnName = field.getName()
                }

                needIndex = column.unique()
            }

            if (field.isAnnotationPresent(Id.class)) {
                isPrimaryKey = true
            }

            if (field.isAnnotationPresent(Index.class)) {
                needIndex = true
            }

            ColumnDefinition columnDefinition = new ColumnDefinition()
            columnDefinition.columnName = columnName
            columnDefinition.columnType = convert(field.getType())
            columnDefinition.isPrimaryKey = isPrimaryKey
            columnDefinition.needIndex = needIndex
            columnDefinitions.add(columnDefinition)
        }

        // generate sql in mysql innodb syntax
        String schemaContent = ""
        String primaryKeySQL = ""
        schemaContent += String.format("CREATE TABLE IF NOT EXISTS `zstack`.`%s` (%n", entityClass.getSimpleName())
        for (ColumnDefinition columnDefinition : columnDefinitions) {
            String line = String.format("  `%s` %s", columnDefinition.columnName, columnDefinition.columnType)
            if (columnDefinition.isPrimaryKey) {
                line = String.format("%s NOT NULL", line)
                primaryKeySQL = String.format("  PRIMARY KEY (`%s`)", columnDefinition.columnName)
            } else {
                line = String.format("%s NULL", line)
            }

            if (columnDefinition.needIndex) {
                line = String.format("%s UNIQUE", line)
            }

            line = String.format("%s,", line)
            schemaContent += String.format("  %s%n", line)
        }

        if (!primaryKeySQL.isEmpty()) {
            System.out.println(primaryKeySQL)
            schemaContent += String.format("%s,%n", primaryKeySQL)
        }

        schemaContent += String.format(") ENGINE=InnoDB DEFAULT CHARSET=utf8;")
        writeToFile(String.format("%s.sql", entityClass.getSimpleName()), schemaContent)
    }

    private static void createRestAPI(PojoClass pojoClass) {
        String resourceName = pojoClass.inventoryClassName.replace("Inventory", "")
        String[] r = resourceName.split("(?=\\p{Upper})")
        String resourceNameInRest = r.join("-").toLowerCase()

        generateQueryAPI(pojoClass, resourceName, resourceNameInRest)
        generateCreateAPI(pojoClass, resourceName, resourceNameInRest)
        generateUpdateAPI(pojoClass, resourceName, resourceNameInRest)
        generateDeleteAPI(pojoClass, resourceName, resourceNameInRest)
    }

    private static void generateDeleteAPI(PojoClass pojoClass, String resourceName, String resourceNameInRest) {
        println "############ generate delete api ############%n"

        Map<String, Object> bindings = new HashMap<>()
        bindings.put(PACKAGE_NAME, pojoClass.packageName)
        bindings.put(RESOURCE_NAME, resourceName)
        bindings.put(LOWER_CASE_RESOURCE_NAME, resourceName.toLowerCase())

        writeToFile("APIDelete${resourceName}Msg.java",
                StringTemplateUtils.createStringFromTemplate(DELETE_API_TEMPLATE, bindings))

        System.out.printf("############ generate delete event ############%n")

        bindings = new HashMap<>()
        bindings.put(PACKAGE_NAME, pojoClass.packageName)
        bindings.put(RESOURCE_NAME, resourceName)
        bindings.put(LOWER_CASE_RESOURCE_NAME, resourceNameInRest)

        writeToFile("APIDelete${resourceName}Event.java",
                StringTemplateUtils.createStringFromTemplate(DELETE_EVENT_TEMPLATE, bindings))
    }

    private static void generateUpdateAPI(PojoClass pojoClass, String resourceName, String resourceNameInRest) {
        System.out.printf("############ generate update api ############%n")

        Map<String, Object> bindings = new HashMap<>()
        bindings.put(PACKAGE_NAME, pojoClass.packageName)
        bindings.put(RESOURCE_NAME, resourceName)
        bindings.put(LOWER_CASE_RESOURCE_NAME, resourceNameInRest)

        String allPrivateFields = pojoClass.pojoFields.collect {
            String.format(API_PARAM_TEMPLATE,
                    it.field.getAnnotation(Column.class).length(),
                    it.fieldType,
                    it.fieldName,
            )
        }.join("\n")

        bindings.put(PRIVATE_FIELDS, allPrivateFields)

        String allGetters = pojoClass.gettersAndSetters.join("\n")
        bindings.put(GETTER_SETTER, allGetters)

        writeToFile(String.format("APIUpdate%sMsg.java", resourceName),
                StringTemplateUtils.createStringFromTemplate(UPDATE_API_TEMPLATE, bindings))

        System.out.printf("############ generate update event ############%n")

        bindings = new HashMap<>()
        bindings.put(PACKAGE_NAME, pojoClass.packageName)
        bindings.put(RESOURCE_NAME, resourceName)
        bindings.put(LOWER_CASE_RESOURCE_NAME, resourceName.toLowerCase())

        writeToFile(String.format("APIUpdate%sEvent.java", resourceName),
                StringTemplateUtils.createStringFromTemplate(UPDATE_EVENT_TEMPLATE, bindings))
    }

    private static void generateCreateAPI(PojoClass pojoClass, String resourceName, String resourceNameInRest) {
        System.out.printf("############ generate create api ############%n")

        Map<String, Object> bindings = new HashMap<>()
        bindings.put(RESOURCE_NAME, resourceName)
        bindings.put(PACKAGE_NAME, pojoClass.packageName)
        bindings.put(LOWER_CASE_RESOURCE_NAME, resourceNameInRest)

        String allPrivateFields = pojoClass.pojoFields.collect {
            String.format(API_PARAM_TEMPLATE,
                    it.field.getAnnotation(Column.class).length(),
                    it.fieldType,
                    it.fieldName,
            )
        }.join("\n")

        bindings.put(PRIVATE_FIELDS, allPrivateFields)

        String allGetters = pojoClass.gettersAndSetters.join("\n")
        bindings.put(GETTER_SETTER, allGetters)

        writeToFile(String.format("APICreate%sMsg.java", resourceName),
                StringTemplateUtils.createStringFromTemplate(CREATE_API_TEMPLATE, bindings))

        System.out.printf("############ generate create event ############%n")

        bindings = new HashMap<>()
        bindings.put(RESOURCE_NAME, resourceName)
        bindings.put(PACKAGE_NAME, pojoClass.packageName)

        writeToFile(String.format("APICreate%sEvent.java", resourceName),
                StringTemplateUtils.createStringFromTemplate(CREATE_EVENT_TEMPLATE, bindings))
    }

    private static void generateQueryAPI(PojoClass pojoClass, String resourceName, String resourceNameInRest) {
        System.out.printf("############ generate query api ############%n")

        Map<String, Object> bindings = new HashMap<>()
        bindings.put(PACKAGE_NAME, pojoClass.packageName)
        bindings.put(RESOURCE_NAME, resourceName)
        bindings.put(LOWER_CASE_RESOURCE_NAME, resourceNameInRest)

        writeToFile(String.format("APIQuery%sMsg.java", resourceName),
                StringTemplateUtils.createStringFromTemplate(QUERY_API_TEMPLATE, bindings))

        System.out.printf("############ generate query reply ############%n")

        bindings = new HashMap<>()
        bindings.put(PACKAGE_NAME, pojoClass.packageName)
        bindings.put(RESOURCE_NAME, resourceName)

        writeToFile(String.format("APIQuery%sReply.java", resourceName),
                StringTemplateUtils.createStringFromTemplate(QUERY_REPLY_TEMPLATE, bindings))
    }

    private static void writeToFile(String path, String content) {
        new File(path).write content
        println "re-written a request doc template ${path}"
        println "content: ${content}"
    }

    static final String QUERY_API_TEMPLATE = '''
package ${PACKAGE_NAME};

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import ${PACKAGE_NAME}.${RESOURCE_NAME}Inventory;
import ${PACKAGE_NAME}.APIQuery${RESOURCE_NAME}Reply;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQuery${RESOURCE_NAME}Reply.class, inventoryClass = ${RESOURCE_NAME}Inventory.class)
@RestRequest(
        path = "/${LOWER_CASE_RESOURCE_NAME}s",
        optionalPaths = {"/${LOWER_CASE_RESOURCE_NAME}s/{uuid}"},
        responseClass = APIQuery${RESOURCE_NAME}Reply.class,
        method = HttpMethod.GET
)
public class APIQuery${RESOURCE_NAME}Msg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("uuid=xxx", "name=xxx");
    }
}
'''

    static final String QUERY_REPLY_TEMPLATE = '''
package ${PACKAGE_NAME};

import org.zstack.header.query.APIQueryReply;
import ${PACKAGE_NAME}.${RESOURCE_NAME}Inventory;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQuery${RESOURCE_NAME}Reply extends APIQueryReply {

    private List<${RESOURCE_NAME}Inventory> inventories;

    public List<${RESOURCE_NAME}Inventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<${RESOURCE_NAME}Inventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQuery${RESOURCE_NAME}Reply __example__() {
        APIQuery${RESOURCE_NAME}Reply reply = new APIQuery${RESOURCE_NAME}Reply();

        return reply;
    }
}
'''

    static final String CREATE_API_TEMPLATE = '''
package ${PACKAGE_NAME};

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;
import org.zstack.header.message.APIAuditor;

import java.util.concurrent.TimeUnit;

@TagResourceType(${RESOURCE_NAME}VO.class)
@RestRequest(
        path = "/${LOWER_CASE_RESOURCE_NAME}s",
        method = HttpMethod.POST,
        responseClass = APICreate${RESOURCE_NAME}Event.class,
        parameterName = "params"
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 12)
public class APICreate${RESOURCE_NAME}Msg extends APICreateMessage implements APIAuditor {
    ${PRIVATE_FIELDS}

    ${GETTER_SETTER}

    public static APICreate${RESOURCE_NAME}Msg __example__() {
        APICreate${RESOURCE_NAME}Msg msg = new APICreate${RESOURCE_NAME}Msg();
        msg.setName("example");

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(((APICreate${RESOURCE_NAME}Msg) msg).getName(), ${RESOURCE_NAME}VO.class);
    }
}
'''

    static final String CREATE_EVENT_TEMPLATE = '''
package ${PACKAGE_NAME};

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreate${RESOURCE_NAME}Event extends APIEvent {
    private ${RESOURCE_NAME}Inventory inventory;

    public APICreate${RESOURCE_NAME}Event() {
    }

    public APICreate${RESOURCE_NAME}Event(String apiId) {
        super(apiId);
    }

    public ${RESOURCE_NAME}Inventory getInventory() {
        return inventory;
    }

    public void setInventory(${RESOURCE_NAME}Inventory inventory) {
        this.inventory = inventory;
    }

    public static APICreate${RESOURCE_NAME}Event __example__() {
        APICreate${RESOURCE_NAME}Event event = new APICreate${RESOURCE_NAME}Event();
        ${RESOURCE_NAME}Inventory inventory = new ${RESOURCE_NAME}Inventory();
        event.setInventory(inventory);
        return event;
    }
}
'''

    static final String UPDATE_API_TEMPLATE = '''
package ${PACKAGE_NAME};

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/${LOWER_CASE_RESOURCE_NAME}s/{uuid}",
        method = HttpMethod.PUT,
        responseClass = APIUpdate${RESOURCE_NAME}Event.class,
        parameterName = "params"
)
public class APIUpdate${RESOURCE_NAME}Msg extends APIMessage {
    @APIParam(resourceType = ${RESOURCE_NAME}VO.class, checkAccount = true, operationTarget = true)
    private String uuid;

    ${PRIVATE_FIELDS}

    ${GETTER_SETTER}

    public static APIUpdate${RESOURCE_NAME}Msg __example__() {
        APIUpdate${RESOURCE_NAME}Msg msg = new APIUpdate${RESOURCE_NAME}Msg();
        msg.setUuid(uuid());
        msg.setName("example");

        return msg;
    }
}
'''

    static final String UPDATE_EVENT_TEMPLATE = '''
package ${PACKAGE_NAME};

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIUpdate${RESOURCE_NAME}Event extends APIEvent {
    private ${RESOURCE_NAME}Inventory inventory;

    public APIUpdate${RESOURCE_NAME}Event() {
    }

    public APIUpdate${RESOURCE_NAME}Event(String apiId) {
        super(apiId);
    }

    public ${RESOURCE_NAME}Inventory getInventory() {
        return inventory;
    }

    public static APIUpdate${RESOURCE_NAME}Event __example__() {
        APIUpdate${RESOURCE_NAME}Event event = new APIUpdate${RESOURCE_NAME}Event();
        return event;
    }
}
'''
    static final String DELETE_API_TEMPLATE = '''
package ${PACKAGE_NAME};

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/${LOWER_CASE_RESOURCE_NAME}s/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDelete${RESOURCE_NAME}Event.class
)
public class APIDelete${RESOURCE_NAME}Msg extends APIMessage {
    @APIParam(resourceType = ${RESOURCE_NAME}VO.class, checkAccount = true, operationTarget = true)
    private String uuid;

    public static APIDelete${RESOURCE_NAME}Msg __example__() {
        APIDelete${RESOURCE_NAME}Msg msg = new APIDelete${RESOURCE_NAME}Msg();
        msg.setUuid(uuid());
        return msg;
    }
}
'''

    static final String DELETE_EVENT_TEMPLATE = '''
package ${PACKAGE_NAME};

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDelete${RESOURCE_NAME}Event extends APIEvent {
    public APIDelete${RESOURCE_NAME}Event() {
    }

    public APIDelete${RESOURCE_NAME}Event(String apiId) {
        super(apiId);
    }

    public static APIDelete${RESOURCE_NAME}Event __example__() {
        APIDelete${RESOURCE_NAME}Event event = new APIDelete${RESOURCE_NAME}Event();
        return event;
    }
}
'''

    static final String INVENTORY_TEMPLATE = '''
package ${PACKAGE_NAME};

${IMPORT_PACKAGE}

@Inventory(mappingVOClass = ${ENTITY_CLASS}.class)
@PythonClassInventory
public class ${INVENTORY_NAME} implements Serializable, Cloneable {
    ${PRIVATE_FIELDS}

    ${GETTER_SETTER}
}
'''

    static final String API_PARAM_TEMPLATE = '''
    @APIParam(maxLength = %s)
    private %s %s;'''
}