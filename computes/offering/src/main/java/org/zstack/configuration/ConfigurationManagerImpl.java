package org.zstack.configuration;

import javassist.Modifier;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.DbEntityLister;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.core.log.LogSafeGson;
import org.zstack.core.rest.RESTApiJsonTemplateGenerator;
import org.zstack.header.AbstractService;
import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.allocator.HostAllocatorStrategyType;
import org.zstack.header.configuration.*;
import org.zstack.header.configuration.userconfig.DiskOfferingUserConfigValidator;
import org.zstack.header.configuration.userconfig.InstanceOfferingUserConfigValidator;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.*;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.search.APIGetMessage;
import org.zstack.header.search.APISearchMessage;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.tag.SystemTagCreateMessageValidator;
import org.zstack.header.tag.SystemTagValidator;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.NoView;
import org.zstack.identity.AccountManager;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.FieldUtils;
import org.zstack.utils.ObjectUtils;
import org.zstack.utils.TypeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.FieldPrinter;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;

public class ConfigurationManagerImpl extends AbstractService implements ConfigurationManager {
    private static final CLogger logger = Utils.getLogger(ConfigurationManagerImpl.class);
    private static final FieldPrinter printer = Utils.getFieldPrinter();
    private static final Set<Class> allowedInstanceOfferingMessageAfterSoftDeletion = new HashSet<>();
    private static final Set<Class> allowedDiskOfferingMessageAfterSoftDeletion = new HashSet<>();

    static {
        allowedDiskOfferingMessageAfterSoftDeletion.add(DiskOfferingDeletionMsg.class);
        allowedInstanceOfferingMessageAfterSoftDeletion.add(InstanceOfferingDeletionMsg.class);
    }

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private DbEntityLister dl;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private GlobalConfigFacade gcf;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private AccountManager acntMgr;
    private Set<String> generatedPythonClassName;
    private Map<String, InstanceOfferingFactory> instanceOfferingFactories = new HashMap<>();
    private Map<String, DiskOfferingFactory> diskOfferingFactories = new HashMap<>();
    private Set<String> generatedGroovyClassName = new HashSet<>();
    private List<PythonApiBindingWriter> pythonApiBindingWriters = new ArrayList<>();

    private void instanceOfferingPassThrough(InstanceOfferingMessage msg) {
        InstanceOfferingVO vo = dbf.findByUuid(msg.getInstanceOfferingUuid(), InstanceOfferingVO.class);
        if (vo == null && allowedInstanceOfferingMessageAfterSoftDeletion.contains(msg.getClass())) {
            InstanceOfferingEO eo = dbf.findByUuid(msg.getInstanceOfferingUuid(), InstanceOfferingEO.class);
            vo = ObjectUtils.newAndCopy(eo, InstanceOfferingVO.class);
        }

        if (vo == null) {
            bus.replyErrorByMessageType((Message) msg, String.format("cannot find InstanceOffering[uuid:%s]," +
                    " it may have been deleted", msg.getInstanceOfferingUuid()));
            return;
        }

        InstanceOfferingFactory factory = getInstanceOfferingFactory(vo.getType());
        InstanceOffering offering = factory.getInstanceOffering(vo);
        offering.handleMessage((Message) msg);
    }

