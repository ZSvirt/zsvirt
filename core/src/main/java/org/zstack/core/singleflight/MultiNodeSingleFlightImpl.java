package org.zstack.core.singleflight;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.ResourceDestinationMakerImpl;
import org.zstack.core.thread.SingleFlightTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.CoreConstant;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.addon.SingleFlightExecutor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import static org.zstack.core.Platform.operr;

public class MultiNodeSingleFlightImpl {
    private static final CLogger logger = Utils.getLogger(MultiNodeSingleFlightImpl.class);

    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ResourceDestinationMakerImpl destinationMaker;

    private static final Map<String, SingleFlightExecutor> singleFlightExecutors = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Method>> singleFlightMethods = new ConcurrentHashMap<>();

    public static void register(SingleFlightExecutor executor) {
        if (!singleFlightMethods.containsKey(executor.getClass().getName())) {
            Method[] methods = executor.getClass().getDeclaredMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(SingleFlightExecutor.SingleFlight.class)) {
                    validateSingleFlight(method);
                    singleFlightMethods.computeIfAbsent(executor.getClass().getName(), k -> new HashMap<>()).put(method.getName(), method);
                }
            }
        }

        logger.debug(String.format("register single flight executor [%s, uuid: %s]", executor.getClass().getSimpleName(), executor.getResourceUuid()));
        singleFlightExecutors.put(executor.getResourceUuid(), executor);
    }

    public static boolean unregister(String resourceUuid) {
        SingleFlightExecutor removed = singleFlightExecutors.remove(resourceUuid);
        if (removed != null) {
            logger.debug(String.format("unregister single flight executor [%s, uuid: %s]", removed.getClass().getSimpleName(), resourceUuid));
        }

        return removed != null;
    }

    public static SingleFlightExecutor getExecutor(String resourceUuid) {
        return singleFlightExecutors.get(resourceUuid);
    }

    public void run(SingleFlightExecutor executor, String method, Object... args) {
        ReturnValueCompletion<Object> completion = (ReturnValueCompletion) args[args.length - 1];
        Method consumer = singleFlightMethods.get(executor.getClass().getName()).get(method);
        if (consumer == null) {
            throw new IllegalArgumentException(executor.getClass() + " has not register single flight method " + method);
        }

        boolean localSingleFlight = !singleFlightExecutors.containsKey(executor.getResourceUuid()) ||
                destinationMaker.isManagedByUs(executor.getResourceUuid());

        boolean unitTestSaySendMsg = CoreGlobalProperty.UNIT_TEST_ON && new Random().nextBoolean();
        if (localSingleFlight && !unitTestSaySendMsg) {
            logger.info(String.format("start running local single flight task [method:%s,resource:%s]", method, executor.getResourceUuid()));
            thdf.singleFlightSubmit(new SingleFlightTask(null)
                    .setSyncSignature("external-single-flight-" + executor.getResourceUuid())
                    .run(outCompletion -> {
                        try {
                            args[args.length - 1] = outCompletion;
                            consumer.invoke(executor, args);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            outCompletion.fail(operr(e.getMessage()));
                        }
                    })
                    .done(result -> {
                        if (result.isSuccess()) {
                            completion.success(result.getResult());
                        } else {
                            completion.fail(result.getErrorCode());
                        }
                    })
            );
            return;
        }

        ExternalSingleFlightMsg msg = new ExternalSingleFlightMsg();
        msg.setResourceUuid(executor.getResourceUuid());
        msg.setMethod(method);
        msg.setArgs(Arrays.copyOfRange(args, 0, args.length - 1));
        bus.makeTargetServiceIdByResourceUuid(msg, CoreConstant.SERVICE_ID, executor.getResourceUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                completion.success(((ExternalSingleFlightReply) reply).getResult());
            }
        });
    }

    private static void validateSingleFlight(Method m) {
        if (m.getParameterTypes()[m.getParameterTypes().length - 1] != ReturnValueCompletion.class) {
            throw new IllegalArgumentException("The last parameter of " + m.getName() + " must be ReturnValueCompletion");
        }

        if (m.getReturnType() != void.class) {
            throw new IllegalArgumentException("The return type of " + m.getName() + " must be void");
        }

        m.setAccessible(true);
    }
}
