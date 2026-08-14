package org.zstack.portal.apimediator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.Service;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor.InterceptorPosition;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.description.PackageDescriptionRegistry;
import org.zstack.header.description.route.ApiRouteUtils;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.OverriddenApiParam;
import org.zstack.header.message.OverriddenApiParams;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.FieldUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds {@link ApiMessageDescriptor} from classpath {@link APIMessage}s and
 * {@link PackageDescriptionRegistry} service routes (no serviceConfig XML).
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ApiMessageProcessorImpl implements ApiMessageProcessor {
    private static CLogger logger = Utils.getLogger(ApiMessageProcessorImpl.class);
    private Map<Class, ApiMessageDescriptor> descriptors = new HashMap<Class, ApiMessageDescriptor>();
    private Map<Class, Set<GlobalApiMessageInterceptor>> globalInterceptors = new HashMap<Class, Set<GlobalApiMessageInterceptor>>();
    private Set<GlobalApiMessageInterceptor> globalInterceptorsForAllMsg = new HashSet<GlobalApiMessageInterceptor>();
    /** serviceId -> interceptors declared with {@link InterceptorForService} */
    private Map<String, List<ApiMessageInterceptor>> serviceInterceptors = new HashMap<>();
    private Comparator<ApiMessageInterceptor> msgInterceptorComparator = Comparator
            .comparingInt(ApiMessageProcessorImpl::interceptorPositionToOrder)
            .thenComparing(ApiMessageInterceptor::getPriority);

    private static int interceptorPositionToOrder(ApiMessageInterceptor interceptor) {
        if (interceptor instanceof GlobalApiMessageInterceptor) {
            return ((GlobalApiMessageInterceptor) interceptor).getPosition().ordinal();
        }

        return InterceptorPosition.DEFAULT.ordinal();
    }

    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    private boolean unitTestOn;
    List<String> supportApis;

    private void dump() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Class, ApiMessageDescriptor> e : descriptors.entrySet()) {
            ApiMessageDescriptor desc = e.getValue();
            List<String> inc = new ArrayList<>();
            for (ApiMessageInterceptor ic : desc.getInterceptors()) {
                inc.add(ic.getClass().getSimpleName());
            }
            sb.append(String.format("\n%s -> services [%s]", desc.getName(), desc.getServiceId()));
            sb.append(String.format("\n        interceptors: <%s>", String.join(", ", inc)));
        }

        logger.debug(String.format("ApiMessageDescriptor dump:%s", sb));
    }

    public ApiMessageProcessorImpl(Map<String, Object> config) {
        this.unitTestOn = CoreGlobalProperty.UNIT_TEST_ON;
        this.supportApis = new ArrayList<>();

        populateGlobalInterceptors();
        populateServiceInterceptors();
        createDescriptorsFromRegistry();

        if (!this.unitTestOn) {
            dump();
        }
    }

    private void createDescriptorsFromRegistry() {
        for (Class<?> msgClz : BeanUtils.reflections.getSubTypesOf(APIMessage.class)) {
            if (msgClz.isInterface() || Modifier.isAbstract(msgClz.getModifiers())) {
                continue;
            }
            String serviceId = ApiRouteUtils.resolveServiceIdFromRegistry(msgClz);
            if (serviceId == null) {
                continue;
            }
            createDescriptor(msgClz, serviceId);
        }

        if (descriptors.isEmpty() && !this.unitTestOn) {
            throw new CloudRuntimeException(
                    "no ApiMessageDescriptor created; PackageDescriptionRegistry has no routes " +
                            "or was not populated before ApiMediator.start");
        }
    }

    private void createDescriptor(Class<?> msgClz, String serviceId) {
        ApiMessageDescriptor desc = new ApiMessageDescriptor();
        desc.setName(msgClz.getName());
        desc.setServiceId(serviceId);
        desc.setConfigPath("PackageDescription");
        desc.setClazz(msgClz);

        prepareInterceptors(desc);
        List<Service> services = pluginRgty.getExtensionList(Service.class);
        if (services.stream().anyMatch(it -> bus.makeLocalServiceId(desc.getServiceId()).equals(it.getId()))) {
            supportApis.add(desc.getClazz().getSimpleName());
        }
        buildApiParams(desc);
        descriptors.put(msgClz, desc);
    }

    private void prepareInterceptors(ApiMessageDescriptor desc) {
        // preserve order; avoid installing the same bean twice (Global + @InterceptorForService)
        Set<ApiMessageInterceptor> seen = new LinkedHashSet<>();

        for (Map.Entry<Class, Set<GlobalApiMessageInterceptor>> e : globalInterceptors.entrySet()) {
            Class baseMsgClz = e.getKey();
            if (baseMsgClz.isAssignableFrom(desc.getClazz())) {
                for (GlobalApiMessageInterceptor gi : e.getValue()) {
                    if (seen.add(gi) && logger.isTraceEnabled()) {
                        logger.trace(String.format("install GlobalApiMessageInterceptor[%s] to message[%s]",
                                gi.getClass().getName(), desc.getClazz().getName()));
                    }
                }
            }
        }

        for (GlobalApiMessageInterceptor gi : globalInterceptorsForAllMsg) {
            if (seen.add(gi) && logger.isTraceEnabled()) {
                logger.trace(String.format("install GlobalApiMessageInterceptor[%s] to message[%s]",
                        gi.getClass().getName(), desc.getClazz().getName()));
            }
        }

        List<ApiMessageInterceptor> byService = serviceInterceptors.get(desc.getServiceId());
        if (byService != null) {
            for (ApiMessageInterceptor ic : byService) {
                if (seen.add(ic) && logger.isTraceEnabled()) {
                    logger.trace(String.format(
                            "install @InterceptorForService interceptor[%s] to message[%s] serviceId[%s]",
                            ic.getClass().getName(), desc.getClazz().getName(), desc.getServiceId()));
                }
            }
        }

        List<ApiMessageInterceptor> interceptors = new ArrayList<>(seen);
        interceptors.sort(this.msgInterceptorComparator);
        desc.setInterceptors(interceptors);
    }

    private void buildApiParams(ApiMessageDescriptor desc) {
        Class msgClz = desc.getClazz();
        List<Field> fields = FieldUtils.getAllFields(msgClz);

        class FP {
            Field field;
            APIParam param;
        }

        Map<String, FP> fmap = new HashMap<String, FP>();
        for (Field f : fields) {
            APIParam at = f.getAnnotation(APIParam.class);
            if (at == null) {
                continue;
            }

            FP fp = new FP();
            fp.field = f;
            fp.param = f.getAnnotation(APIParam.class);
            fmap.put(f.getName(), fp);
        }

        OverriddenApiParams at = desc.getClazz().getAnnotation(OverriddenApiParams.class);
        if (at != null) {
            for (OverriddenApiParam atp : at.value()) {
                Field f = FieldUtils.getField(atp.field(), msgClz);
                if (f == null) {
                    throw new CloudRuntimeException(String.format("cannot find the field[%s] specified in @OverriddenApiParam of class[%s]",
                            atp.field(), msgClz));
                }

                FP fp = new FP();
                fp.field = f;
                fp.param = atp.param();
                fmap.put(atp.field(), fp);
            }
        }

        for (FP fp : fmap.values()) {
            desc.getFieldApiParams().put(fp.field, fp.param);
        }
    }

    @Override
    public APIMessage process(APIMessage msg) throws ApiMessageInterceptionException {
        ApiMessageDescriptor desc = descriptors.get(msg.getClass());
        if (desc == null) {
            throw new CloudRuntimeException(String.format("Message[%s] has no ApiMessageDescriptor", msg.getClass().getName()));
        }

        for (ApiMessageInterceptor ic : desc.getInterceptors()) {
            msg = ic.intercept(msg);
        }

        return msg;
    }

    @Override
    public ApiMessageDescriptor getApiMessageDescriptor(APIMessage msg) {
        return descriptors.get(msg.getClass());
    }

    @Override
    public List<String> getSupportApis() {
        return supportApis;
    }

    private void populateGlobalInterceptors() {
        for (GlobalApiMessageInterceptor gi : pluginRgty.getExtensionList(GlobalApiMessageInterceptor.class)) {
            if (gi.getMessageClassToIntercept() == null) {
                globalInterceptorsForAllMsg.add(gi);
            } else {
                for (Class msgClz : gi.getMessageClassToIntercept()) {
                    Set<GlobalApiMessageInterceptor> gis = globalInterceptors.get(msgClz);
                    if (gis == null) {
                        gis = new HashSet<>();
                        globalInterceptors.put(msgClz, gis);
                    }
                    gis.add(gi);
                }
            }
        }
    }

    /**
     * Index Spring plugins annotated with {@link InterceptorForService}.
     * Collects both {@link ApiMessageInterceptor} and {@link GlobalApiMessageInterceptor}
     * extension lists (beans may register only as Global) and de-duplicates instances.
     */
    private void populateServiceInterceptors() {
        Set<ApiMessageInterceptor> candidates = new LinkedHashSet<>();
        candidates.addAll(pluginRgty.getExtensionList(ApiMessageInterceptor.class));
        candidates.addAll(pluginRgty.getExtensionList(GlobalApiMessageInterceptor.class));

        for (ApiMessageInterceptor interceptor : candidates) {
            InterceptorForService at = interceptor.getClass().getAnnotation(InterceptorForService.class);
            if (at == null) {
                continue;
            }
            for (String serviceId : at.value()) {
                if (serviceId == null || serviceId.isEmpty()) {
                    continue;
                }
                List<ApiMessageInterceptor> list = serviceInterceptors.computeIfAbsent(serviceId, k -> new ArrayList<>());
                if (!list.contains(interceptor)) {
                    list.add(interceptor);
                }
            }
        }
    }
}
