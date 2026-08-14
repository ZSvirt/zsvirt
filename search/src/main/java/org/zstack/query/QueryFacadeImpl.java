package org.zstack.query;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.cloudbus.MarshalReplyMessageExtensionPoint;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.SyncTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.thread.ThreadGlobalProperty;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.query.*;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;
import org.zstack.header.search.SearchConstant;
import org.zstack.utils.*;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.zql.ZQL;
import org.zstack.zql.ZQLContext;
import org.zstack.zql.ZQLQueryReturn;
import org.zstack.zql.ast.ZQLMetadata;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.inerr;

public class QueryFacadeImpl extends AbstractService implements QueryFacade, GlobalApiMessageInterceptor, MarshalReplyMessageExtensionPoint {
    private static CLogger logger = Utils.getLogger(QueryFacadeImpl.class);
    private Map<String, QueryBuilderFactory> builerFactories = new HashMap<>();
    private Map<String, QueryBelongFilter> belongfilters = new HashMap<>();
    private String queryBuilderType = MysqlQueryBuilderFactory.type.toString();
    private List<Class> zqlFilterClasses = Lists.newArrayList();

    private Map<String, List<String>> apiNoSeeFields = new HashMap<>();
    private Map<String, List<Class>> inventoryFamilies = new HashMap<>();
    private static int syncThreadNum = 10;

    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;

    public static final String USER_TAG = "__userTag__";
    public static final String SYSTEM_TAG = "__systemTag__";

    private void validateConditions(List<QueryCondition> conditions) {
        for (QueryCondition cond : conditions) {
            // will throw out IllegalArgumentException if op is invalid
            QueryOp.valueOf(cond.getOp());
        }
    }

    {
        zqlFilterClasses.addAll(BeanUtils.reflections.getSubTypesOf(ZQLFilterReply.class));
    }

    @Override
    public <T> List<T> query(APIQueryMessage msg, Class<T> inventoryClass) {
        validateConditions(msg.getConditions());
        ZQLQueryReturn result = queryUseZQL(msg, inventoryClass);
        return result.inventories;
    }

    @Override
    public long count(APIQueryMessage msg, Class inventoryClass) {
        validateConditions(msg.getConditions());
        ZQLQueryReturn result = queryUseZQL(msg, inventoryClass);
        return result.total;
    }