    private void diskOfferingPassThrough(DiskOfferingMessage msg) {
        DiskOfferingVO vo = dbf.findByUuid(msg.getDiskOfferingUuid(), DiskOfferingVO.class);
        if (vo == null && allowedInstanceOfferingMessageAfterSoftDeletion.contains(msg.getClass())) {
            DiskOfferingEO eo = dbf.findByUuid(msg.getDiskOfferingUuid(), DiskOfferingEO.class);
            vo = ObjectUtils.newAndCopy(eo, DiskOfferingVO.class);
        }

        if (vo == null) {
            bus.replyErrorByMessageType((Message) msg, String.format("cannot find DiskOffering[uuid:%s]," +
                    " it may have been deleted", msg.getDiskOfferingUuid()));
            return;
        }

        DiskOfferingFactory factory = getDiskOfferingFactory(vo.getType());
        DiskOffering offering = factory.getDiskOffering(vo);
        offering.handleMessage((Message) msg);
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof InstanceOfferingMessage) {
            instanceOfferingPassThrough((InstanceOfferingMessage) msg);
        } else if (msg instanceof DiskOfferingMessage) {
            diskOfferingPassThrough((DiskOfferingMessage) msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        try {
            if (msg instanceof APICreateInstanceOfferingMsg) {
                handle((APICreateInstanceOfferingMsg) msg);
            } else if (msg instanceof APICreateDiskOfferingMsg) {
                handle((APICreateDiskOfferingMsg) msg);
            } else if (msg instanceof APIGenerateApiJsonTemplateMsg) {
                handle((APIGenerateApiJsonTemplateMsg) msg);
            } else if (msg instanceof APIGenerateTestLinkDocumentMsg) {
                handle((APIGenerateTestLinkDocumentMsg) msg);
            } else if (msg instanceof APIGenerateGroovyClassMsg) {
                handle((APIGenerateGroovyClassMsg) msg);
            } else if (msg instanceof APIGenerateSqlVOViewMsg) {
                handle((APIGenerateSqlVOViewMsg) msg);
            } else if (msg instanceof APIGenerateSqlForeignKeyMsg) {
                handle((APIGenerateSqlForeignKeyMsg) msg);
            } else if (msg instanceof APIGenerateSqlIndexMsg) {
                handle((APIGenerateSqlIndexMsg) msg);
            } else if (msg instanceof APIGetGlobalPropertyMsg) {
                handle((APIGetGlobalPropertyMsg) msg);
            } else {
                bus.dealWithUnknownMessage(msg);
            }
        } catch (IOException e) {
            throw new CloudRuntimeException(e);
        }
    }

    private void handle(APIGetGlobalPropertyMsg msg) {
        /*
        Properties p = System.getProperties();
        Enumeration keys = p.keys();
        List<String> pps = new ArrayList<String>();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            String value = (String) p.get(key);
            pps.add(String.format("%s = %s", key, value));
        }
         */

        List<String> pps = new ArrayList<>();
        for (Map.Entry<String, String> e : Platform.getGlobalProperties().entrySet()) {
            pps.add(String.format("%s: %s", e.getKey(), e.getValue()));
        }

        APIGetGlobalPropertyReply reply = new APIGetGlobalPropertyReply();
        reply.setProperties(pps);
        bus.reply(msg, reply);
    }

    private void handle(APIGenerateSqlIndexMsg msg) {
        SqlIndexGenerator generator = new SqlIndexGenerator(msg);
        generator.generate();
        APIGenerateSqlIndexEvent evt = new APIGenerateSqlIndexEvent(msg.getId());
        bus.publish(evt);
    }

    private void handle(APIGenerateSqlForeignKeyMsg msg) {
        SqlForeignKeyGenerator generator = new SqlForeignKeyGenerator(msg);
        generator.generate();
        APIGenerateSqlForeignKeyEvent evt = new APIGenerateSqlForeignKeyEvent(msg.getId());
        bus.publish(evt);
    }

    private void generateVOViewSql(StringBuilder sb, Class<?> entityClass) {
        if (!entityClass.isAnnotationPresent(Entity.class)) {
            throw new IllegalArgumentException(String.format("class[%s] is annotated by @EO but not annotated by @Entity", entityClass.getName()));
        }

        EO at = entityClass.getAnnotation(EO.class);
        if (!at.needView()) {
            return;
        }

        Class EOClazz = at.EOClazz();
        List<Field> fs = FieldUtils.getAllFields(entityClass);
        sb.append(String.format("\nCREATE VIEW `zstack`.`%s` AS SELECT ", entityClass.getSimpleName()));
        List<String> cols = new ArrayList<String>();
        for (Field f : fs) {
            if (!f.isAnnotationPresent(Column.class) || f.isAnnotationPresent(NoView.class)) {
                continue;
            }
            cols.add(f.getName());
        }
        sb.append(org.apache.commons.lang.StringUtils.join(cols, ", "));
        sb.append(String.format(" FROM `zstack`.`%s` WHERE %s IS NULL;\n", EOClazz.getSimpleName(), at.softDeletedColumn()));
    }

    private void handle(APIGenerateSqlVOViewMsg msg) {
        try {
            ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(true);
            scanner.addIncludeFilter(new AnnotationTypeFilter(EO.class));
            scanner.addExcludeFilter(new AnnotationTypeFilter(Controller.class));
            scanner.addExcludeFilter(new AnnotationTypeFilter(Component.class));
            StringBuilder sb = new StringBuilder();
            for (String pkg : msg.getBasePackageNames()) {
                for (BeanDefinition bd : scanner.findCandidateComponents(pkg)) {
                    Class<?> entityClazz = Class.forName(bd.getBeanClassName());
                    generateVOViewSql(sb, entityClazz);
                }
            }

            String exportPath = PathUtil.join(System.getProperty("user.home"), "zstack-mysql-view");
            FileUtils.deleteDirectory(new File(exportPath));
            File folder = new File(exportPath);
            folder.mkdirs();
            File outfile = new File(PathUtil.join(exportPath, "view.sql"));
            FileUtils.writeStringToFile(outfile, sb.toString());

            APIGenerateSqlVOViewEvent evt = new APIGenerateSqlVOViewEvent(msg.getId());
            bus.publish(evt);
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    private void handle(APIGenerateGroovyClassMsg msg) {
        generateGroovyClasses(msg);
    }

    private String whiteSpace(int num) {
        return StringUtils.repeat(" ", num);
    }

    private String classToInventoryPythonClass(Class<?> clazz) {
        StringBuilder sb = new StringBuilder();
        boolean hasParent = (clazz.getSuperclass() != Object.class);

        if (hasParent && !isPythonClassGenerated(clazz.getSuperclass())) {
            sb.append(classToInventoryPythonClass(clazz.getSuperclass()));
        }

        if (hasParent) {
            sb.append(String.format("\nclass %s(%s):", clazz.getSimpleName(), clazz.getSuperclass().getSimpleName()));
            sb.append(String.format("\n%sdef __init__(self):", whiteSpace(4)));
            sb.append(String.format("\n%ssuper(%s, self).__init__()", whiteSpace(8), clazz.getSimpleName()));
        } else {
            sb.append(String.format("\nclass %s(object):", clazz.getSimpleName()));
            sb.append(String.format("\n%sdef __init__(self):", whiteSpace(4)));
        }

        Field[] fs = clazz.getDeclaredFields();
        for (Field f : fs) {
            sb.append(String.format("\n%sself.%s = None", whiteSpace(8), f.getName()));
        }

        sb.append(String.format("\n\n%sdef evaluate(self, inv):", whiteSpace(4)));
        if (hasParent) {
            sb.append(String.format("\n%ssuper(%s, self).evaluate(inv)", whiteSpace(8), clazz.getSimpleName()));
        }
        for (Field f : fs) {
            sb.append(String.format("\n%sif hasattr(inv, '%s'):", whiteSpace(8), f.getName()));
            sb.append(String.format("\n%sself.%s = inv.%s", whiteSpace(12), f.getName(), f.getName()));
            sb.append(String.format("\n%selse:", whiteSpace(8)));
            sb.append(String.format("\n%sself.%s = None\n", whiteSpace(12), f.getName()));
        }

        sb.append("\n\n");
        markPythonClassAsGenerated(clazz);
        return sb.toString();
    }

    private void classToApiMessageGroovyInformation(StringBuilder sb, Class<?> clazz) {
        if (Modifier.isStatic(clazz.getModifiers())) {
            return;
        }

        String name = clazz.getSimpleName().replace("\\.", "_").toUpperCase();
        sb.append(String.format("\n%sdef %s = [name: '%s',", whiteSpace(4), name, clazz.getName()));

        List<Field> fs = FieldUtils.getAllFields(clazz);
        List<String> mandatoryFields = new ArrayList<String>();
        for (Field f : fs) {
            APIParam at = f.getAnnotation(APIParam.class);
            if (at != null && at.required()) {
                mandatoryFields.add(String.format("'%s'", f.getName()));
            }
        }
        sb.append(String.format("requiredFields: [%s]]\n", StringUtils.join(mandatoryFields.iterator(), ",")));
    }

    private void generateApiMessageGroovyClass(StringBuilder sb, List<String> basePkgs) {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(true);
        scanner.addIncludeFilter(new AssignableTypeFilter(APIMessage.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(APIReply.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(APIEvent.class));
        scanner.addExcludeFilter(new AnnotationTypeFilter(Controller.class));
        scanner.addExcludeFilter(new AnnotationTypeFilter(Component.class));
        for (String pkg : basePkgs) {
            for (BeanDefinition bd : scanner.findCandidateComponents(pkg)) {
                try {
                    Class<?> clazz = Class.forName(bd.getBeanClassName());
                    //classToApiMessageGroovyClass(sb, clazz);
                    classToApiMessageGroovyInformation(sb, clazz);
                } catch (ClassNotFoundException e) {
                    logger.warn(String.format("Unable to generate groovy class for %s", bd.getBeanClassName()), e);
                }
            }
        }
    }

    private void generateGroovyNotNullObjectClass(StringBuilder sb) {
        sb.append(String.format("public class NotNullObject {}\n\n"));
    }

    private void generateRootMessageGroovyClass(StringBuilder sb) {
        sb.append(String.format("\npublic class %s {", Message.class.getSimpleName()));
        sb.append(String.format("\n%sMessageProperites props", whiteSpace(4)));
        sb.append(String.format("\n%sdef headers = [:]", whiteSpace(4)));
        sb.append(String.format("\n%sString id = Platform.uuid()", whiteSpace(4)));
        sb.append(String.format("\n%sString serviceId = 'ApiMediator'", whiteSpace(4)));
        sb.append(String.format("\n%sdef creatingTime\n", whiteSpace(4)));
        sb.append(String.format("\n%sString toString() {", whiteSpace(4)));
        sb.append(String.format("\n%sproperties.each {", whiteSpace(8)));
        sb.append(String.format("\n%sif (it.value instanceof NotNullObject)" +
                " { throw new UIRuntimeException(\"propertiy ${it.key} can not be null in ${fullName()}\")}", whiteSpace(12)));
        sb.append(String.format("\n%s}\n", whiteSpace(8)));
        sb.append(String.format("\n%sreturn JSON.dump([(fullName()):this])", whiteSpace(8)));
        sb.append(String.format("\n%s}\n", whiteSpace(4)));
        sb.append(String.format("\n%sdef fullName() {}", whiteSpace(4)));
        sb.append(String.format("\n}\n\n"));
        generatedGroovyClassName.add(Message.class.getSimpleName());
    }

    private void generateGroovyClasses(APIGenerateGroovyClassMsg msg) {
        try {
            APIGenerateGroovyClassEvent evt = new APIGenerateGroovyClassEvent(msg.getId());
            String exportPath = msg.getOutputPath();
            if (exportPath == null) {
                exportPath = PathUtil.join(System.getProperty("user.home"), "zstack-groovy-template");
            }
            List<String> basePkgs = msg.getBasePackageNames();
            if (basePkgs == null || basePkgs.isEmpty()) {
                basePkgs = new ArrayList<String>(1);
                basePkgs.add("org.zstack");
            }

            FileUtils.deleteDirectory(new File(exportPath));
            File folder = new File(exportPath);
            folder.mkdirs();
            StringBuilder sb = new StringBuilder();
            sb.append("package zstack.ui.api\n\n");
            sb.append("interface ApiConstants {\n");
            sb.append(String.format("%sdef API_EVENT_TYPE = '%s'\n", whiteSpace(4), new APIEvent().getType()));
            /*
            sb.append("import zstack.ui.core.JSON\n\n");
            sb.append(String.mediaType("public class Session {\n"));
            sb.append(String.mediaType("%sdef uuid\n", whiteSpace(4)));
            sb.append(String.mediaType("}\n\n"));
            generateRootMessageGroovyClass(sb);
            generateGroovyNotNullObjectClass(sb);
            */
            generateApiMessageGroovyClass(sb, basePkgs);
            sb.append("\n}\n");
            File classFile = new File(PathUtil.join(folder.getAbsolutePath(), "ApiConstants.groovy"));
            FileUtils.writeStringToFile(classFile, sb.toString());
            bus.publish(evt);
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    private void classToApiMessageGroovyClass(StringBuilder sb, Class<?> clazz) {
        if (generatedGroovyClassName.contains(clazz.getSimpleName())) {
            /* This class was generated as other's parent class */
            return;
        }

        boolean hasParent = (clazz.getSuperclass() != Object.class);

        if (hasParent && !generatedGroovyClassName.contains(clazz.getSuperclass().getSimpleName())) {
            classToApiMessageGroovyClass(sb, clazz.getSuperclass());
        }

        if (hasParent) {
            sb.append(String.format("\npublic class %s extends %s {", clazz.getSimpleName(), clazz.getSuperclass().getSimpleName()));
        } else {
            sb.append(String.format("\npublic class %s {", clazz.getSimpleName()));
        }

        Field[] fs = clazz.getDeclaredFields();
        for (Field f : fs) {
            APINoSee nosee = f.getAnnotation(APINoSee.class);
            if (nosee != null) {
                continue;
            }

            if (Modifier.isStatic(f.getModifiers())) {
                continue;
            }

            APIParam at = f.getAnnotation(APIParam.class);
            if (at != null && at.required()) {
                sb.append(String.format("\n%sdef %s = new NotNullObject()", whiteSpace(4), f.getName()));
            } else {
                sb.append(String.format("\n%sdef %s", whiteSpace(4), f.getName()));
            }
        }

        sb.append(String.format("\n\n%sdef fullName() { return '%s' }", whiteSpace(4), clazz.getName()));
        if (APIEvent.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
            try {
                APIEvent evt = (APIEvent) clazz.newInstance();
                sb.append(String.format("\n%sdef eventType() { return '%s' }", whiteSpace(4), evt.getType().toString()));
            } catch (Exception e) {
                throw new CloudRuntimeException(String.format("cannot generate event type for %s", clazz.getName()), e);
            }
        }
        sb.append(String.format("\n}\n\n"));
        generatedGroovyClassName.add(clazz.getSimpleName());
    }

    private String classToApiMessagePythonClass(Class<?> clazz) {
        return classToApiMessagePythonClass(clazz, null);
    }

    private String classToApiMessagePythonClass(Class<?> clazz, RestRequest request) {
        StringBuilder result = new StringBuilder();
        boolean emptyLine = true;

        String signature = String.format("%s_FULL_NAME", clazz.getSimpleName()).toUpperCase();
        result.append(String.format("\n%s = '%s'", signature, clazz.getName()));
        result.append(String.format("\nclass %s(object):", clazz.getSimpleName()));
        result.append(String.format("\n%sFULL_NAME='%s'", whiteSpace(4), clazz.getName()));
        if (request != null) {
            result.append(String.format("\n%sRESPONSE_NAME='%s'", whiteSpace(4), request.responseClass().getSimpleName()));
        }

        StringBuilder sb = new StringBuilder();
        List<Field> fs = FieldUtils.getAllFields(clazz);
        Map<String, NoLogging.Type> sensitiveFields = new HashMap<>();
        for (Field f : fs) {
            APINoSee nosee = f.getAnnotation(APINoSee.class);
            if (nosee != null && !f.getName().equals("timeout") && !f.getName().equals("session")) {
                continue;
            }

            APIParam at = f.getAnnotation(APIParam.class);
            OverriddenApiParams o = clazz.getAnnotation(OverriddenApiParams.class);
            if (o != null) {
                for (OverriddenApiParam op : o.value()) {
                    if (op.field().equals(f.getName())) {
                        at = op.param();
                        break;
                    }
                }
            }

            NoLogging noLogging = f.getAnnotation(NoLogging.class);
            if (noLogging != null) {
                sensitiveFields.put(f.getName(), noLogging.type());
            }

            if (at != null && at.required()) {
                sb.append(String.format("\n%s#mandatory field", whiteSpace(8)));
            }
            if (at != null && at.validValues().length != 0) {
                List<String> values = new ArrayList<>(at.validValues().length);
                Collections.addAll(values, at.validValues());
                sb.append(String.format("\n%s#valid values: %s", whiteSpace(8), values));
            } else if (at != null && at.validEnums().length != 0) {
                List<String> values = CollectionUtils.valuesForEnums(at.validEnums()).collect(Collectors.toList());
                sb.append(String.format("\n%s#valid values: %s", whiteSpace(8), values));
            }
            if (at != null && at.validRegexValues() != null && at.validRegexValues().trim().equals("") == false) {
                String regex = at.validRegexValues().trim();
                sb.append(String.format("\n%s#valid regex values: %s", whiteSpace(8), regex));
            }

            if (Collection.class.isAssignableFrom(f.getType())) {
                if (at != null && at.required()) {
                    sb.append(String.format("\n%sself.%s = NotNoneList()", whiteSpace(8), f.getName()));
                } else {
                    sb.append(String.format("\n%sself.%s = OptionalList()", whiteSpace(8), f.getName()));
                }
            } else if (Map.class.isAssignableFrom(f.getType())) {
                if (at != null && at.required()) {
                    sb.append(String.format("\n%sself.%s = NotNoneMap()", whiteSpace(8), f.getName()));
                } else {
                    sb.append(String.format("\n%sself.%s = OptionalMap()", whiteSpace(8), f.getName()));
                }
            } else {
                if (at != null && at.required()) {
                    sb.append(String.format("\n%sself.%s = NotNoneField()", whiteSpace(8), f.getName()));
                } else {
                    sb.append(String.format("\n%sself.%s = None", whiteSpace(8), f.getName()));
                }
            }

            emptyLine = false;
        }

        if (emptyLine) {
            sb.append(String.format("\n%spass", whiteSpace(8)));
        }

        if (!sensitiveFields.isEmpty()) {
            result.append(String.format("\n%s@log.sensitive_fields(\"%s\")", whiteSpace(4),
                    String.join("\", \"", sensitiveFields.keySet())));
        }

        result.append(String.format("\n%sdef __init__(self):", whiteSpace(4)));
        result.append(sb.toString());
        result.append("\n\n");
        markPythonClassAsGenerated(clazz);
        return result.toString();
    }

    private String classToApiEventPythonClass(Class<?> clazz) {
        Map<String, NoLogging.Type> sensitiveFields = LogSafeGson.getSensitiveFields(clazz);
        if (sensitiveFields.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        result.append(String.format("\nclass %s(object):", clazz.getSimpleName()));
        result.append(String.format("\n%sFULL_NAME='%s'", whiteSpace(4), clazz.getName()));
        result.append(String.format("\n%s@log.sensitive_fields(\"%s\")", whiteSpace(4),
                String.join("\", \"", sensitiveFields.keySet())));
        result.append(String.format("\n%sdef __init__(self):", whiteSpace(4)));
        result.append(String.format("\n%spass", whiteSpace(8)));
        result.append("\n\n");
        markPythonClassAsGenerated(clazz);
        return result.toString();
    }

    private void generateSimplePythonClass(StringBuilder sb, Class<?> clazz) {
        sb.append(String.format("\nclass %s(object):", clazz.getSimpleName()));
        sb.append(String.format("\n%sdef __init__(self):", whiteSpace(4)));
        for (Field f : clazz.getDeclaredFields()) {
            sb.append(String.format("\n%sself.%s = None", whiteSpace(8), f.getName()));
        }
        sb.append("\n\n");
    }

    private void generateErrorCodePythonClass(StringBuilder sb) {
        generateSimplePythonClass(sb, ErrorCode.class);
    }

    private void generateSessionPythonClass(StringBuilder sb) {
        sb.append(String.format("\nclass Session(object):"));
        sb.append(String.format("\n%sdef __init__(self):", whiteSpace(4)));
        sb.append(String.format("\n%sself.uuid = None", whiteSpace(8)));
        sb.append("\n\n");
    }


    private void generateSeachConditionClass(StringBuilder sb) {
        generateSimplePythonClass(sb, APISearchMessage.NOLTriple.class);
        generateSimplePythonClass(sb, APISearchMessage.NOVTriple.class);
    }

    private void generateMandoryFieldClass(StringBuilder sb) {
        sb.append(String.format("\n\nclass NotNoneField(object):"));
        sb.append(String.format("\n%spass\n", whiteSpace(4)));

        sb.append(String.format("\n\nclass NotNoneList(object):"));
        sb.append(String.format("\n%spass\n", whiteSpace(4)));

        sb.append(String.format("\n\nclass OptionalList(object):"));
        sb.append(String.format("\n%spass\n", whiteSpace(4)));

        sb.append(String.format("\n\nclass NotNoneMap(object):"));
        sb.append(String.format("\n%spass\n", whiteSpace(4)));

        sb.append(String.format("\n\nclass OptionalMap(object):"));
        sb.append(String.format("\n%spass\n", whiteSpace(4)));
    }

    private void generateBaseApiMessagePythonClass(StringBuilder sb) {
        sb.append(String.format("\nclass %s(object):", APIMessage.class.getSimpleName()));
        sb.append(String.format("\n%sdef __init__(self):", whiteSpace(4)));
        sb.append(String.format("\n%ssuper(%s, self).__init__()", whiteSpace(8), APIMessage.class.getSimpleName()));
        sb.append(String.format("\n%sself.timeout = None", whiteSpace(8)));
        for (Field f : APIMessage.class.getDeclaredFields()) {
            sb.append(String.format("\n%sself.%s = None", whiteSpace(8), f.getName()));
        }
        sb.append("\n\n");
        markPythonClassAsGenerated(APIMessage.class);
        sb.append(classToApiMessagePythonClass(APIDeleteMessage.class));
        markPythonClassAsGenerated(APIDeleteMessage.class);
        generateSeachConditionClass(sb);
    }

    private void generateApiNameList(StringBuilder sb, List<String> apiNames) {
        Collections.sort(apiNames);
        sb.append(String.format("\napi_names = ["));
        for (String name : apiNames) {
            sb.append(String.format("\n%s'%s',", whiteSpace(4), name));
        }
        sb.append("\n]\n");
    }

    private void generateApiMessagePythonClass(StringBuilder sb, List<String> basePkgs) {
        generateSessionPythonClass(sb);
        generateErrorCodePythonClass(sb);
        generateBaseApiMessagePythonClass(sb);
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(true);
        scanner.addIncludeFilter(new AssignableTypeFilter(APIMessage.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(APIReply.class));
        scanner.addExcludeFilter(new AnnotationTypeFilter(Controller.class));
        scanner.addExcludeFilter(new AnnotationTypeFilter(NoPython.class));
        scanner.addExcludeFilter(new AnnotationTypeFilter(Component.class));
        List<String> apiNames = new ArrayList<>(100);
        for (String pkg : basePkgs) {
            for (BeanDefinition bd : scanner.findCandidateComponents(pkg).stream().sorted(Comparator.comparing(BeanDefinition::getBeanClassName)).collect(Collectors.toList())) {
                try {
                    Class<?> clazz = Class.forName(bd.getBeanClassName());
                    if (clazz == APIMessage.class || clazz == APIListMessage.class || clazz == APIDeleteMessage.class || clazz == APISearchMessage.class) {
                        continue;
                    }
                    if (TypeUtils.isTypeOf(clazz, APISearchMessage.class, APIListMessage.class, APIGetMessage.class)) {
                        continue;
                    }
                    if (Modifier.isAbstract(clazz.getModifiers())) {
                        continue;
                    }
                    RestRequest request;
                    if ((request = clazz.getAnnotation(RestRequest.class)) == null) {
                        continue;
                    }
                    if (isPythonClassGenerated(clazz)) {
                        /* This class was generated as other's parent class */
                        continue;
                    }
                    sb.append(classToApiMessagePythonClass(clazz, request));
                    sb.append(classToApiEventPythonClass(request.responseClass()));
                    apiNames.add(clazz.getSimpleName());
                } catch (ClassNotFoundException e) {
                    logger.warn(String.format("Unable to generate python class for %s", bd.getBeanClassName()), e);
                }
            }
        }

        apiNames.remove(APIMessage.class.getSimpleName());
        apiNames.remove(APIListMessage.class.getSimpleName());
        apiNames.remove(APIDeleteMessage.class.getSimpleName());
        apiNames.remove(APISearchMessage.class.getSimpleName());
        generateApiNameList(sb, apiNames);
    }

    private void generateConstantFromClassField(StringBuilder sb, Class<?> clazz) throws IllegalArgumentException, IllegalAccessException {
        sb.append("\n#").append(clazz.getSimpleName());
        if (clazz.isEnum()) {
            for (Field f : clazz.getDeclaredFields()) {
                if (f.isEnumConstant()) {
                    String name = f.getName().toUpperCase();
                    f.setAccessible(true);
                    String value = f.get(null).toString();
                    sb.append(String.format("\n%s = '%s'", name, value));
                }
            }
        } else {
            for (Field f : clazz.getDeclaredFields()) {
                PythonClass at = f.getAnnotation(PythonClass.class);
                if (at == null) {
                    continue;
                }

                String name = f.getName().toUpperCase();
                f.setAccessible(true);
                String value = f.get(null).toString();
                sb.append(String.format("\n%s = '%s'", name, value));
            }
        }
        sb.append("\n");
    }

    private void generateConstantPythonClass(StringBuilder sb, List<String> basePkgs) {
        Reflections reflections = Platform.getReflections();
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(PythonClass.class);
        for (Class<?> clazz : annotated.stream().sorted((c1, c2) -> {
            return c1.getSimpleName().compareTo(c2.getSimpleName());
        }).collect(Collectors.toList())) {
            try {
                generateConstantFromClassField(sb, clazz);
            } catch (Exception e) {
                logger.warn(String.format("Unable to generate python class for %s", clazz.getName()), e);
            }
        }
    }

    private void generateInventoryPythonClass(StringBuilder sb, List<String> basePkgs) {
        List<String> inventoryPython = new ArrayList<>();
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(PythonClassInventory.class));
        scanner.addExcludeFilter(new AnnotationTypeFilter(Component.class));
        for (String pkg : basePkgs) {
            for (BeanDefinition bd : scanner.findCandidateComponents(pkg).stream().sorted((bd1, bd2) -> {
                return bd1.getBeanClassName().compareTo(bd2.getBeanClassName());
            }).collect(Collectors.toList())) {
                try {
                    Class<?> clazz = Class.forName(bd.getBeanClassName());
                    if (isPythonClassGenerated(clazz)) {
                        /* This class was generated as other's parent class */
                        continue;
                    }
                    inventoryPython.add(classToInventoryPythonClass(clazz));
                } catch (Exception e) {
                    logger.warn(String.format("Unable to generate python class for %s", bd.getBeanClassName()), e);
                }
            }
        }

        for (String invstr : inventoryPython) {
            sb.append(invstr);
        }
    }

    private boolean isPythonClassGenerated(Class<?> clazz) {
        return generatedPythonClassName.contains(clazz.getSimpleName());
    }

    private void markPythonClassAsGenerated(Class<?> clazz) {
        generatedPythonClassName.add(clazz.getSimpleName());
    }

    private void generateGlobalConfigPythonConstant(StringBuilder pysb) {
        pysb.append("\n#GlobalConfigPythonConstant");
        Map<String, List<String>> configs = new HashMap<>();
        for (GlobalConfig c : gcf.getAllConfig().values()) {
            List<String> cnames = configs.get(c.getCategory());
            if (cnames == null) {
                cnames = new ArrayList<>();
                configs.put(c.getCategory(), cnames);
            }
            cnames.add(c.getName());
        }

        for (Map.Entry<String, List<String>> e : configs.entrySet()
                .stream()
                .sorted((e1, e2) -> {
                    return e1.getKey().toUpperCase().compareTo(e2.getKey().toUpperCase());
                })
                .collect(Collectors.toList())) {
            pysb.append(String.format("\nclass GlobalConfig_%s(object):", e.getKey().toUpperCase().replaceAll("\\.", "_")));
            for (String cname : e.getValue()) {
                String var = cname.replaceAll("\\.", "_").replaceAll("-", "_");
                pysb.append(String.format("\n%s%s = '%s'", whiteSpace(4), var.toUpperCase(), cname));
            }
            pysb.append("\n");
            pysb.append(String.format("\n%s@staticmethod", whiteSpace(4)));
            pysb.append(String.format("\n%sdef get_category():", whiteSpace(4)));
            pysb.append(String.format("\n%sreturn '%s'\n", whiteSpace(8), e.getKey()));
        }
    }

    private void handle(APIGenerateTestLinkDocumentMsg msg) throws IOException {
        String outputDir = msg.getOutputDir();
        if (outputDir == null) {
            outputDir = PathUtil.join(System.getProperty("user.home"), "zstack-testlink");
        }

        FileUtils.deleteDirectory(new File(outputDir));
        File folder = new File(outputDir);
        folder.mkdirs();
        TestLinkDocumentGenerator.generateRequirementSpec(outputDir);
        APIGenerateTestLinkDocumentEvent evt = new APIGenerateTestLinkDocumentEvent(msg.getId());
        evt.setOutputDir(outputDir);
        bus.publish(evt);
    }

    private void handle(APIGenerateApiJsonTemplateMsg msg) throws IOException {
        generateApiJsonTemplate(msg.getExportPath(), msg.getBasePackageNames());
        APIGenerateApiJsonTemplateEvent evt = new APIGenerateApiJsonTemplateEvent(msg.getId());
        bus.publish(evt);
    }

    public void generateApiJsonTemplate(String exportPath, List<String> basePkgs) throws IOException {
        String pythonApisDir = System.getProperty("pythonApisDir");
        if (pythonApisDir == null) {
            pythonApisDir = System.getProperty("user.home");
        }
        generatedPythonClassName = new HashSet<>();
        if (exportPath == null) {
            exportPath = PathUtil.join(pythonApisDir, "zstack-python-template");
        }
        if (basePkgs == null || basePkgs.isEmpty()) {
            basePkgs = new ArrayList<>(1);
            basePkgs.add("org.zstack");
        }

        FileUtils.deleteDirectory(new File(exportPath));
        File folder = new File(exportPath);
        folder.mkdirs();
        File jsonFolder = new File(PathUtil.join(folder.getAbsolutePath(), "json"));
        jsonFolder.mkdirs();
        File pythonFolder = new File(PathUtil.join(folder.getAbsolutePath(), "python"));
        pythonFolder.mkdirs();

        // write api_messages.py
        {
            StringBuilder apiNameBuilder = new StringBuilder();
            apiNameBuilder.append("api_names = [\n");
            ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AssignableTypeFilter(APIEvent.class));
            scanner.addIncludeFilter(new AssignableTypeFilter(APIReply.class));
            scanner.addIncludeFilter(new AssignableTypeFilter(APIMessage.class));
            scanner.addExcludeFilter(new AnnotationTypeFilter(Controller.class));
            scanner.addExcludeFilter(new AnnotationTypeFilter(Component.class));
            for (String pkg : basePkgs) {
                for (BeanDefinition bd : scanner.findCandidateComponents(pkg)) {
                    Class<?> clazz = null;
                    try {
                        clazz = Class.forName(bd.getBeanClassName());
                        logger.debug(String.format("dumping message: %s", bd.getBeanClassName()));
                        String template = RESTApiJsonTemplateGenerator.dump(clazz);
                        FileUtils.write(new File(PathUtil.join(jsonFolder.getAbsolutePath(),
                                clazz.getName() + ".json")), template);
                    } catch (Exception e) {
                        logger.warn(String.format("Unable to generate json template for %s", bd.getBeanClassName()), e);
                    }

                    if (clazz != null && APIMessage.class.isAssignableFrom(clazz)) {
                        if (TypeUtils.isTypeOf(clazz, APISearchMessage.class, APIGetMessage.class, APIListMessage.class)) {
                            continue;
                        }
                        apiNameBuilder.append(String.format("%s'%s',\n", whiteSpace(4), clazz.getName()));
                    }
                }
            }
            apiNameBuilder.append("]\n");
            FileUtils.write(new File(PathUtil.join(pythonFolder.getAbsolutePath(), "api_messages.py")),
                    apiNameBuilder.toString());
        }

        // write inventory.py
        {
            StringBuilder pysb = new StringBuilder();
            pysb.append("#-*-coding: UTF-8 -*-\n");
            pysb.append("from zstacklib.utils import log");
            generateMandoryFieldClass(pysb);
            generateApiMessagePythonClass(pysb, basePkgs);
            generateInventoryPythonClass(pysb, basePkgs);
            generateConstantPythonClass(pysb, basePkgs);
            generateGlobalConfigPythonConstant(pysb);
            for (PythonApiBindingWriter writer : pythonApiBindingWriters) {
                pysb.append("\n");
                writer.writePython(pysb);
            }
            String pyStr = pysb.toString();
            FileUtils.write(new File(PathUtil.join(pythonFolder.getAbsolutePath(), "inventory.py")), pyStr);
        }

        // write api_actions.py
        {
            PythonApiActionGenerator.generatePythonApiAction(basePkgs, pythonFolder.getAbsolutePath());
        }

        logger.info(String.format("Generated result in %s", folder.getAbsolutePath()));
        generatedPythonClassName = null;
    }

    private void handle(APICreateInstanceOfferingMsg msg) {
        APICreateInstanceOfferingEvent evt = new APICreateInstanceOfferingEvent(msg.getId());

        String type = msg.getType() == null ? UserVmInstanceOfferingFactory.type.toString() : msg.getType();
        InstanceOfferingFactory f = getInstanceOfferingFactory(type);

        InstanceOfferingVO vo = new InstanceOfferingVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        HostAllocatorStrategyType allocType = msg.getAllocatorStrategy() == null ? HostAllocatorStrategyType
                .valueOf(HostAllocatorConstant.LEAST_VM_PREFERRED_HOST_ALLOCATOR_STRATEGY_TYPE) : HostAllocatorStrategyType.valueOf(msg.getAllocatorStrategy());
        vo.setAllocatorStrategy(allocType.toString());
        vo.setName(msg.getName());
        vo.setCpuNum(msg.getCpuNum());
        vo.setCpuSpeed(msg.getCpuSpeed());
        vo.setDescription(msg.getDescription());
        vo.setState(InstanceOfferingState.Enabled);
        vo.setMemorySize(msg.getMemorySize());
        vo.setReservedMemorySize(msg.getReservedMemorySize());
        vo.setDuration(InstanceOfferingDuration.Permanent);
        vo.setType(type);

        InstanceOfferingInventory inv = new SQLBatchWithReturn<InstanceOfferingInventory>() {
            @Override
            @Deferred
            protected InstanceOfferingInventory scripts() {
                Defer.guard(() -> dbf.remove(vo));
                vo.setAccountUuid(msg.getSession().getAccountUuid());
                InstanceOfferingInventory ret = f.createInstanceOffering(vo, msg);
                tagMgr.createTagsFromAPICreateMessage(msg, vo.getUuid(), InstanceOfferingVO.class.getSimpleName());
                return ret;
            }
        }.execute();

        tagMgr.createTagsFromAPICreateMessage(msg, vo.getUuid(), InstanceOfferingVO.class.getSimpleName());

        evt.setInventory(inv);
        bus.publish(evt);
        logger.debug("Successfully added instance offering: " + printer.print(inv));
    }

    @Deferred
    private void handle(APICreateDiskOfferingMsg msg) {
        DiskOfferingVO vo = new DiskOfferingVO();

        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setDescription(msg.getDescription());
        vo.setName(msg.getName());
        vo.setDiskSize(msg.getDiskSize());
        vo.setSortKey(msg.getSortKey());
        vo.setState(DiskOfferingState.Enabled);
        if (msg.getAllocationStrategy() == null) {
            vo.setAllocatorStrategy(PrimaryStorageConstant.DEFAULT_PRIMARY_STORAGE_ALLOCATION_STRATEGY_TYPE);
        } else {
            vo.setAllocatorStrategy(msg.getAllocationStrategy());
        }
        if (msg.getType() == null) {
            vo.setType(DefaultDiskOfferingFactory.type.toString());
        } else {
            vo.setType(msg.getType());
        }

        DiskOfferingFactory f = getDiskOfferingFactory(vo.getType());

        DiskOfferingInventory inv = new SQLBatchWithReturn<DiskOfferingInventory>() {
            @Override
            protected DiskOfferingInventory scripts() {
                vo.setAccountUuid(msg.getSession().getAccountUuid());
                return f.createDiskOffering(vo, msg);
            }
        }.execute();

        Defer.guard(() -> dbf.remove(vo));
        tagMgr.createTagsFromAPICreateMessage(msg, inv.getUuid(), DiskOfferingVO.class.getSimpleName());

        APICreateDiskOfferingEvent evt = new APICreateDiskOfferingEvent(msg.getId());
        evt.setInventory(inv);
        bus.publish(evt);
        logger.debug("Successfully added disk offering: " + printer.print(inv));
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(ConfigurationConstant.SERVICE_ID);
    }

    private void populateExtensions() {
        for (InstanceOfferingFactory f : pluginRgty.getExtensionList(InstanceOfferingFactory.class)) {
            InstanceOfferingFactory old = instanceOfferingFactories.get(f.getInstanceOfferingType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate InstanceOfferingFactory[%s, %s] for type[%s]", f.getClass().getName(),
                        old.getClass().getName(), f.getInstanceOfferingType()));
            }
            instanceOfferingFactories.put(f.getInstanceOfferingType().toString(), f);
        }

        for (DiskOfferingFactory f : pluginRgty.getExtensionList(DiskOfferingFactory.class)) {
            DiskOfferingFactory old = diskOfferingFactories.get(f.getDiskOfferingType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate DiskOfferingFactory[%s, %s] for type[%s]",
                        f.getClass().getName(), old.getClass().getName(), f.getDiskOfferingType()));
            }
            diskOfferingFactories.put(f.getDiskOfferingType().toString(), f);
        }

        List<PythonApiBindingWriter> exts = pluginRgty.getExtensionList(PythonApiBindingWriter.class);
        List<PythonApiBindingWriter> sortedExts = exts.stream().sorted((e1, e2) ->
                e1.getClass().getName().compareTo(e2.getClass().getName())).collect(Collectors.toList());
        for (PythonApiBindingWriter ext : sortedExts) {
            pythonApiBindingWriters.add(ext);
        }

    }

    private InstanceOfferingFactory getInstanceOfferingFactory(String type) {
        InstanceOfferingFactory f = instanceOfferingFactories.get(type);
        if (f == null) {
            throw new IllegalArgumentException(String.format("unable to find InstanceOfferingFactory with type[%s]", type));
        }
        return f;
    }

    private DiskOfferingFactory getDiskOfferingFactory(String type) {
        DiskOfferingFactory f = diskOfferingFactories.get(type);
        if (f == null) {
            throw new IllegalArgumentException(String.format("unable to find DiskOfferingFactory with type[%s]", type));
        }
        return f;
    }

    private void prepareSystemTags() {
        class InstanceOfferingUserConfigValidator implements SystemTagCreateMessageValidator, SystemTagValidator {

            private void check(String resourceUuid, String systemTag) {
                int existUserdataTagCount = InstanceOfferingSystemTags.INSTANCE_OFFERING_USER_CONFIG.getTags(resourceUuid, InstanceOfferingVO.class).size();
                if (existUserdataTagCount > 0) {
                    throw new OperationFailureException(argerr(
                            "Already have one userdata systemTag for instanceOffering[uuid: %s].",
                            resourceUuid));
                }

                String configStr = InstanceOfferingSystemTags.INSTANCE_OFFERING_USER_CONFIG.getTokenByTag(systemTag, InstanceOfferingSystemTags.INSTANCE_OFFERING_USER_CONFIG_TOKEN);
                checkInstanceOfferingUserConfig(configStr);
            }

            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                if (!InstanceOfferingSystemTags.INSTANCE_OFFERING_USER_CONFIG.isMatch(systemTag)) {
                    return;
                }
                check(resourceUuid, systemTag);
            }

            @Override
            public void validateSystemTagInCreateMessage(APICreateMessage msg) {
                int userdataTagCount = 0;
                for (String sysTag : msg.getSystemTags()) {
                    if (InstanceOfferingSystemTags.INSTANCE_OFFERING_USER_CONFIG.isMatch(sysTag)) {
                        if (userdataTagCount > 0) {
                            throw new OperationFailureException(argerr(
                                    "Shouldn't be more than one systemTag for one instanceOffering."));
                        }
                        userdataTagCount++;

                        check(msg.getResourceUuid(), sysTag);
                    }
                }
            }
        }
        InstanceOfferingUserConfigValidator instanceOfferingUserConfigValidator = new InstanceOfferingUserConfigValidator();
        tagMgr.installCreateMessageValidator(InstanceOfferingVO.class.getSimpleName(), instanceOfferingUserConfigValidator);
        InstanceOfferingSystemTags.INSTANCE_OFFERING_USER_CONFIG.installValidator(instanceOfferingUserConfigValidator);

        class DiskOfferingUserConfigValidator implements SystemTagCreateMessageValidator, SystemTagValidator {

            private void check(String resourceUuid, String systemTag) {
                int existUserdataTagCount = DiskOfferingSystemTags.DISK_OFFERING_USER_CONFIG.getTags(resourceUuid, DiskOfferingVO.class).size();
                if (existUserdataTagCount > 0) {
                    throw new OperationFailureException(argerr(
                            "Already have one userdata systemTag for diskOffering[uuid: %s].",
                            resourceUuid));
                }

                String configStr = DiskOfferingSystemTags.DISK_OFFERING_USER_CONFIG.getTokenByTag(systemTag, DiskOfferingSystemTags.DISK_OFFERING_USER_CONFIG_TOKEN);
                checkDiskOfferingUserConfig(configStr);
            }

            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                if (!DiskOfferingSystemTags.DISK_OFFERING_USER_CONFIG.isMatch(systemTag)) {
                    return;
                }
                check(resourceUuid, systemTag);
            }

            @Override
            public void validateSystemTagInCreateMessage(APICreateMessage msg) {
                int userdataTagCount = 0;
                for (String sysTag : msg.getSystemTags()) {
                    if (InstanceOfferingSystemTags.INSTANCE_OFFERING_USER_CONFIG.isMatch(sysTag)) {
                        if (userdataTagCount > 0) {
                            throw new OperationFailureException(argerr(
                                    "Shouldn't be more than one systemTag for one instanceOffering."));
                        }
                        userdataTagCount++;

                        check(msg.getResourceUuid(), sysTag);
                    }
                }
            }
        }
        DiskOfferingUserConfigValidator diskOfferingUserConfigValidator = new DiskOfferingUserConfigValidator();
        tagMgr.installCreateMessageValidator(DiskOfferingVO.class.getSimpleName(), diskOfferingUserConfigValidator);
        DiskOfferingSystemTags.DISK_OFFERING_USER_CONFIG.installValidator(diskOfferingUserConfigValidator);
    }

    private void checkInstanceOfferingUserConfig(String configStr) {
        for (InstanceOfferingUserConfigValidator ext : pluginRgty.getExtensionList(InstanceOfferingUserConfigValidator.class)) {
            ext.validateInstanceOfferingUserConfig(configStr, null);
        }
    }

    private void checkDiskOfferingUserConfig(String configStr) {
        for (DiskOfferingUserConfigValidator ext : pluginRgty.getExtensionList(DiskOfferingUserConfigValidator.class)) {
            ext.validateDiskOfferingUserConfig(configStr, null);
        }
    }

    @Override
    public boolean start() {
        populateExtensions();
        prepareSystemTags();

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
