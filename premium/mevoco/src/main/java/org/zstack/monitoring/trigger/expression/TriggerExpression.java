package org.zstack.monitoring.trigger.expression;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.apache.commons.lang.StringUtils;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.monitoring.trigger.expression.antlr4.TriggerExpressionBaseListener;
import org.zstack.monitoring.trigger.expression.antlr4.TriggerExpressionLexer;
import org.zstack.monitoring.trigger.expression.antlr4.TriggerExpressionParser;
import static org.zstack.core.Platform.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Created by xing5 on 2017/6/4.
 */
public class TriggerExpression {
    private String rawString;
    private String item;
    private Map<String, Object> arguments = new HashMap<>();
    private String operator;
    private String constant;

    public static TriggerExpression expressionFromString(String expr) {
        return expressionListFromString(expr).get(0);
    }

    public static List<TriggerExpression> expressionListFromString(String expr) {
        List<TriggerExpression> triggerExpressions = new ArrayList<>();

        class ThrowingErrorListener extends BaseErrorListener {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e)
                    throws ParseCancellationException {
                throw new ParseCancellationException("line " + line + ":" + charPositionInLine + " " + msg);
            }
        }

        TriggerExpressionLexer l;
        try {
            l = new TriggerExpressionLexer(CharStreams.fromStream(new ByteArrayInputStream(expr.getBytes())));
        } catch (IOException e) {
            throw new CloudRuntimeException(e);
        }

        TriggerExpressionParser p = new TriggerExpressionParser(new CommonTokenStream(l));
        p.removeErrorListeners();
        p.addErrorListener(new ThrowingErrorListener());
        p.addParseListener(new TriggerExpressionBaseListener() {
            Stack<TriggerExpression> stack = new Stack<>();

            private String stripQuotation(String str) {
                return StringUtils.removeEnd(StringUtils.removeStart(str, "\""), "\"");
            }

            @Override
            public void enterExpression(TriggerExpressionParser.ExpressionContext ctx) {
                TriggerExpression triggerExpression = new TriggerExpression();
                stack.push(triggerExpression);
            }

            @Override
            public void exitArgInt(TriggerExpressionParser.ArgIntContext ctx) {
                TriggerExpression triggerExpression = stack.peek();
                triggerExpression.getArguments().put(ctx.ID().getText(), Long.valueOf(ctx.INT().getText()));
            }

            @Override
            public void exitArgString(TriggerExpressionParser.ArgStringContext ctx) {
                TriggerExpression triggerExpression = stack.peek();
                triggerExpression.getArguments().put(ctx.ID().getText(), stripQuotation(ctx.STRING().getText()));
            }

            @Override
            public void exitItem(TriggerExpressionParser.ItemContext ctx) {
                TriggerExpression triggerExpression = stack.peek();
                triggerExpression.setItem(ctx.getText());
            }

            @Override
            public void exitOperator(TriggerExpressionParser.OperatorContext ctx) {
                TriggerExpression triggerExpression = stack.peek();
                triggerExpression.setOperator(ctx.getText());
            }

            @Override
            public void exitConstant(TriggerExpressionParser.ConstantContext ctx) {
                TriggerExpression triggerExpression = stack.peek();
                triggerExpression.setConstant(ctx.getText());
            }

            @Override
            public void exitExpression(TriggerExpressionParser.ExpressionContext ctx) {
                TriggerExpression triggerExpression = stack.pop();
                triggerExpression.setRawString(ctx.getText());
                triggerExpressions.add(triggerExpression);
            }
        });

        try {
            p.trigger();
        } catch (Exception e) {
            throw new OperationFailureException(argerr("invalid expression: %s, %s", expr, e.getMessage()));
        }

        if (triggerExpressions.isEmpty()) {
            throw new OperationFailureException(argerr("invalid expression: %s, no expression found", expr));
        }

        return triggerExpressions;
    }

    // not used, but keep it for future
    private void throwExceptionIfMissingArgument(String key) {
        Object value = argument(key);
        if (value == null) {
            throw new OperationFailureException(argerr("missing parameter '%s' in the expression", key));
        }
    }

    public Object getOrThrowExceptionIfArgumentNotInstanceOf(String key, Class clz) {
        Object value = argument(key);
        if (value == null) {
            throw new OperationFailureException(argerr("missing parameter '%s' in the expression", key));
        }

        if (!clz.isAssignableFrom(value.getClass())) {
            throw new OperationFailureException(argerr("wrong type of parameter '%s' in the expression, it must be type of %s, but got %s", key, clz, value.getClass()));
        }

        return value;
    }

    public String getRawString() {
        return rawString;
    }

    public void setRawString(String rawString) {
        this.rawString = rawString;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public Object argument(String key) {
        return arguments.get(key);
    }

    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getConstant() {
        return constant;
    }

    public void setConstant(String constant) {
        this.constant = constant;
    }
}
