package org.zstack.network.service.lb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.acl.AccessControlListInventory;
import org.zstack.header.acl.AccessControlListVO;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.MessageReply;
import org.zstack.identity.ResourceHelper;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionUtils.transform;
import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 */
public class LoadBalancerCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(LoadBalancerCascadeExtension.class);
    private static final String NAME = LoadBalancerVO.class.getSimpleName();

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

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

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        dbf.eoCleanup(LoadBalancerVO.class);
        completion.success();
    }

    private void handleDeletion(CascadeAction action, final Completion completion) {
        if (AccessControlListVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<AccessControlListInventory> acls = (List<AccessControlListInventory>) action.getParentIssuerContext();
            AccessControlListInventory acl = acls.get(0);

            List<LoadBalancerListenerACLRefVO> refVOS = Q.New(LoadBalancerListenerACLRefVO.class).eq(LoadBalancerListenerACLRefVO_.aclUuid, acl.getUuid()).list();
            if (refVOS.isEmpty()) {
                completion.success();
                return;
            }

            LoadBalancerListenerVO listenerVO = Q.New(LoadBalancerListenerVO.class).eq(LoadBalancerListenerVO_.uuid, refVOS.get(0).getListenerUuid()).find();
            if (listenerVO.getLoadBalancerUuid() == null) {
                completion.success();
                return;
            }

            RemoveAccessControlListFromLoadBalancerMsg msg = new RemoveAccessControlListFromLoadBalancerMsg();
            msg.setLoadBalancerUuid(listenerVO.getLoadBalancerUuid());
            msg.setAclUuids(Collections.singletonList(acl.getUuid()));
            msg.setServerGroupUuids(refVOS.stream().filter(ref -> ref.getServerGroupUuid() != null).map(LoadBalancerListenerACLRefVO::getServerGroupUuid).collect(Collectors.toList()));
            msg.setListenerUuid(listenerVO.getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, LoadBalancerConstants.SERVICE_ID, listenerVO.getLoadBalancerUuid());

            bus.send(msg, new CloudBusCallBack(msg) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        completion.fail(reply.getError());
                        return;
                    }
                    completion.success();
                }
            });

            return;
        }


        FlowChain chain = new SimpleFlowChain();
        chain.setName("delete-lb-and-lb-certs");
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                final List<LoadBalancerInventory> lbs = loadBalancerFromAction(action);
                if (lbs == null || lbs.isEmpty()) {
                    trigger.next();
                    return;
                }

                List<DeleteLoadBalancerMsg> msgs = transform(lbs, arg -> {
                    DeleteLoadBalancerMsg msg = new DeleteLoadBalancerMsg();
                    msg.setUuid(arg.getUuid());
                    bus.makeTargetServiceIdByResourceUuid(msg, LoadBalancerConstants.SERVICE_ID, msg.getUuid());
                    return msg;
                });

                List<ErrorCode> erros = new ArrayList<>();
                new While<>(msgs).all((msg, whileCompletion) -> {
                    bus.send(msg, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(String.format("failed to delete loadBalancer[uuid:%s], %s",
                                        msg.getUuid(), reply.getError()));
                                erros.add(reply.getError());
                            }

                            whileCompletion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(completion) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!erros.isEmpty()) {
                            completion.fail(erros.get(0));
                        }

                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                final List<CertificateInventory> certs = certificateFromAction(action);
                if (certs == null || certs.isEmpty()) {
                    trigger.next();
                    return;
                }

                List<CertificateDeletionMsg> msgs = transform(certs, arg -> {
                    CertificateDeletionMsg msg = new CertificateDeletionMsg();
                    msg.setUuid(arg.getUuid());
                    bus.makeLocalServiceId(msg, LoadBalancerConstants.SERVICE_ID);
                    return msg;
                });

                new While<>(msgs).all((msg, whileCompletion) -> {
                    bus.send(msg, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(String.format("failed to delete loadBalancer[uuid:%s], %s",
                                        msg.getUuid(), reply.getError()));
                            }

                            whileCompletion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
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

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        completion.success();
    }

    @Override
    public List<String> getEdgeNames() {
        List<String> ret = new ArrayList<>();
        ret.add(VipVO.class.getSimpleName());
        ret.add(AccessControlListVO.class.getSimpleName());
        return ret;
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<LoadBalancerInventory> lbs = loadBalancerFromAction(action);
            if (lbs != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(lbs);
            }
        }
        return null;
    }

    private List<CertificateInventory> certificateFromAction(CascadeAction action) {
        if (!AccountVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return null;
        }

        final List<String> auuids = transform(action.getParentIssuerContext(), AccountInventory::getUuid);

        List<CertificateVO> vos = ResourceHelper.findOwnResources(CertificateVO.class, auuids);
        if (!vos.isEmpty()) {
            return CertificateInventory.valueOf(vos);
        }

        return null;
    }

    private List<LoadBalancerInventory> loadBalancerFromAction(CascadeAction action) {
        if (LoadBalancerVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return action.getParentIssuerContext();
        } else if (AccountVO.class.getSimpleName().equals(action.getParentIssuer())) {
            final List<String> auuids = transform(action.getParentIssuerContext(), AccountInventory::getUuid);

            List<LoadBalancerVO> vos = ResourceHelper.findOwnResources(LoadBalancerVO.class, auuids);
            if (!vos.isEmpty()) {
                return LoadBalancerInventory.valueOf(vos);
            }
        } else if (VipVO.class.getSimpleName().equals(action.getParentIssuer())) {
            final List<String> vipUuids = transformAndRemoveNull(action.getParentIssuerContext(), VipInventory::getUuid);

            if (vipUuids.isEmpty()) {
                return null;
            }

            List<LoadBalancerVO> vos = new Callable<List<LoadBalancerVO>>() {
                @Override
                @Transactional(readOnly = true)
                public List<LoadBalancerVO> call() {
                    String sql = "select d from LoadBalancerVO d where d.vipUuid in (:vipUuids)";
                    TypedQuery<LoadBalancerVO> q = dbf.getEntityManager().createQuery(sql, LoadBalancerVO.class);
                    q.setParameter("vipUuids", vipUuids);
                    return q.getResultList();
                }
            }.call();

            if (!vos.isEmpty()) {
                return LoadBalancerInventory.valueOf(vos);
            }
        }

        return null;
    }
}
