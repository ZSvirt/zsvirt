package org.zstack.network.l2.virtualSwitch;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.zstack.core.db.SQL;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.DeleteL2NetworkMsg;
import org.zstack.header.network.l2.DetachL2NetworkFromClusterMsg;
import org.zstack.header.network.l2.L2NetworkClusterRefVO;
import org.zstack.header.network.l2.L2NetworkClusterRefVO_;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.network.l2.virtualSwitch.header.L2VirtualSwitchNetworkInventory;
import org.zstack.network.l2.virtualSwitch.header.L2VirtualSwitchNetworkVO;
import org.zstack.network.l2.virtualSwitch.header.VirtualSwitchSystemTags;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class VirtualSwitchCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(VirtualSwitchCascadeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    private static final String NAME = L2VirtualSwitchNetworkVO.class.getSimpleName();

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(ClusterVO.class.getSimpleName());
    }

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
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<L2VirtualSwitchNetworkInventory> invs = virtualSwitchFromAction(action);
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

    private void handleDeletion(CascadeAction action, Completion completion) {
        List<L2VirtualSwitchNetworkInventory> invs = virtualSwitchFromAction(action);
        if (invs == null || invs.isEmpty()) {
            completion.success();
            return;
        }

        List<ClusterInventory> clusters = action.getParentIssuerContext();
        List<String> clusterUuids = clusters.stream().map(ClusterInventory::getUuid).collect(Collectors.toList());

        FlowChain chain = new SimpleFlowChain();
        chain.then(new NoRollbackFlow() {
            String __name__ = String.format("detach-default-virtual-switch-from-cluster");
    
            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<DetachL2NetworkFromClusterMsg> msgs = new ArrayList<>();
                for (L2VirtualSwitchNetworkInventory inv : invs) {
                    clusterUuids.stream().forEach(clusterUuid -> {
                        if (Q.New(L2NetworkClusterRefVO.class).eq(L2NetworkClusterRefVO_.clusterUuid, clusterUuid).isExists()) {
                            DetachL2NetworkFromClusterMsg msg = new DetachL2NetworkFromClusterMsg();
                            msg.setClusterUuid(clusterUuid);
                            msg.setL2NetworkUuid(inv.getUuid());
                            bus.makeTargetServiceIdByResourceUuid(msg, L2NetworkConstant.SERVICE_ID, inv.getUuid());
                            msgs.add(msg);
                        }
                    });
                }

                new While<>(msgs).step((msg, wcomp) -> {
                    bus.send(msg, new CloudBusCallBack(wcomp) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(String.format("failed to detach default virtual switch[uuid:%s] from cluster[%s], error: %s", msg.getL2NetworkUuid(), msg.getClusterUuid(), reply.getError()));
                            }
                            wcomp.done();
                        }
                    });
                }, 1).run(new WhileDoneCompletion(completion) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = String.format("delete-default-virtual-switch");

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<DeleteL2NetworkMsg> msgs = new ArrayList<>();
                for (L2VirtualSwitchNetworkInventory inv : invs) {
                    boolean isExist = Q.New(L2NetworkClusterRefVO.class).eq(L2NetworkClusterRefVO_.l2NetworkUuid, inv.getUuid()).isExists();
                    if (!isExist) {
                        DeleteL2NetworkMsg msg = new DeleteL2NetworkMsg();
                        msg.setUuid(inv.getUuid());
                        msg.setForceDelete(true);
                        bus.makeTargetServiceIdByResourceUuid(msg, L2NetworkConstant.SERVICE_ID, inv.getUuid());
                        msgs.add(msg);
                    }
                }

                new While<>(msgs).step((msg, wcomp) -> {
                    bus.send(msg, new CloudBusCallBack(wcomp) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(String.format("failed to delete default virtual switch[uuid:%s], %s", msg.getUuid(), reply.getError()));
                            }
                            wcomp.done();
                        }
                    });
                }, 1).run(new WhileDoneCompletion(completion) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.next();
                    }
                });
            }
            
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private List<L2VirtualSwitchNetworkInventory> virtualSwitchFromAction(CascadeAction action) {
        List<L2VirtualSwitchNetworkInventory> ret = null;

        if (ClusterVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<ClusterInventory> clusters = action.getParentIssuerContext();
            List<String> clusterUuids = clusters.stream().map(ClusterInventory::getUuid).collect(Collectors.toList());
            if (!clusterUuids.isEmpty()) {
                List<L2VirtualSwitchNetworkVO> vsvos = SQL.New("select distinct vs from L2VirtualSwitchNetworkVO vs, L2NetworkClusterRefVO ref, SystemTagVO tag" +
                                " where vs.uuid = ref.l2NetworkUuid" +
                                " and ref.clusterUuid in (:clusterUuids)" +
                                " and tag.resourceUuid = vs.uuid" +
                                " and tag.tag = :tag", L2VirtualSwitchNetworkVO.class)
                                .param("clusterUuids", clusterUuids)
                                .param("tag", VirtualSwitchSystemTags.VIRTUAL_SWITCH_DEFAULT.getTagFormat())
                                .list();
                if (!vsvos.isEmpty()) {
                    ret = L2VirtualSwitchNetworkInventory.valueOf1(vsvos);
                }
            }
        } else if (NAME.equals(action.getParentIssuer())) {
            ret = action.getParentIssuerContext();
        }

        return ret;
    }
}
