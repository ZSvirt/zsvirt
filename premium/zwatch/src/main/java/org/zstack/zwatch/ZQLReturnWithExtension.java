package org.zstack.zwatch;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.EntityMetadata;
import org.zstack.core.thread.SyncTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.zql.ReturnWithExtensionPoint;
import org.zstack.query.QueryGlobalConfig;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.zql.ZQLContext;
import org.zstack.zwatch.datatype.*;
import org.zstack.zwatch.returnwith.antlr4.ReturnWithBaseListener;
import org.zstack.zwatch.returnwith.antlr4.ReturnWithLexer;
import org.zstack.zwatch.returnwith.antlr4.ReturnWithParser;
import org.zstack.zwatch.utils.ResourceVOToNamespaceMappingUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

public class ZQLReturnWithExtension implements ReturnWithExtensionPoint {
    private static final CLogger logger = Utils.getLogger(ZQLReturnWithExtension.class);

    public static final String NAME = "zwatch";

    @Autowired
    private ThreadFacade thdf;

    @Override
    public String getReturnWithName() {
        return NAME;
    }

    @Override
    public void returnWith(ReturnWithExtensionParam param, ReturnValueCompletion<Map> completion) {
        if (param.vos.isEmpty()) {
            completion.success(null);
            return;
        }

        thdf.syncSubmit(new SyncTask<Void>() {
            @Override
            public String getName() {
                return "ZQLReturnWith";
            }

            @Override
            public Void call() throws Exception {
                try {
                    String labelName = getRestrictLabelNames(param.voClass);
                    List<String> uuids;
                    if (!param.isFieldsQuery) {
                        Field priKey = EntityMetadata.getPrimaryKeyField(param.voClass);
                        priKey.setAccessible(true);
                        uuids = (List<String>) param.vos.stream().map(v -> {
                            try {
                                return priKey.get(v);
                            } catch (IllegalAccessException e) {
                                throw new CloudRuntimeException(e);
                            }
                        }).collect(Collectors.toList());
                    } else {
                        uuids = (List<String>) param.vos.stream().map(v -> {
                            if (v instanceof Object[]) {
                                Object[] fieldValues = (Object[]) v;
                                return fieldValues[param.primaryKeyIndexInVOs];
                            } else {
                                return v;
                            }
                        }).collect(Collectors.toList());
                    }
                    Map result = callZWatch(param.expression, getNamespaceName(param.voClass), labelName, uuids);
                    completion.success(result);
                } catch (Exception e) {
                    completion.fail(operr(e.getMessage()));
                }
                return null;
            }

            @Override
            public String getSyncSignature() {
                return getName();
            }

            @Override
            public int getSyncLevel() {
                return QueryGlobalConfig.ZQL_RETURN_WITH_CONCURRENCY.value(Integer.class);
            }
        });
    }

    private String createRestrictLabel(String labelName, List<String> uuids) {
        return String.format("%s%s%s", labelName, Label.Operator.Regex, StringUtils.join(uuids, "|"));
    }

    private String getRestrictLabelNames(Class clz) {
        return ResourceVOToNamespaceMappingUtils.getVOToNamespaceMapping(clz)[1];
    }

    private String getNamespaceName(Class clz) {
        return ResourceVOToNamespaceMappingUtils.getVOToNamespaceMapping(clz)[0];
    }

