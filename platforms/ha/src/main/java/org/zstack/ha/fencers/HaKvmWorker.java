package org.zstack.ha.fencers;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.ha.entity.VmHaVO;
import org.zstack.ha.entity.VmHaVO_;
import org.zstack.ha.HaHelper;
import org.zstack.ha.header.HaErrors;
import org.zstack.ha.header.HaGlobalConfig;
import org.zstack.ha.header.SelfFencerStrategy;
import org.zstack.ha.header.VmHaLevel;
import org.zstack.ha.message.CheckerStruct;
import org.zstack.ha.spi.HaCompletionCacher;
import org.zstack.ha.spi.HaHostChecker;
import org.zstack.ha.spi.HaHostStorageBasedChecker;
import org.zstack.ha.spi.HaHypervisorWorker;
import org.zstack.ha.spi.VmHaVOUpdater;
import org.zstack.ha.vmboot.HaStartVmJudgers;
import org.zstack.ha.vmboot.VmHaStartMessageSender;
import org.zstack.header.core.AsyncLatch;
import org.zstack.header.core.Completion;
import org.zstack.header.core.HaCheckerCompletion;
import org.zstack.header.core.HaCompletion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.storage.primary.PrimaryStorageType;
import org.zstack.header.vm.ChangeVmStateMsg;
import org.zstack.header.vm.VmHaExtensionPoint;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceStateEvent;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;
import static org.zstack.utils.CollectionUtils.findOneOrNull;
import static org.zstack.utils.CollectionUtils.transform;

