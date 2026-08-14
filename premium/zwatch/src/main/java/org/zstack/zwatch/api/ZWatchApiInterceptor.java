package org.zstack.zwatch.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.timeout.TimeHelper;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.message.APIMessage;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.query.QueryOp;
import org.zstack.identity.Account;
import org.zstack.identity.AccountManager;
import org.zstack.identity.ResourceHelper;
import org.zstack.identity.rbac.AccessibleResourceChecker;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.TimeUtils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.zwatch.ZWatchConstants;
import org.zstack.zwatch.alarm.*;
import org.zstack.zwatch.alarm.activealarm.api.APIChangeActiveAlarmStateMsg;
import org.zstack.zwatch.alarm.activealarm.entity.ActiveAlarmVO;
import org.zstack.zwatch.alarm.activealarm.entity.ActiveAlarmVO_;
import org.zstack.zwatch.datatype.*;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.function.MetricFunction;
import org.zstack.zwatch.migratedb.AlarmRecordsVO;
import org.zstack.zwatch.migratedb.EventRecordsVO;
import org.zstack.zwatch.namespace.CustomNamespace;
import org.zstack.zwatch.thirdparty.Constants;
import org.zstack.zwatch.thirdparty.api.APIAddThirdpartyPlatformMsg;
import org.zstack.zwatch.thirdparty.api.APIUpdateThirdpartyAlertsMsg;
import org.zstack.zwatch.thirdparty.entity.ThirdpartyPlatformVO;
import org.zstack.zwatch.thirdparty.entity.ThirdpartyPlatformVO_;

import java.sql.Timestamp;
import java.util.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.function.Supplier;

import static org.zstack.core.Platform.argerr;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.findOneOrNull;
import static org.zstack.zwatch.ZWatchConstants.UPDATE_DATA_MODE_INRANGE;
import static org.zstack.zwatch.ZWatchConstants.UPDATE_DATA_MODE_ONLYONE;

@InterceptorForService("zwatch")
public class ZWatchApiInterceptor implements GlobalApiMessageInterceptor {
    private static final String CUSTOM_NAMESPACE_CANONICAL_NAME = Namespace.zstackNamespaceName(CustomNamespace.NAME);
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private TimeHelper timeHelper;

