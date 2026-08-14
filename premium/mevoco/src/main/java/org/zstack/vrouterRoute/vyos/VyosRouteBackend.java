package org.zstack.vrouterRoute.vyos;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.service.VirtualRouterHaCallbackInterface;
import org.zstack.header.network.service.VirtualRouterHaCallbackStruct;
import org.zstack.header.network.service.VirtualRouterHaGetCallbackExtensionPoint;
import org.zstack.header.network.service.VirtualRouterHaTask;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.network.service.virtualrouter.AbstractVirtualRouterBackend;
import org.zstack.network.service.virtualrouter.VirtualRouterAsyncHttpCallMsg;
import org.zstack.network.service.virtualrouter.VirtualRouterAsyncHttpCallReply;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands;
import org.zstack.network.service.virtualrouter.ha.VirtualRouterHaBackend;
import org.zstack.network.service.virtualrouter.vyos.*;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.vrouterRoute.VRouterRouteBackend;
import org.zstack.vrouterRoute.VRouterRouteEntryTo;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.core.Platform.operr;

/**
 * Created by weiwang on 19/06/2017.
 */
public class VyosRouteBackend extends AbstractVirtualRouterBackend implements VRouterRouteBackend,
        VyosPostCreateFlowExtensionPoint, VyosPostRebootFlowExtensionPoint, VyosPostStartFlowExtensionPoint,
        VyosProvisionConfigFlowExtensionPoint, VirtualRouterHaGetCallbackExtensionPoint {

    private static final CLogger logger = Utils.getLogger(VyosRouteBackend.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    protected RESTFacade restf;
    @Autowired
    protected VirtualRouterHaBackend haBackend;

    public static final String SYNC_ROUTES = "/syncroutes";
    public static final String GET_ROUTES = "/getroutes";

    public static final String SYNC_ROUTER_TASK = "syncRoute";

    public static class SyncRoutesCmd extends VirtualRouterCommands.AgentCommand {
        public List<VRouterRouteEntryTo> routes;
    }

    public static class SyncRoutesRsp extends VirtualRouterCommands.AgentResponse {
    }

    public static class GetRoutesCmd extends VirtualRouterCommands.AgentCommand {
    }

    public static class GetRoutesRsp extends VirtualRouterCommands.AgentResponse {
        public String rawRoutes;
    }

    private Flow createResyncFlow() {
        return new VyosResyncRoutesFlow();
    }

    @Override
    public Flow vyosPostStartFlow() {
        return createResyncFlow();
    }

    @Override
    public Flow vyosPostCreateFlow() {
        return createResyncFlow();
    }

    @Override
    public Flow vyosPostRebootFlow() {
        return createResyncFlow();
    }

    @Override
    public Flow vyosProvisionConfigFlow() {
        return createResyncFlow();
    }

    private void syncRouteEntriesToVirtualRouter(String vrUuid, SyncRoutesCmd cmd, Completion completion) {
        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setVmInstanceUuid(vrUuid);
        msg.setPath(SYNC_ROUTES);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vrUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    throw new OperationFailureException(reply.getError());
                }

                VirtualRouterAsyncHttpCallReply ar = reply.castReply();
                VyosRouteBackend.SyncRoutesRsp rsp = ar.toResponse(VyosRouteBackend.SyncRoutesRsp.class);
                if (!rsp.isSuccess()) {
                    throw new OperationFailureException(operr("operation error, because:%s", rsp.getError()));
                }

                completion.success();
            }
        });
    }

    private void submitsyncRouteEntriesToHaRouter(String vrUuid, SyncRoutesCmd cmd, Completion completion) {
        VirtualRouterHaTask task = new VirtualRouterHaTask();
        task.setTaskName(SYNC_ROUTER_TASK);
        task.setOriginRouterUuid(vrUuid);
        task.setJsonData(JSONObjectUtil.toJsonString(cmd));
        haBackend.submitVirtualRouterHaTask(task, completion);
    }

    @Override
    public void syncRouteEntries(List<VRouterRouteEntryTo> tos, List<String> vrouterUuids, boolean syncToHaRouter) {
        SyncRoutesCmd cmd = new SyncRoutesCmd();
        cmd.routes = tos;

        ErrorCodeList errList = new ErrorCodeList();

        new While<>(vrouterUuids).step((uuid, completion) -> {
            syncRouteEntriesToVirtualRouter(uuid, cmd, new Completion(completion) {
                @Override
                public void success() {
                    if (!syncToHaRouter) {
                        completion.done();
                        return;
                    }

                    submitsyncRouteEntriesToHaRouter(uuid, cmd, new Completion(completion) {
                        @Override
                        public void success() {
                            completion.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            errList.getCauses().add(errorCode);
                            completion.done();
                        }
                    });
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    errList.getCauses().add(errorCode);
                    completion.done();
                }
            });
        }, 5).run(new WhileDoneCompletion(null) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errList.getCauses().isEmpty()) {
                    logger.error(String.format("failed to sync routes to vrs[%s] for %s", vrouterUuids, errList.getCauses().get(0)));
                    return;
                }
                logger.info(String.format("succeeded to sync routes to vrouter [uuid: %s]", vrouterUuids));
            }
        });

    }

    @Override
    public void getVRouterRouteTable(String vrouterUuid, ReturnValueCompletion<String> completion) {
        GetRoutesCmd cmd = new GetRoutesCmd();
        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setVmInstanceUuid(vrouterUuid);
        msg.setPath(GET_ROUTES);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vrouterUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    throw new OperationFailureException(reply.getError());
                }

                VirtualRouterAsyncHttpCallReply ar = reply.castReply();
                VyosRouteBackend.GetRoutesRsp rsp = ar.toResponse(VyosRouteBackend.GetRoutesRsp.class);
                if (!rsp.isSuccess()) {
                    throw new OperationFailureException(operr("operation error, because:%s", rsp.getError()));
                }
                completion.success(rsp.rawRoutes);
            }
        });
    }

    @Override
    public String getNetworkServiceProviderType() {
        return org.zstack.network.service.virtualrouter.vyos.VyosConstants.VYOS_ROUTER_PROVIDER_TYPE;
    }

    @Override
    public List<VirtualRouterHaCallbackStruct> getCallback() {
        List<VirtualRouterHaCallbackStruct> structs = new ArrayList<>();

        VirtualRouterHaCallbackStruct syncRoute = new VirtualRouterHaCallbackStruct();
        syncRoute.type = SYNC_ROUTER_TASK;
        syncRoute.callback = new VirtualRouterHaCallbackInterface() {
            @Override
            public void callBack(String vrUuid, VirtualRouterHaTask task, Completion completion) {
                SyncRoutesCmd cmd = JSONObjectUtil.toObject(task.getJsonData(), SyncRoutesCmd.class);
                syncRouteEntriesToVirtualRouter(vrUuid, cmd, completion);
            }
        };
        structs.add(syncRoute);

        return structs;
    }
}
