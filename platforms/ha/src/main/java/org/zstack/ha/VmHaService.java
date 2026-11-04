package org.zstack.ha;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.ha.entity.HaCommands;
import org.zstack.ha.entity.VmHaVO;
import org.zstack.ha.entity.VmHaVO_;
import org.zstack.ha.gc.HaGcManager;
import org.zstack.ha.header.VmHaLevel;
import org.zstack.ha.spi.HaHypervisorFactory;
import org.zstack.ha.spi.HaHypervisorWorker;
import org.zstack.ha.spi.VmHaVOUpdater;
import org.zstack.ha.tracker.VmHaTaskTracker;
import org.zstack.ha.vmboot.HaStartVmJudgers;
import org.zstack.ha.vmboot.VmHaStartMessageSender;
import org.zstack.header.core.Completion;
import org.zstack.header.core.HaCompletion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.host.CheckVmStateOnHypervisorMsg;
import org.zstack.header.host.CheckVmStateOnHypervisorReply;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmHaExtensionPoint;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.zstack.core.Platform.i18n;

public class VmHaService {
    private static final CLogger logger = Utils.getLogger(VmHaService.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRegistry;
    @Autowired
    private ThreadFacade threadFacade;
    @Autowired
    private VmHaVOUpdater vmHAExecutor;
    @Autowired
    private HaGcManager haGcManager;

    static class SetupVmTrackerAndCheckContext {
        boolean checkSuccess;
        String state;

        // write to task.info
        String vmInfo;
        boolean needToStartVm;
    }

    /**
     * This method will update: {@link VmInstanceVO#state}
     * @param vmUuid
     * @param hostUuid
     */
    public void setupHaTrackerForNeverStopVm(String vmUuid, String hostUuid) {
        VmHaTaskTracker tracker = new VmHaTaskTracker(vmUuid);
        tracker.track(VmHaTaskTracker.Process.checking, i18n("vm stopped unexpectedly, double check state"));

        SetupVmTrackerAndCheckContext context = new SetupVmTrackerAndCheckContext();

        SimpleFlowChain.of("setup-vm-ha-tracker-and-check")
        .then(Flow.of("check-vm-state-on-hypervisor")
            .handle(trigger -> {
                CheckVmStateOnHypervisorMsg msg = new CheckVmStateOnHypervisorMsg();
                msg.setVmInstanceUuids(Collections.singletonList(vmUuid));
                msg.setHostUuid(hostUuid);
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            CheckVmStateOnHypervisorReply r = reply.castReply();
                            context.state = r.getStates().get(vmUuid);
                            context.checkSuccess = true;
                        }
                        trigger.next();
                    }
                });
            })
            .build())
        .then(Flow.of("force-start-tracker-if-state-check-failed")
            .skipIf(data -> context.checkSuccess)
            .handle(trigger -> {
                String info = i18n("cannot determine VM[%s] status on host[%s], try to start it", vmUuid, hostUuid);
                logger.warn(String.format("start HA tracker for VM[%s]: %s", vmUuid, info));

                tracker.track(VmHaTaskTracker.Process.starting, info);
                context.needToStartVm = true;
                trigger.next();
            })
            .build())
        .then(Flow.of("if-vm-stopped-on-hypervisor")
            .runIf(data -> Objects.equals(context.state, VmInstanceState.Stopped.toString()))
            .handle(trigger -> {
                SQL.New(VmInstanceVO.class)
                        .eq(VmInstanceVO_.uuid, vmUuid)
                        .set(VmInstanceVO_.state, VmInstanceState.Stopped)
                        .set(VmInstanceVO_.hostUuid, null)
                        .set(VmInstanceVO_.lastHostUuid, hostUuid)
                        .update();

                String info = vmHAExecutor.findVmFencedReason(vmUuid);
                context.vmInfo = info == null ? i18n("vm state is stopped, try to start it") : info;
                tracker.track(VmHaTaskTracker.Process.starting, context.vmInfo);
                logger.debug(String.format("start HA tracker for stopped VM[%s]: %s", vmUuid, context.vmInfo));
                context.needToStartVm = true;
                trigger.next();
            })
            .build())
        .then(Flow.of("if-vm-running-on-hypervisor")
            .runIf(data -> Objects.equals(context.state, VmInstanceState.Running.toString()))
            .handle(trigger -> {
                context.vmInfo = i18n("VM[%s] is running on host[%s]", vmUuid, hostUuid);
                logger.warn(context.vmInfo);
                tracker.track(VmHaTaskTracker.Process.noNeed, context.vmInfo);

                SQL.New(VmInstanceVO.class)
                        .eq(VmInstanceVO_.uuid, vmUuid)
                        .set(VmInstanceVO_.hostUuid, hostUuid)
                        .set(VmInstanceVO_.state, VmInstanceState.Running)
                        .update();
                trigger.next();
            })
            .build())
        .then(Flow.of("if-vm-paused-on-hypervisor")
            .runIf(data -> Objects.equals(context.state, VmInstanceState.Paused.toString()))
            .handle(trigger -> {
                String info = i18n("VM[%s] is paused on host[%s]", vmUuid, hostUuid);
                logger.warn(context.vmInfo);
                tracker.track(VmHaTaskTracker.Process.noNeed, context.vmInfo);

                SQL.New(VmInstanceVO.class)
                        .eq(VmInstanceVO_.uuid, vmUuid)
                        .set(VmInstanceVO_.hostUuid, hostUuid)
                        .set(VmInstanceVO_.state, VmInstanceState.Paused)
                        .update();
                trigger.next();
            })
            .build())
        .propagateExceptionTo(new NopeCompletion())
        .done(() -> {
            if (context.needToStartVm) {
                haStartVmWithTracker(vmUuid, hostUuid, context.vmInfo, tracker);
            }
        })
        .error(errorCode -> {
            logger.warn(String.format("failed to setup VM HA Tracker for VM[%s] on host[%s]: %s",
                    vmUuid, hostUuid, errorCode.getReadableDetails()));
        })
        .start();
    }

    public void setupHaTrackerForOnHostFailureVm(String vmUuid, final boolean retryOnFailure) {
        VmInstanceVO vm = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .find();
        setupHaTrackerForOnHostFailureVm(VmInstanceInventory.valueOf(vm), retryOnFailure);
    }

    public void setupHaTrackerForOnHostFailureVm(VmInstanceInventory vm, final boolean retryOnFailure) {
        VmHaTaskTracker tracker = new VmHaTaskTracker(vm.getUuid());
        final HaHypervisorFactory f = findHypervisorFactory(vm.getHypervisorType());
        if (f == null) {
            tracker.track(VmHaTaskTracker.Process.noNeed, i18n("the hypervisor[%s] does not support VM HA", vm.getHypervisorType()));
            return;
        }

        threadFacade.chainSubmit(new ChainTask(null) {
            @Override
            public String getSyncSignature() {
                return String.format("ha-vm-%s", vm.getUuid());
            }

            @Override
            protected int getMaxPendingTasks() {
                return 1;
            }

            @Override
            protected String getDeduplicateString() {
                return getSyncSignature();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                HaHypervisorWorker worker = f.createHaWorker(vm);
                worker.start(new HaCompletion(chain) {
                    @Override
                    public void noNeed(String details) {
                        logger.debug(details);
                        tracker.track(VmHaTaskTracker.Process.noNeed, details);
                        chain.next();
                    }

                    @Override
                    public void success() {
                        tracker.track(VmHaTaskTracker.Process.success, i18n("HA is successfully completed"));
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.debug(String.format("failed to perform HA to the vm[name:%s, uuid:%s], %s", vm.getName(), vm.getUuid(), errorCode));
                        tracker.error(errorCode);
                        tracker.track(VmHaTaskTracker.Process.failure, i18n("Failed to HA the VM"));
                        if (retryOnFailure) {
                            haGcManager.submitNeverStopVmGCJob(vm.getUuid());
                        }
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

    /**
     * doStartVm
     */
    public void haStartVmWithTracker(String vmUuid, String hostUuid, String reason, VmHaTaskTracker tracker) {
        if (!HaHelper.isSelfFencerDone(vmUuid, hostUuid)) {
            return;
        }
        for (VmHaExtensionPoint ext : pluginRegistry.getExtensionList(VmHaExtensionPoint.class)) {
            ErrorCode errorCode = ext.preHaStartVm(vmUuid);
            if (errorCode != null) {
                logger.warn(String.format("can not start HA for vm[%s]: %s", vmUuid, errorCode.getReadableDetails()));

                if (tracker != null) {
                    tracker.track(VmHaTaskTracker.Process.failure, errorCode.getDetails());
                }
                return;
            }
        }

        VmHaStartMessageSender sender = new VmHaStartMessageSender()
                .withVmInstance(vmUuid)
                .withAvoidHost(hostUuid)
                .withReasonForHA(reason)
                .withJudgerClass(HaStartVmJudgers.GeneralJudger.class.getName());

        sender.send(new Completion(null) {
            @Override
            public void success() {
                trackSuccess(tracker);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                trackFailure(tracker, errorCode);
                if (!errorCode.isError(SysErrors.INTERNAL)) {
                    haGcManager.submitNeverStopVmGCJob(vmUuid);
                }
            }

            private void trackSuccess(VmHaTaskTracker tracker) {
                if (tracker != null) {
                    tracker.track(VmHaTaskTracker.Process.success, i18n("HA is successfully completed"));
                }
            }

            private void trackFailure(VmHaTaskTracker tracker, ErrorCode errorCode) {
                if (tracker != null) {
                    tracker.error(errorCode);
                    tracker.track(VmHaTaskTracker.Process.failure, i18n("Failed to HA the VM"));
                }
            }
        });
    }

    // ------------------------------------- VM HA Configuration Syncing ------------------------------------

    /**
     * MN -> host:
     *   What VM should be added to HA fenced list, even if the haVmList is empty
     */
    public void syncHaVmListOnHost(@NonNull String hostUuid, Completion completion) {
        threadFacade.chainSubmit(new ChainTask(completion) {
            @Override
            public void run(SyncTaskChain chain) {
                List<String> haVmList;
                if (HaHelper.isVmHaEnabled()) {
                    haVmList = Q.New(VmInstanceVO.class, VmHaVO.class)
                        .table0()
                            .select(VmInstanceVO_.uuid)
                            .eq(VmInstanceVO_.hostUuid, hostUuid)
                            .eq(VmInstanceVO_.uuid).table1(VmHaVO_.uuid)
                        .table1()
                            .eq(VmHaVO_.haLevel, VmHaLevel.NeverStop)
                        .list();
                    logger.debug(String.format("The following VMs on Host[%s] has HA enabled:\n\t%s", hostUuid, haVmList));
                } else {
                    haVmList = Collections.emptyList();
                    logger.debug(String.format("No VMs on Host[%s] has HA enabled: HA is disabled now", hostUuid));
                }

                syncHaVmListOnHostChain(hostUuid, haVmList, new Completion(completion, chain) {
                    @Override
                    public void success() {
                        logger.debug(String.format("sync ha vm list on host[%s] successfully", hostUuid));
                        completion.success();
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.warn(String.format(
                                "Failed to sync ha vm list on host[%s]: %s", hostUuid, errorCode.getReadableDetails()));
                        completion.fail(errorCode);
                        chain.next();
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return getName();
            }

            @Override
            public String getName() {
                return String.format("sync-ha-vm-list-on-host-%s", hostUuid);
            }
        });
    }

    private void syncHaVmListOnHostChain(String hostUuid, List<String> vmUuidList, Completion completion) {
        HaCommands.SyncHaVmListCmd cmd = new HaCommands.SyncHaVmListCmd();
        cmd.hostUuid = hostUuid;
        cmd.vmUuids = vmUuidList;

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setPath(HaCommands.SyncHaVmListCmd.PATH);
        msg.setHostUuid(hostUuid);
        msg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    // ------------------------------------- Utility Handers ------------------------------------

    private HaHypervisorFactory findHypervisorFactory(String type) {
        for (HaHypervisorFactory f : pluginRegistry.getExtensionList(HaHypervisorFactory.class)) {
            if (Objects.equals(type, f.getHypervisorType())) {
                return f;
            }
        }
        return null;
    }
}
