package org.zstack.zwatch.namespace;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusEventListener;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.core.log.LogSafeGson;
import org.zstack.core.progress.TaskTracker;
import org.zstack.header.Component;
import org.zstack.header.apimediator.ApiMediatorConstant;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.APILogInReply;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.longjob.APISubmitLongJobEvent;
import org.zstack.header.longjob.LongJob;
import org.zstack.header.longjob.LongJobFor;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.message.*;
import org.zstack.header.message.APIMessage.FieldParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.other.APILoginAuditor;
import org.zstack.header.other.APILongJobAuditor;
import org.zstack.header.other.APIMultiAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.identity.AccountManager;
import org.zstack.longjob.LongJobFactory;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.GsonUtil;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.datatype.*;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.driver.EventDatabaseDriver;

import javax.persistence.Tuple;
import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.i18n;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.distinctByKey;

public class NamespaceEventManagerImpl implements NamespaceEventManager, Component,
        BeforeDeliveryMessageInterceptor, BeforeSendMessageInterceptor, CloudBusEventListener {
    private static final CLogger logger = Utils.getLogger(NamespaceEventManagerImpl.class);

    @Autowired
    private PluginRegistry pluginRgty;

    @Autowired
    private LongJobFactory longJobFactory;

    @Override
    public boolean handleEvent(Event e) {
        processResponse(e);
        return false;
    }

    @Override
    public EventDatabaseDriver getEventDatabaseDriver() {
        return driver;
    }

    class AuditRequest {
        Message request;
        List<EventFamily> families;
        long startTime;

        Bundle makeBundleWithResponse(Message response) {
            if (response instanceof APIEvent) {
                APIBundle bundle = new APIBundle();
                fillBundle(bundle);
                bundle.apiMessage = ((APIMessage) request);
                bundle.apiEvent = ((APIEvent) response);
                bundle.response = response;
                return bundle;
            }

            if (response instanceof APIReply) {
                APIBundleWithReply bundle = new APIBundleWithReply();
                fillBundle(bundle);
                bundle.apiMessage = ((APIMessage) request);
                bundle.apiReply = ((APIReply) response);
                bundle.response = response;
                return bundle;
            }

            Bundle bundle = new Bundle();
            fillBundle(bundle);
            bundle.response = response;
            return bundle;
        }

        LongJobBundle makeLongJobBundle(APIEvent apiEvent) {
            LongJobBundle bundle = new LongJobBundle();
            fillBundle(bundle);
            bundle.apiMessage = (APIMessage) request;
            bundle.apiEvent = apiEvent;
            bundle.response = apiEvent;
            return bundle;
        }

        void fillBundle(Bundle bundle) {
            bundle.families = families;
            bundle.request = request;
            bundle.startTime = startTime;
            bundle.endTime = System.currentTimeMillis();
        }
    }

    class Bundle {
        Message request;
        Message response;
        List<EventFamily> families;
        long startTime;
        long endTime;

        protected List<AuditDataV2> processAudits() {
            return null;
        }

        protected AuditDataV2 createAuditData() {
            AuditDataV2 data = new AuditDataV2();
            data.setDuration(endTime - startTime);
            return data;
        }

        List<AuditDataV2> processAuditForField(FieldParam rf, APIMessage msg) {
            List<AuditDataV2> results = new ArrayList<>();

            try {
                Object resUuidValue = rf.field.get(msg);
                if (resUuidValue == null) {
                    return results;
                }

                List<String> resourceUuids = (resUuidValue instanceof Collection) ?
                        CollectionUtils.transform((Collection<?>) resUuidValue, Object::toString) :
                        Collections.singletonList(resUuidValue.toString());

                final Class<?>[] resourceTypes = rf.param.resourceType();
                if (resourceTypes.length == 1) {
                    for (String resourceUuid : resourceUuids) {
                        results.add(createAuditWithResource(resourceUuid, resourceTypes[0]));
                    }
                    return results;
                }

                // TODO: if resource has been deleted, it is impossible to find resource type
                List<Tuple> tuples = Q.New(ResourceVO.class)
                        .in(ResourceVO_.uuid, Arrays.asList(resourceUuids))
                        .select(ResourceVO_.uuid, ResourceVO_.resourceType)
                        .listTuple();
                for (Tuple tuple : tuples) {
                    results.add(createAuditWithResource(tuple.get(0, String.class), tuple.get(1, String.class)));
                }
            } catch (Exception e) {
                logger.warn(String.format("unhandled exception, auditing %s.%s",
                        msg.getClass().getSimpleName(), rf.field.getName()), e);
            }

            return results;
        }

        protected AuditDataV2 createAuditWithResource(String resourceUuid, Class<?> resourceType) {
            String baseResourceType = Platform.getBaseResourceType(resourceType.getSimpleName());
            if (baseResourceType == null) {
                baseResourceType = resourceType.getSimpleName();
            }

            return createAuditWithResource(resourceUuid, baseResourceType);
        }

        protected AuditDataV2 createAuditWithResource(String resourceUuid, String baseResourceType) {
            AuditDataV2 data = createAuditData();
            data.setResourceType(baseResourceType);
            data.setResourceUuid(resourceUuid);
            data.setResourceName("");
            return data;
        }
    }

    class APIBundle extends Bundle {
        APIMessage apiMessage;
        APIEvent apiEvent;

        @Override
        protected List<AuditDataV2> processAudits() {
            List<FieldParam> resourceUuidFields = buildResourceFieldForApi(apiMessage.getClass());
            List<AuditDataV2> results = new ArrayList<>();

            if (resourceUuidFields.isEmpty()) {
                results.addAll(processNoResourceUuidAudit());
            } else {
                results.addAll(processAuditForFields(resourceUuidFields));
            }

            return results;
        }

        List<AuditDataV2> processNoResourceUuidAudit() {
            List<ApiAuditor> auditors = ApiAuditor.getApiAuditors(apiMessage.getClass());

            if (auditors == null) {
                AuditDataV2 data = createAuditData();
                data.setResourceUuid("");
                data.setResourceType("");
                return list(data);
            }

            List<AuditDataV2> ret = new ArrayList<>();
            for (ApiAuditor auditor : auditors) {
                if (auditor instanceof ApiMultiAuditor) {
                    ApiMultiAuditor multiAuditor = (ApiMultiAuditor) auditor;
                    List<ApiAuditor.Result> results = multiAuditor.multiAudit(apiMessage, apiEvent);
                    if (results == null) {
                        continue;
                    }

                    for (ApiAuditor.Result result : results) {
                        ret.add(createAuditWithResource(result.resourceUuid, result.resourceType));
                    }
                    continue;
                }

                ApiAuditor.Result res = auditor.audit(apiMessage, apiEvent);
                if (res != null) {
                    ret.add(createAuditWithResource(res.resourceUuid, res.resourceType));
                }
            }
            return ret;
        }

        List<AuditDataV2> processAuditForFields(List<FieldParam> resourceUuidFields) {
            List<AuditDataV2> audits = new ArrayList<>();
            for (FieldParam field : resourceUuidFields) {
                audits.addAll(processAuditForField(field, apiMessage));
            }

            List<ApiAuditor> auditors = ApiAuditor.getApiAuditors(apiMessage.getClass());
            if (auditors == null) {
                return audits;
            }

            // for message like APICreateDataVolumeTemplateFromVolumeMsg, it will be audited
            // by code above because it has fields annotated by @APIParam. However, it should
            // also be audited as an image related API since it creates a new image. For such
            // kind of situations, we call API auditor if there is any
            for (ApiAuditor auditor : auditors) {
                if (auditor instanceof ApiMultiAuditor) {
                    ApiMultiAuditor multiAuditor = (ApiMultiAuditor) auditor;
                    List<ApiAuditor.Result> results = multiAuditor.multiAudit(apiMessage, apiEvent);
                    if (results == null) {
                        continue;
                    }

                    for (ApiAuditor.Result result : results) {
                        audits.add(createAuditWithResource(result.resourceUuid, result.resourceType));
                    }
                    continue;
                }

                ApiAuditor.Result res = auditor.audit(apiMessage, apiEvent);
                if (res != null) {
                    audits.add(createAuditWithResource(res.resourceUuid, res.resourceType));
                }
            }
            return audits;
        }

        @Override
        protected AuditDataV2 createAuditData() {
            AuditDataV2 data = super.createAuditData();
            if (apiEvent.getError() != null) {
                data.setError(apiEvent.getError().getReadableDetails());
            }

            final SessionInventory session = apiMessage.getSession();
            data.setSessionUuid(session == null ? "" : session.getUuid());
            data.setOperatorAccountUuid(session == null ? "" : session.getAccountUuid());

            data.setResponseUuid(apiEvent.getId());
            data.setResponseDump(LogSafeGson.toJson(apiEvent));
            data.setClientIp(StringUtils.isNotBlank(apiMessage.getClientIp()) ? apiMessage.getClientIp() : "");
            data.setClientBrowser(StringUtils.isNotBlank(apiMessage.getClientBrowser()) ? apiMessage.getClientBrowser() : "");
            data.setApiName(apiMessage.getClass().getName());
            data.setRequestUuid(apiMessage.getId());
            data.setRequestDump(LogSafeGson.toJson(apiMessage));

            if (StringUtils.isEmpty(data.getOperatorAccountUuid())) {
                data.setOperator(apiMessage.getOperator());
            } else {
                data.setOperator(Q.New(ResourceVO.class)
                        .select(ResourceVO_.resourceName)
                        .eq(ResourceVO_.uuid, data.getOperatorAccountUuid())
                        .findValue());
            }
            return data;
        }

        @Override
        protected AuditDataV2 createAuditWithResource(String resourceUuid, Class<?> resourceType) {
            AuditDataV2 data = super.createAuditWithResource(resourceUuid, resourceType);

            if (StringUtils.isBlank(resourceUuid) || StringUtils.isNotBlank(data.getResourceName())) {
                return data;
            }

            if (apiMessage instanceof APIDeleteMessage) {
                APIDeleteMessage deleteMessage = ((APIDeleteMessage) apiMessage);
                final Map<String, String> resourceNameMap = deleteMessage.getResourceNameMap();
                if (resourceNameMap != null) {
                    data.setResourceName(resourceNameMap.get(resourceUuid));
                }
                if (data.getResourceName() != null) {
                    return data;
                }
            }

            String name = Q.New(ResourceVO.class)
                    .eq(ResourceVO_.uuid, resourceUuid)
                    .select(ResourceVO_.resourceName)
                    .findValue();
            data.setResourceName(name == null ? "" : name);
            return data;
        }
    }

    class APIBundleWithReply extends Bundle {
        APIMessage apiMessage;
        APIReply apiReply;

        @Override
        protected List<AuditDataV2> processAudits() {
            if (!(apiReply instanceof APILogInReply)) {
                return null;
            }
            return processNoResourceUuidAudit();
        }

        List<AuditDataV2> processNoResourceUuidAudit() {
            List<ApiAuditor> auditors = ApiAuditor.getApiAuditors(apiMessage.getClass());

            if (auditors == null) {
                AuditDataV2 data = createAuditData();
                data.setResourceUuid("");
                data.setResourceType("");
                return list(data);
            }

            List<AuditDataV2> ret = new ArrayList<>();
            for (ApiAuditor auditor : auditors) {
                if (!(auditor instanceof ApiLoginAuditor)) {
                    continue;
                }

                ApiLoginAuditor apiLoginAuditor = (ApiLoginAuditor) auditor;
                ApiLoginAuditor.LoginResult res = apiLoginAuditor.loginAuditor(apiMessage, apiReply);
                if (res != null) {
                    AuditDataV2 data = createAuditWithResource(res.resourceUuid, res.resourceType);
                    data.setClientIp(StringUtils.isNotEmpty(res.clientIp) ? res.clientIp : "");
                    data.setClientBrowser(StringUtils.isNotEmpty(res.clientBrowser) ? res.clientBrowser : "");
                    ret.add(data);
                }
            }
            return ret;
        }

        @Override
        protected AuditDataV2 createAuditData() {
            AuditDataV2 data = super.createAuditData();
            if (apiReply.getError() != null) {
                data.setError(apiReply.getError().getReadableDetails());
            }

            final SessionInventory session = apiMessage.getSession();
            data.setSessionUuid(session == null ? "" : session.getUuid());
            data.setOperatorAccountUuid(session == null ? "" : session.getAccountUuid());

            data.setResponseUuid(apiReply.getId());
            data.setResponseDump(LogSafeGson.toJson(apiReply));
            data.setClientIp(StringUtils.isNotBlank(apiMessage.getClientIp()) ? apiMessage.getClientIp() : "");
            data.setClientBrowser(StringUtils.isNotBlank(apiMessage.getClientBrowser()) ? apiMessage.getClientBrowser() : "");
            data.setApiName(apiMessage.getClass().getName());
            data.setRequestUuid(apiMessage.getId());
            data.setRequestDump(LogSafeGson.toJson(apiMessage));

            if (StringUtils.isEmpty(data.getOperatorAccountUuid())) {
                data.setOperator(apiMessage.getOperator());
            } else {
                data.setOperator(Q.New(ResourceVO.class)
                        .select(ResourceVO_.resourceName)
                        .eq(ResourceVO_.uuid, data.getOperatorAccountUuid())
                        .findValue());
            }
            return data;
        }
    }

    class LongJobBundle extends APIBundle {
        LongJob job;
        LongJobVO longJobVO;
        String apiName;

        @Override
        protected List<AuditDataV2> processAudits() {
            List<ApiAuditor> apiAuditors = ApiAuditor.getApiAuditors(apiMessage.getClass());
            apiAuditors = CollectionUtils.filter(apiAuditors, a -> a instanceof ApiLongJobAuditor);

            if (CollectionUtils.isEmpty(apiAuditors)) {
                return null;
            }

            ApiLongJobAuditor auditor = (ApiLongJobAuditor) apiAuditors.get(0);
            List<AuditDataV2> results = new ArrayList<>();
            ApiAuditor.Result res = auditor.longJobAudit(job, longJobVO, apiEvent);
            if (res != null) {
                results.add(createAuditWithResource(res.resourceUuid, res.resourceType));
            }

            // extract message from 'LongJobFor' annotation from LongJobMsg
            LongJobFor[] annotations = job.getClass().getAnnotationsByType(LongJobFor.class);
            if (annotations.length == 0) {
                return results;
            }

            // filter Class assigned from class 'APIMessage'
            List<Class<?>> cs = Arrays.stream(annotations)
                    .map(LongJobFor::value)
                    .filter(APIMessage.class::isAssignableFrom)
                    .collect(Collectors.toList());
            if (cs.isEmpty()) {
                return results;
            }

            cs.forEach(c -> {
                try {
                    // Rebuild APIMessage so we can get wrapped audit data
                    Object wrappedMsg = gson.fromJson(longJobVO.getJobData(), c);
                    List<FieldParam> wrappedResourceUuidFields = buildResourceFieldForApi(c);
                    if (wrappedResourceUuidFields.isEmpty()) {
                        return;
                    }

                    for (FieldParam field : wrappedResourceUuidFields) {
                        results.addAll(processAuditForField(field, (APIMessage) wrappedMsg));
                    }
                } catch (Exception ignored) {
                }
            });

            return results;
        }

        @Override
        protected AuditDataV2 createAuditData() {
            AuditDataV2 data = super.createAuditData();
            data.setApiName(apiName);
            data.setRequestUuid(apiEvent.getApiId());
            data.setRequestDump(longJobVO.getJobData());
            return data;
        }
    }

    private Map<Class<?>, List<EventFamily>> collectors = new HashMap<>();
    private Map<String, AuditRequest> processingRequests = new ConcurrentHashMap<>();
    private Map<String, AuditRequest> processingLongJobRequests = new ConcurrentHashMap<>();

    @Autowired
    private CloudBus bus;
    @Autowired
    private EventFacade eventf;
    @Autowired
    private AccountManager acntMgr;

    private EventDatabaseDriver driver;

    public NamespaceEventManagerImpl(EventDatabaseDriver driver) {
        this.driver = driver;
    }

    public void setDriver(EventDatabaseDriver driver) {
        this.driver = driver;
    }

    @Override
    public boolean start() {
        Namespace.namespaces.forEach((k, namespaces) -> {
            namespaces.forEach(ns -> {
                List<EventFamily> eventFamilies = ns.getEvents();
                if (eventFamilies == null) {
                    return;
                }

                // set namespace name, a litter ugly
                eventFamilies.forEach(ef -> {
                    if (!ef.getCollectors().isEmpty() && ef.getResourceIdGetter() == null) {
                        throw new CloudRuntimeException(String.format("forget calling onErrorReturnResourceId() of %s[name:%s]", ef.getClass(), ef.getName()));
                    }
                    ef.setNamespace(ns.getName());
                });

                eventFamilies.forEach(ef -> {
                    ef.getCollectors().forEach((clz, cs) -> {
                        List<EventFamily> c = collectors.computeIfAbsent(clz, key -> new ArrayList<>());
                        c.add(ef);
                    });

                    onCanonicalEvent(ef);
                    onTaskTracker(ef);
                });
            });
        });

        Namespace.namespaces.forEach((k, namespaces) -> {
            namespaces.forEach(ns -> {
                List<Metric> metrics = ns.getMetrics();
                if (metrics == null) {
                    return;
                }

                metrics.forEach(m -> {
                    m.setNamespace(ns.getName());
                });
            });
        });

        bus.installBeforeDeliveryMessageInterceptor(this);
        bus.installBeforeSendMessageInterceptor(this);

        bus.subscribeEvent(this, new APIEvent());

        collectAPIAuditors();

        return true;
    }

    private Object createAPIAuditor(Class<?> clz) {
        if (!Modifier.isAbstract(clz.getModifiers())) {
            try {
                return clz.getConstructor().newInstance();
            } catch (Exception e) {
                throw new CloudRuntimeException(e);
            }
        }

        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(clz);
        enhancer.setCallback((MethodInterceptor) (o, method, objects, methodProxy) -> methodProxy.invokeSuper(o, objects));
        return enhancer.create();
    }

    private void collectAPIAuditors() {
        Platform.getReflections().getSubTypesOf(APIAuditor.class).forEach(clz -> {
            APIAuditor auditor = (APIAuditor) createAPIAuditor(clz);
            ApiAuditor.register(new ApiAuditor(clz) {
                @Override
                public Result audit(APIMessage msg, APIEvent rsp) {
                    APIAuditor.Result r = auditor.audit(msg, rsp);
                    return r == null ? null : new Result(r.resourceUuid, r.resourceType);
                }
            });
        });

        Platform.getReflections().getSubTypesOf(APIMultiAuditor.class).forEach(clz -> {
            APIMultiAuditor auditor = (APIMultiAuditor) createAPIAuditor(clz);
            ApiAuditor.register(new ApiMultiAuditor(clz) {
                @Override
                public List<Result> multiAudit(APIMessage msg, APIEvent rsp) {
                    List<APIAuditor.Result> res = auditor.multiAudit(msg, rsp);
                    return res == null ? null : res.stream().map(r -> new Result(r.resourceUuid, r.resourceType)).collect(Collectors.toList());
                }
            });
        });

        Platform.getReflections().getSubTypesOf(APILongJobAuditor.class).forEach(clz -> {
            APILongJobAuditor auditor = (APILongJobAuditor) createAPIAuditor(clz);
            ApiAuditor.register(new ApiLongJobAuditor(clz) {
                @Override
                public Result longJobAudit(LongJob job, LongJobVO vo, APIEvent rsp) {
                    APIAuditor.Result res = auditor.longJobAudit(job, vo, rsp);
                    return res == null ? null : new Result(res.resourceUuid, res.resourceType);
                }
            });
        });

        Platform.getReflections().getSubTypesOf(APILoginAuditor.class).forEach(clz -> {
            APILoginAuditor auditor = (APILoginAuditor) createAPIAuditor(clz);
            ApiAuditor.register(new ApiLoginAuditor(clz) {
                @Override
                public LoginResult loginAuditor(APIMessage msg, APIReply reply) {
                    APILoginAuditor.LoginResult res = auditor.loginAudit(msg, reply);
                    return new LoginResult(res.clientIp, res.clientBrowser, res.resourceUuid, res.resourceType);
                }
            });
        });
    }

    private void onTaskTracker(EventFamily ef) {
        ef.getTaskCollectors().forEach((taskName, collectors) -> TaskTracker.registerConsumer(taskName, (task) -> {
            List<EventData> events = new ArrayList<>();

            collectors.forEach(new Consumer<EventFamily.EventCollector4>() {
                @Override
                @ExceptionSafe
                public void accept(EventFamily.EventCollector4 collector) {
                    events.add(new EventData(ef, collector.collect(task)));
                }
            });

            driver.consumeEvents(events);
//            mysqlDriver.consumeEvents(events);
        }));
    }

    private List<FieldParam> buildResourceFieldForApi(Class<?> apiClass) {
        final Collection<FieldParam> params = APIMessage.apiParams.get(apiClass);
        return CollectionUtils.filter(params, param -> param.param.resourceType().length > 0);
    }

    private void onCanonicalEvent(EventFamily ef) {
        ef.getCollectorsOnCanonicalEvents().forEach((path, cs) -> eventf.on(path, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                AtomicBoolean skipProcessing = new AtomicBoolean(false);
                List<EventFamily.EventBarrier> tbs = ef.getEventBarriersByEventPath(path);
                if (tbs != null && !tbs.isEmpty()) {
                    tbs.forEach(tb -> {
                        String identifier = tb.getIdentifier(data);

                        if (identifier == null) {
                            return;
                        }

                        if (cachedResourcesMap.containsKey(identifier)) {
                            skipProcessing.set(true);
                            return;
                        }

                        cachedResourcesMap.put(identifier, Timestamp.valueOf(LocalDateTime.now()));
                    });
                }

                EventFamily.RecoverResourceIdGetter idGetter = ef.getRecoverResourceIdGetter();
                if (idGetter != null) {
                    String recoverResourceId = idGetter.getResourceId(data);
                    if (StringUtils.isNotEmpty(recoverResourceId)
                            && cachedResourcesMap.containsKey(recoverResourceId)) {
                        skipProcessing.set(false);
                        cachedResourcesMap.remove(recoverResourceId);
                    } else {
                        skipProcessing.set(true);
                    }
                }

                if (skipProcessing.get()) {
                    return;
                }

                List<EventData> events = new ArrayList<>();

                for (EventFamily.EventCollector3 c : cs) {
                    EventFamily.Event e = c.collect(data);
                    if (e != null) {
                        events.add(new EventData(ef, e));
                    }
                }

                if (!events.isEmpty()) {
                    driver.consumeEvents(events);
//                    mysqlDriver.consumeEvents(events);
                }
            }
        }));

        ef.getBarrierCleaners().forEach((path, bcs) -> eventf.onLocal(path, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                bcs.forEach(bc -> {
                    String identifier = bc.getIdentifier(data);

                    if (identifier != null && ef.getRecoverResourceIdGetter() == null) {
                        cachedResourcesMap.remove(identifier);
                    }
                });
            }
        }));
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public int orderOfBeforeDeliveryMessageInterceptor() {
        return 0;
    }

    @Override
    public void beforeDeliveryMessage(Message msg) {
        if (msg instanceof MessageReply) {
            processResponse(msg);
        }
    }

    @Override
    public int orderOfBeforeSendMessageInterceptor() {
        return 0;
    }

    @Override
    @ExceptionSafe
    public void beforeSendMessage(Message msg) {
        if (!(msg instanceof APIMessage || collectors.containsKey(msg.getClass()))) {
            return;
        }

        if (msg.getServiceId().equals(ApiMediatorConstant.SERVICE_ID)) {
            // the API message will be routed by ApiMediator,
            // filter out this message to avoid reporting the same
            // API message twice
            return;
        }

        AuditRequest auditRequest = new AuditRequest();
        auditRequest.request = msg;
        auditRequest.startTime = System.currentTimeMillis();
        auditRequest.families = collectors.get(msg.getClass());

        if (auditRequest.families != null) {
            auditRequest.families.forEach(ef -> ef.getCollectorsByMessageClass(msg.getClass()).forEach(c -> {
                if (c instanceof EventFamily.EventCollector2) {
                    ((EventFamily.EventCollector2) c).beforeMessageSent(msg);
                }
            }));
        }

        processingRequests.put(msg.getId(), auditRequest);
    }


    private static final int numberOfResourcesInCache = 10000;
    private static Map<String, Timestamp> cachedResourcesMap = Collections.synchronizedMap(new LinkedHashMap<String, Timestamp>(numberOfResourcesInCache, 0.9f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > numberOfResourcesInCache;
        }
    });

    private void processEvent(Bundle b) {
        if (b.families == null) {
            return;
        }

        List<EventData> events = new ArrayList<>();
        b.families.forEach(ef -> {
            AtomicBoolean skipProcessing = new AtomicBoolean(false);
            ef.getMessageThrottleBarriersByMessageClass(b.request.getClass()).forEach(barrier -> {
                String identifier = barrier.getIdentifier(b.request);

                if (identifier == null) {
                    return;
                }

                if (cachedResourcesMap.containsKey(identifier)) {
                    skipProcessing.set(true);
                } else {
                    cachedResourcesMap.put(identifier, Timestamp.valueOf(LocalDateTime.now()));
                }
            });

            if (skipProcessing.get()) {
                return;
            }

            String error = null;

            Message response = b.response;
            if (response instanceof APIEvent && !((APIEvent) response).isSuccess()) {
                error = ((APIEvent) response).getError().getReadableDetails();
                if (error == null) {
                    error = i18n("error happened but reason not specified");
                }
            } else if (response instanceof MessageReply && !((MessageReply) response).isSuccess()) {
                error = ((MessageReply) response).getError().getReadableDetails();
                if (error == null) {
                    error = i18n("error happened but reason not specified");
                }
            }

            if (error == null) {
                ef.getCollectorsByMessageClass(b.request.getClass()).forEach(c -> {
                    EventData data = new EventData(ef, c.collect(b.request, response));
                    data.setDuration(b.endTime - b.startTime);
                    events.add(data);
                });

            } else {
                EventFamily.EventResourceIdGetter getter = ef.getResourceIdGetter();
                EventData data = new EventData(ef, new EventFamily.Event(getter.getResourceId(b.request)).setError(error));
                data.setDuration(b.endTime - b.startTime);

                events.add(data);
            }
        });

//        mysqlDriver.consumeEvents(events);
        driver.consumeEvents(events);
    }

    @Override
    @ExceptionSafe
    public void longJobAudit(LongJob job, LongJobVO vo, APIEvent evt) {
        String msgId = evt.getApiId();
        AuditRequest auditRequest = processingLongJobRequests.remove(msgId);
        if (auditRequest == null) {
            return;
        }

        LongJobBundle bundle = auditRequest.makeLongJobBundle(evt);
        bundle.job = job;
        bundle.longJobVO = vo;
        bundle.apiName = longJobFactory.getFullJobName().get(vo.getJobName());
        processBundle(bundle);
    }

    private void processBundle(Bundle b) {
        processAudit(b);
        processEvent(b);
    }

    @ExceptionSafe
    private void processResponse(Message response) {
        String msgId;
        if (response instanceof APIEvent) {
            msgId = ((APIEvent) response).getApiId();
        } else if (response instanceof MessageReply) {
            msgId = response.getHeaderEntry(CloudBus.HEADER_CORRELATION_ID);
        } else {
            throw new CloudRuntimeException(String.format("should not here, %s", response.getClass()));
        }

        AuditRequest b = processingRequests.remove(msgId);
        if (b == null) {
            return;
        }
        if (response instanceof APISubmitLongJobEvent) {
            if (((APISubmitLongJobEvent) response).isNeedAudit()) {
                // put it back and wait for longjob audit, it will call longJobAudit
                processingLongJobRequests.put(msgId, b);
            }
            return;
        }

        processBundle(b.makeBundleWithResponse(response));
    }

    private static Gson gson = new GsonUtil().setExclusionStrategies(new ExclusionStrategy[]{new ExclusionStrategy() {
        @Override
        public boolean shouldSkipField(FieldAttributes fieldAttributes) {
            return fieldAttributes.getAnnotation(GsonTransient.class) != null || fieldAttributes.getAnnotation(APINoSee.class) != null;
        }

        @Override
        public boolean shouldSkipClass(Class<?> aClass) {
            return false;
        }
    }}).create();

    private void processAudit(Bundle b) {
        if (AuditConstants.API_AUDIT_BLOCK_LIST.contains(b.request.getClass().getName())) {
            return;
        }

        List<AuditDataV2> audits = b.processAudits();
        if (CollectionUtils.isEmpty(audits)) {
            return;
        }

        if (logger.isTraceEnabled()) {
            logger.trace(String.format("audit message[class:%s] with" +
                    " audit data:\n %s", b.request.getClass(),
                    JSONObjectUtil.toJsonString(audits)));
        }

        List<AuditDataV2> distinctAudits = audits.stream().filter(distinctByKey(it ->
                it.getRequestUuid() != null ? it.getResourceUuid() : "")
        ).collect(Collectors.toList());
        driver.audit(distinctAudits);
    }
}
