package org.zstack.storage.ceph.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostConnectionReestablishExtensionPoint;
import org.zstack.header.host.HostException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostConnectExtensionPoint;
import org.zstack.kvm.KVMHostConnectedContext;
import org.zstack.kvm.KVMHostFactory;
import org.zstack.storage.primary.CheckHostStorageConnectionMsg;
import org.zstack.storage.ceph.CephConstants;

import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.transform;

/**
 * Created by frank on 8/17/2015.
 */
public class CephKvmExtension implements KVMHostConnectExtensionPoint, HostConnectionReestablishExtensionPoint {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void connectionReestablished(HostInventory inv) throws HostException {
        if (!KVMConstant.KVM_HYPERVISOR_TYPE.equals(inv.getHypervisorType())) {
            return;
        }

        FutureCompletion completion = new FutureCompletion(null);
        createSecret(inv.getUuid(), inv.getClusterUuid(), new Completion(completion) {
            @Override
            public void success() {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });

        completion.await();

        if (!completion.isSuccess()) {
            throw new OperationFailureException(completion.getErrorCode());
        }
    }

    @Override
    public HypervisorType getHypervisorTypeForReestablishExtensionPoint() {
        return KVMHostFactory.hypervisorType;
    }

    @Transactional(readOnly = true)
    private List<String> findCephPrimaryStorage(String clusterUuid) {
        String sql = "select pri.uuid from PrimaryStorageVO pri, PrimaryStorageClusterRefVO ref where" +
                " ref.clusterUuid = :cuuid and pri.uuid = ref.primaryStorageUuid and pri.type = :ptype";
        TypedQuery<String>  q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("cuuid", clusterUuid);
        q.setParameter("ptype", CephConstants.CEPH_PRIMARY_STORAGE_TYPE);
        return q.getResultList();
    }

    private void createSecret(final String hostUuid, String clusterUuid, Completion completion) {
        List<String> psUuids = findCephPrimaryStorage(clusterUuid);
        if (psUuids.isEmpty()) {
            completion.success();
            return;
        }


        List<CreateKvmSecretMsg> msgs = transform(psUuids, puuid -> {
            CreateKvmSecretMsg msg = new CreateKvmSecretMsg();
            msg.setPrimaryStorageUuid(puuid);
            msg.setHostUuids(list(hostUuid));
            bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, puuid);
            return msg;
        });

        new While<>(msgs).all((msg, whileCompletion) -> {
            bus.send(msg, new CloudBusCallBack(whileCompletion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        whileCompletion.addError(reply.getError());
                    }

                    whileCompletion.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    completion.fail(errorCodeList.getCauses().get(0));
                } else {
                    completion.success();
                }
            }
        });
    }

    @Override
    public Flow createKvmHostConnectingFlow(final KVMHostConnectedContext context) {
        return new NoRollbackFlow() {
            String __name__ = "prepare-ceph-primary-storage";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                doPrepareCephPrimaryStorage(context, new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        };
    }

    private void checkHostStorageConnection(final String hostUuid, String clusterUuid, Completion completion) {
        List<String> psUuids = findCephPrimaryStorage(clusterUuid);
        if (psUuids.isEmpty()) {
            completion.success();
            return;
        }

        List<CheckHostStorageConnectionMsg> msgs = transform(psUuids, puuid -> {
            CheckHostStorageConnectionMsg msg = new CheckHostStorageConnectionMsg();
            msg.setPrimaryStorageUuid(puuid);
            msg.setHostUuids(list(hostUuid));
            bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, puuid);
            return msg;
        });

        new While<>(msgs).step((msg, whileCompletion) -> {
            bus.send(msg, new CloudBusCallBack(whileCompletion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        whileCompletion.addError(reply.getError());
                    }

                    whileCompletion.done();
                }
            });
        }, 10).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    completion.fail(errorCodeList.getCauses().get(0));
                } else {
                    completion.success();
                }
            }
        });
    }

    private void doPrepareCephPrimaryStorage(final KVMHostConnectedContext context, Completion completion) {
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName("do-prepare-ceph-primary-storage");
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                checkHostStorageConnection(context.getInventory().getUuid(), context.getInventory().getClusterUuid(), new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                createSecret(context.getInventory().getUuid(), context.getInventory().getClusterUuid(), new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).start();
    }
}
