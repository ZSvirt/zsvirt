package org.zstack.core.gc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.TimeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

/**
 * Created by xing5 on 2017/3/1.
 */
public class GarbageCollectorManagerImpl extends AbstractService
        implements GarbageCollectorManager, Component, ManagementNodeReadyExtensionPoint {
    static final CLogger logger = Utils.getLogger(GarbageCollectorManagerImpl.class);

    @Autowired
    private ResourceDestinationMaker destinationMaker;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    private Future<Void> scanOrphanJobsTask;

    private Future<Void> cleanUpCompletedJobsTask;

    private ConcurrentHashMap<String, GarbageCollector> managedGarbageCollectors = new ConcurrentHashMap<>();

    private synchronized void startScanOrphanJobs() {
        if (scanOrphanJobsTask != null) {
            scanOrphanJobsTask.cancel(true);
        }

        scanOrphanJobsTask = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return GCGlobalConfig.SCAN_ORPHAN_JOB_INTERVAL.value(Long.class);
            }

            @Override
            public String getName() {
                return "scan-orphan-gc-jobs";
            }

            @Override
            public void run() {
                try {
                    loadOrphanJobs();
                } catch (Exception e) {
                    throw new CloudRuntimeException(e);
                }
            }
        });

        logger.debug(String.format("[GC] starts scanning orphan job thread with the interval[%ss]", GCGlobalConfig.SCAN_ORPHAN_JOB_INTERVAL.value(Integer.class)));
    }

    private synchronized void startCleanUpCompletedJobs() {
        if (cleanUpCompletedJobsTask != null) {
            cleanUpCompletedJobsTask.cancel(true);
        }

        cleanUpCompletedJobsTask = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.HOURS;
            }

            @Override
            public long getInterval() {
                return GCGlobalConfig.CLEAN_UP_COMPLETED_JOB_INTERVAL.value(Long.class);
            }

            @Override
            public String getName() {
                return "clean-up-completed-gc-jobs";
            }

            @Override
            public void run() {
                try {
                    cleanUpCompletedJobs();
                } catch (Exception e) {
                    logger.debug(String.format("clean up completed jobs failed, %s",e.getMessage()));
                }
            }
        });

        logger.debug(String.format("[GC] starts cleaning up completed job thread with the interval[%sh]", GCGlobalConfig.CLEAN_UP_COMPLETED_JOB_INTERVAL.value(Integer.class)));
    }

    private void cleanUpCompletedJobs() {
        String time = GCGlobalConfig.RETENTION_TIME.value();

        List<String> completedJobs = Q.New(GarbageCollectorVO.class)
                                    .select(GarbageCollectorVO_.uuid)
                                    .eq(GarbageCollectorVO_.status, GCStatus.Done)
                                    .lt(GarbageCollectorVO_.lastOpDate,
                                            new Timestamp(System.currentTimeMillis() - TimeUtils.parseTimeInMillis(time)))
                                    .listValues();

        List<String> jobs = completedJobs
                .stream()
                .filter(v -> destinationMaker.isManagedByUs(v))
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(jobs)) {
            return;
        }

        dbf.removeByPrimaryKeys(jobs, GarbageCollectorVO.class);
        logger.info(String.format("clean up %s completed GC jobs", jobs.size()));
    }

    void registerGC(GarbageCollector gc) {
        if (managedGarbageCollectors.containsKey(gc.uuid)) {
            logger.warn(String.format("[GC] job[id:%s] already registered, skip this registration. This largely happens" +
                    " when a management node loads orphan jobs from a offline management node", gc.uuid));
            return;
        }

        managedGarbageCollectors.put(gc.uuid, gc);
    }

    void deregisterGC(GarbageCollector gc) {
        managedGarbageCollectors.remove(gc.uuid);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private GarbageCollector loadGCJob(GarbageCollectorVO vo) {
        try {
            GarbageCollector ret = null;
            Class clz = Class.forName(vo.getRunnerClass());
            if (vo.getType().equals(GarbageCollectorType.EventBased.toString())) {
                EventBasedGarbageCollector gc = (EventBasedGarbageCollector) clz.newInstance();
                gc.load(vo);
                ret = gc;
            } else if (vo.getType().equals(GarbageCollectorType.TimeBased.toString())) {
                TimeBasedGarbageCollector gc = (TimeBasedGarbageCollector) clz.newInstance();
                gc.load(vo);
                ret = gc;
            } else if (vo.getType().equals(GarbageCollectorType.CycleBased.toString())) {
                TimeBasedGarbageCollector gc = (TimeBasedGarbageCollector) clz.newInstance();
                gc.load(vo);
                ret = gc;
            } else {
                DebugUtils.Assert(false, "should not be here");
            }

            return ret;
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    private void loadOrphanJobs() {
        List<GarbageCollectorVO> vos = Q.New(GarbageCollectorVO.class)
                .eq(GarbageCollectorVO_.status, GCStatus.Idle)
                .isNull(GarbageCollectorVO_.managementNodeUuid)
                .list();

        int count = 0;

        for (GarbageCollectorVO vo : vos) {
            if (!destinationMaker.isManagedByUs(vo.getUuid())) {
                continue;
            }

            loadGCJob(vo);

            count ++;
        }

        logger.debug(String.format("[GC] loaded %s orphan jobs", count));
    }

    @Override
    public void managementNodeReady() {
        startScanOrphanJobs();
        startCleanUpCompletedJobs();

        GCGlobalConfig.SCAN_ORPHAN_JOB_INTERVAL.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                startScanOrphanJobs();
            }
        });

        GCGlobalConfig.CLEAN_UP_COMPLETED_JOB_INTERVAL.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                startCleanUpCompletedJobs();
            }
        });
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof TriggerGcJobMsg) {
            handle((TriggerGcJobMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final TriggerGcJobMsg msg) {
        MessageReply reply = new MessageReply();
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getName();
            }

            @Override
            public void run(SyncTaskChain chain) {
                triggerGC(msg.getUuid());
                bus.reply(msg, reply);
                chain.next();
            }

            @Override
            public String getName() {
                return String.format("trigger-gc-job-%s", msg.getUuid());
            }
        });
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APITriggerGCJobMsg) {
            handle((APITriggerGCJobMsg) msg);
        } else if (msg instanceof APIDeleteGCJobMsg) {
            handle((APIDeleteGCJobMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIDeleteGCJobMsg msg) {
        GarbageCollector gc = managedGarbageCollectors.get(msg.getUuid());
        if (gc != null) {
            gc.cancel();
        }

        GarbageCollectorVO vo = dbf.findByUuid(msg.getUuid(), GarbageCollectorVO.class);
        dbf.remove(vo);

        APIDeleteGCJobEvent evt = new APIDeleteGCJobEvent(msg.getId());
        bus.publish(evt);
    }

    private void triggerGC(String gcUuid) {
        GarbageCollector gc = managedGarbageCollectors.get(gcUuid);
        if (gc != null) {
            gc.runTrigger();
        } else {
            GarbageCollectorVO vo = dbf.findByUuid(gcUuid, GarbageCollectorVO.class);
            if (vo.getStatus() == GCStatus.Done) {
                throw new OperationFailureException(operr("cannot trigger a finished GC job[uuid:%s, name:%s]",
                        vo.getUuid(), vo.getName()));
            }

            gc = loadGCJob(vo);
            gc.runTrigger();
        }
    }

    private void handle(APITriggerGCJobMsg msg) {
        APITriggerGCJobEvent evt = new APITriggerGCJobEvent(msg.getId());
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getName();
            }

            @Override
            public void run(SyncTaskChain chain) {
                triggerGC(msg.getUuid());
                bus.publish(evt);
                chain.next();
            }

            @Override
            public String getName() {
                return String.format("trigger-gc-job-%s", msg.getUuid());
            }
        });
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(GCConstants.SERVICE_ID);
    }
}
