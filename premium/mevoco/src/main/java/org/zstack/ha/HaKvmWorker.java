package org.zstack.ha;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.Q;
import org.zstack.ha.fencers.KvmFencerManager;
import org.zstack.header.core.HaCheckerCompletion;
import org.zstack.header.core.HaCompletion;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.core.AsyncLatch;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import org.apache.commons.collections.CollectionUtils;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.i18n;
import static org.zstack.core.Platform.operr;

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
    private KvmFencerManager fencerManager;

    private VmInstanceVO self;
    private List<HostScanExtensionPoint> extensionPoints;
    private boolean onlyCheckAlive = false;

    public HaKvmWorker(VmInstanceInventory vm) {
        self = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
        extensionPoints = pluginRgty.getExtensionList(HostScanExtensionPoint.class);
        onlyCheckAlive = Q.New(VmHaVO.class)
                .eq(VmHaVO_.uuid, vm.getUuid())
                .eq(VmHaVO_.haLevel, VmHaLevel.None)
                .isExists();
    }

    private void checkHost(List<HaHostChecker> checkers, final CheckerStruct s, ReturnValueCompletion<HostCheckResult> completion) {
        final HostCheckResult ret = new HostCheckResult();

        for (HaHostChecker checker : checkers) {
            ret.weightTotal.addAndGet(checker.getWeight());
        }


        HostCheckAliveTaskTracker tracker = new HostCheckAliveTaskTracker(s.getHostUuid());
        final AsyncLatch latch = new AsyncLatch(checkers.size(), new NoErrorCompletion() {
            @Override
            public void done() {
                HostCheckResult.HostScanResult status = ret.getCheckResult(s.getHostUuid());
                tracker.track(HostCheckAliveTaskTracker.Process.mapFrom(status), ret.details);
                extensionPoints.forEach(ext -> {
                    try {
                        ext.onScanResult(s.getHostUuid(), status);
                    } catch (Throwable ignored) {}
                });
                completion.success(ret);
            }
        });

        for (final HaHostChecker checker : checkers) {
            final String checkName = checker.getClass().getSimpleName();
            logger.debug(String.format("[HA Worker]: start HaHostChecker[%s] for the vm[name: %s, uuid: %s]",
                    checkName, self.getName(), self.getUuid()));

            tracker.track(HostCheckAliveTaskTracker.Process.checking, i18n("(%d/%d) start HaHostChecker %s: predict time is [%d] seconds",
                    checkers.indexOf(checker) + 1, checkers.size(), checker.getClass().getSimpleName(), s.getSuccessTimes() * s.getSuccessInterval()));

            if (checker instanceof HaHostStorageBasedChecker) {
                doStorageBaseHostChecker((HaHostStorageBasedChecker) checker, s, ret, latch);
            } else {
                doHostChecker(checker, s, ret, latch);
            }
        }
    }

    private void doHostChecker(HaHostChecker checker, final CheckerStruct s, final HostCheckResult ret, final AsyncLatch latch) {
        checker.check(s, new HaCheckerCompletion<Void>(latch) {
            @Override
            public void noWay() {
                logger.warn(String.format("[Ha worker]: checker[%s] says it has no way to check the host[uuid: %s, ip: %s]," +
                                " remove the checker's weight",
                        checker.getClass(), s.getHostUuid(), s.getHostIp()));
                ret.weightTotal.addAndGet(-checker.getWeight());
                latch.ack();
            }

            @Override
            public void notStable() {
                logger.warn(String.format("[Ha worker]: checker[%s] says it can check the host[uuid: %s, ip: %s], but the connection is not stable," +
                        " so we consider the host is still alive", checker.getClass(), s.getHostUuid(), s.getHostIp()));
                ret.weight.addAndGet(checker.getWeight());
                latch.ack();
            }

            @Override
            public void success(Void aVoid) {
                logger.debug(String.format("[Ha worker]: checker[%s] confirm the host[uuid: %s, ip: %s] is still alive", checker.getClass(),
                        s.getHostUuid(), s.getHostIp()));
                ret.weight.addAndGet(checker.getWeight());
                latch.ack();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.debug(String.format("[HA Worker]: checker[%s] failed to check the host[uuid: %s, ip: %s], %s",
                        checker.getClass(), s.getHostUuid(), s.getHostIp(), errorCode));
                ret.errors.add(errorCode);
                latch.ack();
            }
        });
    }

    private void doStorageBaseHostChecker(HaHostStorageBasedChecker checker, final CheckerStruct s, final HostCheckResult ret, final AsyncLatch latch) {
        checker.check(s, new HaCheckerCompletion<List<String>>(latch) {
            @Override
            public void noWay() {
                logger.warn(String.format("[Ha worker]: checker[%s] says it has no way to check the host[uuid: %s, ip: %s]," +
                                " remove the checker's weight",
                        checker.getClass(), s.getHostUuid(), s.getHostIp()));
                ret.weightTotal.addAndGet(-checker.getWeight());
                latch.ack();
            }

            @Override
            public void notStable() {
                logger.warn(String.format("[Ha worker]: checker[%s] says it can check the host[uuid: %s, ip: %s], but the connection is not stable," +
                        " so we consider the host is still alive", checker.getClass(), s.getHostUuid(), s.getHostIp()));
                ret.weight.addAndGet(checker.getWeight());
                latch.ack();
            }

            @Override
            public void success(List<String> vmUuids) {
                logger.debug(String.format("[Ha worker]: checker[%s] confirm the host[uuid: %s, ip: %s] is still alive, vm[%s] is running on host", checker.getClass(),
                        s.getHostUuid(), s.getHostIp(), CollectionUtils.isEmpty(vmUuids) ? " " : vmUuids.stream().collect(Collectors.joining(", "))));
                ret.weight.addAndGet(checker.getWeight());

                if (vmUuids != null && !vmUuids.isEmpty()) {
                    ret.vmUuids.addAll(vmUuids);
                }

                if (checker.getPrimaryStorageType() != null) {
                    ret.isCheckVmOnHost.put(checker.getPrimaryStorageType(), Boolean.TRUE);
                }
                latch.ack();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.debug(String.format("[HA Worker]: checker[%s] failed to check the host[uuid: %s, ip: %s], %s",
                        checker.getClass(), s.getHostUuid(), s.getHostIp(), errorCode));
                ret.errors.add(errorCode);
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
            completion.fail(operr("cannot find the host of the vm[name:%s, uuid:%s], hostUuid is null",
                    self.getName(), self.getUuid()));
            return;
        }

        List<HaHostChecker> checkers = pluginRgty.getExtensionList(HaHostChecker.class);
        if (checkers.isEmpty()) {
            completion.fail(operr("no HaHostChecker found, cannot do HA"));
            return;
        }

        completion.setVmUuid(self.getUuid());

        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.select(HostVO_.managementIp);
        q.add(HostVO_.uuid, Op.EQ, self.getHostUuid());
        String mgmtIp = q.findValue();

        final CheckerStruct s = new CheckerStruct();
        s.setHostIp(mgmtIp);
        s.setHostUuid(self.getHostUuid());
        s.setVmInstance(VmInstanceInventory.valueOf(self));
        s.setInterval(HaGlobalConfig.HOST_CHECK_INTERVAL.value(Long.class));
        s.setMaxTimes(HaGlobalConfig.HOST_CHECK_MAX_ATTEMPTS.value(Integer.class));
        s.setSuccessInterval(HaGlobalConfig.HOST_CHECK_SUCCESS_INTERVAL.value(Long.class));
        s.setSuccessTimes(HaGlobalConfig.HOST_CHECK_SUCCESS_TIMES.value(Integer.class));

        // We need a single flight to avoid scan the same hosts repeatedly at the same time.
        // But we should *not* wait synchronously, as normal implementation does.
        final String psUuid = findVmRootVolumePsUuid(self.getUuid());

        // A quick hack for ZSTAC-14456
        final String key = s.getHostUuid()+psUuid;
        boolean isFirst = cacher.enqueCompletion(completion, key);

        if (!isFirst) {
            logger.info("queued an HA host scanner for host: "+s.getHostUuid());
            return;
        }

        checkHost(checkers, s, new ReturnValueCompletion<HostCheckResult>(completion) {
            @Override
            public void success(HostCheckResult returnValue) {
                Collection<HaCompletion> rcs = cacher.dequeCompletions(key);
                rcs.forEach(c -> tryHaWith(c, s.getHostUuid(), returnValue));
            }

            @Override
            public void fail(ErrorCode errorCode) {
                Collection<HaCompletion> rcs = cacher.dequeCompletions(key);
                rcs.forEach(c -> failHaWith(c, s.getHostUuid(), errorCode));
            }
        });
    }

    private boolean vmIsNotActiveOnStorage(HostCheckResult ret, String vmUuid) {
        return ret.isCheckVmOnHost.containsValue(Boolean.TRUE) && !ret.vmUuids.contains(vmUuid);
    }

    @AsyncThread
    private void tryHaWith(HaCompletion completion, String hostUuid, HostCheckResult ret) {
        if (onlyCheckAlive) {
            completion.noNeed(String.format("only check alive, no need to do HA for vm[uuid:%s]", completion.getVmUuid()));
            return;
        }

        final HostCheckResult.HostScanResult result = ret.getCheckResult(hostUuid);
        String reason = fencerManager.findVmFencedReasonByTag(completion.getVmUuid());
        if (result.isDead()) {
            startHa(hostUuid, reason == null ? result.getEvidence() : reason, completion);
            return;
        } else if (vmIsNotActiveOnStorage(ret, completion.getVmUuid())) {
            startHa(hostUuid, reason == null ? i18n("vm is not active on storage") : reason, completion);
            return;
        }
        completion.noNeed(ret.details);
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

        for (HaHostDeviceExtensionPoint ext : pluginRgty.getExtensionList(HaHostDeviceExtensionPoint.class)) {
            if (!ext.canDoVmHa(completion.getVmUuid())) {
                logger.debug(String.format("host device still attached, skip HA for vm[uuid:%s]", completion.getVmUuid()));
                completion.success();
                return;
            }
        }

        VmHaLevel level = Q.New(VmHaVO.class)
                .eq(VmHaVO_.uuid, completion.getVmUuid())
                .select(VmHaVO_.haLevel)
                .findValue();
        if (VmHaLevel.FaultTolerance.equals(level)) {
            return;
        }

        VmHaStartMessageSender sender = new VmHaStartMessageSender()
                .withVmInstance(completion.getVmUuid())
                .withAvoidHost(hostUuid)
                .withReasonForHA(reason)
                .withJudgerClass(UnknownVmHaJudger.class.getName());
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
}
