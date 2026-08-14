package org.zstack.zwatch.datatype;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.apache.commons.lang.StringUtils;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.rest.SDK;
import org.zstack.monitoring.trigger.expression.antlr4.TriggerExpressionLexer;
import org.zstack.zwatch.api.antlr4.MetricFunctionBaseListener;
import org.zstack.zwatch.api.antlr4.MetricFunctionLexer;
import org.zstack.zwatch.api.antlr4.MetricFunctionParser;

import static org.zstack.core.Platform.argerr;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Function {
    public static class Argument {
        public String name;
        public String value;
    }

    public static Function fromString(String expr) {
        List<Function> functions = new ArrayList<>();

        class ThrowingErrorListener extends BaseErrorListener {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e)
                    throws ParseCancellationException {
                throw new ParseCancellationException("line " + line + ":" + charPositionInLine + " " + msg);
            }
        }

        MetricFunctionLexer l;
        try {
            l = new MetricFunctionLexer(CharStreams.fromStream(new ByteArrayInputStream(expr.getBytes())));
        } catch (IOException e) {
            throw new CloudRuntimeException(e);
        }

        MetricFunctionParser p = new MetricFunctionParser(new CommonTokenStream(l));
        p.removeErrorListeners();
        p.addErrorListener(new ThrowingErrorListener());
        p.addParseListener(new MetricFunctionBaseListener() {
            Stack<Function> stack = new Stack<>();

            private String stripQuotation(String str) {
                return StringUtils.removeEnd(StringUtils.removeStart(str, "\""), "\"");
            }

            @Override public void enterFunction(MetricFunctionParser.FunctionContext ctx) {
                Function function = new Function();
                stack.push(function);
            }

            @Override public void exitFunction(MetricFunctionParser.FunctionContext ctx) {
                functions.add(stack.pop());
            }

            @Override public void exitFunc(MetricFunctionParser.FuncContext ctx) {
                Function function = stack.peek();
                function.name = ctx.ID().getText();
            }

            @Override public void exitArgInt(MetricFunctionParser.ArgIntContext ctx) {
                Function function = stack.peek();
                Argument arg = new Argument();
                arg.name = ctx.ID().getText();
                arg.value = ctx.INT().getText();
                function.arguments.add(arg);
            }

            @Override public void exitArgString(MetricFunctionParser.ArgStringContext ctx) {
                Function function = stack.peek();
                Argument arg = new Argument();
                arg.name = ctx.ID().getText();
                arg.value = stripQuotation(ctx.STRING().getText());
                function.arguments.add(arg);
            }

            @Override public void exitArgFloat(MetricFunctionParser.ArgFloatContext ctx) {
                Function function = stack.peek();
                Argument arg = new Argument();
                arg.name = ctx.ID().getText();
                arg.value = ctx.FLOAT().getText();
                function.arguments.add(arg);
            }

            @Override public void exitArgChar(MetricFunctionParser.ArgCharContext ctx) {
                Function function = stack.peek();
                Argument arg = new Argument();
                arg.name = ctx.ID().getText();
                arg.value = ctx.VALUE().getText();
                function.arguments.add(arg);
            }
        });

        try {
            p.function();
        } catch (Exception e) {
            throw new OperationFailureException(argerr("invalid function: %s, %s", expr, e.getMessage()));
        }

        if (functions.isEmpty()) {
            throw new OperationFailureException(argerr("invalid expression: %s, no function found", expr));
        }

        return functions.get(0);
    }

    private String name;
    private List<Argument> arguments = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Argument> getArguments() {
        return arguments;
    }

    public void setArguments(List<Argument> arguments) {
        this.arguments = arguments;
    }
}