    private static String MAX_QUERY_PERIOD = "1Y";
    private static final String CASE_SENSITIVE_PREFIX = "binary ";

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIGetMetricDataMsg) {
            validate((APIGetMetricDataMsg) msg);
        } else if (msg instanceof APIGetEventDataMsg) {
            validate((APIGetEventDataMsg) msg);
        } else if (msg instanceof APIGetAuditDataMsg) {
            validate((APIGetAuditDataMsg) msg);
        } else if (msg instanceof APIGetAlarmDataMsg) {
            validate((APIGetAlarmDataMsg) msg);
        } else if (msg instanceof APIGetMetricLabelValueMsg) {
            validate((APIGetMetricLabelValueMsg) msg);
        } else if (msg instanceof APIPutMetricDataMsg) {
            validate((APIPutMetricDataMsg) msg);
        } else if (msg instanceof APIUpdateAlarmDataMsg) {
            validate((APIUpdateAlarmDataMsg) msg);
        } else if (msg instanceof APIUpdateEventDataMsg) {
            validate((APIUpdateEventDataMsg) msg);
        } else if (msg instanceof APIDeleteMetricDataMsg) {
            validate((APIDeleteMetricDataMsg) msg);
        } else if (msg instanceof APICreateMetricDataHttpReceiverMsg) {
            validate((APICreateMetricDataHttpReceiverMsg) msg);
        } else if (msg instanceof APICreateMetricTemplateMsg) {
            validate((APICreateMetricTemplateMsg) msg);
        } else if (msg instanceof APIAddThirdpartyPlatformMsg) {
            validate((APIAddThirdpartyPlatformMsg) msg);
        } else if (msg instanceof APIUpdateThirdpartyAlertsMsg) {
            validate((APIUpdateThirdpartyAlertsMsg) msg);
        } else if (msg instanceof APIUpdateAlertDataAckMsg) {
            validate((APIUpdateAlertDataAckMsg) msg);
        } else if (msg instanceof APIGetZWatchAlertHistogramMsg) {
            validate((APIGetZWatchAlertHistogramMsg) msg);
        } else if (msg instanceof APIChangeActiveAlarmStateMsg) {
            validate((APIChangeActiveAlarmStateMsg) msg);
        } else if (msg instanceof APIQueryAlarmRecordMsg) {
            validate((APIQueryAlarmRecordMsg) msg);
        } else if (msg instanceof APIQueryEventRecordMsg) {
            validate((APIQueryEventRecordMsg) msg);
        } else if (msg instanceof APIGetPrometheusMetricLabelValueMsg) {
            validate((APIGetPrometheusMetricLabelValueMsg) msg);
        } else if (msg instanceof APIQueryAuditMsg) {
            validate((APIQueryAuditMsg) msg);
        }

        return msg;
    }

    private void validate(APIGetAlarmDataMsg msg) {
        Map<String, Label> labels = new HashMap<>();
        if (msg.getConditions() != null) {
            msg.getConditions().forEach(label -> {
                Label l = new Label(label);
                if (!AlarmDataV2.queryableLabels.contains(l.getKey())) {
                    throw new ApiMessageInterceptionException(argerr("invalid label[%s], valid queryable labels are %s", l.getKey(), AlarmDataV2.queryableLabels));
                }

                labels.put(l.getKey(), l);
            });
        }

        if (Account.supportToQueryEventsFromAllAccounts(msg.getSession())) {
            if (msg.isExcludeOtherAccount()) {
                labels.put(AlarmDataV2.TAG_ACCOUNT_UUID, new Label(String.format("%s=%s", AlarmDataV2.TAG_ACCOUNT_UUID, msg.getSession().getAccountUuid())));
            }
        } else {
            labels.put(AlarmDataV2.TAG_ACCOUNT_UUID, new Label(String.format("%s=%s", AlarmDataV2.TAG_ACCOUNT_UUID, msg.getSession().getAccountUuid())));
        }

        msg.setLabelList(new ArrayList<>(labels.values()));

        if (msg.getStartTime() != null && msg.getEndTime() != null && msg.getStartTime() > msg.getEndTime()) {
            throw new ApiMessageInterceptionException(argerr("startTime[%s] is greater than endTime[%s]", msg.getStartTime(), msg.getEndTime()));
        }
    }

    private void validate(APIUpdateEventDataMsg msg) {
        String updateMode = msg.getUpdateMode();
        if (UPDATE_DATA_MODE_ONLYONE.equals(updateMode)) {
            if (msg.getDataUuid() == null) {
                throw new ApiMessageInterceptionException(argerr("dataUuid cannot be missed"));
            }
        }

        if (UPDATE_DATA_MODE_INRANGE.equals(updateMode)) {
            if (msg.getDataEndTime() == null || msg.getDataStartTime() == null) {
                throw new ApiMessageInterceptionException(argerr("dataStartTime and dataEndTime cannot be missed"));
            }

            if (msg.getDataStartTime() > msg.getDataEndTime()) {
                throw new ApiMessageInterceptionException(argerr("dataStartTime[%s] is greater than dataEndTime[%s]",
                        msg.getDataStartTime(), msg.getDataEndTime()));
            }
        }

        if (msg.getDataStartTime() != null && msg.getDataEndTime() != null && msg.getDataStartTime() > msg.getDataEndTime()) {
            throw new ApiMessageInterceptionException(argerr("startTime[%s] is greater than endTime[%s]", msg.getDataStartTime(), msg.getDataEndTime()));
        }

        Map<String, Label> labels = new HashMap<>();
        if (!acntMgr.isAdmin(msg.getSession())) {
            labels.put(ZWatchConstants.DATA_ACCOUNT_UUID, new Label(String.format("%s=%s", ZWatchConstants.DATA_ACCOUNT_UUID, msg.getSession().getAccountUuid())));
        }
        msg.setLabelList(new ArrayList<>(labels.values()));
    }

    private void validate(APIUpdateAlarmDataMsg msg) {
        String updateMode = msg.getUpdateMode();
        if (UPDATE_DATA_MODE_ONLYONE.equals(updateMode)) {
            if (msg.getDataUuid() == null) {
                throw new ApiMessageInterceptionException(argerr("dataUuid cannot be missed"));
            }
        }

        if (UPDATE_DATA_MODE_INRANGE.equals(updateMode)) {
            if (msg.getDataEndTime() == null || msg.getDataStartTime() == null) {
                throw new ApiMessageInterceptionException(argerr("dataStartTime and dataEndTime cannot be missed"));
            }

            if (msg.getDataStartTime() > msg.getDataEndTime()) {
                throw new ApiMessageInterceptionException(argerr("dataStartTime[%s] is greater than dataEndTime[%s]",
                        msg.getDataStartTime(), msg.getDataEndTime()));
            }
        }

        if (!Account.isAdminPermission(msg.getSession())) {
            Map<String, Label> labels = new HashMap<>();
            labels.put(AlarmDataV2.TAG_ACCOUNT_UUID, new Label(String.format("%s=%s", AlarmDataV2.TAG_ACCOUNT_UUID, msg.getSession().getAccountUuid())));
            msg.setLabelList(new ArrayList<>(labels.values()));
        }

        if (msg.getDataStartTime() != null && msg.getDataEndTime() != null && msg.getDataStartTime() > msg.getDataEndTime()) {
            throw new ApiMessageInterceptionException(argerr("startTime[%s] is greater than endTime[%s]", msg.getDataStartTime(), msg.getDataEndTime()));
        }
    }

    private void validate(APIPutMetricDataMsg msg) {
        if (msg.getNamespace().startsWith(Namespace.ZSTACK_NAMESPACE_PREFIX)) {
            throw new ApiMessageInterceptionException(argerr("namespace name cannot start with %s that is reserved", Namespace.ZSTACK_NAMESPACE_PREFIX));
        }
    }

    private void validate(APIGetMetricLabelValueMsg msg) {
        if (msg.getEndTime() == null || msg.getEndTime() == 0) {
            msg.setEndTime(TimeUnit.MILLISECONDS.toSeconds(timeHelper.getCurrentTimeMillis()));
        }

        if (msg.getStartTime() == null) {
            msg.setStartTime(msg.getEndTime() - TimeUnit.MINUTES.toSeconds(1));
        }

        Timestamp start = new Timestamp(TimeUnit.SECONDS.toMillis(msg.getStartTime()));
        Timestamp end = new Timestamp(TimeUnit.SECONDS.toMillis(msg.getEndTime()));
        if (end.before(start)) {
            throw new ApiMessageInterceptionException(argerr("endTime[%s, %sms] must not be before startTime[%s, %sms]", end, msg.getEndTime(), start, msg.getStartTime()));
        }

        long period = end.getTime() - start.getTime();
        if (period > TimeUtils.parseTimeInMillis(MAX_QUERY_PERIOD)) {
            throw new ApiMessageInterceptionException(argerr("query period cannot exceed %s", MAX_QUERY_PERIOD));
        }

        if (msg.getFilterLabels() == null) {
            msg.setFilterLabels(new ArrayList<>());
        }

        Namespace ns = Namespace.getMetricNameSpace(msg.getNamespace(), msg.getMetricName());
        ns = ensureNamespaceMatched(ns,
                () -> new ApiMessageInterceptionException(argerr("cannot find namespace[%s]", msg.getNamespace())),
                msg.getNamespace());

        Metric metric = null;
        if (ns.getMetrics() != null && !ns.getMetrics().isEmpty()) {
            metric = ns.getMetrics().stream().filter(m -> m.getName().equals(msg.getMetricName())).findFirst().orElse(null);
        }

        if (metric == null) {
            throw new ApiMessageInterceptionException(argerr("cannot find metric[%s] in namespace[%s]", msg.getMetricName(), msg.getNamespace()));
        }

        Set<String> allowedLabels = new LinkedHashSet<>(ns.getEffectiveMetricLabelNames(metric));
        for (String name : msg.getLabelNames()) {
            if (!allowedLabels.contains(name)) {
                throw new ApiMessageInterceptionException(argerr("metric[%s]'s labels[%s] does not include [%s]", msg.getMetricName(),
                        allowedLabels, name));
            }
        }

        List<Label> allLabels = msg.getFilterLabels().stream().map(Label::new).collect(Collectors.toList());
        for (Label l : allLabels) {
            if (!allowedLabels.contains(l.getKey())) {
                throw new ApiMessageInterceptionException(argerr("metric[%s]'s labels[%s] does not include [%s]", msg.getMetricName(),
                        allowedLabels, l.getKey()));
            }
        }

        List<Label> filters = allLabels.stream().filter(l -> l.getOp() == Label.Operator.Filter).collect(Collectors.toList());
        if (filters.isEmpty()) {
            return;
        }

        for (Label l : filters) {
            if (!metric.isFilterExisted(l.getValue())) {
                throw new ApiMessageInterceptionException(argerr("metric[%s] does not has filter[%s]", msg.getMetricName(), l.getValue()));
            }
        }
    }

    private void validate(APIGetPrometheusMetricLabelValueMsg msg) {
        if (msg.getEndTime() == null || msg.getEndTime() == 0) {
            msg.setEndTime(TimeUnit.MILLISECONDS.toSeconds(timeHelper.getCurrentTimeMillis()));
        }

        if (msg.getStartTime() == null) {
            msg.setStartTime(msg.getEndTime() - TimeUnit.MINUTES.toSeconds(1));
        }

        Timestamp start = new Timestamp(TimeUnit.SECONDS.toMillis(msg.getStartTime()));
        Timestamp end = new Timestamp(TimeUnit.SECONDS.toMillis(msg.getEndTime()));
        if (end.before(start)) {
            throw new ApiMessageInterceptionException(argerr(
                    "endTime[%s, %sms] must not be before startTime[%s, %sms]", end, msg.getEndTime(), start, msg.getStartTime())
                    .withOpaque("end.time", end.toString())
                    .withOpaque("start.time", start.toString()));
        }

        long period = end.getTime() - start.getTime();
        if (period > TimeUtils.parseTimeInMillis(MAX_QUERY_PERIOD)) {
            throw new ApiMessageInterceptionException(argerr("query period cannot exceed %s", MAX_QUERY_PERIOD)
                    .withOpaque("period.millis", MAX_QUERY_PERIOD));
        }

        if (msg.getFilterLabels() == null) {
            msg.setFilterLabels(new ArrayList<>());
        }

        Namespace ns = Namespace.getMetricNameSpace(msg.getNamespace(), msg.getMetricName());
        ns = ensureNamespaceMatched(ns,
                () -> new ApiMessageInterceptionException(argerr("cannot find namespace[%s]", msg.getNamespace())
                        .withOpaque("namespace.name", msg.getNamespace())),
                msg.getNamespace());

        Metric metric = null;
        if (ns.getMetrics() != null && !ns.getMetrics().isEmpty()) {
            metric = findOneOrNull(ns.getMetrics(), m -> m.getName().equals(msg.getMetricName()));
        }

        if (metric == null) {
            throw new ApiMessageInterceptionException(argerr(
                    "cannot find metric[%s] in namespace[%s]", msg.getMetricName(), msg.getNamespace())
                    .withOpaque("metric.name", msg.getMetricName())
                    .withOpaque("namespace.name", msg.getNamespace()));
        }

        Set<String> allowedLabels = new LinkedHashSet<>(ns.getEffectiveMetricLabelNames(metric));
        for (String name : msg.getLabelNames()) {
            if (!allowedLabels.contains(name)) {
                throw new ApiMessageInterceptionException(argerr(
                        "metric[%s]'s labels[%s] does not include [%s]", msg.getMetricName(), allowedLabels, name)
                        .withOpaque("metric.name", msg.getMetricName())
                        .withOpaque("label.name", name));
            }
        }

        List<Label> allLabels = msg.getFilterLabels().stream().map(Label::new).collect(Collectors.toList());
        for (Label l : allLabels) {
            if (!allowedLabels.contains(l.getKey())) {
                throw new ApiMessageInterceptionException(argerr(
                        "metric[%s]'s labels[%s] does not include [%s]", msg.getMetricName(), allowedLabels, l.getKey())
                        .withOpaque("metric.name", msg.getMetricName())
                        .withOpaque("label.name", l.getKey()));
            }
        }

        List<Label> filters = allLabels.stream().filter(l -> l.getOp() == Label.Operator.Filter).collect(Collectors.toList());
        if (filters.isEmpty()) {
            return;
        }

        for (Label l : filters) {
            if (!metric.isFilterExisted(l.getValue())) {
                throw new ApiMessageInterceptionException(argerr(
                        "metric[%s] does not has filter[%s]", msg.getMetricName(), l.getValue())
                        .withOpaque("metric.name", msg.getMetricName()));
            }
        }
    }

    private void validate(APIGetAuditDataMsg msg) {
        List<Label> ls = new ArrayList<>();
        //Compatible with existing cases
        if(StringUtils.isEmpty(msg.getAuditType())){
            msg.setAuditType(AuditType.Resource);
        }
        if(msg.getAuditType().equals(AuditType.Login)){
            ls.add(new Label("resourceType=SessionVO"));
            if (msg.getConditions() != null) {
                msg.getConditions().forEach(label -> {
                    Label l = new Label(label);
                    if (!AuditDataV2.queryableLoginLabels.contains(l.getKey())) {
                        throw new ApiMessageInterceptionException(argerr("invalid label[%s], valid queryable labels are %s", l.getKey(), AuditDataV2.queryableLoginLabels));
                    }
                    ls.add(l);
                });
            }
        }else{
            ls.add(new Label("resourceType!=SessionVO"));
            if (msg.getConditions() != null) {
                msg.getConditions().forEach(label -> {
                    Label l = new Label(label);
                    if (!AuditDataV2.queryableLabels.contains(l.getKey())) {
                        throw new ApiMessageInterceptionException(argerr("invalid label[%s], valid queryable labels are %s", l.getKey(), AuditDataV2.queryableLabels));
                    }
                    ls.add(l);
                });
            }
        }

        ls.stream().filter(label -> label.getOp() == Label.Operator.Regex || label.getOp() == Label.Operator.RegexAgainst).forEach(label -> {
            String v = String.format("%%%s%%", label.getValue());
            label.setValue(v);
        });


        msg.setLabelList(ls);

        if (!acntMgr.isAdmin(msg.getSession())) {
            validateNormalAccountSession(msg);
        }

        if (msg.getStartTime() != null && msg.getEndTime() != null && msg.getStartTime() > msg.getEndTime()) {
            throw new ApiMessageInterceptionException(argerr("startTime[%s] is greater than endTime[%s]", msg.getStartTime(), msg.getEndTime()));
        }
    }

    private Namespace ensureNamespaceMatched(Namespace ns, Supplier<ApiMessageInterceptionException> missingNamespaceSupplier,
                                             String requestedNamespace) {
        if (ns == null) {
            Namespace actual = Namespace.getNamespaceByName(requestedNamespace);
            if (actual != null && !(actual instanceof CustomNamespace)) {
                return actual;
            }
            throw missingNamespaceSupplier.get();
        }
        return ns;

    }

    private void validateNormalAccountSession(APIGetAuditDataMsg msg) {
        if (msg.getLabelList() == null || msg.getLabelList().isEmpty()) {
            return;
        }

        Optional<Label> opt = msg.getLabelList().stream().filter(l-> AuditDataV2.TAG_RESOURCE_UUID.equals(l.getKey())).findFirst();
        if (!opt.isPresent()) {
            return;
        }

        // find multi resourceUuid
        // UI Request Example : "resourceUuid=~uuid0|uuid1|uuidn..|"
        if (opt.get().getValue().contains("|") && opt.get().getValue().contains("%")) {
            opt.get().setValue(opt.get().getValue().replace("%",""));
        }

        // check resource ownership
        List<String> resourceUuids = Arrays.asList(opt.get().getValue().split("\\|"));
        resourceUuids = AccessibleResourceChecker.forAccount(msg.getSession().getAccountUuid())
                .allowGlobalReadableResource()
                .findOutAllInaccessibleResources(resourceUuids);
        if (!resourceUuids.isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("account[uuid: %s] has no access to the resource[uuid: %s]",
                    msg.getSession().getAccountUuid(), opt.get().getValue()));
        }
    }

    private void validate(APIGetEventDataMsg msg) {
        if (msg.getConditions() != null) {
            msg.setLabelList(msg.getConditions().stream().map(Label::new).collect(Collectors.toList()));
        }

        if (msg.getStartTime() != null && msg.getEndTime() != null && msg.getStartTime() > msg.getEndTime()) {
            throw new ApiMessageInterceptionException(argerr("startTime[%s] is greater than endTime[%s]", msg.getStartTime(), msg.getEndTime()));
        }

        if (msg.getOffsetAheadOfCurrentTime() != null) {
            msg.setEndTime(TimeUnit.MILLISECONDS.toSeconds(timeHelper.getCurrentTimeMillis()));
            msg.setStartTime(msg.getEndTime() - msg.getOffsetAheadOfCurrentTime());
        }
    }

    private void validate(APIDeleteMetricDataMsg msg) {
        if ("all".equals(msg.getNamespace()) && (!"all".equals(msg.getMetricName()) || !CollectionUtils.isEmpty(msg.getLabels()))) {
            throw new ApiMessageInterceptionException(argerr("if namespace is all, not support specify metric and labels"));
        }

        if ("all".equals(msg.getNamespace())) {
            return;
        }

        Namespace ns = Namespace.getMetricNameSpace(msg.getNamespace(), msg.getMetricName());
        ns = ensureNamespaceMatched(ns,
                () -> new ApiMessageInterceptionException(argerr("cannot find namespace[%s]", msg.getNamespace())),
                msg.getNamespace());

        Set<String> labels = new HashSet<>();
        if ("all".equals(msg.getMetricName())) {
            for (Metric metric : ns.getMetrics()) {
                labels.addAll(new HashSet<>(ns.getEffectiveMetricLabelNames(metric)));
            }
        } else {
            labels = ns.getMetrics().stream()
                    .filter(v -> v.getName().equals(msg.getMetricName()))
                    .map(ns::getEffectiveMetricLabelNames)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());
        }

        if (CollectionUtils.isEmpty(msg.getLabels())) {
            return;
        }

        for (String name : msg.getLabels()) {
            Label label = new Label(name);
            if (!labels.contains(label.getKey())) {
                throw new ApiMessageInterceptionException(argerr("metric[%s]'s labels[%s] does not include [%s]", msg.getMetricName(),
                        labels, label.getKey()));
            }
        }
    }

    private void validate(APIGetMetricDataMsg msg) {
        if (msg.getEndTime() == null || msg.getEndTime() == 0) {
            msg.setEndTime(TimeUnit.MILLISECONDS.toSeconds(timeHelper.getCurrentTimeMillis()));
        }

        if (msg.getStartTime() == null) {
            msg.setStartTime(msg.getEndTime() - TimeUnit.MINUTES.toSeconds(1));
        }

        Timestamp start = new Timestamp(TimeUnit.SECONDS.toMillis(msg.getStartTime()));
        Timestamp end = new Timestamp(TimeUnit.SECONDS.toMillis(msg.getEndTime()));
        if (end.before(start)) {
            throw new ApiMessageInterceptionException(argerr("endTime[%s, %sms] must not be before startTime[%s, %sms]", end, msg.getEndTime(), start, msg.getStartTime()));
        }

        if (msg.getLabels() != null) {
            msg.setLabelList(msg.getLabels().stream().map(Label::new).collect(Collectors.toList()));
        }

        if (msg.getValueConditions() != null) {
            msg.setValueConditionList(msg.getValueConditions().stream().map(ValueCondition::new).collect(Collectors.toList()));
        }

        if (msg.getFunctions() != null) {
            msg.setFunctionList(msg.getFunctions().stream().map(Function::fromString).collect(Collectors.toList()));
        }

        if (msg.getFunctionList() != null) {
            MetricFunction.checkFunctions(msg.getFunctionList());
        }

        Namespace ns = Namespace.getMetricNameSpace(msg.getNamespace(), msg.getMetricName());
        ns = ensureNamespaceMatched(ns,
                () -> new ApiMessageInterceptionException(argerr("no namespace[%s] defined in the system", msg.getNamespace())),
                msg.getNamespace());

        if (!ns.hasMetric(msg.getMetricName())) {
            throw new ApiMessageInterceptionException(argerr("the namespace[%s] has no metric[%s]", msg.getNamespace(), msg.getMetricName()));
        }

        if (msg.getOffsetAheadOfCurrentTime() != null) {
            msg.setEndTime(TimeUnit.MILLISECONDS.toSeconds(timeHelper.getCurrentTimeMillis()));
            msg.setStartTime(msg.getEndTime() - msg.getOffsetAheadOfCurrentTime());
        }
    }

    private void validate(APICreateMetricDataHttpReceiverMsg msg) {
        if (!msg.getUrl().startsWith("http")) {
            throw new ApiMessageInterceptionException(argerr("The url format is invalid, the beginning is not http"));
        }
    }

    private void validate(APICreateMetricTemplateMsg msg) {
        Namespace ns = Namespace.getMetricNameSpace(msg.getNamespace(), msg.getMetricName());
        ns = ensureNamespaceMatched(ns,
                () -> new ApiMessageInterceptionException(argerr("no namespace[%s] defined in the system", msg.getNamespace())),
                msg.getNamespace());
        if (!ns.hasMetric(msg.getMetricName())) {
            throw new ApiMessageInterceptionException(argerr("the namespace[%s] has no metric[%s]", msg.getNamespace(), msg.getMetricName()));
        }

        String labelsJsonStr = msg.getLabelsJsonStr();
        if (labelsJsonStr != null) {
            if (!(labelsJsonStr.startsWith("[") && labelsJsonStr.endsWith("]"))) {
                throw new ApiMessageInterceptionException(argerr("Illegal json string, labelsJsonStr format is invalid"));
            }

            List<String> labelStrings = JSONObjectUtil.toObject(labelsJsonStr, List.class);
            for (String labelStr : labelStrings) {
                new Label(labelStr);
            }
        }
    }

    private void validate(APIAddThirdpartyPlatformMsg msg) {
        boolean exist = Q.New(ThirdpartyPlatformVO.class)
                .eq(ThirdpartyPlatformVO_.type, msg.getType())
                .eq(ThirdpartyPlatformVO_.url, msg.getUrl())
                .isExists();
        if (exist) {
            throw new ApiMessageInterceptionException(argerr("platform[url=%s] already exists", msg.getUrl()));
        }

        if (Constants.XSKY.equals(msg.getType())) {
            String url = msg.getUrl();
            int end = url.indexOf("?");
            url = url.substring(0, end > 0 ? end : url.length());
            exist = Q.New(ThirdpartyPlatformVO.class)
                    .eq(ThirdpartyPlatformVO_.type, msg.getType())
                    .like(ThirdpartyPlatformVO_.url, String.format("%s%%", url))
                    .isExists();
            if (exist) {
                throw new ApiMessageInterceptionException(argerr("platform[url=%s] already exists", url));
            }
        }
    }

    private void validate(APIUpdateThirdpartyAlertsMsg msg) {
        Long startTime = msg.getStartTimeMillis();
        Long endTime = msg.getEndTimeMillis();
        if (startTime != null && endTime != null) {
            if (startTime > endTime) {
                throw new ApiMessageInterceptionException(argerr("startTime[%s] is greater than endTime[%s]",
                        startTime, endTime));
            }
        }
    }

    private void validate(APIUpdateAlertDataAckMsg msg) {
        boolean exists = Q.New(AlertDataAckVO.class)
                .eq(AlertDataAckVO_.alertDataUuid, msg.getAlertDataUuid())
                .isExists();
        if (!exists) {
            throw new ApiMessageInterceptionException(argerr("alert acknowledgement record does not exist", msg.getAlertDataUuid()));
        }
    }

    private void validate(APIGetZWatchAlertHistogramMsg msg) {
        if (msg.getGroupColumns() == null) {
            msg.setGroupColumns(Collections.emptyList());
        }

        List<String> groupColumns = msg.getGroupColumns();
        if (!groupColumns.isEmpty()) {
            groupColumns.removeIf(Objects::isNull);
            groupColumns.removeIf(String::isEmpty);
        }

        String tableName = msg.getTableName();
        if (!(tableName.equals(AlarmRecordsVO.class.getSimpleName()) ||
                tableName.equals(EventRecordsVO.class.getSimpleName()))) {
            throw new ApiMessageInterceptionException(argerr("invalid table[%s]", tableName));
        }

        long startTime = msg.getStartTime();
        long endTime = msg.getEndTime();
        if (startTime >= endTime) {
            throw new ApiMessageInterceptionException(argerr("endTime[%s] must not be before startTime[%s]", endTime, startTime));
        }

        long maxDurationDay = 30;
        if (endTime - startTime > TimeUnit.DAYS.toMillis(maxDurationDay)) {
            throw new ApiMessageInterceptionException(argerr("the time interval exceeds % days", maxDurationDay));
        }
    }

    private void validate(APIChangeActiveAlarmStateMsg msg) {
        if (msg.getStateEvent().equals("enable")) {
            deleteInvalidActiveAlarm(msg);
        }

    }

    private void deleteInvalidActiveAlarm(APIChangeActiveAlarmStateMsg msg) {
        String accountUuid = msg.getSession().getAccountUuid();
        String namespace = msg.getNamespace();

        List<ActiveAlarmVO> alarms = ResourceHelper.findOwnResources(ActiveAlarmVO.class, accountUuid,
                q -> q.eq(ActiveAlarmVO_.namespace, namespace));
        List<String> alarmUuids = CollectionUtils.transform(alarms, ActiveAlarmVO::getAlarmUuid);
        if (alarmUuids.isEmpty()) {
            return;
        }

        List<String> existingAlarmUuids = Q.New(AlarmVO.class)
                .select(AlarmVO_.uuid)
                .in(AlarmVO_.uuid, alarmUuids)
                .listValues();
        alarmUuids.removeAll(existingAlarmUuids);
        if (alarmUuids.isEmpty()) {
            return;
        }

        SQL.New(ActiveAlarmVO.class)
                .in(ActiveAlarmVO_.alarmUuid, alarmUuids)
                .hardDelete();
    }

    private void validate(APIQueryAuditMsg msg) {
        if (Account.supportToQueryAuditsFromAllAccounts(msg.getSession())) {
            return;
        }

        QueryCondition qc = new QueryCondition();
        qc.setName("operatorAccountUuid");
        qc.setOp(QueryOp.EQ.toString());
        qc.setValue(msg.getSession().getAccountUuid());
        msg.getConditions().add(qc);
    }

    private void validate(APIQueryAlarmRecordMsg msg) {
        if (Account.supportToQueryEventsFromAllAccounts(msg.getSession())) {
            return;
        }

        if (!acntMgr.isAdmin(msg.getSession())) {
            QueryCondition qc = new QueryCondition();
            qc.setName("accountUuid");
            qc.setOp(QueryOp.EQ.toString());
            qc.setValue(msg.getSession().getAccountUuid());
            msg.getConditions().add(qc);
        }
    }

    private void validate(APIQueryEventRecordMsg msg) {
        if (Account.supportToQueryEventsFromAllAccounts(msg.getSession())) {
            return;
        }

        if (!acntMgr.isAdmin(msg.getSession())) {
            QueryCondition qc = new QueryCondition();
            qc.setName("accountUuid");
            qc.setOp(QueryOp.EQ.toString());
            qc.setValue(msg.getSession().getAccountUuid());
            msg.getConditions().add(qc);
        }
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return list(
            APICreateMetricDataHttpReceiverMsg.class,
            APICreateMetricTemplateMsg.class,
            APIDeleteMetricDataHttpReceiverMsg.class,
            APIDeleteMetricTemplateMsg.class,
            APIAddThirdpartyPlatformMsg.class,
            APIUpdateThirdpartyAlertsMsg.class,
            APIChangeActiveAlarmStateMsg.class,
            APIQueryAuditMsg.class,
            APIQueryAlarmRecordMsg.class,
            APIQueryEventRecordMsg.class
        );
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.DEFAULT;
    }
}