/**
 * Created by xing5 on 2016/3/28.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HaKvmWorker implements HaHypervisorWorker {
    private static final CLogger logger = Utils.getLogger(HaKvmWorker.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private CloudBus bus;
    @Autowired
    private HaCompletionCacher cacher;
    @Autowired
    private ResourceConfigFacade rcf;
    @Autowired
    private VmHaVOUpdater vmHAExecutor;

    private VmInstanceVO self;

    public final Context context = new Context();

    public HaKvmWorker(VmInstanceInventory vm) {
        self = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);

        synchronized (context.lock) {
            context.onlyCheckAlive = Q.New(VmHaVO.class)
                    .eq(VmHaVO_.uuid, vm.getUuid())
                    .eq(VmHaVO_.haLevel, VmHaLevel.None)
                    .isExists();
        }
    }

    private void checkHost(List<HaHostChecker> checkers, final CheckerStruct s, Completion completion) {
        synchronized (context.lock) {
            if (context.started) {
                completion.fail(err(HaErrors.HA_WORKER_ALREADY_STARTED, "HaKvmWorker already started"));
                return;
            }
            context.started = true;
        }

        for (HaHostChecker checker : checkers) {
            ContextItem item = new ContextItem();
            item.checkerName = checker.getClass().getSimpleName();
            item.weight = checker.getWeight();
            synchronized (context.lock) {
                context.items.add(item);
            }

            if (checker instanceof HaHostStorageBasedChecker) {
                PrimaryStorageType storageType = ((HaHostStorageBasedChecker) checker).getPrimaryStorageType();
                item.storageType = storageType == null ? null : storageType.toString();
            }
        }

        HostCheckAliveTaskTracker tracker = new HostCheckAliveTaskTracker(s.hostUuid);
        final AsyncLatch latch = new AsyncLatch(checkers.size(), new NoErrorCompletion() {
            @Override
            public void done() {
                Report report = generateReport(s.hostUuid);
                tracker.track(HostCheckAliveTaskTracker.Process.mapFrom(report), report.evidence);
                onScanResult(s.hostUuid, report);
                completion.success();
            }
        });

        for (final HaHostChecker checker : checkers) {
            final String checkName = checker.getClass().getSimpleName();
            ContextItem item = Objects.requireNonNull(
                    findOneOrNull(context.items, it -> Objects.equals(it.checkerName, checkName)));

            logger.debug(String.format("[HA Worker]: start HaHostChecker[%s] for the vm[name: %s, uuid: %s]",
                    checkName, self.getName(), self.getUuid()));

            tracker.track(HostCheckAliveTaskTracker.Process.checking, i18n("(%d/%d) start HaHostChecker %s: predict time is [%d] seconds",
                    checkers.indexOf(checker) + 1, checkers.size(), checker.getClass().getSimpleName(), s.successTimes * s.successInterval));

            if (checker instanceof HaHostStorageBasedChecker) {
                doStorageBaseHostChecker((HaHostStorageBasedChecker) checker, s, item, latch);
            } else {
                doHostChecker(checker, s, item, latch);
            }
        }
    }

    public void onScanResult(String hostUuid, Report report) {
        if (report.alive == AliveLevel.MustAlive || report.alive == AliveLevel.Alive) {
            return;
        }

        List<String> unknownVmUuids = Q.New(VmInstanceVO.class, VmHaVO.class)
            .table0()
                .select(VmInstanceVO_.uuid)
                .eq(VmInstanceVO_.hostUuid, hostUuid)
                .in(VmInstanceVO_.type, Arrays.asList(VmInstanceConstant.USER_VM_TYPE, VmInstanceConstant.APPLIANCE_VM_TYPE))
                .eq(VmInstanceVO_.state, VmInstanceState.Unknown)
                .eq(VmInstanceVO_.uuid).table1(VmHaVO_.uuid)
            .table1()
                .eq(VmHaVO_.haLevel, VmHaLevel.None)
            .toSqlInstance(String.class)
            .list();

        logger.info(String.format("Host[uuid: %s] is dead, has %d unknown VMs with HA disabled", hostUuid, unknownVmUuids.size()));

        List<ChangeVmStateMsg> cmsgs = unknownVmUuids.stream()
                .filter(uuid -> HaStrategyHelper.getVmHaFencerStrategy(uuid) == SelfFencerStrategy.Force)
                .map(uuid -> {
                    ChangeVmStateMsg msg = new ChangeVmStateMsg();
                    msg.setVmInstanceUuid(uuid);
                    msg.setStateEvent(VmInstanceStateEvent.stopped.toString());
                    bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, uuid);
                    return msg;
                })
                .collect(Collectors.toList());

        logger.debug(String.format("remaining vm %s", transform(cmsgs, ChangeVmStateMsg::getVmInstanceUuid)));
        bus.send(cmsgs);
    }

    private void doHostChecker(HaHostChecker checker, final CheckerStruct s, final ContextItem item, final AsyncLatch latch) {
        checker.check(s, new HaCheckerCompletion<Void>(latch) {
            @Override
            public void noWay() {
                logger.warn(String.format("[Ha worker]: checker[%s] says it has no way to check the host[uuid: %s, ip: %s]," +
                                " remove the checker's weight",
                        checker.getClass(), s.hostUuid, s.hostIp));
                synchronized (item.lock) {
                    item.alive = AliveLevel.NotKnow;
                }
                latch.ack();
            }

            @Override
            public void notStable() {
                logger.warn(String.format("[Ha worker]: checker[%s] says it can check the host[uuid: %s, ip: %s], but the connection is not stable," +
                        " so we consider the host is still alive", checker.getClass(), s.hostUuid, s.hostIp));
                synchronized (item.lock) {
                    item.alive = AliveLevel.Alive;
                }
                latch.ack();
            }

            @Override
            public void success(Void aVoid) {
                logger.debug(String.format("[Ha worker]: checker[%s] confirm the host[uuid: %s, ip: %s] is still alive", checker.getClass(),
                        s.hostUuid, s.hostIp));
                synchronized (item.lock) {
                    item.alive = AliveLevel.Alive;
                }
                latch.ack();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.debug(String.format("[HA Worker]: checker[%s] failed to check the host[uuid: %s, ip: %s], %s",
                        checker.getClass(), s.hostUuid, s.hostIp, errorCode));
                synchronized (item.lock) {
                    item.alive = AliveLevel.MayDied;
                    item.error = errorCode;
                }
                latch.ack();
            }
        });
    }

    private void doStorageBaseHostChecker(HaHostStorageBasedChecker checker, final CheckerStruct s, final ContextItem item, final AsyncLatch latch) {
        checker.check(s, new HaCheckerCompletion<List<String>>(latch) {
            @Override
            public void noWay() {
                logger.warn(String.format("[Ha worker]: checker[%s] says it has no way to check the host[uuid: %s, ip: %s]," +
                                " remove the checker's weight",
                        checker.getClass(), s.hostUuid, s.hostIp));
                synchronized (item.lock) {
                    item.alive = AliveLevel.NotKnow;
                }
                latch.ack();
            }

            @Override
            public void notStable() {
                logger.warn(String.format("[Ha worker]: checker[%s] says it can check the host[uuid: %s, ip: %s], but the connection is not stable," +
                        " so we consider the host is still alive", checker.getClass(), s.hostUuid, s.hostIp));
                synchronized (item.lock) {
                    item.alive = AliveLevel.Alive;
                }
                latch.ack();
            }

            @Override
            public void success(List<String> vmUuids) {
                logger.debug(String.format("[Ha worker]: checker[%s] confirm the host[uuid: %s, ip: %s] is still alive, vm[%s] is running on host", checker.getClass(),
                        s.hostUuid, s.hostIp, CollectionUtils.isEmpty(vmUuids) ? " " : vmUuids.stream().collect(Collectors.joining(", "))));
                synchronized (item.lock) {
                    item.alive = AliveLevel.MustAlive;
                    item.aliveVmUuidList.addAll(vmUuids);
                }
                latch.ack();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.debug(String.format("[HA Worker]: checker[%s] failed to check the host[uuid: %s, ip: %s], %s",
                        checker.getClass(), s.hostUuid, s.hostIp, errorCode));
                synchronized (item.lock) {
                    item.alive = AliveLevel.MayDied;
                    item.error = errorCode;
                }
                latch.ack();
            }
        });
    }

    private String findVmRootVolumePsUuid(String vmUuid) {
        return SQL.New("select vol.primaryStorageUuid from VolumeVO vol, VmInstanceVO vm " +
                "where vm.uuid = :vmUuid and vol.uuid = vm.rootVolumeUuid", String.class)
                .param("vmUuid", vmUuid)
                .find();
    }

    @Override
    public void start(final HaCompletion completion) {
        if (VmInstanceState.Unknown != self.getState()) {
            logger.debug(String.format("the vm[uuid: %s, name: %s] is out of the state unknown, current state is %s, no need to" +
                    " do the HA", self.getUuid(), self.getName(), self.getState()));
            completion.success();
            return;
        }

        if (self.getHostUuid() == null) {
            completion.fail(err(HaErrors.VM_HOST_NOT_FOUND,
                    "cannot find the host of the vm[name:%s, uuid:%s], hostUuid is null",
                    self.getName(), self.getUuid()));
            return;
        }

        List<HaHostChecker> checkers = pluginRgty.getExtensionList(HaHostChecker.class);
        if (checkers.isEmpty()) {
            completion.fail(err(HaErrors.NO_HA_HOST_CHECKER, "no HaHostChecker found, cannot do HA"));
            return;
        }

        completion.setVmUuid(self.getUuid());
        String mgmtIp = Q.New(HostVO.class)
                .select(HostVO_.managementIp)
                .eq(HostVO_.uuid, self.getHostUuid())
                .findValue();

        final CheckerStruct s = new CheckerStruct();
        s.hostIp = mgmtIp;
        s.hostUuid = self.getHostUuid();
        s.vmInstance = VmInstanceInventory.valueOf(self);
        s.interval = HaGlobalConfig.HOST_CHECK_INTERVAL.value(Long.class);
        s.maxTimes = HaGlobalConfig.HOST_CHECK_MAX_ATTEMPTS.value(Integer.class);
        s.successInterval = HaGlobalConfig.HOST_CHECK_SUCCESS_INTERVAL.value(Long.class);
        s.successTimes = HaGlobalConfig.HOST_CHECK_SUCCESS_TIMES.value(Integer.class);

        // We need a single flight to avoid scan the same hosts repeatedly at the same time.
        // But we should *not* wait synchronously, as normal implementation does.
        final String psUuid = findVmRootVolumePsUuid(self.getUuid());

        // A quick hack for ZSTAC-14456
        // TODO a better way for here is to use threadFacade.singleFlightSubmit
        final String key = s.hostUuid+psUuid;
        boolean isFirst = cacher.enqueCompletion(completion, key);

        if (!isFirst) {
            logger.info("queued an HA host scanner for host: "+s.hostUuid);
            return;
        }

        checkHost(checkers, s, new Completion(null) { // completion is in cacher
            @Override
            public void success() {
                Collection<HaCompletion> rcs = cacher.dequeCompletions(key);
                rcs.forEach(c -> tryHaWith(c, s.hostUuid));
            }

            @Override
            public void fail(ErrorCode errorCode) {
                Collection<HaCompletion> rcs = cacher.dequeCompletions(key);
                rcs.forEach(c -> failHaWith(c, s.hostUuid, errorCode));
            }
        });
    }

    @AsyncThread
    private void tryHaWith(HaCompletion completion, String hostUuid) {
        logger.info("result of HaKvmWorker: hostUuid=" + hostUuid + "\n" + context);

        if (context.onlyCheckAlive) {
            completion.noNeed(String.format("only check alive, no need to do HA for vm[uuid:%s]", completion.getVmUuid()));
            return;
        }

        final Report report = generateReport(hostUuid);
        String reason = vmHAExecutor.findVmFencedReason(completion.getVmUuid());
        if (report.alive == AliveLevel.MayDied) {
            startHa(hostUuid, reason == null ? report.evidence : reason, completion);
            return;
        } else if (report.alive == AliveLevel.MustAlive && !report.aliveVmUuids.contains(completion.getVmUuid())) {
            startHa(hostUuid, reason == null ? i18n("vm is not active on storage") : reason, completion);
            return;
        }
        completion.noNeed(report.evidence);
    }

    @AsyncThread
    private static void failHaWith(HaCompletion c, String hostUuid, ErrorCode e) {
        logger.warn(String.format("[HA Worker]: failed to check the status of host %s, for vm %s, error: %s",
                hostUuid, c.getVmUuid(), e.toString()));
        c.fail(e);
    }

    private void startHa(String hostUuid, String reason, HaCompletion completion) {
        SelfFencerStrategy strategy = HaStrategyHelper.getVmHaFencerStrategy(completion.getVmUuid());

        if (strategy == SelfFencerStrategy.Permissive) {
            logger.debug(String.format("Self fencer strategy if Permissive, skip HA for vm[uuid:%s]", completion.getVmUuid()));
            completion.success();
            return;
        }

        if (!HaHelper.isSelfFencerDone(completion.getVmUuid(), hostUuid)) {
            completion.success();
            return;
        }

        for (VmHaExtensionPoint ext : pluginRgty.getExtensionList(VmHaExtensionPoint.class)) {
            ErrorCode errorCode = ext.preHaStartVm(completion.getVmUuid());
            if (errorCode != null) {
                logger.warn(String.format("skip HA for vm[%s]: %s", completion.getVmUuid(), errorCode.getReadableDetails()));
                completion.success(); // skip should not call `fail`
                return;
            }
        }

        VmHaStartMessageSender sender = new VmHaStartMessageSender()
                .withVmInstance(completion.getVmUuid())
                .withAvoidHost(hostUuid)
                .withReasonForHA(reason)
                .withJudgerClass(HaStartVmJudgers.UnknownVmJudger.class.getName());

        sender.send(new Completion(completion) {
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

    public Report generateReport(String hostUuid) {
        synchronized (context.lock) {
            Report report = new Report();
            report.hostUuid = hostUuid;
            report.alive = AliveLevel.Alive;
            report.weightTotal = 0;
            report.aliveWeightTotal = 0;

            for (ContextItem item : context.items) {
                synchronized (item.lock) {
                    report.aliveVmUuids.addAll(item.aliveVmUuidList);
                }
            }

            for (ContextItem item : context.items) {
                synchronized (item.lock) {
                    switch (item.alive) {
                        case MustAlive: {
                            report.weightTotal = item.weight; // not +=
                            report.aliveWeightTotal = item.weight;
                            report.evidence = i18n("%s confirm host[uuid:%s] is alive",
                                    item.checkerName, hostUuid);
                            report.alive = AliveLevel.MustAlive;
                            return report;
                        }

                        case Alive: {
                            report.weightTotal += item.weight;
                            report.aliveWeightTotal += item.weight;
                        }
                        break;

                        case MayDied: {
                            report.weightTotal += item.weight;
                        }
                        break;

                        case NotKnow: {
                        }
                        break;
                    }
                }
            }

            if (report.weightTotal == 0) {
                report.evidence = i18n("No way to check the host status");
                return report;
            }

            float ratio = report.aliveWeightTotal / (float) report.weightTotal;
            float threshold = HaGlobalConfig.CHECK_SUCCESS_RATIO.value(Float.class);
            if (ratio >= threshold) {
                report.alive = AliveLevel.Alive;
                report.evidence = i18n("the host[uuid:%s] is alive, no HA required. The host will be reconnected soon", hostUuid);
            } else {
                String errorDetails = "\n\t" + String.join("\n\t",
                        transform(context.items, it -> it.checkerName + ": " + it.alive + ((it.error == null) ? "" : it.error.getDetails())));
                report.alive = AliveLevel.MayDied;
                report.evidence = i18n("the host[uuid:%s] is dead. (%s) The vm on this host start HA", hostUuid, errorDetails);
            }

            return report;
        }
    }
}
