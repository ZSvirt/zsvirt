package org.zstack.network.l2.virtualSwitch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.network.l2.virtualSwitch.header.HostKernelInterfaceInventory;
import org.zstack.network.l2.virtualSwitch.header.HostKernelInterfaceUsedIpVO;
import org.zstack.network.l2.virtualSwitch.header.HostKernelInterfaceUsedIpVO_;
import org.zstack.network.l2.virtualSwitch.header.HostKernelInterfaceVO;
import org.zstack.network.l2.virtualSwitch.header.HostKernelInterfaceVO_;
import org.zstack.network.l2.virtualSwitch.header.RefreshHostKernelInterfaceOnHostMsg;
import org.zstack.network.l2.virtualSwitch.header.VirtualSwitchConstant;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class HostKernelInterfaceCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(HostKernelInterfaceCascadeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    private static final String NAME = HostKernelInterfaceVO.class.getSimpleName();

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_CHECK_CODE)) {
            handleDeletionCheck(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE, CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            handleDeletion(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_CLEANUP_CODE)) {
            handleDeletionCleanup(action, completion);
        } else {
            completion.success();
        }
    }

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(
                HostVO.class.getSimpleName(), L3NetworkVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<HostKernelInterfaceInventory> invs = hostKernelInterfaceFromAction(action);
            if (invs != null && !invs.isEmpty()) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(invs);
            }
        }

        return null;
    }


    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        completion.success();
    }

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        completion.success();
    }

    private void handelDeletionCode(final CascadeAction action, List<HostKernelInterfaceInventory> invs, Completion completion) {
        FlowChain chain = new SimpleFlowChain();
        chain.then(new NoRollbackFlow() {
            String __name__ = "delete-host-kernel-interface";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<String> usedIpUuids = Q.New(HostKernelInterfaceUsedIpVO.class).in(HostKernelInterfaceUsedIpVO_.hostKernelInterfaceUuid, invs.stream().map(HostKernelInterfaceInventory::getUuid).collect(Collectors.toList())).select(HostKernelInterfaceUsedIpVO_.uuid).listValues();
                if (usedIpUuids.isEmpty()) {
                    trigger.next();
                    return;
                }

                Map<String, List<String>> hostUuidsMap = new HashMap<>();
                for (HostKernelInterfaceInventory inv : invs) {
                    List<String> l2Uuids = hostUuidsMap.computeIfAbsent(inv.getHostUuid(), k -> new ArrayList<>());
                    if (!l2Uuids.contains(inv.getL2NetworkUuid())) {
                        l2Uuids.add(inv.getL2NetworkUuid());
                    }
                }
                if (hostUuidsMap.isEmpty()) {
                    trigger.next();
                    return;
                }

                List<RefreshHostKernelInterfaceOnHostMsg> rmsgs = new ArrayList<>();
                for (Map.Entry<String, List<String>> e : hostUuidsMap.entrySet()) {
                    RefreshHostKernelInterfaceOnHostMsg msg = new RefreshHostKernelInterfaceOnHostMsg();
                    msg.setHostUuid(e.getKey());
                    msg.setL2NetworkUuids(e.getValue());
                    msg.setDeleteAllInterfaces(true);
                    rmsgs.add(msg);
                }

                new While<>(rmsgs).step((rmsg, wcomp) -> {
                    bus.makeTargetServiceIdByResourceUuid(rmsg, VirtualSwitchConstant.VIRTUAL_SWITCH_SERVICE_ID, rmsg.getHostUuid());
                    bus.send(rmsg, new CloudBusCallBack(wcomp) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(String.format("failed to delete host kernel interface on host[uuid:%s], %s", rmsg.getHostUuid(), reply.getError()));
                            }
                            wcomp.done();
                        }
                    });
                }, 2).run(new WhileDoneCompletion(completion) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        dbf.removeByPrimaryKeys(usedIpUuids, HostKernelInterfaceUsedIpVO.class);
                        trigger.next();
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                dbf.removeByPrimaryKeys(invs.stream().map(HostKernelInterfaceInventory::getUuid).collect(Collectors.toList()), HostKernelInterfaceVO.class);
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void handleDeletion(CascadeAction action, Completion completion) {
        final List<HostKernelInterfaceInventory> invs = hostKernelInterfaceFromAction(action);
        if (invs == null || invs.isEmpty()) {
            completion.success();
            return;
        }

        handelDeletionCode(action, invs, completion);
    }

    private List<HostKernelInterfaceInventory> hostKernelInterfaceFromAction(CascadeAction action) {
        List<HostKernelInterfaceInventory> ret = null;
        if (HostVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<HostInventory> hosts = action.getParentIssuerContext();
            List<String> huuids = hosts.stream().map(HostInventory::getUuid).collect(Collectors.toList());
            if (!huuids.isEmpty()) {
                List<HostKernelInterfaceVO> vos = Q.New(HostKernelInterfaceVO.class).in(HostKernelInterfaceVO_.hostUuid, huuids).list();
                if (!vos.isEmpty()) {
                    ret = HostKernelInterfaceInventory.valueOf1(vos);
                }
            }
        } else if (L3NetworkVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<L3NetworkInventory> l3Invs = action.getParentIssuerContext();
            List<String> l3Uuids = l3Invs.stream().map(L3NetworkInventory::getUuid).collect(Collectors.toList());
            if (!l3Uuids.isEmpty()) {
                List<HostKernelInterfaceVO> vos = Q.New(HostKernelInterfaceVO.class).in(HostKernelInterfaceVO_.l3NetworkUuid, l3Uuids).list();
                if (!vos.isEmpty()) {
                    ret = HostKernelInterfaceInventory.valueOf1(vos);
                }
            }

        } else if (NAME.equals(action.getParentIssuer())) {
            ret = action.getParentIssuerContext();
        }

        return ret;
    }
}
