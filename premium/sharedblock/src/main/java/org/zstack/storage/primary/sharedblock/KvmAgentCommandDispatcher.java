package org.zstack.storage.primary.sharedblock;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostErrors;
import org.zstack.header.host.HostInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.PrimaryStorageCapacityUpdaterRunnable;
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.storage.primary.PrimaryStorageCapacityUpdater;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.zstack.core.Platform.operr;
import static org.zstack.storage.primary.sharedblock.SharedBlockKvmBackend.updateCapacity;

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
    @Autowired
    protected SharedBlockGroupPrimaryStorageFactory primaryStorageFactory;

    private final List<String> hostUuids;
    private final List<ErrorCode> errors = new ArrayList<ErrorCode>();
    private final String primaryStorageUuid;

    public KvmAgentCommandDispatcher(String psUuid, String huuid) {
        this.primaryStorageUuid = psUuid;
        hostUuids = new ArrayList<String>();
        if (huuid != null) {
            hostUuids.add(huuid);
        }

        if (hostUuids.isEmpty()) {
            List<HostInventory> hinvs = primaryStorageFactory.getConnectedHostsForOperation(psUuid);
            hinvs.forEach(it -> hostUuids.add(it.getUuid()));
            if (hostUuids.isEmpty()) {
                throw new OperationFailureException(operr("cannot find any connected host to perform the operation, it seems all KVM hosts" +
                                " in the clusters attached with the shared block group storage[uuid:%s] are disconnected",
                        primaryStorageUuid));
            }
        }
    }

    public KvmAgentCommandDispatcher(String psUuid, VolumeInventory volumeInventory) {
        this.primaryStorageUuid = psUuid;
        if (volumeInventory == null){
            throw new OperationFailureException(operr("can not find volume need to operate shared block group " +
                    "primary storage"));
        }

        hostUuids = new ArrayList<>();
        List<HostInventory> hinvs = primaryStorageFactory.getConnectedHostsForOperation(getPrimaryStorageInventory(), volumeInventory);
        hinvs.forEach(it -> hostUuids.add(it.getUuid()));
        if (hostUuids.isEmpty()) {
            throw new OperationFailureException(operr("KVM host which volume[uuid%s] attached disconnected with" +
                    " the shared block group storage[uuid:%s]", volumeInventory.getUuid(), primaryStorageUuid));
        }
    }

    public KvmAgentCommandDispatcher(String psUuid, boolean tmp) {
        this.primaryStorageUuid = psUuid;
        hostUuids = new ArrayList<>();
        List<HostInventory> hinvs = primaryStorageFactory.getConnectedHostsForOperation(psUuid);
        if (hinvs == null || hinvs.isEmpty()) {
            throw new OperationFailureException(operr("can not find qualified kvm host for shared block group " +
                    "primary storage[uuid: %s]", psUuid));
        }
        hinvs.forEach(it -> hostUuids.add(it.getUuid()));
    }

    public KvmAgentCommandDispatcher(String psUuid) {
        this.primaryStorageUuid = psUuid;
        hostUuids = new ArrayList<>();
        List<HostInventory> hinvs = primaryStorageFactory.getConnectedHostsForOperation(getPrimaryStorageInventory(),0,50);
        hinvs.forEach(it -> hostUuids.add(it.getUuid()));
        if (hostUuids.isEmpty()) {
            throw new OperationFailureException(operr("cannot find any connected host to perform the operation, it seems all KVM hosts" +
                            " in the clusters attached with the shared block group storage[uuid:%s] are disconnected",
                    primaryStorageUuid));
        }
    }

    void go(String path, SharedBlockKvmCommands.AgentCmd cmd, ReturnValueCompletion<SharedBlockKvmCommands.AgentRsp> completion) {
        go(path, cmd, SharedBlockKvmCommands.AgentRsp.class, completion);
    }

    <T extends SharedBlockKvmCommands.AgentRsp> void go(String path, SharedBlockKvmCommands.AgentCmd cmd, Class<T> rspType, ReturnValueCompletion<T> completion) {
        doCommand(hostUuids.iterator(), path, cmd, rspType, completion);
    }

    private <T extends SharedBlockKvmCommands.AgentRsp> void doCommand(final Iterator<String> it, final String path, final SharedBlockKvmCommands.AgentCmd cmd, final Class<T> rspType, final ReturnValueCompletion<T> completion) {
        if (!it.hasNext()) {
            completion.fail(errf.stringToOperationError("an operation failed on all hosts", errors));
            return;
        }

        final String hostUuid = it.next();
        httpCall(path, hostUuid, cmd, rspType, new ReturnValueCompletion<T>(completion) {
            @Override
            public void success(T rsp) {
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

    private <T extends SharedBlockKvmCommands.AgentRsp> void httpCall(String path, final String hostUuid, SharedBlockKvmCommands.AgentCmd cmd, final Class<T> rspType, final ReturnValueCompletion<T> completion) {
        httpCall(path, hostUuid, cmd, false, rspType, completion);
    }

    private <T extends SharedBlockKvmCommands.AgentRsp> void httpCall(String path, final String hostUuid, SharedBlockKvmCommands.AgentCmd cmd, boolean noCheckStatus, final Class<T> rspType, final ReturnValueCompletion<T> completion) {
        if (cmd.vgUuid == null) {
            cmd.vgUuid = primaryStorageUuid;
        }

        if (cmd.hostUuid == null) {
            cmd.hostUuid = hostUuid;
        }
        cmd.primaryStorageUuid = cmd.vgUuid;

        SharedBlockKvmBackend.callAgentHook(path, hostUuid, cmd, noCheckStatus);

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setPath(path);
        msg.setNoStatusCheck(noCheckStatus);
        msg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        SharedBlockKvmBackend.markVgInExclusiveLockIfNeed(cmd, path);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                SharedBlockKvmBackend.discardVgInExclusiveLockIfNeed(cmd, path);
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply r = reply.castReply();
                final T rsp = r.toResponse(rspType);
                ErrorCode errorCode = rsp.buildErrorCode();
                if (errorCode != null) {
                    completion.fail(errorCode);
                    return;
                }

                if (rsp.totalCapacity != null && rsp.availableCapacity != null) {
                    new PrimaryStorageCapacityUpdater(cmd.vgUuid).run(new PrimaryStorageCapacityUpdaterRunnable() {
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

                if (rsp.lunCapacities != null) {
                    updateCapacity(rsp);
                }

                completion.success(rsp);
            }
        });
    }

    private PrimaryStorageInventory getPrimaryStorageInventory() {
        DebugUtils.Assert(primaryStorageUuid != null, "primaryStorageUuid cannot be null");
        return PrimaryStorageInventory.valueOf(dbf.findByUuid(primaryStorageUuid, PrimaryStorageVO.class));
    }
}
