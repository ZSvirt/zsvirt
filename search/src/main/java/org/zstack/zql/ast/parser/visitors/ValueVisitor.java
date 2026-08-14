package org.zstack.zql.ast.parser.visitors;

import com.google.common.base.Throwables;
import org.apache.commons.lang.StringUtils;
import org.zstack.core.Platform;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.zql.ASTNode;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.FieldUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.zql.ZQLContext;
import org.zstack.zql.antlr4.ZQLBaseVisitor;
import org.zstack.zql.antlr4.ZQLParser;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

public class ValueVisitor extends ZQLBaseVisitor<ASTNode.Value> {
    private static final CLogger logger = Utils.getLogger(ValueVisitor.class);
    public ASTNode.ComplexValue visitSubQueryValue(ZQLParser.SubQueryValueContext ctx) {
        ASTNode.ComplexValue v = new ASTNode.ComplexValue();
        v.setSubQuery(ctx.subQuery().accept(new SubQueryVisitor()));
        return v;
    }

    public ASTNode.Value visitValue(ZQLParser.ValueContext ctx) {
        if (!ctx.value().isEmpty()) {
            ASTNode.ListValue l = new ASTNode.ListValue();
            l.setValues(ctx.value().stream().map(it->it.accept(new ValueVisitor())).collect(Collectors.toList()));
            return l;
        }

        ASTNode.PlainValue v = new ASTNode.PlainValue();
        v.setText(ctx.getText());
        if (ctx.INT() != null) {
            v.setType(Long.class);
        } else if (ctx.FLOAT() != null) {
            v.setType(Double.class);
        } else if (ctx.STRING() != null) {
            v.setType(String.class);
        } else if (ctx.BOOLEAN() != null) {
            v.setType(Boolean.class);
            v.setText(StringUtils.strip(v.getText(), "'"));
        } else {
            DebugUtils.Assert(false, "should not be here");
        }

        v.setCtype(v.getType().getName());
        return v;
    }

    @Override
    public ASTNode.Value visitSimpleValue(ZQLParser.SimpleValueContext ctx) {
        return visitValue(ctx.value());
    }

    public ASTNode.Value visitApiGetValue(ZQLParser.ApiGetValueContext ctx) {
        return apiGet(ctx.input(), ctx.output(), ctx.apiparams());
    }

    private ASTNode.Value apiGet(ZQLParser.InputContext ictx, ZQLParser.OutputContext octx, List<ZQLParser.ApiparamsContext> pctx) {
        ASTNode.ListValue v = new ASTNode.ListValue();
        v.setValues(new ArrayList<>());

        String apiName = formatValue(ictx.namedAsValue().getText());
        String output = formatValue(octx.namedAsValue().getText());
        Map<String, Object> params = formatParams(pctx);
        logger.debug(String.format("input: %s, output: %s, params: %s", apiName, output, JSONObjectUtil.toJsonString(params)));

        Object res = callAction(apiName, output, params);
        logger.debug(String.format("call action result: %s", JSONObjectUtil.toJsonString(res)));

        if (res instanceof List) {
            List<Object> tmp = (List)res;
            if (tmp.isEmpty()) {
                v.getValues().add(generateEmptyValue());
            } else {
                v.setValues(tmp.stream().filter(Objects::nonNull).map(id -> {
                    ASTNode.PlainValue strValue = new ASTNode.PlainValue();
                    strValue.setText("'" + id.toString() + "'");
                    strValue.setCtype(String.class.getName());
                    strValue.setType(String.class);
                    return strValue;
                }).collect(Collectors.toList()));
            }
        } else {
            if (res.toString().isEmpty()) {
                v.getValues().add(generateEmptyValue());
            } else {
                ASTNode.PlainValue strValue = new ASTNode.PlainValue();
                strValue.setText("'" + res.toString() + "'");
                strValue.setCtype(String.class.getName());
                strValue.setType(String.class);
                v.getValues().add(strValue);
            }
        }

        return v;
    }