    private void populateExtensions() {
        for (QueryBuilderFactory extp : pluginRgty.getExtensionList(QueryBuilderFactory.class)) {
            QueryBuilderFactory old = builerFactories.get(extp.getQueryBuilderType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate QueryBuilderFactory[%s, %s] for type[%s]",
                        extp.getClass().getName(), old.getClass().getName(), extp.getQueryBuilderType()));
            }
            builerFactories.put(extp.getQueryBuilderType().toString(), extp);
        }

        for (QueryBelongFilter extp : pluginRgty.getExtensionList(QueryBelongFilter.class)) {
            QueryBelongFilter old = belongfilters.get(extp.filterName());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate QueryBelongFilter[%s, %s] for type[%s]",
                        extp.getClass().getName(), old.getClass().getName(), extp.filterName()));
            }
            belongfilters.put(extp.filterName(), extp);
        }
    }

    private QueryBuilderFactory getFactory(String type) {
        QueryBuilderFactory factory = builerFactories.get(type);
        if (factory == null) {
            throw new CloudRuntimeException(String.format("unable to find QueryBuilderFactory with type[%s]", type));
        }
        return factory;
    }

    private QueryBelongFilter getBelongFilter(String type) {
        QueryBelongFilter filter = belongfilters.get(type.split(":")[0]);
        if (filter == null) {
            throw new CloudRuntimeException(String.format("unable to find QueryBelongFilter with type[%s]", type));
        }
        return filter;
    }

    private void checkBoxTypeInInventory() {
        if (!CoreGlobalProperty.CHECK_BOX_TYPE_IN_INVENTORY) {
            return;
        }

        Set<Class<?>> inventoryClasses = BeanUtils.reflections.getTypesAnnotatedWith(Inventory.class);
        List<String> errors = new ArrayList<>();
        for (Class clz : inventoryClasses) {
            boolean error = false;
            StringBuilder sb = new StringBuilder(String.format("inventory class[%s] contains below primitive fields:",
                    clz.getName()));
            for (Field f : FieldUtils.getAllFields(clz)) {
                if (f.isAnnotationPresent(APINoSee.class)) {
                    continue;
                }

                if (TypeUtils.isPrimitiveType(f.getType())) {
                    error = true;
                    sb.append(String.format("\n%s[%s]", f.getName(), f.getType().getName()));
                }
            }

            if (error) {
                errors.add(sb.toString());
            }
        }

        if (!errors.isEmpty()) {
            throw new CloudRuntimeException(String.format("detected some inventory class using primitive type." +
                    " Please change those primitive type field to corresponding box type:\n %s",
                    StringUtils.join(errors, "\n\n")));
        }
    }

    // ZQL and batch-query both can use just less than half sync threads
    private static int getQuerySyncLevel() {
        return syncThreadNum / 2 - 1;
    }

    @Override
    public boolean start() {
        syncThreadNum = thdf.getSyncThreadNum(ThreadGlobalProperty.MAX_THREAD_NUM);
        checkBoxTypeInInventory();
        populateExtensions();
        collectInventoryAPINoSee();
        cleanSlowZQLCache();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIGenerateInventoryQueryDetailsMsg) {
            handle((APIGenerateInventoryQueryDetailsMsg) msg);
        } else if (msg instanceof APIQueryMessage) {
            handle((APIQueryMessage) msg);
        } else if (msg instanceof APIGenerateQueryableFieldsMsg) {
            handle((APIGenerateQueryableFieldsMsg) msg);
        } else if (msg instanceof APIBatchQueryMsg) {
            handle((APIBatchQueryMsg) msg);
        } else if (msg instanceof APIZQLQueryMsg) {
            handle((APIZQLQueryMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIZQLQueryMsg msg) {
        thdf.syncSubmit(new SyncTask<Void>() {
            @Override
            public Void call() {
                APIZQLQueryReply reply = new APIZQLQueryReply();
                ZQLContext.putAPISession(msg.getSession());

                // use doCall to make message exception safe
                doCall(new ReturnValueCompletion<List<ZQLQueryReturn>>(msg) {
                    @Override
                    public void success(List<ZQLQueryReturn> returnValue) {
                        ZQLContext.cleanAPISession();
                        reply.setResults(returnValue);
                        bus.reply(msg, reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        ZQLContext.cleanAPISession();
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                    }
                });

                return null;
            }

            private void doCall(ReturnValueCompletion<List<ZQLQueryReturn>> completion) {
                completion.success(ZQL.fromString(msg.getZql()).getResultList());
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }

            @Override
            public String getSyncSignature() {
                return "zql";
            }

            @Override
            public int getSyncLevel() {
                return getQuerySyncLevel();
            }
        });
    }

    private void handle(APIBatchQueryMsg msg) {
        thdf.syncSubmit(new SyncTask<Void>() {
            @Override
            public Void call() {
                APIBatchQueryReply reply = new APIBatchQueryReply();
                doCall(new ReturnValueCompletion<Map<String, Object>>(msg) {
                    @Override
                    public void success(Map<String, Object> returnValue) {
                        reply.setResult(returnValue);
                        bus.reply(msg, reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                    }
                });
                return null;
            }

            // use doCall to make message exception safe
            private void doCall(ReturnValueCompletion<Map<String,Object>> completion) {
                completion.success(new BatchQuery().query(msg));
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }

            @Override
            public String getSyncSignature() {
                return "batch-query";
            }

            @Override
            public int getSyncLevel() {
                return getQuerySyncLevel();
            }
        });
    }

    private void handle(APIGenerateQueryableFieldsMsg msg) {
        QueryBuilderFactory factory = getFactory(queryBuilderType);
        QueryBuilder builder = factory.createQueryBuilder();
        Map<String, List<String>> ret = builder.populateQueryableFields();

        if (APIGenerateQueryableFieldsMsg.PYTHON_FORMAT.equals(msg.getFormat())) {
            QueryableFieldsPythonWriter writer = new QueryableFieldsPythonWriter(msg.getOutputFolder(), ret);
            writer.write();
        } else {
            throw new CloudRuntimeException(String.format("unknown mediaType[%s]", msg.getFormat()));
        }

        APIGenerateQueryableFieldsEvent evt = new APIGenerateQueryableFieldsEvent(msg.getId());
        bus.publish(evt);
    }

    private Map<Class, Method> replySetter = new HashMap<>();
    private Map<Class, AutoQuery> autoQueryMap = new HashMap<>();

    private Method getReplySetter(AutoQuery at) {
        Class replyClass = at.replyClass();
        Class inventoryClass = at.inventoryClass();
        try {
            Method setter = replySetter.get(inventoryClass);
            if (setter == null) {
                setter = replyClass.getDeclaredMethod("setInventories", List.class);
                if (setter == null) {
                    throw new OperationFailureException(inerr(
                            "query reply[%s] has no method setInventories()", replyClass.getName()
                    ));
                }
                setter.setAccessible(true);
                replySetter.put(inventoryClass, setter);
            }

            return setter;
        } catch (OperationFailureException of) {
            throw of;
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
            throw new OperationFailureException(inerr(e.getMessage()));
        }
    }

    private void handle(APIQueryMessage msg) {
        AutoQuery at = autoQueryMap.get(msg.getClass());
        if (at == null) {
            at = msg.getClass().getAnnotation(AutoQuery.class);
            if (at == null) {
                throw new OperationFailureException(inerr(
                        "message[%s] is not annotated by @AutoQuery", msg.getClass()
                ));
            }
            autoQueryMap.put(msg.getClass(), at);
        }

        Class replyClass = at.replyClass();
        Class inventoryClass = at.inventoryClass();

        try {
            APIQueryReply reply = (APIQueryReply) replyClass.getConstructor().newInstance();
            Method replySetter = getReplySetter(at);
            ZQLQueryReturn result = queryUseZQL(msg, inventoryClass);
            if (result.total != null) {
                reply.setTotal(result.total);
            }
            if (result.inventories != null) {
                replySetter.invoke(reply, result.inventories);
            }
            bus.reply(msg, reply);
        } catch (OperationFailureException of) {
            throw of;
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    private String toZQLConditionString(QueryCondition c) {
        // make every condition value as string, the ZQL
        // will put them in right type because it knows every field's type

        QueryOp op = QueryOp.valueOf(c.getOp());
        if (c.getValue() == null) {
            return String.format("%s %s", c.getName(), c.getOp());
        } else if (op == QueryOp.IN || op == QueryOp.NOT_IN) {
            return String.format("%s %s %s", c.getName(), c.getOp(), toZQLConditionString(c.getValue(), true));
        } else {
            return String.format("%s %s %s", c.getName(), c.getOp(), toZQLConditionString(c.getValue(), false));
        }
    }

    private static String toZQLConditionString(String rawText, final boolean supportSplitByComma) {
        if (rawText.isEmpty()) {
            return supportSplitByComma ? "('')" : "''";
        }

        boolean specialCharFound = rawText.contains("'") || rawText.contains("\\");
        specialCharFound |= (supportSplitByComma && rawText.contains(","));
        if (!specialCharFound) {
            return supportSplitByComma ? "('" + rawText + "')" : '\'' + rawText + '\'';
        }

        StringBuilder builder = new StringBuilder(rawText.length() << 1);
        IntConsumer consumer = supportSplitByComma ? ch -> {
            switch (ch) {
                case '\'': builder.append("\\'"); break;
                case '\\': builder.append("\\\\"); break;
                case ',': builder.append("','"); break;
                default: builder.append((char) ch); break;
            }
        } : ch -> {
            switch (ch) {
                case '\'': builder.append("\\'"); break;
                case '\\': builder.append("\\\\"); break;
                default: builder.append((char) ch); break;
            }
        };

        builder.append('\'');
        rawText.chars().forEach(consumer);
        builder.append('\'');

        if (supportSplitByComma) {
            builder.insert(0, '(');
            builder.append(')');
        }

        return builder.toString();
    }

    private Class getQueryTargetInventoryClass(APIQueryMessage msg, Class inventoryClass) {
        Field tf = ZQLMetadata.getTypeFieldOfInventoryClass(inventoryClass);
        if (tf == null) {
            return inventoryClass;
        }

        Optional<QueryCondition> opt = msg.getConditions().stream().filter(c->c.getName().equals(tf.getName())).findFirst();
        if (!opt.isPresent()) {
            return inventoryClass;
        }

        QueryCondition condition = opt.get();

        if (!condition.getOp().equals(QueryOp.EQ.toString())) {
            return inventoryClass;
        }

        return ZQLMetadata.getChildInventoryClassByType(inventoryClass, condition.getValue());
    }

    public ZQLQueryReturn queryUseZQL(APIQueryMessage msg, Class inventoryClass) {
        List<String> sb = new ArrayList<>();
        sb.add(msg.isCount() ? "count" : "query");
        Class targetInventoryClass = getQueryTargetInventoryClass(msg, inventoryClass);
        sb.add(msg.getFields() == null || msg.getFields().isEmpty() ? ZQL.queryTargetNameFromInventoryClass(targetInventoryClass) : ZQL.queryTargetNameFromInventoryClass(targetInventoryClass) + "." + StringUtils.join(msg.getFields(), ","));

        if (msg.getConditions() != null && !msg.getConditions().isEmpty()) {
            Set<String> conds = new HashSet<>();
            msg.getConditions().forEach(c -> conds.add(toZQLConditionString(c)));

            if (!conds.isEmpty()) {
                sb.add("where");
                sb.add(StringUtils.join(conds, " and "));
            }
        }

        String filterName = msg.getFilterName();
        if (!StringUtils.isEmpty(filterName)) {
            QueryBelongFilter exp = validateFilterNameAndGetExp(filterName);
            String condition = exp.convertFilterNameToZQL(filterName);
            sb.add(sb.contains("where")?" and ":" where ");
            sb.add(String.format(" %s %s ", SYSTEM_TAG, condition));
        }

        if (!msg.isCount() && msg.isReplyWithCount()) {
            sb.add("return with (total)");
        }

        if (msg.getSortBy() != null) {
            sb.add(String.format("order by %s %s", msg.getSortBy(), msg.getSortDirection()));
        }

        if (msg.getLimit() != null) {
            sb.add(String.format("limit %s", msg.getLimit()));
        }

        if (msg.getStart() != null) {
            sb.add(String.format("offset %s", msg.getStart()));
        }

        String text = StringUtils.join(sb, " ");
        if (logger.isTraceEnabled()) {
            logger.trace(text);
        }
        ZQL zql = ZQL.fromString(text);
        ZQLQueryReturn result;

        try {
            ZQLContext.putAPISession(msg.getSession());
            result = zql.getSingleResult();
        } finally {
            ZQLContext.cleanAPISession();
        }

        if (result.inventories != null) {
            dropAPINoSee(result.inventories, targetInventoryClass);
        }

        return result;
    }

    private QueryBelongFilter validateFilterNameAndGetExp(String filterName) {
        if (filterName.split(":").length < 2) {
            throw new OperationFailureException(argerr("filterName must be formatted as [filterType:condition(s)]"));
        }
        return getBelongFilter(filterName);
    }

    private void collectInventoryAPINoSee() {
        BeanUtils.reflections.getTypesAnnotatedWith(Inventory.class).stream()
                .filter(clz -> !Modifier.isStatic(clz.getModifiers()))
                .forEach(clz -> {
            Inventory inventory = clz.getAnnotation(Inventory.class);
            if (inventory == null) {
                return;
            }
            List<String> skip = new ArrayList<>();
            for (Field field : clz.getDeclaredFields()) {
                if (field.isAnnotationPresent(APINoSee.class)) {
                    skip.add(field.getName());
                }
            }
            if (!skip.isEmpty()) {
                apiNoSeeFields.put(clz.getName(), skip);
                for (Parent parent : inventory.parent()) {
                    inventoryFamilies.compute(parent.inventoryClass().getName(), (k, v) -> {
                        if (v == null) {
                            v = CollectionDSL.list(clz);
                        } else {
                            v.add(clz);
                        }
                        return v;
                    });
                }
            }
        });
    }

    private void cleanSlowZQLCache() {
        thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.MINUTES;
            }

            @Override
            public long getInterval() {
                return 5;
            }

            @Override
            public String getName() {
                return "clean-slow-zql-cache";
            }

            @Override
            public void run() {
                ZQL.cleanSlowZqlCache();
            }
        });
    }

    private Class getAPINoSeeClassDirectly(Class inventoryClass) {
        if (apiNoSeeFields.get(inventoryClass.getName()) != null && !apiNoSeeFields.get(inventoryClass.getName()).isEmpty()) {
            return inventoryClass;
        }
        return null;
    }

    private List<Class> getAPINoSeeClasses(Class inventoryClass) {
        List<Class> apiNoSeeClasses = new ArrayList<>();
        Class clz = getAPINoSeeClassDirectly(inventoryClass);
        if (clz != null) {
            apiNoSeeClasses.add(clz);
        }
        if (!inventoryFamilies.containsKey(inventoryClass.getName())) {
            return apiNoSeeClasses;
        }
        for (Class inventory: inventoryFamilies.get(inventoryClass.getName())) {
            clz = getAPINoSeeClassDirectly(inventory);
            if (clz != null) {
                apiNoSeeClasses.add(clz);
            }
        }
        return apiNoSeeClasses;
    }

    private void dropAPINoSee(List<Object> inventories, Class inventoryClass) {
        List<Class> clzs = getAPINoSeeClasses(inventoryClass);
        if (clzs.isEmpty()) {
            return;
        }
        for(Class clz: clzs) {
            for (String field : apiNoSeeFields.get(clz.getName())) {
                try {
                    Field f = clz.getDeclaredField(field);
                    f.setAccessible(true);
                    for (Object inv : inventories) {
                        if (!f.getType().isPrimitive() && clz.isInstance(inv)) {
                            f.set(inv, null);
                        }
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    logger.warn(e.getMessage());
                }
            }
        }
    }

    private void handle(APIGenerateInventoryQueryDetailsMsg msg) {
        InventoryQueryDetailsGenerator.generate(msg.getOutputDir(), msg.getBasePackageNames());
        APIGenerateInventoryQueryDetailsEvent evt = new APIGenerateInventoryQueryDetailsEvent(msg.getId());
        bus.publish(evt);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SearchConstant.QUERY_FACADE_SERVICE_ID);
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        List<Class> ret = new ArrayList<>();
        ret.add(APIQueryMessage.class);
        return ret;
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.FRONT;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        APIQueryMessage qmsg = (APIQueryMessage) msg;
        for (QueryCondition cond : qmsg.getConditions()) {
            try {
                QueryOp.valueOf(cond.getOp());
            } catch (IllegalArgumentException e) {
                throw new ApiMessageInterceptionException(argerr(e.getMessage()));
            }

            if (!QueryOp.NOT_NULL.equals(cond.getOp()) && !QueryOp.IS_NULL.equals(cond.getOp()) && cond.getValue() == null) {
                throw new ApiMessageInterceptionException(argerr("'value' of query condition %s cannot be null",
                                JSONObjectUtil.toJsonString(cond)));
            }
        }

        return msg;
    }

    @Override
    public List<Class> getReplyMessageClassForMarshalExtensionPoint() {
        return zqlFilterClasses;
    }

    @Override
    public void marshalReplyMessageBeforeSending(Message replyOrEvent, NeedReplyMessage msg) {
        if (replyOrEvent instanceof ZQLFilterReply) {
            ZQLFilterReply reply = (ZQLFilterReply) replyOrEvent;
            List<String> resourceUuids = reply.getInventoryUuids();
            if (resourceUuids == null || resourceUuids.isEmpty() || !(msg instanceof APIMessage)) {
                return;
            }

            String queryVOName = StringUtils.removeEnd(reply.getInventoryName(), "Inventory").toLowerCase();
            String zql = String.format("query %s where uuid in (%s)", queryVOName,
                    resourceUuids.stream().map(s -> "'" + s + "'")
                            .collect(Collectors.joining(", ")));

            reply.setFilteredInventories(ZQL.fromString(zql).getSingleResultWithSession(((APIMessage) msg).getSession()).inventories);
        }
    }
}
