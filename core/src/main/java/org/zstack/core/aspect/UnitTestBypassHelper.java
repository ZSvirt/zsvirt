package org.zstack.core.aspect;

import org.aspectj.lang.JoinPoint;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class UnitTestBypassHelper {
    private static final CLogger logger = Utils.getLogger(UnitTestBypassHelper.class);

    private static Map<String, Consumer<JoinPoint>> consumers = new HashMap<>();
    private static Map<String, List<Function<JoinPoint, Boolean>>> judgers = new HashMap<>();

    public static Runnable registerBypassConsumer(String signature, Consumer<JoinPoint> consumer) {
        consumers.put(signature, consumer);
        return () -> consumers.remove(signature);
    }

    public static Runnable registerBypassJudger(String signature, Function<JoinPoint, Boolean> judger) {
        List<Function<JoinPoint, Boolean>> lst = judgers.computeIfAbsent(signature, x->new ArrayList<>());
        lst.add(judger);
        return () ->  {
            lst.remove(judger);
            if (lst.isEmpty()) {
                judgers.remove(signature);
            }
        };
    }

    static boolean callJudger(JoinPoint jp) {
        String name = String.format("%s.%s", jp.getSignature().getDeclaringType().getName(), jp.getSignature().getName());

        for (Map.Entry<String, List<Function<JoinPoint, Boolean>>> e : judgers.entrySet()) {
            String sig = e.getKey();
            if (!name.startsWith(sig)) {
                continue;
            }

            List<Function<JoinPoint, Boolean>> lst = e.getValue();
            for (Function<JoinPoint, Boolean> func : lst) {
                if (!func.apply(jp)) {
                    logger.debug(String.format("UnitTestBypass judger[%s] says not by pass the function[%s]", func.getClass(), name));
                    return false;
                }
            }
        }

        return true;
    }

    static void callConsumer(JoinPoint jp) {
        String name = String.format("%s.%s", jp.getSignature().getDeclaringType().getName(), jp.getSignature().getName());
        Consumer<JoinPoint> consumer = consumers.get(name);
        if (consumer != null) {
            consumer.accept(jp);
        }
    }
}
