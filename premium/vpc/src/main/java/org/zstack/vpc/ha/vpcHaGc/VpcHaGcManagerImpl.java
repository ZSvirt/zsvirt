package org.zstack.vpc.ha.vpcHaGc;


import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.appliancevm.ApplianceVmStatus;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.db.Q;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.network.service.VirtualRouterHaCallbackInterface;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO_;
import org.zstack.network.service.virtualrouter.ha.VirtualRouterHaBackend;
import org.zstack.vpc.ha.VirtualRouterPingFailureTracker;

import javax.persistence.Tuple;

import static org.zstack.core.Platform.operr;

public class VpcHaGcManagerImpl implements Component, VpcHaGcManager {

    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    VirtualRouterHaBackend haBackend;
    @Autowired
    VirtualRouterPingFailureTracker failureTracker;

    @Override
    public void submitGc(VpcHaGcStruct struct, Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return String.format("replay-vpc-%s", struct.getVmInstanceUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                if (!CoreGlobalProperty.UNIT_TEST_ON && failureTracker.inTracking(struct.getVmInstanceUuid())) {
                    completion.success();
                    chain.next();
                    return;
                }

                doHaBackupRouterGcJob(struct, new Completion(completion, chain) {
                    @Override
                    public void success() {
                        completion.success();
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        failureTracker.notifyFailure(struct.getVmInstanceUuid());
                        completion.success();
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void doHaBackupRouterGcJob(VpcHaGcStruct struct, Completion completion) {
        /* if virtual router is not running or disconnected, skip the gc job */
        final Tuple t = Q.New(VirtualRouterVmVO.class)
                .eq(VirtualRouterVmVO_.uuid, struct.getVmInstanceUuid())
                .select(VirtualRouterVmVO_.state, VirtualRouterVmVO_.status)
                .findTuple();
        if (t == null) {
            completion.success();
            return;
        }

        if (t.get(0, VmInstanceState.class) != VmInstanceState.Running) {
            completion.fail(operr("VR[uuid: %s] not running", struct.getVmInstanceUuid()));
            return;
        }

        if (t.get(1, ApplianceVmStatus.class) != ApplianceVmStatus.Connected) {
            completion.fail(operr("VR[uuid: %s] not connected", struct.getVmInstanceUuid()));
            return;
        }

        VirtualRouterHaCallbackInterface callback = haBackend.getCallback(struct.getTaskName());
        callback.callBack(struct.getVmInstanceUuid(), struct.getTaskData(), new Completion(completion) {
            @Override
            public void success() {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
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