    class ThrowingErrorListener extends BaseErrorListener {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e)
                throws ParseCancellationException {
            throw new ParseCancellationException("line " + line + ":" + charPositionInLine + " " + msg);
        }
    }

    private String normalizeExpr(String expr) {
        expr = StringUtils.removeStart(expr, "{");
        expr = StringUtils.removeEnd(expr, "}");
        return expr;
    }

    private Map callZWatch(String expr, String namespace, String restrictLabelName, List<String> uuids) {
        Map<String, Object> result = new HashMap<>();

        String restrictLabel = createRestrictLabel(restrictLabelName, uuids);

        class ResultName {
            String name;
        }

        ResultName resultName = new ResultName();
        resultName.name = NAME;

        SessionInventory session = ZQLContext.getAPISession();
        if (session == null) {
            // this is from an internal use of zql
            session = new SessionInventory();
            session.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
        }

        MetricQueryObject.QueryObjectBuilder qb = MetricQueryObject.New();

        ReturnWithLexer l;
        String normalizedExpr = normalizeExpr(expr);
        try {
            l = new ReturnWithLexer(CharStreams.fromStream(new ByteArrayInputStream(normalizedExpr.getBytes())));
        } catch (IOException e) {
            throw new CloudRuntimeException(e);
        }

        List<String> labels = new ArrayList<>();
        List<String> valueConditions = new ArrayList<>();
        final Boolean[] fillNoValuePoints = {false};
        List<String> functions = new ArrayList<>();
        ReturnWithParser p = new ReturnWithParser(new CommonTokenStream(l));
        p.removeErrorListeners();
        p.addErrorListener(new ThrowingErrorListener());
        p.addParseListener(new ReturnWithBaseListener() {
            boolean timeSet;

            private String normalizeString(String str) {
                str = StringUtils.removeStart(str, "\"");
                str = StringUtils.removeEnd(str, "\"");
                str = StringUtils.removeEnd(str, "'");
                str = StringUtils.removeStart(str, "'");
                return str;
            }

            @Override
            public void exitExpr(ReturnWithParser.ExprContext ctx) {
                String paramName = ctx.ID().getText();
                switch (paramName) {
                    case "metricName": {
                        qb.metricName(normalizeString(ctx.value().getText()));
                        break;
                    }
                    case "startTime": {
                        if (!timeSet) {
                            qb.startTime(Long.valueOf(ctx.value().getText()));
                        }
                        break;
                    }
                    case "endTime": {
                        if (!timeSet) {
                            qb.endTime(Long.valueOf(ctx.value().getText()));
                        }
                        break;
                    }
                    case "offsetAheadOfCurrentTime": {
                        long offsetAheadOfCurrentTime = Long.valueOf(ctx.value().getText());
                        long now = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
                        qb.endTime(now);
                        qb.startTime(now - offsetAheadOfCurrentTime);
                        timeSet = true;
                        break;
                    }
                    case "period": {
                        qb.period(Integer.valueOf(ctx.value().getText()));
                        break;
                    }
                    case "labels": {
                        labels.add(normalizeString(ctx.value().getText()));
                        break;
                    }
                    case "valueConditions": {
                        valueConditions.add(normalizeString(ctx.value().getText()));
                        break;
                    }
                    case "functions": {
                        functions.add(normalizeString(ctx.value().getText()));
                        break;
                    }
                    case "namespace": {
                        qb.namespace(normalizeString(ctx.value().getText()));
                        break;
                    }
                    case "resultName": {
                        resultName.name = normalizeString(ctx.value().getText());
                        break;
                    }
                    case "fillNoValuePoints": {
                        fillNoValuePoints[0] = Boolean.valueOf(normalizeString(ctx.value().getText()));
                        break;
                    }
                    default: {
                        throw new OperationFailureException(argerr("unknown parameter[%s] in zwatch return with clause, %s", paramName, normalizedExpr));
                    }
                }
            }
        });

        try {
            p.expression();
        } catch (Exception e) {
            throw new OperationFailureException(argerr("invalid zwatch return with clause: %s, %s", expr, e.getMessage()));
        }

        MetricQueryObject qo = qb
                .namespace(namespace)
                .labels(labels.stream().map(Label::new).collect(Collectors.toList()))
                .valueConditions(valueConditions.stream().map(ValueCondition::new).collect(Collectors.toList()))
                .account(session.getAccountUuid()).build();
        qo.getLabels().add(new Label(restrictLabel));

        if (logger.isTraceEnabled()) {
            logger.trace(String.format("[zwatch return with]query object: %s", qo));
        }

        GetMetricDataFunc func = new GetMetricDataFunc();
        func.session = session;
        func.queryObject = qo;
        if (!functions.isEmpty()) {
            func.functions = functions.stream().map(Function::fromString).collect(Collectors.toList());
        }

        Map<Integer, List<Datapoint>> resultMap;
        if (fillNoValuePoints[0] != null && fillNoValuePoints[0]) {
            resultMap = func.applyWithMissingData(restrictLabelName, uuids, qo.getEndTime());
        } else {
            resultMap = func.applyReturnWithTotal();
        }

        resultMap.forEach((key, value) -> {
            result.put(resultName.name, value);
            result.put(String.format("%sTotal", resultName.name), key);
        });

        return result;
    }
}
