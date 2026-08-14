package org.zstack.scheduler;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.*;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.NopeWhileDoneCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.*;
import org.zstack.header.identity.quota.QuotaMessageHandler;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.scheduler.*;
import org.zstack.header.vm.*;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.volume.VolumeBeforeExpungeExtensionPoint;
import org.zstack.header.volume.VolumeDeletionExtensionPoint;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeJustBeforeDeleteFromDbExtensionPoint;
import org.zstack.identity.Account;
import org.zstack.identity.AccountManager;
import org.zstack.scheduler.quota.SchedulerJobNumQuotaDefinition;
import org.zstack.scheduler.quota.SchedulerTriggerNumQuotaDefinition;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.JobKey.jobKey;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.zstack.core.Platform.*;
import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by Mei Lei on 6/22/16.
 */
public class SchedulerFacadeImpl extends AbstractService implements SchedulerFacade, ManagementNodeReadyExtensionPoint,
        ManagementNodeChangeListener, ResourceOwnerAfterChangeExtensionPoint, VmStateChangedExtensionPoint,
        VmBeforeExpungeExtensionPoint, VmInstanceDestroyExtensionPoint, RecoverVmExtensionPoint, VolumeDeletionExtensionPoint,
        VolumeBeforeExpungeExtensionPoint, ReportQuotaExtensionPoint, VolumeJustBeforeDeleteFromDbExtensionPoint, VmJustBeforeDeleteFromDbExtensionPoint {
    private static final CLogger logger = Utils.getLogger(SchedulerFacadeImpl.class);

    @Autowired
    private transient CloudBus bus;
    @Autowired
    private transient ErrorFacade errf;
    @Autowired
    protected transient DatabaseFacade dbf;
    @Autowired
    private transient ResourceDestinationMaker destinationMaker;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private EventFacade evtf;

    private static Map<String, SchedulerJobFactory> schedulerJobFactories = Collections.synchronizedMap(new HashMap<String, SchedulerJobFactory>());
    private static Map<String, Class<? extends AbstractSchedulerJob>> jobClasses = Collections.synchronizedMap(new HashMap<String, Class<? extends AbstractSchedulerJob>>());
    private Cache<Set<String>, SchedulerExecutionHistory> historyCache = CacheBuilder.newBuilder().maximumSize(100).build();
    private Cache<String, SchedulerCronExecutionPlan> cronPlanCache = CacheBuilder.newBuilder().maximumSize(1000).build();
    private Cache<String, Boolean> firedGroupResultId = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build();

    private Scheduler scheduler;

    public Scheduler getScheduler() {
        return scheduler;
    }

    static Map<String, Class<? extends AbstractSchedulerJob>> getJobClasses() {
        return jobClasses;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    static Map<String, SchedulerJobFactory> getSchedulerJobFactories() {
        return schedulerJobFactories;
    }

    private String getThreadSyncName(Message msg) {
        if (msg instanceof SchedulerJobGroupMessage) {
            return "scheduler-job-group-" + ((SchedulerJobGroupMessage) msg).getSchedulerJobGroupUuid();
        }
        return "scheduler";
    }

    private void populateSchedulerJobFactories() {
        for (SchedulerJobFactory ext : pluginRgty.getExtensionList(SchedulerJobFactory.class)) {
            SchedulerJobFactory old = schedulerJobFactories.get(ext.getJobType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate SchedulerJobFactory[%s, %s] for type[%s]",
                        old.getClass().getName(), ext.getClass().getName(), ext.getJobType()));
            }

            schedulerJobFactories.put(ext.getJobType(), ext);
        }
    }

    private SchedulerJobFactory getSchedulerJobFactory(String jobType) {
        SchedulerJobFactory factory = schedulerJobFactories.get(jobType);
        if (factory == null) {
            throw new CloudRuntimeException(String.format("No SchedulerJobFactory of type[%s] found", jobType));
        }

        return factory;
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIDeleteSchedulerJobMsg) {
            handle((APIDeleteSchedulerJobMsg) msg);
        } else if (msg instanceof APIDeleteSchedulerJobGroupMsg) {
            handle((APIDeleteSchedulerJobGroupMsg) msg);
        } else if (msg instanceof APIUpdateSchedulerJobMsg) {
            handle((APIUpdateSchedulerJobMsg) msg);
        } else if (msg instanceof APIUpdateSchedulerJobGroupMsg) {
            handle((APIUpdateSchedulerJobGroupMsg) msg);
        } else if (msg instanceof APIChangeSchedulerStateMsg) {
            handle((APIChangeSchedulerStateMsg) msg);
        } else if (msg instanceof APICreateSchedulerTriggerMsg) {
            handle((APICreateSchedulerTriggerMsg) msg);
        } else if (msg instanceof APIUpdateSchedulerTriggerMsg) {
            handle((APIUpdateSchedulerTriggerMsg) msg);
        } else if (msg instanceof APIDeleteSchedulerTriggerMsg) {
            handle((APIDeleteSchedulerTriggerMsg) msg);
        } else if (msg instanceof APIAddSchedulerJobToSchedulerTriggerMsg) {
            handle((APIAddSchedulerJobToSchedulerTriggerMsg) msg);
        } else if (msg instanceof APIRemoveSchedulerJobFromSchedulerTriggerMsg) {
            handle((APIRemoveSchedulerJobFromSchedulerTriggerMsg) msg);
        } else if (msg instanceof APIAddSchedulerJobGroupToSchedulerTriggerMsg) {
            handle((APIAddSchedulerJobGroupToSchedulerTriggerMsg) msg);
        } else if (msg instanceof APIRemoveSchedulerJobGroupFromSchedulerTriggerMsg) {
            handle((APIRemoveSchedulerJobGroupFromSchedulerTriggerMsg) msg);
        } else if (msg instanceof APIAddSchedulerJobsToSchedulerJobGroupMsg) {
            handle((APIAddSchedulerJobsToSchedulerJobGroupMsg) msg);
        } else if (msg instanceof APIRemoveSchedulerJobsFromSchedulerJobGroupMsg) {
            handle((APIRemoveSchedulerJobsFromSchedulerJobGroupMsg) msg);
        } else if (msg instanceof APICreateSchedulerJobMsg) {
            handle((APICreateSchedulerJobMsg) msg);
        } else if (msg instanceof APICreateSchedulerJobGroupMsg) {
            handle((APICreateSchedulerJobGroupMsg) msg);
        } else if (msg instanceof APIGetAvailableTriggersMsg) {
            handle((APIGetAvailableTriggersMsg) msg);
        } else if (msg instanceof APIGetNoTriggerSchedulerJobsMsg) {
            handle((APIGetNoTriggerSchedulerJobsMsg) msg);
        } else if (msg instanceof APIRunSchedulerTriggerMsg ) {
            handle((APIRunSchedulerTriggerMsg) msg);
        } else if (msg instanceof APIGetSchedulerExecutionReportMsg) {
            handle((APIGetSchedulerExecutionReportMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof SchedulerDeletionMsg) {
            handle((SchedulerDeletionMsg) msg);
        } else if (msg instanceof SchedulerTriggerDeletionMsg) {
            handle((SchedulerTriggerDeletionMsg) msg);
        } else if (msg instanceof ResumeSchedulerJobMsg) {
            handle((ResumeSchedulerJobMsg) msg);
        } else if (msg instanceof PauseSchedulerJobMsg) {
            handle((PauseSchedulerJobMsg) msg);
        } else if (msg instanceof ScheduleJobMsg) {
            handle((ScheduleJobMsg) msg);
        } else if (msg instanceof ScheduleJobGroupMsg) {
            handle((ScheduleJobGroupMsg) msg);
        } else if (msg instanceof UnscheduleJobMsg) {
            handle((UnscheduleJobMsg) msg);
        } else if (msg instanceof UnscheduleJobGroupMsg) {
            handle((UnscheduleJobGroupMsg) msg);
        } else if (msg instanceof RunSchedulerTriggerMsg) {
            handle((RunSchedulerTriggerMsg) msg);
        } else if (msg instanceof FireSchedulerGroupResultMsg) {
            handle((FireSchedulerGroupResultMsg) msg);
        } else if (msg instanceof DeleteSchedulerJobMsg) {
            handle((DeleteSchedulerJobMsg) msg);
        } else if (msg instanceof DeleteSchedulerTriggerMsg) {
            handle((DeleteSchedulerTriggerMsg) msg);
        } else if (msg instanceof DeleteSchedulerJobGroupMsg) {
            handle((DeleteSchedulerJobGroupMsg) msg);
        } else if (msg instanceof CreateSchedulerTriggerMsg) {
            handle((CreateSchedulerTriggerMsg) msg);
        } else if (msg instanceof AddSchedulerJobToSchedulerTriggerMsg) {
            handle((AddSchedulerJobToSchedulerTriggerMsg) msg);
        } else if (msg instanceof CreateSchedulerJobMsg) {
            handle((CreateSchedulerJobMsg) msg);
        } else if (msg instanceof AddSchedulerJobGroupToSchedulerTriggerMsg) {
            handle((AddSchedulerJobGroupToSchedulerTriggerMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(CreateSchedulerJobMsg msg) {
        CreateSchedulerJobReply reply = new CreateSchedulerJobReply();
        SchedulerJobFactory factory = this.getSchedulerJobFactory(msg.getType());
        SchedulerJob job = factory.createSchedulerJob(msg);
        if (job.getQueueName() == null) {
            SchedulerJobInventory jobInventory = doPersistSchedulerJob(msg, msg.getTargetResourceUuid(), job);
            reply.setInventory(jobInventory);
            bus.reply(msg, reply);
        }
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                SchedulerJobInventory jobInventory = doPersistSchedulerJob(msg, msg.getTargetResourceUuid(), job);
                reply.setInventory(jobInventory);
                bus.reply(msg, reply);
                chain.next();
            }

            @Override
            public String getSyncSignature() {
                return job.getQueueName();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private SchedulerJobInventory doPersistSchedulerJob(CreateSchedulerJobDescMsg msg, String targetResourceUuid, SchedulerJob job) {
        if (job == null) {
            throw new CloudRuntimeException("Create scheduler job failed for: " + msg.getType());
        }

        if (jobClasses.get(job.getClass().getName()) == null) {
            throw new CloudRuntimeException("No matched class can be found: " + job.getClass().getName());
        }

        SchedulerJobVO vo = new SchedulerJobVO();
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        job.setUuid(vo.getUuid());
        vo.setJobClassName(job.getClass().getName());
        vo.setJobData(JSONObjectUtil.toJsonString(job));
        vo.setManagementNodeUuid(destinationMaker.makeDestination(vo.getUuid()));
        vo.setTargetResourceUuid(targetResourceUuid);
        vo.setState(SchedulerState.Enabled.toString());
        vo.setAccountUuid(msg.getAccountUuid());
        dbf.persistAndRefresh(vo);
        return SchedulerJobInventory.valueOf(vo);
    }

    private void handle(AddSchedulerJobToSchedulerTriggerMsg msg) {
        AddSchedulerJobToSchedulerTriggerReply reply = new AddSchedulerJobToSchedulerTriggerReply();

        SchedulerJobSchedulerTriggerRefVO vo = new SchedulerJobSchedulerTriggerRefVO();
        vo.setUuid(Platform.getUuid());
        vo.setSchedulerJobUuid(msg.getSchedulerUuid());
        vo.setSchedulerTriggerUuid(msg.getSchedulerTriggerUuid());
        dbf.persist(vo);
        scheduleJob(msg.getSchedulerJobUuid(), Collections.singletonList(msg.getSchedulerTriggerUuid()), msg.getTriggerNowId(), new Completion(msg) {
            @Override
            public void success() {
                reply.setInventory(SchedulerJobSchedulerTriggerInventory.valueOf(vo));
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                dbf.remove(vo);
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(CreateSchedulerTriggerMsg msg) {
        CreateSchedulerTriggerReply reply = new CreateSchedulerTriggerReply();

        SchedulerTriggerVO vo = new SchedulerTriggerVO();
        if (msg.getUuid() != null) {
            vo.setUuid(msg.getUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());

        Timestamp start = null;
        if (msg.getStartTime() != null) {
            start = new Timestamp(msg.getStartTime() * 1000L);
            vo.setStartTime(start);
        }

        if (msg.getSchedulerType().equals(SchedulerConstant.SIMPLE_TYPE_STRING)) {
            DebugUtils.Assert(start != null, "Simple scheduler should have start time");

            // if execute once
            if (msg.getStartTime() != 0 && msg.getRepeatCount() != 0) {
                if (msg.getRepeatCount() == 1) {
                    vo.setStopTime(start);
                } else {
                    vo.setStopTime(new Timestamp(start.getTime() + (long) (msg.getRepeatCount() - 1) * (long) msg.getSchedulerInterval() * 1000L));
                }
            } else {
                vo.setStopTime(null);
            }
        } else {
            vo.setStopTime(null);
        }

        vo.setRepeatCount(msg.getRepeatCount());
        vo.setSchedulerInterval(msg.getSchedulerInterval());
        vo.setSchedulerType(msg.getSchedulerType());
        vo.setAccountUuid(msg.getAccountUuid());
        vo.setCron(msg.getCron());
        dbf.persist(vo);

        reply.setInventory(SchedulerTriggerInventory.valueOf(vo));
        bus.reply(msg, reply);
    }


    private void handle(DeleteSchedulerJobGroupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncName(msg);
            }

            @Override
            public void run(SyncTaskChain chain) {
                DeleteSchedulerJobGroupReply reply = new DeleteSchedulerJobGroupReply();

                deleteSchedulerJobGroup(msg.getUuid(), new Completion(chain) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "delete-scheduler-job-group" + msg.getUuid();
            }
        });
    }

    private void handle(DeleteSchedulerTriggerMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncName(msg);
            }

            @Override
            public void run(SyncTaskChain chain) {
                DeleteSchedulerTriggerReply reply = new DeleteSchedulerTriggerReply();
                deleteSchedulerTrigger(msg.getUuid(), new Completion(chain) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "delete-scheduler-trigger-" + msg.getUuid();
            }
        });
    }

    private void handle(DeleteSchedulerJobMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncName(msg);
            }

            @Override
            public void run(SyncTaskChain chain) {
                DeleteSchedulerJobReply reply = new DeleteSchedulerJobReply();

                deleteSchedulerJob(msg.getUuid(), new Completion(chain) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "delete-scheduler-job-" + msg.getUuid();
            }
        });
    }

    private void handle(PauseSchedulerJobMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getName();
            }

            @Override
            public void run(SyncTaskChain chain) {
                PauseSchedulerJobReply reply = new PauseSchedulerJobReply();
                pauseSchedulerJob(msg.getUuid());
                bus.reply(msg, reply);
                chain.next();
            }

            @Override
            public String getName() {
                return String.format("change-scheduler-job-state-%s", msg.getUuid());
            }
        });
    }

    private void handle(ResumeSchedulerJobMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getName();
            }

            @Override
            public void run(SyncTaskChain chain) {
                ResumeSchedulerJobReply reply = new ResumeSchedulerJobReply();
                resumeSchedulerJob(msg.getUuid());
                bus.reply(msg, reply);
                chain.next();
            }

            @Override
            public String getName() {
                return String.format("change-scheduler-job-state-%s", msg.getUuid());
            }
        });
    }

    private void handle(ScheduleJobMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncName(msg);
            }

            @Override
            public void run(SyncTaskChain chain) {
                ScheduleJobReply reply = new ScheduleJobReply();
                doScheduleJobLocally(msg.getSchedulerJobUuid(), msg.getSchedulerTriggerUuids(), msg.getTriggerNowId());
                bus.reply(msg, reply);
                chain.next();
            }

            @Override
            public String getName() {
                return String.format("schedule-job-%s-to-triggers-%s",
                        msg.getSchedulerJobUuid(), msg.getSchedulerTriggerUuids());
            }
        });
    }

    private void handle(AddSchedulerJobGroupToSchedulerTriggerMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncName(msg);
            }

            @Override
            public void run(SyncTaskChain chain) {
                AddSchedulerJobGroupToSchedulerTriggerReply reply = new AddSchedulerJobGroupToSchedulerTriggerReply();
                scheduleJobGroup(msg.getSchedulerJobGroupUuid(), msg.getSchedulerTriggerUuids(), msg.getTriggerNowId(), new Completion(msg) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        reply.setSuccess(false);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("add-schedule-job-group-%s-to-schedule-trigger", msg.getSchedulerJobGroupUuid());
            }
        });
    }

    private void handle(ScheduleJobGroupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncName(msg);
            }

            @Override
            public void run(SyncTaskChain chain) {
                ScheduleJobReply reply = new ScheduleJobReply();
                doScheduleJobGroupLocally(msg.getSchedulerJobGroupUuid(), msg.getSchedulerTriggerUuids(), msg.getTriggerNowId());
                bus.reply(msg, reply);
                chain.next();
            }

            @Override
            public String getName() {
                return String.format("schedule-job-group-%s-to-triggers-%s",
                        msg.getSchedulerJobGroupUuid(), msg.getSchedulerTriggerUuids().toString());
            }
        });
    }

    private void handle(UnscheduleJobMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncName(msg);
            }

            @Override
            public void run(SyncTaskChain chain) {
                UnscheduleJobReply reply = new UnscheduleJobReply();
                doUnscheduleJobLocally(msg.getSchedulerJobUuid(), msg.getSchedulerTriggerUuids());
                bus.reply(msg, reply);
                chain.next();
            }

            @Override
            public String getName() {
                return String.format("unschedule-job-%s-from-trigger-%s",
                        msg.getSchedulerJobUuid(), msg.getSchedulerTriggerUuids());
            }
        });
    }

    private void handle(UnscheduleJobGroupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncName(msg);
            }

            @Override
            public void run(SyncTaskChain chain) {
                UnscheduleJobReply reply = new UnscheduleJobReply();
                doUnscheduleJobGroupLocally(msg.getSchedulerJobGroupUuid(), msg.getSchedulerTriggerUuids());
                bus.reply(msg, reply);
                chain.next();
            }

            @Override
            public String getName() {
                return String.format("unschedule-job-group-%s-from-trigger-%s",
                        msg.getSchedulerJobGroupUuid(), msg.getSchedulerTriggerUuids());
            }
        });
    }

    private void handle(RunSchedulerTriggerMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncName(msg);
            }

            @Override
            public void run(SyncTaskChain chain) {
                RunSchedulerTriggerReply reply = new RunSchedulerTriggerReply();
                doRunTriggerLocally(msg.getTriggerUuid(), msg.getJobUuids(), msg.getFireInstanceId());
                if (msg.getGroupJobUuids() != null) {
                    msg.getGroupJobUuids().forEach((groupUuid, jobUuids) ->
                            doRunTriggerForGroupLocally(msg.getTriggerUuid(), groupUuid, jobUuids, msg.getFireInstanceId()));
                }
                bus.reply(msg, reply);
                chain.next();
            }

            @Override
            public String getName() {
                return String.format("run-scheduler-trigger-%s", msg.getTriggerUuid());
            }
        });
    }

    private void handle(FireSchedulerGroupResultMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "scheduler-group-fireInstanceId-" + msg.getFireInstanceId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                if (firedGroupResultId.getIfPresent((msg.getFireInstanceId())) == null) {
                    evtf.fire(SchedulerCanonicalEvents.GROUP_SCHEDULER_PATH, msg.getData());
                    firedGroupResultId.put(msg.getFireInstanceId(), true);
                }

                chain.next();
            }

            @Override
            public String getName() {
                return String.format("fire-scheduler-group-%s-result", msg.getSchedulerJobGroupUuid());
            }
        });
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handle(SchedulerTriggerDeletionMsg msg) {
        SchedulerTriggerDeletionReply reply = new SchedulerTriggerDeletionReply();
        deleteSchedulerTrigger(msg.getUuid(), new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(SchedulerDeletionMsg msg) {
        SchedulerDeletionReply reply = new SchedulerDeletionReply();
        deleteSchedulerJob(msg.getUuid(), new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(APIGetNoTriggerSchedulerJobsMsg msg) {
        APIGetNoTriggerSchedulerJobsReply reply = new APIGetNoTriggerSchedulerJobsReply();

        List<SchedulerJobVO> jobs = SQL.New("select vo from SchedulerJobVO vo, AccountResourceRefVO ref where vo.uuid not in " +
                "(select job.uuid from SchedulerJobVO job " +
                "join SchedulerJobSchedulerTriggerRefVO ref on ref.schedulerJobUuid = job.uuid) and " +
                "ref.accountUuid = :accountUuid and ref.resourceUuid = vo.uuid and ref.type = 'Own'")
                .param("accountUuid", msg.getSession().getAccountUuid())
                .list();

        List<SchedulerJobInventory> invs = SchedulerJobInventory.valueOf(jobs);
        reply.setInventories(invs);
        bus.reply(msg, reply);
    }

    private void handle(APIGetAvailableTriggersMsg msg) {
        APIGetAvailableTriggersReply reply = new APIGetAvailableTriggersReply();
        List<SchedulerTriggerVO> vos;

        String sql = "select st from SchedulerTriggerVO st, AccountResourceRefVO ref where " +
                "ref.resourceUuid = st.uuid and ref.type = 'Own' " +
                "and (st.stopTime is NULL or st.stopTime > CURRENT_TIMESTAMP())";

        boolean isAdmin = Account.isAdminPermission(msg.getSession());
        if (!isAdmin) {
            sql += " and ref.accountUuid = :accountUuid";
        }
        SQL query = SQL.New(sql);
        if (!isAdmin) {
            query.param("accountUuid", msg.getSession().getAccountUuid());
        }
        vos = query.list();

        List<SchedulerTriggerInventory> invs = SchedulerTriggerInventory.valueOf(vos);
        reply.setInventories(invs);
        bus.reply(msg, reply);
    }

    private void handle(APIGetSchedulerExecutionReportMsg msg) {
        APIGetSchedulerExecutionReportReply reply = new APIGetSchedulerExecutionReportReply();
        SchedulerExecutionReport report = new SchedulerExecutionReport(msg.getStartTime(), getTimeUnit(msg.getIntervalTimeUnit()), msg.getRange());

        // 1. fill history into report
        Set<String> key = new HashSet<>(msg.getSchedulerJobTypes());
        SchedulerExecutionHistory cache = historyCache.asMap().computeIfAbsent(key, k -> new SchedulerExecutionHistory(msg.getSchedulerJobTypes()));
        cache.fillReport(report);

        // 2. fill plan into report
        for (Tuple t : getTriggerAndAttachCount(msg.getSchedulerJobTypes())) {
            int attachCount = t.get(0, Long.class).intValue();
            SchedulerTriggerVO trigger = t.get(1, SchedulerTriggerVO.class);
            if (trigger.getSchedulerType().equals(SchedulerConstant.CRON_TYPE_STRING)) {
                SchedulerCronExecutionPlan planCache = cronPlanCache.asMap().computeIfAbsent(trigger.getCron(),
                        k -> new SchedulerCronExecutionPlan(trigger.getCron()));
                planCache.fillReport(report, trigger.getStartTime(), attachCount);
            } else if (trigger.getSchedulerType().equals(SchedulerConstant.SIMPLE_TYPE_STRING)) {
                SchedulerSimpleTriggerPlanWriter reckoner = new SchedulerSimpleTriggerPlanWriter(trigger);
                reckoner.fillReport(report, attachCount);
            }
        }

        reply.loadFromReport(report);
        bus.reply(msg, reply);
    }

    @Transactional(readOnly = true)
    protected List<Tuple> getTriggerAndAttachCount(Collection<String> jobTypes) {
        List<Tuple> ts1 = SQL.New("select count(ref), trigger" +
                " from SchedulerTriggerVO trigger, SchedulerJobGroupSchedulerTriggerRefVO ref, SchedulerJobGroupVO jobGroup" +
                " where jobGroup.jobType in :jobTypes" +
                " and jobGroup.uuid = ref.schedulerJobGroupUuid" +
                " and jobGroup.state = :state" +
                " and ref.schedulerTriggerUuid = trigger.uuid" +
                " group by trigger.uuid", Tuple.class)
                .param("state", SchedulerJobGroupState.Enabled.toString())
                .param("jobTypes", jobTypes)
                .list();

        List<String> notInGroupJobUuids = SQL.New("select job.uuid from SchedulerJobVO job" +
                " where job.uuid not in (select ref.schedulerJobUuid from SchedulerJobGroupJobRefVO ref)" +
                " and job.jobClassName in :jobClassNames" +
                " and job.state = :state", String.class)
                .param("jobClassNames", getJobClassNames(jobTypes))
                .param("state", SchedulerState.Enabled.toString())
                .list();

        if (notInGroupJobUuids.isEmpty()) {
            return ts1;
        }

        List<Tuple> ts2 = SQL.New("select count(ref), trigger" +
                " from SchedulerTriggerVO trigger, SchedulerJobSchedulerTriggerRefVO ref" +
                " where ref.schedulerJobUuid in :jobUuids" +
                " and ref.schedulerTriggerUuid = trigger.uuid" +
                " group by trigger.uuid", Tuple.class)
                .param("jobUuids", notInGroupJobUuids)
                .list();

        ts1.addAll(ts2);
        return ts1;
    }

    private List<String> getJobClassNames(Collection<String> jobTypes) {
        List<String> results = new ArrayList<>();
        jobTypes.forEach(k -> results.add(schedulerJobFactories.get(k).getJobClassName()));
        return results;
    }

    private int getTimeUnit(String timeUnitInStr) {
        if (timeUnitInStr.equals("Hour")) {
            return Calendar.HOUR_OF_DAY;
        } else if (timeUnitInStr.equals("Day")) {
            return Calendar.DATE;
        } else if (timeUnitInStr.equals("Month")) {
            return Calendar.MONTH;
        } else {
            throw new OperationFailureException(argerr("invalid time unit: %s", timeUnitInStr));
        }
    }


    private void handle(APICreateSchedulerJobGroupMsg msg) {
        APICreateSchedulerJobGroupEvent evt = new APICreateSchedulerJobGroupEvent(msg.getId());

        SchedulerJobGroupVO vo = new SchedulerJobGroupVO();
        SchedulerJobFactory factory = this.getSchedulerJobFactory(msg.getType());
        SchedulerJob job = factory.createSchedulerJob(msg);
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setState(SchedulerJobGroupState.Enabled.toString());
        vo.setAccountUuid(msg.getSession().getAccountUuid());
        vo.setJobType(msg.getType());
        vo.setJobClassName(factory.getJobClassName());
        vo.setJobData(JSONObjectUtil.toJsonString(job));
        vo.setManagementNodeUuid(getManagementServerId());
        dbf.persistAndRefresh(vo);

        evt.setInventory(SchedulerJobGroupInventory.valueOf(vo));
        bus.publish(evt);
    }

    private void handle(APICreateSchedulerJobMsg msg) {
        APICreateSchedulerJobEvent evt = new APICreateSchedulerJobEvent(msg.getId());
        SchedulerJobFactory factory = this.getSchedulerJobFactory(msg.getType());
        SchedulerJob job = factory.createSchedulerJob(msg);
        if (job.getQueueName() == null) {
            SchedulerJobInventory jobInventory = doPersistSchedulerJob(msg, msg.getTargetResourceUuid(), job);
            evt.setInventory(jobInventory);
            bus.publish(evt);
            return;
        }
        CreateSchedulerJobMsg createSchedulerJobMsg = buildCreateSchedulerJobMsg(msg, job);
        bus.send(createSchedulerJobMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    CreateSchedulerJobReply csJobReply = reply.castReply();
                    evt.setInventory(csJobReply.getInventory());
                } else {
                    evt.setError(reply.getError());
                }
                bus.publish(evt);
            }
        });
    }

    private CreateSchedulerJobMsg buildCreateSchedulerJobMsg(APICreateSchedulerJobMsg msg, SchedulerJob job) {
        CreateSchedulerJobMsg csJobMsg = new CreateSchedulerJobMsg();
        if (msg.getResourceUuid() != null) {
            csJobMsg.setUuid(msg.getResourceUuid());
        } else {
            csJobMsg.setUuid(Platform.getUuid());
        }
        csJobMsg.setType(msg.getType());
        csJobMsg.setTargetResourceUuid(msg.getTargetResourceUuid());
        csJobMsg.setName(msg.getName());
        csJobMsg.setParameters(msg.getParameters());
        csJobMsg.setAccountUuid(msg.getAccountUuid());
        /* use const string as uuid to make all APICreateSchedulerJobMsg that need to be queued is handled by same management node */
        bus.makeTargetServiceIdByResourceUuid(csJobMsg, SchedulerConstant.SERVICE_ID, job.getQueueName());
        return csJobMsg;
    }

    private void handle(APIRunSchedulerTriggerMsg msg) {
        APIRunSchedulerTriggerEvent evt = new APIRunSchedulerTriggerEvent(msg.getId());
        runTrigger(msg.getUuid(), msg.getJobUuids(), msg.getId(), new Completion(evt) {
            @Override
            public void success() {
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void runTrigger(String triggerUuid, List<String> designatedJobUuids, String fireInstanceId, Completion completion) {
        List<String> jobUuids = getEnabledJobUuids(triggerUuid);
        Optional.ofNullable(designatedJobUuids).ifPresent(jobUuids::retainAll);

        Map<String, List<String>> nodeJobUuids = new HashMap<>();
        jobUuids.forEach(it -> nodeJobUuids.computeIfAbsent(destinationMaker.makeDestination(it), k -> new ArrayList<>()).add(it));

        List<String> groupUuids = getEnabledGroupUuidsFromTriggerUuid(triggerUuid);
        Map<String, List<String>> nodeGroupUuids = new HashMap<>();
        groupUuids.forEach(it -> nodeGroupUuids.computeIfAbsent(destinationMaker.makeDestination(it), k -> new ArrayList<>()).add(it));

        Set<String> mnIds = new HashSet<>(nodeJobUuids.keySet());
        mnIds.addAll(nodeGroupUuids.keySet());
        ErrorCodeList err = new ErrorCodeList();
        new While<>(mnIds).all((mnId, compl) -> {
            RunSchedulerTriggerMsg msg = new RunSchedulerTriggerMsg();
            msg.setJobUuids(nodeJobUuids.get(mnId));
            msg.setGroupJobUuids(getEnabledGroupJobUuids(nodeGroupUuids.get(mnId), designatedJobUuids));
            msg.setTriggerUuid(triggerUuid);
            msg.setFireInstanceId(fireInstanceId);
            bus.makeServiceIdByManagementNodeId(msg, SchedulerConstant.SERVICE_ID, mnId);
            bus.send(msg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        err.getCauses().add(reply.getError());
                    }

                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!err.getCauses().isEmpty() && err.getCauses().size() == nodeJobUuids.size()) {
                    completion.fail(err.getCauses().get(0));
                    return;
                }
                completion.success();
            }
        });
    }

    private void doRunTriggerLocally(String triggerUuid, List<String> jobUuids, String fireInstanceId) {
        if (CollectionUtils.isEmpty(jobUuids)) {
            return;
        }
        for (String jobUuid : jobUuids) {
            try {
                scheduler.triggerJob(new JobKey(jobUuid, triggerUuid), new JobDataMap(Collections.singletonMap(SchedulerConstant.FIRE_INSTANCE_ID, fireInstanceId)));
            } catch (SchedulerException e) {
                throw new OperationFailureException(operr("trigger job[uuid: %s] failed, because %s",
                        jobUuid, e.getMessage()));
            }
        }
    }

    private void doRunTriggerForGroupLocally(String triggerUuid, String groupUuid, List<String> jobUuids, String fireInstanceId) {
        try {
            Map<String, String> jobData = new HashMap<>();
            jobData.put(SchedulerConstant.FIRE_INSTANCE_ID, fireInstanceId);
            if (!CollectionUtils.isEmpty(jobUuids)) {
                jobData.put(SchedulerConstant.GROUP_JOBS_TO_SCHEDULER, String.join(",", jobUuids));
            }
            scheduler.triggerJob(new JobKey(groupUuid, triggerUuid), new JobDataMap(jobData));
        } catch (SchedulerException e) {
            throw new OperationFailureException(operr("trigger job group[uuid: %s] failed, because %s",
                    groupUuid, e.getMessage()));
        }
    }

    // triggerNowId is not null means trigger now.
    private void scheduleJob(String jobUuid, List<String> triggerUuids, String triggerNowId, Completion completion) {
        if (jobInGroup(jobUuid)) {
            completion.success();
            return;
        }

        if (triggerUuids == null || triggerUuids.isEmpty()) {
            completion.success();
            return;
        }

        if (!isJobPermitted(jobUuid)) {
            completion.success();
            return;
        }

        if (destinationMaker.isManagedByUs(jobUuid)) {
            doScheduleJobLocally(jobUuid, triggerUuids, triggerNowId);
            completion.success();
            return;
        }

        logger.debug(String.format("schedulerJob[uuid:%s] is not managed by us, send it to other node", jobUuid));
        ScheduleJobMsg smsg = new ScheduleJobMsg();
        smsg.setSchedulerJobUuid(jobUuid);
        smsg.setSchedulerTriggerUuids(triggerUuids);
        smsg.setTriggerNowId(triggerNowId);
        bus.makeTargetServiceIdByResourceUuid(smsg, SchedulerConstant.SERVICE_ID, jobUuid);
        bus.send(smsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                } else {
                    completion.success();
                }
            }
        });
    }

    private void scheduleJobGroup(String jobGroupUuid, List<String> triggerUuids, String triggerNowId, Completion completion) {
        if (triggerUuids == null || triggerUuids.isEmpty()) {
            completion.success();
            return;
        }

        if (!isJobGroupPermitted(jobGroupUuid)) {
            completion.success();
            return;
        }

        if (destinationMaker.isManagedByUs(jobGroupUuid)) {
            doScheduleJobGroupLocally(jobGroupUuid, triggerUuids, triggerNowId);
            completion.success();
            return;
        }

        logger.debug(String.format("schedulerJobGroup[uuid:%s] is not managed by us, send it to other node", jobGroupUuid));
        ScheduleJobGroupMsg smsg = new ScheduleJobGroupMsg();
        smsg.setSchedulerJobGroupUuid(jobGroupUuid);
        smsg.setSchedulerTriggerUuids(triggerUuids);
        smsg.setTriggerNowId(triggerNowId);
        bus.makeTargetServiceIdByResourceUuid(smsg, SchedulerConstant.SERVICE_ID, jobGroupUuid);
        bus.send(smsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                } else {
                    completion.success();
                }
            }
        });
    }

    private void doScheduleJobLocally(String jobUuid, List<String> triggerUuids, String triggerNowId) {
        DebugUtils.Assert(destinationMaker.isManagedByUs(jobUuid),
                String.format("schedulerJob[uuid:%s] is not managed by us", jobUuid));
        doRunScheduler(jobUuid, triggerUuids, triggerNowId);
    }

    private void doScheduleJobGroupLocally(String jobGroupUuid, List<String> triggerUuids, String triggerNowId) {
        DebugUtils.Assert(destinationMaker.isManagedByUs(jobGroupUuid),
                String.format("schedulerJobGroup[uuid:%s] is not managed by us", jobGroupUuid));
        doRunScheduler(jobGroupUuid, triggerUuids, triggerNowId);
    }

    private void doRunScheduler(String jobUuid, List<String> triggerUuids, String triggerNowId){
        for (String triggerUuid : triggerUuids) {
            String fireId = triggerNowId == null ? null : Platform.getUuidFromBytes((triggerNowId + triggerUuid).getBytes());
            SchedulerTask task = prepareSchedulerTaskFromMsg(triggerUuid, fireId, jobUuid);
            ErrorCode err = runScheduler(task);
            if (err != null) {
                throw new OperationFailureException(err);
            }
        }
    }

    private boolean jobInGroup(String jobUuid) {
        return Q.New(SchedulerJobGroupJobRefVO.class).eq(SchedulerJobGroupJobRefVO_.schedulerJobUuid, jobUuid).isExists();
    }

    private void unscheduleJob(String jobUuid, List<String> triggerUuids, Completion completion) {
        if (jobInGroup(jobUuid)) {
            completion.success();
            return;
        }

        if (triggerUuids == null || triggerUuids.isEmpty()) {
            completion.success();
            return;
        }

        if (destinationMaker.isManagedByUs(jobUuid)) {
            doUnscheduleJobLocally(jobUuid, triggerUuids);
            completion.success();
            return;
        }

        logger.debug(String.format("schedulerJob[uuid:%s] is not managed by us, send it to other node", jobUuid));
        UnscheduleJobMsg umsg = new UnscheduleJobMsg();
        umsg.setSchedulerJobUuid(jobUuid);
        umsg.setSchedulerTriggerUuids(triggerUuids);
        bus.makeTargetServiceIdByResourceUuid(umsg, SchedulerConstant.SERVICE_ID, jobUuid);
        bus.send(umsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                } else {
                    completion.success();
                }
            }
        });
    }

    private void unscheduleJobGroup(String jobGroupUuid, List<String> triggerUuids, Completion completion) {
        if (triggerUuids == null || triggerUuids.isEmpty()) {
            completion.success();
            return;
        }

        if (destinationMaker.isManagedByUs(jobGroupUuid)) {
            doUnscheduleJobGroupLocally(jobGroupUuid, triggerUuids);
            completion.success();
            return;
        }

        logger.debug(String.format("schedulerJobGroup[uuid:%s] is not managed by us, send it to other node", jobGroupUuid));
        UnscheduleJobGroupMsg umsg = new UnscheduleJobGroupMsg();
        umsg.setSchedulerJobGroupUuid(jobGroupUuid);
        umsg.setSchedulerTriggerUuids(triggerUuids);
        bus.makeTargetServiceIdByResourceUuid(umsg, SchedulerConstant.SERVICE_ID, jobGroupUuid);
        bus.send(umsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                } else {
                    completion.success();
                }
            }
        });
    }

    private void doUnscheduleJobLocally(String jobUuid, List<String> triggerUuids) {
        logger.debug(String.format("unschedule job[uuid%s] from trigger [uuids:%s]", jobUuid, triggerUuids.toString()));
        for (String triggerUuid : triggerUuids) {
            try {
                scheduler.deleteJob(jobKey(jobUuid, triggerUuid));
            } catch (SchedulerException se) {
                throw new OperationFailureException(
                        inerr("unschedule job fail, job uuid[uuid:%s], trigger uuid[uuid:%s]", jobUuid, triggerUuid));
            }
        }
    }

    private void doUnscheduleJobGroupLocally(String jobGroupUuid, List<String> triggerUuids) {
        logger.debug(String.format("unschedule job group[uuid%s] from trigger [uuids:%s]", jobGroupUuid, triggerUuids.toString()));
        for (String triggerUuid : triggerUuids) {
            try {
                scheduler.deleteJob(jobKey(jobGroupUuid, triggerUuid));
            } catch (SchedulerException se) {
                throw new OperationFailureException(
                        inerr("unschedule job group fail, group[uuid:%s], trigger uuid[uuid:%s]", jobGroupUuid, triggerUuid));
            }
        }
    }

    private void addSchedulerJobsToSchedulerJobGroup(APIAddSchedulerJobsToSchedulerJobGroupMsg msg, Completion completion) {
        msg.setPriorities(msg.getPriorities() == null ? new HashMap<>() : msg.getPriorities());
        SchedulerJobGroupVO jobGroupVO = dbf.findByUuid(msg.getSchedulerJobGroupUuid(), SchedulerJobGroupVO.class);
        List<String> jobUuids = msg.getSchedulerJobUuids();
        List<SchedulerJobVO> jobVOs = dbf.listByPrimaryKeys(jobUuids, SchedulerJobVO.class);
        List<String> jobsInGroup = Q.New(SchedulerJobGroupJobRefVO.class)
                .eq(SchedulerJobGroupJobRefVO_.schedulerJobGroupUuid, jobGroupVO.getUuid())
                .select(SchedulerJobGroupJobRefVO_.schedulerJobUuid).listValues();
        String jobGroupUuid = jobGroupVO.getUuid();

        List<SchedulerJobGroupJobRefVO> refVOsToPersist = new ArrayList<>();
        List<SchedulerJobGroupJobRefVO> refVOsToUpdate = new ArrayList<>();
        jobVOs.forEach(jobVO -> {
            String jobUuid = jobVO.getUuid();
            if (!jobsInGroup.contains(jobUuid)) {
                SchedulerJobGroupJobRefVO refVO = new SchedulerJobGroupJobRefVO();
                refVO.setSchedulerJobGroupUuid(jobGroupUuid);
                refVO.setSchedulerJobUuid(jobUuid);
                refVO.setPriority(msg.getPriorities().get(jobUuid) == null ? 0 : msg.getPriorities().get(jobUuid));
                refVOsToPersist.add(refVO);
            } else {
                SchedulerJobGroupJobRefVO refVO = Q.New(SchedulerJobGroupJobRefVO.class)
                        .eq(SchedulerJobGroupJobRefVO_.schedulerJobGroupUuid, jobGroupUuid)
                        .eq(SchedulerJobGroupJobRefVO_.schedulerJobUuid, jobUuid).find();
                if (msg.getPriorities().get(jobUuid) != null) {
                    refVO.setPriority(msg.getPriorities().get(jobUuid));
                    refVOsToUpdate.add(refVO);
                }
            }

            if (jobGroupVO.getZoneUuid() == null) {
                Class<? extends AbstractSchedulerJob> clz = jobClasses.get(jobVO.getJobClassName());
                AbstractSchedulerJob schedulerJob = JSONObjectUtil.toObject(jobVO.getJobData(), clz);
                String zoneUuid = schedulerJob.getZoneUuid();
                if (zoneUuid != null) {
                    SQL.New(SchedulerJobGroupVO.class).eq(SchedulerJobGroupVO_.uuid, jobGroupUuid)
                            .set(SchedulerJobGroupVO_.zoneUuid, zoneUuid).update();
                    jobGroupVO.setZoneUuid(zoneUuid);
                }
            }
        });
        if (!refVOsToPersist.isEmpty()) {
            dbf.persistCollection(refVOsToPersist);
        }
        if (!refVOsToUpdate.isEmpty()) {
            dbf.updateCollection(refVOsToUpdate);
        }
        completion.success();
    }

    private void handle(APIAddSchedulerJobsToSchedulerJobGroupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncName(msg);
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIAddSchedulerJobsToSchedulerJobGroupEvent evt = new APIAddSchedulerJobsToSchedulerJobGroupEvent(msg.getId());
                addSchedulerJobsToSchedulerJobGroup(msg, new Completion(chain) {
                    @Override
                    public void success() {
                        List<SchedulerJobGroupJobRefVO> vos = Q.New(SchedulerJobGroupJobRefVO.class)
                                .eq(SchedulerJobGroupJobRefVO_.schedulerJobGroupUuid, msg.getSchedulerJobGroupUuid())
                                .in(SchedulerJobGroupJobRefVO_.schedulerJobUuid, msg.getSchedulerJobUuids())
                                .list();

                        evt.setInventories(SchedulerJobGroupJobRefInventory.valueOf(vos));
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "add-jobs-to-scheduler-group-" + msg.getSchedulerJobGroupUuid();
            }
        });
    }

    private void removeSchedulerJobsFromSchedulerJobGroup(List<String> jobUuids,
                                                          SchedulerJobGroupVO jobGroupVO,
                                                          Completion completion) {
        SQL.New(SchedulerJobGroupJobRefVO.class)
                .eq(SchedulerJobGroupJobRefVO_.schedulerJobGroupUuid, jobGroupVO.getUuid())
                .in(SchedulerJobGroupJobRefVO_.schedulerJobUuid, jobUuids)
                .hardDelete();
        SQL.New(SchedulerJobVO.class).in(SchedulerJobVO_.uuid, jobUuids)
                .set(SchedulerJobVO_.state, SchedulerState.Disabled.toString())
                .update();
        completion.success();
    }

    private void handle(APIRemoveSchedulerJobsFromSchedulerJobGroupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncName(msg);
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIRemoveSchedulerJobsFromSchedulerJobGroupEvent evt = new APIRemoveSchedulerJobsFromSchedulerJobGroupEvent(msg.getId());
                SchedulerJobGroupVO jobGroupVO = dbf.findByUuid(msg.getSchedulerJobGroupUuid(), SchedulerJobGroupVO.class);

                removeSchedulerJobsFromSchedulerJobGroup(msg.getSchedulerJobUuids(), jobGroupVO, new Completion(chain) {
                    @Override
                    public void success() {
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "remove-jobs-to-scheduler-group-" + msg.getSchedulerJobGroupUuid();
            }
        });
    }

    private void handle(APIAddSchedulerJobGroupToSchedulerTriggerMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncName(msg);
            }

            @Override
            public void run(SyncTaskChain chain) {
                addSchedulerJobGroupToSchedulerTrigger(msg, chain);
            }

            @Override
            public String getName() {
                return String.format("add-job-group-%s-to-trigger-%s",
                        msg.getSchedulerJobGroupUuid(), msg.getSchedulerTriggerUuid());
            }
        });
    }

    private void handle(APIRemoveSchedulerJobGroupFromSchedulerTriggerMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncName(msg);
            }

            @Override
            public void run(SyncTaskChain chain) {
                removeSchedulerJobGroupFromTrigger(msg, chain);
            }

            @Override
            public String getName() {
                return String.format("remove-job-group-%s-from-trigger-%s",
                        msg.getSchedulerJobGroupUuid(), msg.getSchedulerTriggerUuid());
            }
        });
    }

    private void addSchedulerJobGroupToSchedulerTrigger(APIAddSchedulerJobGroupToSchedulerTriggerMsg msg, SyncTaskChain chain) {
        APIAddSchedulerJobGroupToSchedulerTriggerEvent evt = new APIAddSchedulerJobGroupToSchedulerTriggerEvent(msg.getId());
        scheduleJobGroup(msg.getSchedulerJobGroupUuid(), Collections.singletonList(msg.getSchedulerTriggerUuid()), msg.getTriggerNowId(), new Completion(chain) {
            @Override
            public void success() {
                SchedulerJobGroupSchedulerTriggerRefVO refVO = new SchedulerJobGroupSchedulerTriggerRefVO();
                refVO.setSchedulerJobGroupUuid(msg.getSchedulerJobGroupUuid());
                refVO.setSchedulerTriggerUuid(msg.getSchedulerTriggerUuid());
                dbf.persist(refVO);
                evt.setInventory(SchedulerJobGroupSchedulerTriggerRefInventory.valueOf(refVO));
                bus.publish(evt);
                chain.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
                chain.next();
            }
        });
    }

    private void removeSchedulerJobGroupFromTrigger(APIRemoveSchedulerJobGroupFromSchedulerTriggerMsg msg, SyncTaskChain chain) {
        APIRemoveSchedulerJobGroupFromSchedulerTriggerEvent evt = new APIRemoveSchedulerJobGroupFromSchedulerTriggerEvent(msg.getId());

        unscheduleJobGroup(msg.getSchedulerJobGroupUuid(), Collections.singletonList(msg.getSchedulerTriggerUuid()), new Completion(chain) {
            @Override
            public void success() {
                SQL.New(SchedulerJobGroupSchedulerTriggerRefVO.class)
                        .eq(SchedulerJobGroupSchedulerTriggerRefVO_.schedulerTriggerUuid, msg.getSchedulerTriggerUuid())
                        .eq(SchedulerJobGroupSchedulerTriggerRefVO_.schedulerJobGroupUuid, msg.getSchedulerJobGroupUuid())
                        .hardDelete();
                bus.publish(evt);
                chain.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
                chain.next();
            }
        });
    }

    private void handle(APIRemoveSchedulerJobFromSchedulerTriggerMsg msg) {
        APIRemoveSchedulerJobFromSchedulerTriggerEvent evt = new APIRemoveSchedulerJobFromSchedulerTriggerEvent(msg.getId());
        doUnscheduleJobLocally(msg.getSchedulerJobUuid(), Collections.singletonList(msg.getSchedulerTriggerUuid()));

        SQL.New(SchedulerJobSchedulerTriggerRefVO.class)
                .eq(SchedulerJobSchedulerTriggerRefVO_.schedulerJobUuid, msg.getSchedulerJobUuid())
                .eq(SchedulerJobSchedulerTriggerRefVO_.schedulerTriggerUuid, msg.getSchedulerTriggerUuid())
                .hardDelete();

        bus.publish(evt);
    }

    private void handle(APIAddSchedulerJobToSchedulerTriggerMsg msg) {
        APIAddSchedulerJobToSchedulerTriggerEvent evt = new APIAddSchedulerJobToSchedulerTriggerEvent(msg.getId());

        SchedulerJobSchedulerTriggerRefVO vo = new SchedulerJobSchedulerTriggerRefVO();
        vo.setUuid(Platform.getUuid());
        vo.setSchedulerJobUuid(msg.getSchedulerJobUuid());
        vo.setSchedulerTriggerUuid(msg.getSchedulerTriggerUuid());
        dbf.persist(vo);

        scheduleJob(msg.getSchedulerJobUuid(), Collections.singletonList(msg.getSchedulerTriggerUuid()), msg.getTriggerNowId(), new Completion(msg) {
            @Override
            public void success() {
                evt.setInventory(SchedulerJobSchedulerTriggerInventory.valueOf(vo));
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                dbf.remove(vo);
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    @Transactional(readOnly = true)
    private String getGroupJobData(SchedulerJobDesc jobDesc) {
        if (Q.New(SchedulerJobGroupVO.class).eq(SchedulerJobGroupVO_.uuid, jobDesc.getUuid()).isExists()) {
            return jobDesc.getJobData();
        }

        String data = SQL.New("select jobgroup.jobData from SchedulerJobGroupVO jobgroup, SchedulerJobGroupJobRefVO jobref " +
                "where jobref.schedulerJobUuid = :jobUuid " +
                "and jobgroup.uuid = jobref.schedulerJobGroupUuid", String.class)
                .param("jobUuid", jobDesc.getUuid())
                .find();
        if (StringUtils.isEmpty(data)) {
            data = jobDesc.getJobData();
        }

        return data;
    }

    private AbstractSchedulerJob getSchedulerJobInstance(SchedulerJobDesc job, String jobUuid, String triggerUuid) {
        Class<? extends AbstractSchedulerJob> clz = jobClasses.get(job.getJobClassName());
        String groupJobData = getGroupJobData(job);
        AbstractSchedulerJob schedulerJob = JSONObjectUtil.toObject(groupJobData, clz);
        schedulerJob.setUuid(jobUuid);
        schedulerJob.setTriggerUuid(triggerUuid);
        return schedulerJob;
    }

    @Transactional(readOnly = true)
    private SchedulerTask prepareSchedulerTaskFromMsg(String triggerUuid, String triggerNowFireInstanceId, String uuid) {
        SchedulerJobGroupVO groupVO = Q.New(SchedulerJobGroupVO.class).eq(SchedulerJobGroupVO_.uuid, uuid).find();
        if (groupVO != null) {
            return prepareSchedulerTaskFromMsg(triggerUuid, triggerNowFireInstanceId, groupVO);
        }
        SchedulerJobVO jobVO = Q.New(SchedulerJobVO.class).eq(SchedulerJobVO_.uuid, uuid).find();
        return prepareSchedulerTaskFromMsg(triggerUuid, triggerNowFireInstanceId, jobVO);
    }

    @Transactional(readOnly = true)
    private SchedulerTask prepareSchedulerTaskFromMsg(String triggerUuid, String triggerNowFireInstanceId, SchedulerJobDesc job) {
        SchedulerTriggerVO trigger = Q.New(SchedulerTriggerVO.class).eq(SchedulerTriggerVO_.uuid, triggerUuid).find();
        SchedulerTask task = new SchedulerTask();
        task.setStartTime(trigger.getStartTime());
        task.setStopTime(trigger.getStopTime());
        task.setJobUuid(job.getUuid());
        task.setJobClassName(job.getJobClassName());
        task.setTriggerNowFireInstanceId(triggerNowFireInstanceId);

        AbstractSchedulerJob schedulerJob = getSchedulerJobInstance(job, job.getUuid(), triggerUuid);

        task.setJobData(JSONObjectUtil.toJsonString(schedulerJob));
        task.setTriggerUuid(triggerUuid);
        task.setTaskInterval(trigger.getSchedulerInterval());
        task.setTaskRepeatCount(trigger.getRepeatCount());
        task.setType(trigger.getSchedulerType());
        task.setCron(trigger.getCron());

        return task;
    }

    private void handle(APIDeleteSchedulerTriggerMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncName(msg);
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIDeleteSchedulerTriggerEvent evt = new APIDeleteSchedulerTriggerEvent(msg.getId());
                deleteSchedulerTrigger(msg.getUuid(), new Completion(chain) {
                    @Override
                    public void success() {
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "delete-scheduler-trigger-" + msg.getUuid();
            }
        });
    }

    private void deleteSchedulerTrigger(String uuid, Completion completion) {
        deleteSchedulerTriggerFromJobGroups(uuid, new Completion(completion) {
            @Override
            public void success() {
                deleteSchedulerTriggerFromJobs(uuid, new Completion(completion) {
                    @Override
                    public void success() {
                        dbf.removeByPrimaryKey(uuid, SchedulerTriggerVO.class);
                        completion.success();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void deleteSchedulerTriggerFromJobGroups(String triggerUuid, Completion completion) {
        List<String> jobGroupUuids = Q.New(SchedulerJobGroupSchedulerTriggerRefVO.class)
                .select(SchedulerJobGroupSchedulerTriggerRefVO_.schedulerJobGroupUuid)
                .eq(SchedulerJobGroupSchedulerTriggerRefVO_.schedulerTriggerUuid, triggerUuid)
                .listValues();

        if (jobGroupUuids.isEmpty()) {
            completion.success();
            return;
        }

        ErrorCodeList errList = new ErrorCodeList();
        new While<>(jobGroupUuids).all((jobGroupUuid, whileComplection) -> {
            unscheduleJobGroup(jobGroupUuid, Collections.singletonList(triggerUuid), new Completion(whileComplection) {
                @Override
                public void success() {
                    whileComplection.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    errList.getCauses().add(errorCode);
                    whileComplection.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errList.getCauses().isEmpty()) {
                    completion.fail(errList.getCauses().get(0));
                    return;
                }

                SQL.New(SchedulerJobGroupSchedulerTriggerRefVO.class)
                        .eq(SchedulerJobGroupSchedulerTriggerRefVO_.schedulerTriggerUuid, triggerUuid)
                        .hardDelete();

                completion.success();
            }
        });
    }

    private void deleteSchedulerTriggerFromJobs(String uuid, Completion completion) {
        List<String> jobUuids = getJobNotInGroupUuids(uuid);

        ErrorCodeList errList = new ErrorCodeList();
        new While<>(jobUuids).all((jobUuid, whileCompletion) -> {
            unscheduleJob(jobUuid, Collections.singletonList(uuid), new Completion(whileCompletion) {
                @Override
                public void success() {
                    whileCompletion.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    errList.getCauses().add(errorCode);
                    whileCompletion.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errList.getCauses().isEmpty()) {
                    completion.fail(errList.getCauses().get(0));
                    return;
                }

                SQL.New(SchedulerJobSchedulerTriggerRefVO.class)
                        .eq(SchedulerJobSchedulerTriggerRefVO_.schedulerTriggerUuid, uuid)
                        .delete();

                completion.success();
            }
        });
    }

    private void handle(APIUpdateSchedulerTriggerMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncName(msg);
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIUpdateSchedulerTriggerEvent evt = new APIUpdateSchedulerTriggerEvent(msg.getId());
                SchedulerTriggerVO vo = dbf.findByUuid(msg.getUuid(), SchedulerTriggerVO.class);
                boolean ifTimeUpdated = false;

                if (msg.getName() != null) {
                    vo.setName(msg.getName());
                }

                if (msg.getDescription() != null) {
                    vo.setDescription(msg.getDescription());
                }

                if (msg.getSchedulerType() != null) {
                    vo.setSchedulerType(msg.getSchedulerType());
                }

                if (vo.getSchedulerType().equals(SchedulerConstant.SIMPLE_TYPE_STRING)) {
                    Timestamp start;
                    if (msg.getStartTime() != null) {
                        start = new Timestamp(msg.getStartTime());
                        vo.setStartTime(new Timestamp(msg.getStartTime() * 1000L));
                        ifTimeUpdated = true;
                    } else {
                        start = vo.getStartTime();
                    }

                    if (msg.getStartTime() != null && msg.getStartTime() != 0 && msg.getRepeatCount() != 0) {
                        if (msg.getRepeatCount() == 1) {
                            vo.setStopTime(new Timestamp(start.getTime() * 1000L));
                        } else {
                            vo.setStopTime(new Timestamp(start.getTime() * 1000L + (long) (msg.getRepeatCount() - 1) * (long) msg.getSchedulerInterval() * 1000L));
                        }
                    }

                    if (msg.getRepeatCount() != null && !msg.getRepeatCount().equals(vo.getRepeatCount())) {
                        vo.setRepeatCount(msg.getRepeatCount());
                        ifTimeUpdated = true;
                    }

                    if (msg.getSchedulerInterval() != null && !msg.getSchedulerInterval().equals(vo.getSchedulerInterval())) {
                        vo.setSchedulerInterval(msg.getSchedulerInterval());
                        ifTimeUpdated = true;
                    }

                    if (msg.getStopTime() != null) {
                        if (msg.getStopTime() > 0) {
                            vo.setStopTime(new Timestamp(msg.getStopTime() * 1000L));
                        }
                        if (msg.getStopTime() == 0) {
                            vo.setStopTime(null);
                        }
                        ifTimeUpdated = true;
                    }
                }

                if (vo.getSchedulerType().equals(SchedulerConstant.CRON_TYPE_STRING)) {
                    if (msg.getCron() != null) {
                        vo.setCron(msg.getCron());
                        ifTimeUpdated = true;
                    }

                    if (msg.getStartTime() != null) {
                        vo.setStartTime(new Timestamp(msg.getStartTime() * 1000L));
                        ifTimeUpdated = true;
                    }

                    if (msg.getStopTime() != null) {
                        if (msg.getStopTime() > 0) {
                            vo.setStopTime(new Timestamp(msg.getStopTime() * 1000L));
                        }
                        if (msg.getStopTime() == 0) {
                            vo.setStopTime(null);
                        }
                        ifTimeUpdated = true;
                    }
                }

                vo = dbf.updateAndRefresh(vo);

                if (ifTimeUpdated) {
                    updateTriggerRelatedTask(vo);
                }

                evt.setInventory(SchedulerTriggerInventory.valueOf(vo));
                bus.publish(evt);

                chain.next();
            }

            @Override
            public String getName() {
                return "update-scheduler-trigger-" + msg.getUuid();
            }
        });
    }

    private void updateTriggerRelatedTask(SchedulerTriggerVO trigger) {
        List<String> jobUuids = getJobUuidsFromTriggerUuid(trigger.getUuid());
        if (!jobUuids.isEmpty()) {
            // FIXME: error handle
            for (String jobUuid : jobUuids) {
                rescheduleJob(jobUuid, Collections.singletonList(trigger.getUuid()));
            }
        }
        List<String> jobGroupUuids = getGroupUuidsFromTriggerUuid(trigger.getUuid());
        if (!jobGroupUuids.isEmpty()) {
            for (String jobGroupUuid : jobGroupUuids) {
                rescheduleJobGroup(jobGroupUuid, Collections.singletonList(trigger.getUuid()));
            }
        }
    }

    private void handle(APICreateSchedulerTriggerMsg msg) {
        APICreateSchedulerTriggerEvent evt = new APICreateSchedulerTriggerEvent(msg.getId());
        SchedulerTriggerVO vo = new SchedulerTriggerVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());

        Timestamp start = null;
        if (msg.getStartTime() != null) {
            start = new Timestamp(msg.getStartTime() * 1000L);
            vo.setStartTime(start);
        }

        if (msg.getSchedulerType().equals(SchedulerConstant.SIMPLE_TYPE_STRING)) {
            DebugUtils.Assert(start != null, "Simple scheduler should have start time");

            // if execute once
            if (msg.getStartTime() != 0 && msg.getRepeatCount() != 0) {
                if (msg.getRepeatCount() == 1) {
                    vo.setStopTime(start);
                } else {
                    vo.setStopTime(new Timestamp(start.getTime() + (long) (msg.getRepeatCount() - 1) * (long) msg.getSchedulerInterval() * 1000L));
                }
            }

            if (msg.getStopTime() != null) {
                vo.setStopTime(new Timestamp(msg.getStopTime() * 1000L));
            }
        }

        if (msg.getSchedulerType().equals(SchedulerConstant.CRON_TYPE_STRING)) {
            vo.setStopTime(msg.getStopTime() == null ? null : new Timestamp(msg.getStopTime() * 1000L));
        }

        vo.setRepeatCount(msg.getRepeatCount());
        vo.setSchedulerInterval(msg.getSchedulerInterval());
        vo.setSchedulerType(msg.getSchedulerType());
        vo.setAccountUuid(msg.getSession().getAccountUuid());
        vo.setCron(msg.getCron());
        dbf.persist(vo);

        evt.setInventory(SchedulerTriggerInventory.valueOf(vo));
        bus.publish(evt);
    }

    private void handle(APIChangeSchedulerStateMsg msg) {
        APIChangeSchedulerStateEvent evt = new APIChangeSchedulerStateEvent(msg.getId());

        SchedulerJobVO job = Q.New(SchedulerJobVO.class)
                .eq(SchedulerJobVO_.uuid, msg.getUuid())
                .find();

        if (job.getManagementNodeUuid() == null) {
            SchedulerJobVO vo = dbf.findByUuid(msg.getSchedulerUuid(), SchedulerJobVO.class);
            evt.setInventory(SchedulerJobInventory.valueOf(vo));
            bus.publish(evt);
            return;
        }

        ErrorCode err = allowChangeState(job, msg.getStateEvent().equals(SchedulerStateEvent.enable.toString()));
        if (err != null) {
            evt.setError(err);
            bus.publish(evt);
            return;
        }

        if (msg.getStateEvent().equals(SchedulerStateEvent.enable.toString())) {
            ResumeSchedulerJobMsg rmsg = new ResumeSchedulerJobMsg();
            rmsg.setUuid(msg.getUuid());
            bus.makeServiceIdByManagementNodeId(rmsg, SchedulerConstant.SERVICE_ID, job.getManagementNodeUuid());
            bus.send(rmsg, new CloudBusCallBack(msg) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        evt.setError(reply.getError());
                        bus.publish(evt);
                        return;
                    }

                    bus.publish(evt);
                }
            });
        } else {
            PauseSchedulerJobMsg pmsg = new PauseSchedulerJobMsg();
            pmsg.setUuid(msg.getUuid());
            bus.makeServiceIdByManagementNodeId(pmsg, SchedulerConstant.SERVICE_ID, job.getManagementNodeUuid());
            bus.send(pmsg, new CloudBusCallBack(msg) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        evt.setError(reply.getError());
                        bus.publish(evt);
                        return;
                    }

                    bus.publish(evt);
                }
            });
        }
    }

    private ErrorCode allowChangeState(SchedulerJobDesc job, boolean enable) {
        Class<? extends AbstractSchedulerJob> clz = jobClasses.get(job.getJobClassName());
        AbstractSchedulerJob schedulerJob = JSONObjectUtil.toObject(job.getJobData(), clz);
        schedulerJob.prepareJob();
        ErrorCode err = schedulerJob.allowStateChange();
        if (err == null && enable) {
            err = SchedulerJobParamCascadeUpdater.allowEnabled(schedulerJob);
        }
        return err;
    }

    private void handle(APIDeleteSchedulerJobGroupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncName(msg);
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIDeleteSchedulerJobGroupEvent evt = new APIDeleteSchedulerJobGroupEvent(msg.getId());

                deleteSchedulerJobGroup(msg.getUuid(), new Completion(chain) {
                    @Override
                    public void success() {
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "delete-scheduler-job-group" + msg.getUuid();
            }
        });
    }

    private void handle(APIDeleteSchedulerJobMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncName(msg);
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIDeleteSchedulerJobEvent evt = new APIDeleteSchedulerJobEvent(msg.getId());

                deleteSchedulerJob(msg.getUuid(), new Completion(chain) {
                    @Override
                    public void success() {
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "delete-scheduler-job-" + msg.getUuid();
            }
        });
    }

    private void deleteSchedulerJobGroup(String schedulerJobGroupUuid, Completion completion) {
        SchedulerJobGroupVO group = Q.New(SchedulerJobGroupVO.class).eq(SchedulerJobGroupVO_.uuid, schedulerJobGroupUuid).find();
        List<String> triggerUuids = group.getAddedTriggerRefs().stream().map(SchedulerJobGroupSchedulerTriggerRefVO::getSchedulerTriggerUuid).collect(Collectors.toList());
        unscheduleJobGroup(schedulerJobGroupUuid, triggerUuids, new Completion(completion) {
            @Override
            public void success() {
                List<String> schedulerJobUuids = Q.New(SchedulerJobGroupJobRefVO.class)
                        .select(SchedulerJobGroupJobRefVO_.schedulerJobUuid)
                        .eq(SchedulerJobGroupJobRefVO_.schedulerJobGroupUuid, schedulerJobGroupUuid)
                        .listValues();
                schedulerJobUuids.forEach(jobUuid -> cleanDb(jobUuid));
                dbf.removeByPrimaryKey(schedulerJobGroupUuid, SchedulerJobGroupVO.class);
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private List<String> getTriggerUuids(String jobUuid) {
        return new SQLBatchWithReturn<List<String>>() {
            @Override
            protected List<String> scripts() {
                final List<String> jobGroupUuids = q(SchedulerJobGroupJobRefVO.class)
                        .eq(SchedulerJobGroupJobRefVO_.schedulerJobUuid, jobUuid)
                        .select(SchedulerJobGroupJobRefVO_.schedulerJobGroupUuid)
                        .listValues();
                if (jobGroupUuids.isEmpty()) {
                    return q(SchedulerJobSchedulerTriggerRefVO.class)
                            .select(SchedulerJobSchedulerTriggerRefVO_.schedulerTriggerUuid)
                            .eq(SchedulerJobSchedulerTriggerRefVO_.schedulerJobUuid, jobUuid)
                            .listValues();
                }

                return q(SchedulerJobGroupSchedulerTriggerRefVO.class)
                        .select(SchedulerJobGroupSchedulerTriggerRefVO_.schedulerTriggerUuid)
                        .in(SchedulerJobGroupSchedulerTriggerRefVO_.schedulerJobGroupUuid, jobGroupUuids)
                        .listValues();
            }
        }.execute();
    }

    private Map<String, List<String>> getEnabledGroupJobUuids(List<String> groupUuids, List<String> designatedJobUuids) {
        List<Tuple> tuples = SQL.New("select ref.schedulerJobGroupUuid, ref.schedulerJobUuid" +
                        " from SchedulerJobVO job, SchedulerJobGroupJobRefVO ref" +
                        " where ref.schedulerJobGroupUuid in :groupUuids" +
                        " and ref.schedulerJobUuid = job.uuid" +
                        " and job.state = :state", Tuple.class)
                .param("groupUuids", groupUuids)
                .param("state", SchedulerState.Enabled.toString())
                .list();
        Map<String, List<String>> groupJobUuids = new HashMap<>();
        tuples.forEach(t -> groupJobUuids.computeIfAbsent(t.get(0, String.class), k -> new ArrayList<>()).add(t.get(1, String.class)));
        groupJobUuids.forEach((k, v) -> Optional.ofNullable(designatedJobUuids).ifPresent(v::retainAll));
        return groupJobUuids;
    }

    @Transactional(readOnly = true)
    public List<String> getEnabledJobUuids(String triggerUuid) {
        List<String> jobUuids = SQL.New("select job.uuid" +
                " from SchedulerJobVO job, SchedulerJobSchedulerTriggerRefVO ref" +
                " where ref.schedulerTriggerUuid = :triggerUuid" +
                " and ref.schedulerJobUuid = job.uuid" +
                " and job.state = :state", String.class)
                .param("triggerUuid", triggerUuid)
                .param("state", SchedulerState.Enabled.toString())
                .list();

        List<String> jobGroupUuids = getEnabledGroupUuidsFromTriggerUuid(triggerUuid);

        if (jobGroupUuids.isEmpty()) {
            return jobUuids;
        }

        List<String> jobInGroupUuids = SQL.New("select job.uuid" +
                " from SchedulerJobVO job, SchedulerJobGroupJobRefVO ref" +
                " where ref.schedulerJobGroupUuid in :groupUuids" +
                " and ref.schedulerJobUuid = job.uuid" +
                " and job.state = :state", String.class)
                .param("groupUuids", jobGroupUuids)
                .param("state", SchedulerState.Enabled.toString())
                .list();

        jobUuids.removeAll(jobInGroupUuids);
        return jobUuids.stream().distinct().collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getEnabledGroupUuidsFromTriggerUuid(String triggerUuid) {
        return SQL.New("select jobGroup.uuid" +
                " from SchedulerJobGroupVO jobGroup, SchedulerJobGroupSchedulerTriggerRefVO ref" +
                " where ref.schedulerTriggerUuid = :triggerUuid" +
                " and ref.schedulerJobGroupUuid = jobGroup.uuid" +
                " and jobGroup.state = :state", String.class)
                .param("triggerUuid", triggerUuid)
                .param("state", SchedulerJobGroupState.Enabled.toString())
                .list();
    }

    @Transactional(readOnly = true)
    public List<String> getJobUuids(String triggerUuid) {
        List<String> jobUuids = getJobUuidsFromTriggerUuid(triggerUuid);

        List<String> jobGroupUuids = getGroupUuidsFromTriggerUuid(triggerUuid);

        if (jobGroupUuids.isEmpty()) {
            return jobUuids;
        }

        List<String> jobInGroupUuids = getJobInGroupUuids(jobGroupUuids);

        jobUuids.addAll(jobInGroupUuids);
        return jobUuids.stream().distinct().collect(Collectors.toList());
    }

    public List<String> getJobNotInGroupUuids(String triggerUuid) {
        List<String> jobUuids = getJobUuidsFromTriggerUuid(triggerUuid);

        List<String> jobGroupUuids = getGroupUuidsFromTriggerUuid(triggerUuid);

        if (jobGroupUuids.isEmpty()) {
            return jobUuids;
        }

        List<String> jobInGroupUuids = getJobInGroupUuids(jobGroupUuids);

        jobUuids.removeAll(jobInGroupUuids);
        return jobUuids.stream().distinct().collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public static List<String> getJobUuidsFromTriggerUuid(String triggerUuid) {
        return Q.New(SchedulerJobSchedulerTriggerRefVO.class)
                .select(SchedulerJobSchedulerTriggerRefVO_.schedulerJobUuid)
                .eq(SchedulerJobSchedulerTriggerRefVO_.schedulerTriggerUuid, triggerUuid)
                .listValues();
    }

    @Transactional(readOnly = true)
    public static List<String> getGroupUuidsFromTriggerUuid(String triggerUuid) {
        return Q.New(SchedulerJobGroupSchedulerTriggerRefVO.class)
                .select(SchedulerJobGroupSchedulerTriggerRefVO_.schedulerJobGroupUuid)
                .eq(SchedulerJobGroupSchedulerTriggerRefVO_.schedulerTriggerUuid, triggerUuid)
                .listValues();
    }

    @Transactional(readOnly = true)
    public static List<String> getJobInGroupUuids(List<String> jobGroupUuids) {
        return Q.New(SchedulerJobGroupJobRefVO.class)
                .in(SchedulerJobGroupJobRefVO_.schedulerJobGroupUuid, jobGroupUuids)
                .select(SchedulerJobGroupJobRefVO_.schedulerJobUuid)
                .listValues();
    }

    private void deleteSchedulerJob(String jobUuid, Completion completion) {
        if (jobInGroup(jobUuid)) {
            cleanDb(jobUuid);
            completion.success();
            return;
        }

        List<String> triggerUuids = getTriggerUuids(jobUuid);
        unscheduleJob(jobUuid, triggerUuids, new Completion(completion) {
            @Override
            public void success() {
                cleanDb(jobUuid);
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                // FIXME: multi triggers, some has been deleted.
                completion.fail(errorCode);
            }
        });
    }

    @Transactional
    private void cleanDb(String jobUuid) {
        SQL.New(SchedulerJobSchedulerTriggerRefVO.class)
                .eq(SchedulerJobSchedulerTriggerRefVO_.schedulerJobUuid, jobUuid)
                .delete();
        SQL.New(SchedulerJobGroupJobRefVO.class)
                .eq(SchedulerJobGroupJobRefVO_.schedulerJobUuid, jobUuid)
                .delete();
        SQL.New(SchedulerJobVO.class)
                .eq(SchedulerJobVO_.uuid, jobUuid)
                .delete();
    }

    private void handle(APIUpdateSchedulerJobGroupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            class Params {
                List<String> triggerUuids;
            }

            @Override
            public String getSyncSignature() {
                return getThreadSyncName(msg);
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIUpdateSchedulerJobGroupEvent evt = new APIUpdateSchedulerJobGroupEvent(msg.getId());
                SchedulerJobGroupVO vo = dbf.findByUuid(msg.getUuid(), SchedulerJobGroupVO.class);
                boolean updated = false;
                boolean reschedule = false;
                boolean changeState = false;

                if (msg.getName() != null) {
                    vo.setName(msg.getName());
                    updated = true;
                }

                if (msg.getDescription() != null) {
                    vo.setDescription(msg.getDescription());
                    updated = true;
                }

                if (msg.getParameters() != null && !msg.getParameters().isEmpty()) {
                    Class<? extends AbstractSchedulerJob> clz = jobClasses.get(vo.getJobClassName());
                    AbstractSchedulerJob schedulerJob = JSONObjectUtil.toObject(vo.getJobData(), clz);
                    schedulerJob.updateSchedulerJob(msg.getParameters());
                    vo.setJobData(JSONObjectUtil.toJsonString(schedulerJob));
                    updated = true;
                    reschedule = true;
                }

                if (msg.getState() != null) {
                    ErrorCode err = allowChangeState(vo, msg.getState().equals(SchedulerJobGroupState.Enabled.toString()));
                    if (err != null) {
                        evt.setError(err);
                        bus.publish(evt);
                        chain.next();
                        return;
                    }
                    changeState = !msg.getState().equals(vo.getState());
                    vo.setState(msg.getState());
                    updated = true;
                }

                if (updated) {
                    vo = dbf.updateAndRefresh(vo);
                }

                if (changeState) {
                    Params params = prepareParams();
                    changeGroupState(msg.getSchedulerJobGroupUuid(), params.triggerUuids, msg.getState());
                } else if (reschedule) {
                    Params params = prepareParams();
                    rescheduleJobGroup(msg.getSchedulerJobGroupUuid(), params.triggerUuids);
                }

                evt.setInventory(SchedulerJobGroupInventory.valueOf(vo));
                bus.publish(evt);

                chain.next();
            }

            @Override
            public String getName() {
                return "update-scheduler-job-" + msg.getUuid();
            }

            @Transactional(readOnly = true)
            private Params prepareParams() {
                Params result = new Params();
                result.triggerUuids = Q.New(SchedulerJobGroupSchedulerTriggerRefVO.class)
                        .eq(SchedulerJobGroupSchedulerTriggerRefVO_.schedulerJobGroupUuid, msg.getUuid())
                        .select(SchedulerJobGroupSchedulerTriggerRefVO_.schedulerTriggerUuid)
                        .listValues();
                return result;
            }
        });
    }

    private void changeGroupState(String groupUuid, List<String> triggerUuids, String state) {
        Completion updateCompletion = new Completion(null) {
            @Override
            public void success() {
                SQL.New(SchedulerJobGroupVO.class).eq(SchedulerJobGroupVO_.uuid, groupUuid)
                        .set(SchedulerJobGroupVO_.state, state)
                        .update();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("scheduler job group[uuid:%s] change state fail, because %s",
                        groupUuid, errorCode.getDetails()));
            }
        };

        if (SchedulerJobGroupState.Disabled.toString().equals(state)) {
            unscheduleJobGroup(groupUuid, triggerUuids, updateCompletion);
        } else {
            scheduleJobGroup(groupUuid, triggerUuids, null, updateCompletion);
        }
    }

    private void handle(APIUpdateSchedulerJobMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncName(msg);
            }

            @Override
            public void run(SyncTaskChain chain) {
                SchedulerJobVO vo = dbf.findByUuid(msg.getUuid(), SchedulerJobVO.class);
                boolean updated = false;
                boolean reschedule = false;

                if (msg.getName() != null) {
                    vo.setName(msg.getName());
                    updated = true;
                }

                if (msg.getDescription() != null) {
                    vo.setDescription(msg.getDescription());
                    updated = true;
                }

                if (msg.getParameters() != null && !msg.getParameters().isEmpty()) {
                    Class<? extends AbstractSchedulerJob> clz = jobClasses.get(vo.getJobClassName());
                    AbstractSchedulerJob schedulerJob = JSONObjectUtil.toObject(vo.getJobData(), clz);
                    schedulerJob.updateSchedulerJob(msg.getParameters());
                    vo.setJobData(JSONObjectUtil.toJsonString(schedulerJob));
                    updated = true;
                    reschedule = true;
                }

                if (updated) {
                    vo = dbf.updateAndRefresh(vo);
                }

                if (reschedule) {
                    rescheduleJob(vo);
                }

                APIUpdateSchedulerJobEvent evt = new APIUpdateSchedulerJobEvent(msg.getId());
                evt.setInventory(SchedulerJobInventory.valueOf(vo));
                bus.publish(evt);

                chain.next();
            }

            @Override
            public String getName() {
                return "update-scheduler-job-" + msg.getUuid();
            }
        });
    }

    private void rescheduleJob(SchedulerJobVO job) {
        List<String> triggerUuids = getTriggerUuids(job.getUuid());
        rescheduleJob(job.getUuid(), triggerUuids);
    }

    private void rescheduleJob(String jobUuid, List<String> triggerUuids) {
        if (triggerUuids.isEmpty()) {
            return;
        }

        logger.debug(String.format("start to reschedule updated job[uuid:%s], triggerUuids: %s", jobUuid, triggerUuids));
        unscheduleJob(jobUuid, triggerUuids, new Completion(null) {
            @Override
            public void success() {
                scheduleJob(jobUuid, triggerUuids, null, new NopeCompletion());
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.error(String.format("failed to delete scheduler job[uuid:%s] with trigger[uuids:%s]",
                        jobUuid, triggerUuids));
            }
        });
    }

    private void rescheduleJobGroup(String jobGroupUuid, List<String> triggerUuids) {
        if (triggerUuids.isEmpty()) {
            return;
        }

        logger.debug(String.format("start to reschedule updated job group[uuid:%s], triggerUuids: %s", jobGroupUuid, triggerUuids));
        unscheduleJobGroup(jobGroupUuid, triggerUuids, new Completion(null) {
            @Override
            public void success() {
                scheduleJobGroup(jobGroupUuid, triggerUuids, null, new NopeCompletion());
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.error(String.format("failed to delete scheduler job group[uuid:%s] with trigger[uuids:%s]",
                        jobGroupUuid, triggerUuids));
            }
        });
    }

    public String getId() {
        return bus.makeLocalServiceId(SchedulerConstant.SERVICE_ID);
    }

    private void populateSchedulerJobClasses() {
        Platform.getReflections()
                .getSubTypesOf(AbstractSchedulerJob.class)
                .forEach(clazz -> jobClasses.put(clazz.getCanonicalName(), clazz));
    }

    public boolean start() {
        SchedulerJobParamCascadeUpdater.init();
        try {
            populateSchedulerJobFactories();
            populateSchedulerJobClasses();
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
        } catch (SchedulerException e) {
            logger.warn("Start Scheduler failed!");
            throw new RuntimeException(e);
        }

        return true;
    }

    public boolean stop() {
        try {
            scheduler.shutdown();
        } catch (SchedulerException e) {
            logger.warn("Stop Scheduler failed!");
            throw new RuntimeException(e);
        }
        return true;
    }

    @Override
    public void pauseSchedulerJob(String uuid) {
        logger.debug(String.format("Scheduler [uuid:%s] will change state to Disabled", uuid));
        SQL.New(SchedulerJobVO.class)
                .eq(SchedulerJobVO_.uuid, uuid)
                .set(SchedulerJobVO_.state, SchedulerState.Disabled.toString())
                .update();
        if (jobInGroup(uuid)) {
            return;
        }
        List<String> triggerUuids = getTriggerUuids(uuid);
        doUnscheduleJobLocally(uuid, triggerUuids);
    }

    @Override
    public void resumeSchedulerJob(String uuid) {
        logger.debug(String.format("Scheduler job[uuid:%s] will change state to Enabled", uuid));
        SQL.New(SchedulerJobVO.class)
                .eq(SchedulerJobVO_.uuid, uuid)
                .set(SchedulerJobVO_.state, SchedulerState.Enabled.toString())
                .update();
        if (jobInGroup(uuid)) {
            return;
        }
        List<String> triggerUuids = getTriggerUuids(uuid);
        doScheduleJobLocally(uuid, triggerUuids, null);
    }

    public void deleteSchedulerJobByResourceUuid(String uuid, Completion completion) {
        List<String> jobUuids = Q.New(SchedulerJobVO.class)
                .select(SchedulerJobVO_.uuid)
                .eq(SchedulerJobVO_.targetResourceUuid, uuid)
                .listValues();

        if (jobUuids.isEmpty()) {
            completion.success();
            return;
        }

        List<ErrorCode> errs = new ArrayList<>();
        new While<>(jobUuids).each((jobUuid, compl) -> {
            deleteSchedulerJob(jobUuid, new Completion(compl) {
                @Override
                public void success() {
                    compl.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    errs.add(errorCode);
                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errs.isEmpty()) {
                    completion.success();
                } else {
                    completion.fail(errs.get(0));
                }
            }
        });
    }

    @Override
    public void handleJobUpdated(List<String> jobUuids, List<String> groupUuids, boolean needDisable) {
        for (String groupUuid : groupUuids) {
            List<String> jobUuidsInGroup = Q.New(SchedulerJobGroupJobRefVO.class)
                    .eq(SchedulerJobGroupJobRefVO_.schedulerJobGroupUuid, groupUuid)
                    .select(SchedulerJobGroupJobRefVO_.schedulerJobUuid)
                    .listValues();
            List<String> triggerUuids = Q.New(SchedulerJobGroupSchedulerTriggerRefVO.class)
                    .select(SchedulerJobGroupSchedulerTriggerRefVO_.schedulerTriggerUuid)
                    .eq(SchedulerJobGroupSchedulerTriggerRefVO_.schedulerJobGroupUuid, groupUuid)
                    .listValues();
            jobUuids.removeAll(jobUuidsInGroup);

            if (needDisable) {
                changeGroupState(groupUuid, triggerUuids, SchedulerJobGroupState.Disabled.toString());
            } else {
                rescheduleJobGroup(groupUuid, triggerUuids);
            }
        }

        // jobs not in group
        for (String jobUuid : jobUuids) {
            List<String> triggerUuids = Q.New(SchedulerJobSchedulerTriggerRefVO.class)
                    .select(SchedulerJobSchedulerTriggerRefVO_.schedulerTriggerUuid)
                    .eq(SchedulerJobSchedulerTriggerRefVO_.schedulerJobUuid, jobUuid)
                    .listValues();

            if (needDisable) {
                unscheduleJob(jobUuid, triggerUuids, new Completion(null) {
                    @Override
                    public void success() {
                        SQL.New(SchedulerJobVO.class).eq(SchedulerJobVO_.uuid, jobUuid)
                                .set(SchedulerJobVO_.state, SchedulerState.Disabled.toString())
                                .update();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.warn(String.format("disable scheduler job[uuid:%s] failed", jobUuid));
                    }
                });
            } else {
                rescheduleJob(jobUuid, triggerUuids);
            }
        }
    }

    @Override
    public List<String> getResourceSchedulerJobTypes(String resourceUuid) {
        if (resourceUuid == null) {
            return null;
        }

        List<String> schedulerJobClassNames = Q.New(SchedulerJobVO.class)
                .eq(SchedulerJobVO_.targetResourceUuid, resourceUuid)
                .select(SchedulerJobVO_.jobClassName)
                .listValues();

        if (CollectionUtils.isEmpty(schedulerJobClassNames)) {
            return null;
        }

        List<String> jobTypes = new ArrayList<>();
        for (SchedulerJobFactory ext : pluginRgty.getExtensionList(SchedulerJobFactory.class)) {
            if (schedulerJobClassNames.contains(ext.getJobClassName())) {
                jobTypes.add(ext.getJobType());
            }
        }
        return jobTypes;

    }

    private void loadSchedulerManagedByUs(boolean skipManaged) {
        List<String> jobsInGroup = Q.New(SchedulerJobGroupJobRefVO.class).select(SchedulerJobGroupJobRefVO_.schedulerJobUuid).listValues();

        long count;
        if (jobsInGroup.isEmpty()) {
            count = Q.New(SchedulerJobVO.class).count();
        } else {
            count = SQL.New("select count(uuid) from SchedulerJobVO where uuid not in (:jobsInGroup)", Long.class)
                .param("jobsInGroup", new HashSet<>(jobsInGroup)).find();
        }

        SQL.New("select vo from SchedulerJobVO vo where vo.uuid not in (:jobsInGroup)", SchedulerJobVO.class)
                .param("jobsInGroup", new HashSet<>(jobsInGroup)).limit(1000).paginate(count, (List<SchedulerJobVO> jobs) -> {
            HashMap<String, List<SchedulerJobVO>> ours = new HashMap<>();
            for (SchedulerJobVO job : jobs) {
                if (skipManaged && getManagementServerId().equals(job.getManagementNodeUuid())) {
                    continue;
                }

                if (destinationMaker.isManagedByUs(job.getUuid())) {
                    ours.computeIfAbsent(job.getManagementNodeUuid(), k -> new ArrayList<>()).add(job);
                    job.setManagementNodeUuid(getManagementServerId());
                }
            }

            ours.values().forEach(it -> dbf.updateCollection(it));
            for (Map.Entry<String, List<SchedulerJobVO>> e : ours.entrySet()) {
                jobLoaderFromMN(e.getValue(), e.getKey());
            }
        });

        long groupCount = dbf.count(SchedulerJobGroupVO.class);
        SQL.New("select vo from SchedulerJobGroupVO vo", SchedulerJobGroupVO.class).limit(1000).paginate(groupCount, (List<SchedulerJobGroupVO> groupJobs) -> {
            HashMap<String, List<SchedulerJobGroupVO>> ours = new HashMap<>();
            for (SchedulerJobGroupVO group : groupJobs) {
                if (skipManaged && getManagementServerId().equals(group.getManagementNodeUuid())) {
                    continue;
                }

                if (destinationMaker.isManagedByUs(group.getUuid())) {
                    ours.computeIfAbsent(group.getManagementNodeUuid(), k -> new ArrayList<>()).add(group);
                    group.setManagementNodeUuid(getManagementServerId());
                }
            }

            ours.values().forEach(it -> dbf.updateCollection(it));
            for (Map.Entry<String, List<SchedulerJobGroupVO>> e : ours.entrySet()) {
                groupLoaderFromMN(e.getValue(), e.getKey());
            }
        });
    }

    private String findJobGroupState(String jobUuid) {
        return SQL.New("select jobgroup.state from SchedulerJobGroupVO jobgroup, SchedulerJobGroupJobRefVO jobref " +
                "where jobref.schedulerJobUuid = :jobUuid " +
                "and jobgroup.uuid = jobref.schedulerJobGroupUuid", String.class)
                .param("jobUuid", jobUuid)
                .find();
    }

    private boolean isJobPermittedByGroup(String jobUuid) {
        String jobGroupState = findJobGroupState(jobUuid);

        if (jobGroupState == null) {
            return true;
        }

        return jobGroupState.equals(SchedulerJobGroupState.Enabled.toString());
    }

    private boolean isJobGroupPermitted(String jobGroupUuid) {
        String state = Q.New(SchedulerJobGroupVO.class).eq(SchedulerJobGroupVO_.uuid, jobGroupUuid)
                .select(SchedulerJobGroupVO_.state).findValue();
        return Objects.equals(state, SchedulerJobGroupState.Enabled.toString());
    }

    @Transactional(readOnly = true)
    private boolean isJobPermitted(String jobUuid) {
        String state = Q.New(SchedulerJobVO.class).eq(SchedulerJobVO_.uuid, jobUuid).select(SchedulerJobVO_.state).findValue();
        return !SchedulerState.Disabled.toString().equals(state);
    }

    private void jobLoaderFromMN(List<SchedulerJobVO> ours, String mnId) {
        if (ours.isEmpty()) {
            logger.debug("no Scheduler managed by us");
        } else {
            List<SchedulerJobVO> enableJobVOS = ours.stream()
                    .filter(it -> SchedulerState.Enabled.toString().equals(it.getState()))
                    .collect(Collectors.toList());

            if (mnId == null) {
                logger.debug("Scheduler is going to clean untracked scheduler history");
                AbstractSchedulerJob.cleanStaleResult(ours.stream().map(ResourceVO::getUuid).collect(Collectors.toList()));
            }

            logger.debug(String.format("Scheduler is going to load %s jobs", enableJobVOS.size()));

            for (SchedulerJobVO job : enableJobVOS) {
                List<String> triggerUuids = getTriggerUuids(job.getUuid());
                if (triggerUuids.isEmpty()) {
                    continue;
                }

                if (mnId != null && !mnId.equals(getManagementServerId())) {
                    logger.debug(String.format("unschedule job[uuid:%s] from MN[id:%s]", job.getUuid(), mnId));
                    UnscheduleJobMsg msg = new UnscheduleJobMsg();
                    msg.setSchedulerJobUuid(job.getUuid());
                    msg.setSchedulerTriggerUuids(triggerUuids);
                    bus.makeServiceIdByManagementNodeId(msg, SchedulerConstant.SERVICE_ID, mnId);
                    bus.send(msg);
                }

                List<SchedulerTriggerVO> triggerVOS = dbf.listByPrimaryKeys(triggerUuids, SchedulerTriggerVO.class);
                for (SchedulerTriggerVO trigger : triggerVOS) {
                    SchedulerTask task = new SchedulerTask();
                    task.setStartTime(trigger.getStartTime());
                    task.setStopTime(trigger.getStopTime());
                    task.setTriggerUuid(trigger.getUuid());
                    task.setType(trigger.getSchedulerType());
                    task.setTaskRepeatCount(trigger.getRepeatCount());
                    task.setTaskInterval(trigger.getSchedulerInterval());
                    task.setJobUuid(job.getUuid());

                    AbstractSchedulerJob schedulerJob = getSchedulerJobInstance(job, job.getUuid(), trigger.getUuid());

                    task.setJobData(JSONObjectUtil.toJsonString(schedulerJob));
                    task.setJobClassName(job.getJobClassName());
                    task.setCron(trigger.getCron());

                    ErrorCode err = runScheduler(task);
                    if (err != null) {
                        logger.error(String.format("schedule job[uuid:%s] with trigger[uuid:%s] fails!" +
                                " please reschedule it manually", job.getUuid(), trigger.getUuid()));
                    }
                }
            }
        }
    }

    private void groupLoaderFromMN(List<SchedulerJobGroupVO> ours, String mnId) {
        if (ours.isEmpty()) {
            logger.debug("no scheduler group managed by us");
            return;
        }

        List<SchedulerJobGroupVO> enableGroupVOS = ours.stream()
                .filter(it -> SchedulerState.Enabled.toString().equals(it.getState()))
                .collect(Collectors.toList());

        if (mnId == null) {
            logger.debug("scheduler group is going to clean untracked scheduler history");
            List<String> jobUuids = Q.New(SchedulerJobGroupJobRefVO.class)
                    .in(SchedulerJobGroupJobRefVO_.schedulerJobGroupUuid, ours.stream().map(SchedulerJobGroupVO::getUuid).collect(Collectors.toList()))
                    .select(SchedulerJobGroupJobRefVO_.schedulerJobUuid).listValues();
            AbstractSchedulerJob.cleanStaleResult(jobUuids.stream().distinct().collect(Collectors.toList()));
        }

        logger.debug(String.format("scheduler is going to load %s groups", enableGroupVOS.size()));

        for (SchedulerJobGroupVO group : enableGroupVOS) {
            List<String> triggerUuids = group.getAddedTriggerRefs()
                    .stream().map(SchedulerJobGroupSchedulerTriggerRefVO::getSchedulerTriggerUuid).collect(Collectors.toList());
            if (triggerUuids.isEmpty()) {
                continue;
            }

            if (mnId != null && !mnId.equals(getManagementServerId())) {
                logger.debug(String.format("unschedule job group[uuid:%s] from MN[id:%s]", group.getUuid(), mnId));
                UnscheduleJobGroupMsg msg = new UnscheduleJobGroupMsg();
                msg.setSchedulerJobGroupUuid(group.getUuid());
                msg.setSchedulerTriggerUuids(triggerUuids);
                bus.makeServiceIdByManagementNodeId(msg, SchedulerConstant.SERVICE_ID, mnId);
                bus.send(msg);
            }

            SchedulerJobGroupVO groupVO = new SchedulerJobGroupVO();
            groupVO.setUuid(group.getUuid());
            groupVO.setJobClassName(group.getJobClassName());
            groupVO.setJobData(group.getJobData());
            doRunScheduler(groupVO, triggerUuids);
        }
    }

    private void doRunScheduler(SchedulerJobDesc job, List<String> triggerUuids){
        List<SchedulerTriggerVO> triggerVOS = dbf.listByPrimaryKeys(triggerUuids, SchedulerTriggerVO.class);
        for (SchedulerTriggerVO trigger : triggerVOS) {
            SchedulerTask task = new SchedulerTask();
            task.setStartTime(trigger.getStartTime());
            task.setTriggerUuid(trigger.getUuid());
            task.setType(trigger.getSchedulerType());
            task.setTaskRepeatCount(trigger.getRepeatCount());
            task.setTaskInterval(trigger.getSchedulerInterval());
            task.setJobUuid(job.getUuid());

            AbstractSchedulerJob schedulerJob = getSchedulerJobInstance(job, job.getUuid(), trigger.getUuid());

            task.setJobData(JSONObjectUtil.toJsonString(schedulerJob));
            task.setJobClassName(job.getJobClassName());
            task.setCron(trigger.getCron());

            ErrorCode err = runScheduler(task);
            if (err != null) {
                logger.error(String.format("schedule job[uuid:%s] with trigger[uuid:%s] fails!" +
                        " please reschedule it manually", job.getUuid(), trigger.getUuid()));
            }
        }
    }

    private void loadSchedulerJobs() {
        loadSchedulerManagedByUs(false);
    }

    private void takeOverScheduler() {
        loadSchedulerManagedByUs(true);
    }

    @Override
    public ErrorCode runScheduler(SchedulerTask schedulerJob) {
        logger.debug(String.format("Starting to generate Scheduler job[uuid:%s, type:%s]", schedulerJob.getJobUuid(), schedulerJob.getJobClassName()));
        boolean triggerNow = schedulerJob.isTriggerNow();
        String jobData = schedulerJob.getJobData();
        String jobClassName = schedulerJob.getJobClassName();

        if (schedulerJob.getStartTime() == null && schedulerJob.getType().equals(SchedulerConstant.SIMPLE_TYPE_STRING)) {
            triggerNow = false;
        }
        try {
            JobDetail job = newJob(SchedulerRunner.class)
                    .withIdentity(schedulerJob.getJobUuid(), schedulerJob.getTriggerUuid())
                    .usingJobData("jobClassName", jobClassName)
                    .usingJobData("jobData", jobData)
                    .build();

            String triggerGroup = schedulerJob.getTriggerUuid() + "." + schedulerJob.getJobUuid();
            String triggerId = schedulerJob.getTriggerUuid();

            // use triggerUuid.jobUuid as the key of a new trigger and jobUuid as group name
            // to support that several jobs use same trigger
            if (schedulerJob.getType().equals("simple")) {
                Trigger trigger;
                if (schedulerJob.getTaskRepeatCount() != 0) {
                    if (schedulerJob.getTaskRepeatCount() == 1) {
                        //repeat only once, ignore interval
                        if (schedulerJob.getStartTime() == null) {
                            trigger = newTrigger()
                                    .withIdentity(triggerId, triggerGroup)
                                    .withSchedule(simpleSchedule()
                                            .withMisfireHandlingInstructionNextWithRemainingCount())
                                    .build();
                        } else {
                            trigger = newTrigger()
                                    .withIdentity(triggerId, triggerGroup)
                                    .startAt(schedulerJob.getStartTime())
                                    .withSchedule(simpleSchedule()
                                            .withMisfireHandlingInstructionNextWithRemainingCount())
                                    .build();
                        }

                    } else {
                        //repeat more than once
                        if (schedulerJob.getStartTime() == null) {
                            trigger = newTrigger()
                                    .withIdentity(triggerId, triggerGroup)
                                    .withSchedule(simpleSchedule()
                                            .withIntervalInSeconds(schedulerJob.getTaskInterval())
                                            .withRepeatCount(schedulerJob.getTaskRepeatCount() - 1)
                                            .withMisfireHandlingInstructionNextWithRemainingCount())
                                    .build();
                        } else {
                            trigger = newTrigger()
                                    .withIdentity(triggerId, triggerGroup)
                                    .startAt(schedulerJob.getStartTime())
                                    .withSchedule(simpleSchedule()
                                            .withIntervalInSeconds(schedulerJob.getTaskInterval())
                                            .withRepeatCount(schedulerJob.getTaskRepeatCount() - 1)
                                            .withMisfireHandlingInstructionNextWithRemainingCount())
                                    .build();
                        }
                    }
                } else {
                    TriggerBuilder<Trigger> triggerBuilder = newTrigger();
                    if (schedulerJob.getStopTime() != null) {
                        triggerBuilder.endAt(schedulerJob.getStopTime());
                    }
                    if (schedulerJob.getStartTime() == null) {
                        trigger = triggerBuilder.withIdentity(triggerId, triggerGroup)
                                .withSchedule(simpleSchedule()
                                        .withIntervalInSeconds(schedulerJob.getTaskInterval())
                                        .repeatForever()
                                        .withMisfireHandlingInstructionNextWithRemainingCount())
                                .build();
                    } else {
                        trigger = triggerBuilder.withIdentity(triggerId, triggerGroup)
                                .startAt(schedulerJob.getStartTime())
                                .withSchedule(simpleSchedule()
                                        .withIntervalInSeconds(schedulerJob.getTaskInterval())
                                        .repeatForever()
                                        .withMisfireHandlingInstructionNextWithRemainingCount())
                                .build();
                    }
                }

                scheduler.scheduleJob(job, trigger);
            } else if (schedulerJob.getType().equals(SchedulerConstant.CRON_TYPE_STRING)) {
                TriggerBuilder<CronTrigger> tb = newTrigger()
                        .withIdentity(triggerId, triggerGroup)
                        .withSchedule(cronSchedule(schedulerJob.getCron())
                                .withMisfireHandlingInstructionDoNothing());

                if (schedulerJob.getStartTime() != null) {
                    tb = tb.startAt(schedulerJob.getStartTime());
                }

                if (schedulerJob.getStopTime() != null) {
                    tb = tb.endAt(schedulerJob.getStopTime());
                }

                CronTrigger trigger = tb.build();
                scheduler.scheduleJob(job, trigger);
            }

            if (triggerNow) {
                scheduler.triggerJob(job.getKey(), new JobDataMap(Collections.singletonMap(SchedulerConstant.FIRE_INSTANCE_ID, schedulerJob.getTriggerNowFireInstanceId())));
            }
        } catch (SchedulerException se) {
            return inerr("Run Scheduler task by job[uuid:%s] and trigger[uuid:%s] failed, because %s",
                    schedulerJob.getJobUuid(), schedulerJob.getTriggerUuid(), se.getMessage());
        }

        return null;
    }

    @AsyncThread
    @Override
    public void managementNodeReady() {
        logger.debug(String.format("Management node[uuid:%s] joins, start loading Scheduler jobs...", Platform.getManagementServerId()));

        loadSchedulerJobs();
        AbstractSchedulerJob.cleanStaleResult();
    }

    @Override
    public void nodeJoin(ManagementNodeInventory inv) {

    }

    @Override
    @SyncThread
    public void nodeLeft(ManagementNodeInventory inv) {
        logger.debug(String.format("Management node[uuid:%s] left, node[uuid:%s] starts to take over schedulers", inv.getUuid(), Platform.getManagementServerId()));
        takeOverScheduler();
    }

    @Override
    public void iAmDead(ManagementNodeInventory inv) {

    }

    @Override
    public void iJoin(ManagementNodeInventory inv) {

    }

    public void vmStateChanged(VmInstanceInventory vm, VmInstanceState oldState, VmInstanceState newState) {
        if (oldState == VmInstanceState.Running && newState == VmInstanceState.Unknown) {
            pauseSchedulerJobsByTargetResourceUuid(vm.getUuid());
        } else if (oldState == VmInstanceState.Unknown && newState == VmInstanceState.Running) {
            resumeSchedulerJobsByTargetResourceUuid(vm.getUuid());
        }
    }

    public String preDestroyVm(VmInstanceInventory inv) {
        return null;
    }

    public void beforeDestroyVm(VmInstanceInventory inv) {
        logger.debug(String.format("will pause scheduler before destroy vm %s", inv.getUuid()));
        pauseSchedulerJobsByTargetResourceUuid(inv.getUuid());
    }

    private void resumeSchedulerJobsByTargetResourceUuid(String targetResourceUuid) {
        List<Tuple> Tuples = Q.New(SchedulerJobVO.class)
                .select(SchedulerJobVO_.uuid, SchedulerJobVO_.managementNodeUuid)
                .eq(SchedulerJobVO_.targetResourceUuid, targetResourceUuid)
                .notNull(SchedulerJobVO_.managementNodeUuid)
                .listTuple();

        if (Tuples.isEmpty()) {
            logger.debug(String.format("vm [uuid:%s] not set any scheduler", targetResourceUuid));
            return;
        }

        new While<>(Tuples).all((tuple, completion) -> {
            String jobUuid = tuple.get(0, String.class);
            if (!isJobPermittedByGroup(jobUuid)) {
                logger.debug(String.format("job[uuid:%s] is inhibited by its group", jobUuid));
                completion.done();
                return;
            }

            ResumeSchedulerJobMsg msg = new ResumeSchedulerJobMsg();
            msg.setUuid(jobUuid);
            bus.makeServiceIdByManagementNodeId(msg, SchedulerConstant.SERVICE_ID, (String) tuple.get(1));
            bus.send(msg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.debug(String.format("failed to change scheduler job[uuid:%s] state", (String) tuple.get(0)));
                    }
                    completion.done();
                }
            });
        }).run(new NopeWhileDoneCompletion());
    }

    private void pauseSchedulerJobsByTargetResourceUuid(String targetResourceUuid) {
        List<Tuple> Tuples = Q.New(SchedulerJobVO.class)
                .select(SchedulerJobVO_.uuid, SchedulerJobVO_.managementNodeUuid)
                .eq(SchedulerJobVO_.targetResourceUuid, targetResourceUuid)
                .notNull(SchedulerJobVO_.managementNodeUuid)
                .listTuple();

        if (Tuples.isEmpty()) {
            logger.debug(String.format("vm [uuid:%s] not set any scheduler", targetResourceUuid));
            return;
        }

        new While<>(Tuples).all((tuple, completion) -> {
            PauseSchedulerJobMsg msg = new PauseSchedulerJobMsg();
            msg.setUuid((String) tuple.get(0));
            bus.makeServiceIdByManagementNodeId(msg, SchedulerConstant.SERVICE_ID, (String) tuple.get(1));
            bus.send(msg, new CloudBusCallBack(null) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.debug(String.format("failed to change scheduler job[uuid:%s] state", (String) tuple.get(0)));
                    }
                }
            });
        }).run(new NopeWhileDoneCompletion());
    }

    public void afterDestroyVm(VmInstanceInventory vm) {

    }

    public void failedToDestroyVm(VmInstanceInventory vm, ErrorCode reason) {

    }

    public void preRecoverVm(VmInstanceInventory vm) {

    }

    public void beforeRecoverVm(VmInstanceInventory vm) {

    }

    public void afterRecoverVm(VmInstanceInventory vm) {

    }

    public void vmBeforeExpunge(VmInstanceInventory inv) {
        logger.debug(String.format("will delete scheduler before expunge vm[uuid:%s]", inv.getUuid()));
        deleteSchedulerJobByResourceUuid(inv.getUuid(), new NopeCompletion());
    }

    @Override
    public void preDeleteVolume(VolumeInventory volume) {

    }

    @Override
    public void beforeDeleteVolume(VolumeInventory volume) {
        logger.debug(String.format("will pause scheduler before expunge volume %s", volume.getUuid()));
        pauseSchedulerJobsByTargetResourceUuid(volume.getUuid());
    }

    @Override
    public void afterDeleteVolume(VolumeInventory volume, Completion completion) {
        completion.success();
    }

    @Override
    public void failedToDeleteVolume(VolumeInventory volume, ErrorCode errorCode) {

    }

    @Override
    public void volumePreExpunge(VolumeInventory volume) {}

    @Override
    public void volumeBeforeExpunge(VolumeInventory volume, Completion completion) {
        logger.debug(String.format("will delete scheduler before expunge volume %s", volume.getUuid()));
        deleteSchedulerJobByResourceUuid(volume.getUuid(), new Completion(completion) {
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
    public List<Quota> reportQuota() {
        Quota quota = new Quota();
        quota.defineQuota(new SchedulerJobNumQuotaDefinition());
        quota.defineQuota(new SchedulerTriggerNumQuotaDefinition());

        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APICreateSchedulerJobMsg.class)
                .addCounterQuota(SchedulerQuotaConstant.SCHEDULER_NUM));
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APICreateSchedulerTriggerMsg.class)
                .addCounterQuota(SchedulerQuotaConstant.SCHEDULER_TRIGGER_NUM));
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APIChangeResourceOwnerMsg.class)
                .addCheckCondition((msg) -> Q.New(SchedulerJobVO.class)
                        .eq(SchedulerJobVO_.uuid, msg.getResourceUuid())
                        .isExists())
                .addCounterQuota(SchedulerQuotaConstant.SCHEDULER_NUM));
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APIChangeResourceOwnerMsg.class)
                .addCheckCondition((msg) -> Q.New(SchedulerTriggerVO.class)
                        .eq(SchedulerTriggerVO_.uuid, msg.getResourceUuid())
                        .isExists())
                .addCounterQuota(SchedulerQuotaConstant.SCHEDULER_NUM));

        return list(quota);
    }

    @Override
    public void resourceOwnerAfterChange(AccountResourceRefInventory ref, String newOwnerUuid) {
        if (SchedulerJobVO.class.getSimpleName().equals(ref.getResourceType())
                || SchedulerTriggerVO.class.getSimpleName().equals(ref.getResourceType())) {
            return;
        }

        if (!Q.New(SchedulerJobVO.class)
                .eq(SchedulerJobVO_.targetResourceUuid, ref.getResourceUuid()).isExists()) {
            return;
        }

        List<String> uuids = Q.New(SchedulerJobVO.class)
                .select(SchedulerJobVO_.uuid)
                .eq(SchedulerJobVO_.targetResourceUuid, ref.getResourceUuid()).listValues();

        for (String uuid : uuids) {
            acntMgr.changeResourceOwner(uuid, newOwnerUuid);
        }
    }

    @Override
    public void volumeJustBeforeDeleteFromDb(VolumeInventory inv) {
        unscheduleJob(inv.getUuid());
    }

    private void cleanSchedulerJobDB (String jobUuid) {
        SQL.New(SchedulerJobSchedulerTriggerRefVO.class)
                .eq(SchedulerJobSchedulerTriggerRefVO_.schedulerJobUuid, jobUuid)
                .delete();
        SQL.New(SchedulerJobGroupJobRefVO.class)
                .eq(SchedulerJobGroupJobRefVO_.schedulerJobUuid, jobUuid)
                .delete();
        SQL.New(SchedulerJobVO.class)
                .eq(SchedulerJobVO_.uuid, jobUuid)
                .delete();
    }


    private void unscheduleJob(String targetResourceUuid) {
        List<String> jobUuids = Q.New(SchedulerJobVO.class)
                .select(SchedulerJobVO_.uuid)
                .eq(SchedulerJobVO_.targetResourceUuid, targetResourceUuid)
                .listValues();

        if (jobUuids.isEmpty()) {
            return;
        }

        for (String jobUuid :jobUuids) {
            List<String> triggerUuids = getTriggerUuids(jobUuid);

            if (triggerUuids == null || triggerUuids.isEmpty()) {
                cleanSchedulerJobDB(jobUuid);
                continue;
            }

            if (destinationMaker.isManagedByUs(jobUuid)) {
                doUnscheduleJobLocally(jobUuid, triggerUuids);
                cleanSchedulerJobDB(jobUuid);
                continue;
            }

            logger.debug(String.format("schedulerJob[uuid:%s] is not managed by us, send it to other node", jobUuid));
            UnscheduleJobMsg umsg = new UnscheduleJobMsg();
            umsg.setSchedulerJobUuid(jobUuid);
            umsg.setSchedulerTriggerUuids(triggerUuids);
            bus.makeTargetServiceIdByResourceUuid(umsg, SchedulerConstant.SERVICE_ID, jobUuid);
            bus.send(umsg);
            cleanSchedulerJobDB(jobUuid);
        }
    }

    @Override
    public void vmJustBeforeDeleteFromDb(VmInstanceInventory inv) {
        unscheduleJob(inv.getUuid());
    }
}
