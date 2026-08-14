package org.zstack.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.singleflight.ExternalSingleFlightMsg;
import org.zstack.core.singleflight.ExternalSingleFlightReply;
import org.zstack.core.singleflight.MultiNodeSingleFlightImpl;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.core.*;
import org.zstack.header.core.progress.ChainInfo;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.addon.SingleFlightExecutor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

public class CoreManagerImpl extends AbstractService implements CoreManager {
    private static final CLogger logger = Utils.getLogger(CoreManagerImpl.class);

    @Autowired
    private ResourceDestinationMaker destMaker;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private MultiNodeSingleFlightImpl singleFlight;

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIGetChainTaskMsg) {
            handleMessage((APIGetChainTaskMsg) msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof GetLocalTaskMsg) {
            handle((GetLocalTaskMsg) msg);
        } else if (msg instanceof ExternalSingleFlightMsg) {
            handle((ExternalSingleFlightMsg) msg);
        }
    }

    private void handle(ExternalSingleFlightMsg msg) {
        SingleFlightExecutor executor = MultiNodeSingleFlightImpl.getExecutor(msg.getResourceUuid());
        if (executor == null) {
            bus.replyErrorByMessageType(msg, operr("no executor found for resourceUuid[%s]", msg.getResourceUuid()));
            return;
        }

        Object[] args = Arrays.copyOf(msg.getArgs(), msg.getArgs().length + 1);
        args[args.length - 1] = new ReturnValueCompletion<Object>(msg) {
            @Override
            public void success(Object returnValue) {
                ExternalSingleFlightReply reply = new ExternalSingleFlightReply();
                reply.setResult(returnValue);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                bus.replyErrorByMessageType(msg, errorCode);
            }
        };
        singleFlight.run(executor, msg.getMethod(), args);
    }

    private void handleMessage(APIGetChainTaskMsg msg) {
        APIGetChainTaskReply reply = new APIGetChainTaskReply();
        Function<String, String> resourceUuidMaker = msg.getResourceUuidMaker();
        Map<String, List<String>> mnIds;
        if (resourceUuidMaker == null) {
            mnIds = destMaker.getAllNodeInfo().stream().collect(Collectors.toMap(
                    ResourceDestinationMaker.NodeInfo::getNodeUuid, list -> msg.getSyncSignatures()));
        } else {
            mnIds = msg.getSyncSignatures().stream().collect(Collectors.groupingBy(
                    syncSignature -> destMaker.makeDestination(resourceUuidMaker.apply(syncSignature))));
        }

        new While<>(mnIds.entrySet()).all((e, compl) -> {
            GetLocalTaskMsg gmsg = new GetLocalTaskMsg();
            gmsg.setSyncSignatures(e.getValue());
            bus.makeServiceIdByManagementNodeId(gmsg, CoreConstant.SERVICE_ID, e.getKey());
            bus.send(gmsg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply r) {
                    if (r.isSuccess()) {
                        GetLocalTaskReply gr = r.castReply();
                        Map<String, ChainInfo> result = gr.getResults();
                        if (resourceUuidMaker == null) {
                            reply.putAllResults(result);
                        } else {
                            Map<String, ChainInfo> chainInfoMap = result.keySet().stream().collect(
                                    Collectors.toMap(resourceUuidMaker, result::get));
                            reply.putAllResults(chainInfoMap);
                        }
                    } else {
                        logger.error("get task fail, because " + r.getError().getDetails());
                    }

                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(GetLocalTaskMsg msg) {
        GetLocalTaskReply reply = new GetLocalTaskReply();
        Map<String, ChainInfo> results = msg.getSyncSignatures().stream()
                .collect(Collectors.toMap(Function.identity(), thdf::getChainTaskInfo));
        if (msg.isRunningTasksOnly()) {
            results.values().forEach(c -> c.getPendingTask().clear());
        }
        reply.setResults(results);
        bus.reply(msg, reply);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(CoreConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
