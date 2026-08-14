package org.zstack.header.cluster;

import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusImpl3;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;

public class PowerOffHardwareExecutor {
    private static final CLogger logger = Utils.getLogger(PowerOffHardwareExecutor.class);

    private Long maxWaitTime;
    private boolean waitTaskCompleted;
    private static CloudBus bus;

    private List<PowerOffHardwareResult> results = Collections.synchronizedList(new ArrayList<>());


    public static PowerOffHardwareExecutor New(boolean waitTaskCompleted, Long maxWaitTime) {
        PowerOffHardwareExecutor result = new PowerOffHardwareExecutor();
        result.maxWaitTime = maxWaitTime;
        result.waitTaskCompleted = waitTaskCompleted;
        if (bus == null) {
            bus = Platform.getComponentLoader().getComponent(CloudBus.class);
        }
        return result;
    }

    public void powerOffHardware(List<PowerOffHardwareMsg> pmsgs, ReturnValueCompletion<List<PowerOffHardwareResult>> completion) {
        ensureMsgsAllHandledByOurselves(pmsgs);

        List<PowerOffHardwareMsg> powerOffMnMsgs = new ArrayList<>();
        for (PowerOffHardwareMsg pmsg : pmsgs) {
            if (pmsg.powerOffManagementNode()) {
                powerOffMnMsgs.add(pmsg);
            }
        }
        pmsgs.removeAll(powerOffMnMsgs);
        ensureShutdownOurselvesLast(powerOffMnMsgs);
        new While<>(pmsgs).all(this::doPowerOff).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                for (PowerOffHardwareResult result : results) {
                    if (!result.isSuccess()) {
                        powerOffMnMsgs.forEach(msg -> msg.getUuids().forEach(uuid -> results.add(PowerOffHardwareResult.valueOf(uuid,
                                err(SysErrors.OPERATION_ERROR, "some error happened, skip management node power off")
                                        .withCause(result.getError())))));
                        completion.success(results);
                        return;
                    }
                }

                powerOffMn();
            }

            private void powerOffMn() {
                if (powerOffMnMsgs.isEmpty()) {
                    completion.success(results);
                    return;
                }

                logger.debug("start to power off host which management node local");
                new While<>(powerOffMnMsgs).each((pmsg, compl) -> doPowerOff(pmsg, compl)).run(new WhileDoneCompletion(completion) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        completion.success(results);
                    }
                });
            }
        });
    }

    private void doPowerOff(PowerOffHardwareMsg pmsg, NoErrorCompletion completion) {
        pmsg.setMaxWaitTime(maxWaitTime);
        pmsg.setWaitTaskCompleted(waitTaskCompleted);
        bus.send(pmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply instanceof PowerOffHardwareReply) {
                    results.addAll(((PowerOffHardwareReply) reply).getResults());
                } else {
                    pmsg.getUuids().forEach(it -> results.add(PowerOffHardwareResult.valueOf(it, reply.getError())));
                }
                completion.done();
            }
        });
    }

    private static void ensureMsgsAllHandledByOurselves(List<PowerOffHardwareMsg> msgs) {
        DebugUtils.Assert(msgs.stream().allMatch(msg ->
                Platform.getManagementServerId().equals(CloudBusImpl3.getManagementNodeUUIDFromServiceID(msg.getServiceId()))
                ), "all PowerOffHardwareMsg must handled by MN node which received API message.");
    }

    private static void ensureShutdownOurselvesLast(List<PowerOffHardwareMsg> msgs) {
        msgs.sort(((o1, o2) -> {
            if (o2.powerOffOurself() || !o1.powerOffManagementNode() && o2.powerOffManagementNode()) {
                return -1;
            } else if (o1.powerOffOurself() || o1.powerOffManagementNode() && !o2.powerOffManagementNode()) {
                return 1;
            } else {
                return 0;
            }
        }));
    }

    public static void validate(List<PowerOffHardwareMsg> msgs, List<String> expectUuids) {
        List<String> actualUuids = msgs.stream().map(PowerOffHardwareMsg::getUuids).flatMap(Collection::stream).collect(Collectors.toList());
        DebugUtils.Assert(actualUuids.size() == expectUuids.size() && actualUuids.containsAll(expectUuids),
                "pass wrong power off hardware msgs");
    }
}
