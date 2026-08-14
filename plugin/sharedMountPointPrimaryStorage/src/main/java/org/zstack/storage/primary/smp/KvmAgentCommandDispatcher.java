package org.zstack.storage.primary.smp;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostErrors;
import org.zstack.header.host.HostStatus;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.PrimaryStorageCapacityUpdaterRunnable;
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.storage.primary.PrimaryStorageCapacityUpdater;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by david on 7/22/16.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE, dependencyCheck = true)
public class KvmAgentCommandDispatcher {
    private static final CLogger logger = Utils.getLogger(KvmAgentCommandDispatcher.class);

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected ApiTimeoutManager timeoutManager;

    private List<String> hostUuids;
    private List<ErrorCode> errors = new ArrayList<ErrorCode>();
    private String primaryStorageUuid;

    public KvmAgentCommandDispatcher(String psUuid, String hostUuid) {
        hostUuids = new ArrayList<String>();
        hostUuids.add(hostUuid);
        this.primaryStorageUuid = psUuid;
    }

    public KvmAgentCommandDispatcher(String psUuid) {
        this.primaryStorageUuid = psUuid;
        hostUuids = findConnectedHosts(50);
        if (hostUuids.isEmpty()) {
            throw new OperationFailureException(operr("cannot find any connected host to perform the operation, it seems all KVM hosts" +
                            " in the clusters attached with the shared mount point storage[uuid:%s] are disconnected",
                    this.primaryStorageUuid));
        }
    }

    private <T extends KvmBackend.AgentRsp> void httpCall(String path, final String hostUuid, KvmBackend.AgentCmd cmd, final Class<T> rspType, final ReturnValueCompletion<T> completion) {
        httpCall(path, hostUuid, cmd, false, rspType, completion);
    }

    private <T extends KvmBackend.AgentRsp> void httpCall(String path, final String hostUuid, KvmBackend.AgentCmd cmd, boolean noCheckStatus, final Class<T> rspType, final ReturnValueCompletion<T> completion) {
        cmd.mountPoint = Q.New(PrimaryStorageVO.class).
                select(PrimaryStorageVO_.mountPath).
                eq(PrimaryStorageVO_.uuid, this.primaryStorageUuid).
                findValue();

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setPath(path);
        msg.setNoStatusCheck(noCheckStatus);
        msg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply r = reply.castReply();
                final T rsp = r.toResponse(rspType);
                if (!rsp.success) {
                    completion.fail(operr("operation error, because:%s", rsp.error));
                    return;
                }

                if (rsp.totalCapacity != null && rsp.availableCapacity != null) {
                    new PrimaryStorageCapacityUpdater(primaryStorageUuid).run(new PrimaryStorageCapacityUpdaterRunnable() {
                        @Override
                        public PrimaryStorageCapacityVO call(PrimaryStorageCapacityVO cap) {
                            if (cap.getTotalCapacity() == 0 || cap.getAvailableCapacity() == 0) {
                                cap.setAvailableCapacity(rsp.availableCapacity);
                            }

                            cap.setTotalCapacity(rsp.totalCapacity);
                            cap.setTotalPhysicalCapacity(rsp.totalCapacity);
                            cap.setAvailablePhysicalCapacity(rsp.availableCapacity);

                            return cap;
                        }
                    });
                }

                completion.success(rsp);
            }
        });
    }

    @Transactional(readOnly = true)
    private List<String> findConnectedHosts(int num) {
        String sql = "select h.uuid from HostVO h, PrimaryStorageClusterRefVO ref where ref.clusterUuid = h.clusterUuid and" +
                " ref.primaryStorageUuid = :psUuid and h.status = :status and h.hypervisorType = :htype";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("psUuid", this.primaryStorageUuid);
        q.setParameter("status", HostStatus.Connected);
        q.setParameter("htype", KVMConstant.KVM_HYPERVISOR_TYPE);
        q.setMaxResults(num);
        List<String> hostUuids = q.getResultList();
        Collections.shuffle(hostUuids);
        return hostUuids;
    }

    public <T extends KvmBackend.AgentRsp> void go(String path, KvmBackend.AgentCmd cmd, Class<T> rspType, ReturnValueCompletion<T> completion) {
        doCommand(hostUuids.iterator(), path, cmd, rspType, completion);
    }

    private <T extends  KvmBackend.AgentRsp> void doCommand(final Iterator<String> it, final String path, final KvmBackend.AgentCmd cmd, final Class<T> rspType, final ReturnValueCompletion<T> completion) {
        if (!it.hasNext()) {
            completion.fail(errf.stringToOperationError("an operation failed on all hosts", errors));
            return;
        }

        final String hostUuid = it.next();
        httpCall(path, hostUuid, cmd, rspType, new ReturnValueCompletion<T>(completion) {
            @Override
            public void success(final T rsp) {
                completion.success(rsp);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                if (!errorCode.isError(HostErrors.OPERATION_FAILURE_GC_ELIGIBLE)) {
                    completion.fail(errorCode);
                    return;
                }

                errors.add(errorCode);
                logger.warn(String.format("failed to do the command[%s] on the kvm host[uuid:%s], %s, try next one",
                        cmd.getClass(), hostUuid, errorCode));
                doCommand(it, path, cmd, rspType, completion);
            }
        });
    }
}