    private Map<String, Object> formatParams(List<ZQLParser.ApiparamsContext> pctx) {
        Map<String, Object> params = new HashMap<>();
        pctx.forEach(p -> {
            String key = p.namedAsKey().getText();
            if (p.value() != null) {
                params.put(key, getParamsByType(p.value()));
            } else {  // p.listValue is not empty
                params.put(key, p.listValue().value().stream().map(this::getParamsByType).collect(Collectors.toList()));
            }
        });
        return params;
    }

    private String formatValue(String str) {
        return StringUtils.strip(str, "'");
    }

    private Object getParamsByType(ZQLParser.ValueContext context) {
        if (context.BOOLEAN() != null) {
            return Boolean.valueOf(context.getText());
        } else if (context.INT() != null) {
            return Long.valueOf(context.getText());
        } else if (context.FLOAT() != null) {
            return Double.valueOf(context.getText());
        } else {
            return formatValue(context.getText());
        }
    }

    private Object callAction(String apiStr, String outputStr, Map<String, Object> params) {
        List<String> o = Arrays.asList(outputStr.split("\\."));
        if (o.isEmpty()) {
            throw new OperationFailureException(Platform.operr("output from [%s] is empty", apiStr));
        }

        String apiName = "org.zstack.sdk." + apiStr + "Action";
        logger.debug(String.format("start to call sdk: %s, params: %s", apiName, JSONObjectUtil.toJsonString(params)));
        try {
            Object action = Class.forName(apiName).newInstance();
            params.forEach((k, v) -> {
                setField(action, k, v);
            });
            Method call = action.getClass().getMethod("call");
            Field f = action.getClass().getDeclaredField("sessionId");
            f.setAccessible(true);
            f.set(action, ZQLContext.getAPISessionUuid());
            Object result = call.invoke(action);
            Field err = result.getClass().getField("error");
            err.setAccessible(true);
            Object ob = err.get(result);
            if (ob != null) {
                throw new OperationFailureException(operr("call action[%s] failed, cause: %s", apiName, JSONObjectUtil.toJsonString(ob)));
            } else {
                Field field = result.getClass().getField("value");
                field.setAccessible(true);
                ob = field.get(result);
                return result(ob, o);
            }
        } catch (Exception e) {
            logger.debug(String.format("failed to call sdk: %s, params: %s\n%s", apiName, JSONObjectUtil.toJsonString(params), Throwables.getStackTraceAsString(e)));
            // InvocationTargetException contains actual exception in its target
            // but no error message in itself
            if (e instanceof InvocationTargetException) {
                throw new OperationFailureException(operr(((InvocationTargetException) e).getTargetException().getMessage()));
            }

            throw new OperationFailureException(operr(e.getMessage()));
        }
    }

    private Object result(Object res, List<String> outputs) throws ReflectiveOperationException {
        Field f = FieldUtils.getField(outputs.get(0), res.getClass());
        f.setAccessible(true);
        Object tmp = f.get(res);
        if (outputs.size() == 1) {
            return tmp;
        } else {
            List<String> output = outputs.subList(1, outputs.size());
            if (tmp instanceof List) {
                List<Object> out = new ArrayList<>();
                for (Object t: (List)tmp) {
                    out.add(result(t, output));
                }
                return out;
            } else {
                return result(tmp, output);
            }
        }
    }

    private void setField(Object o, String key, Object value) {
        try {
            Field f = o.getClass().getDeclaredField(key);
            f.setAccessible(true);
            if (Integer.class.isAssignableFrom(f.getType()) && value instanceof Long) {
                f.set(o, ((Long)value).intValue());
            } else if (Long.class.isAssignableFrom(f.getType()) && value instanceof Integer) {
                f.set(o, ((Integer)value).longValue());
            } else {
                f.set(o, value);
            }
        } catch (Exception e) {
            throw new OperationFailureException(operr(e.getMessage()));
        }
    }

    private ASTNode.PlainValue generateEmptyValue() {
        ASTNode.PlainValue v = new ASTNode.PlainValue();
        v.setText("\'empty_id\'");
        v.setCtype(String.class.getName());
        v.setType(String.class);
        return v;
    }
}
