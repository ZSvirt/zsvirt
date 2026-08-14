package org.zstack.core.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusEventListener;
import org.zstack.core.cloudbus.EventSubscriberReceipt;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.GLock;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.NopeReturnValueCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.message.Event;
import org.zstack.utils.Bucket;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.JsonWrapper;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.serializable.SerializableHelper;

import javax.persistence.NoResultException;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.inerr;

/**
 */
public class JobQueueFacadeImpl2 implements JobQueueFacade, CloudBusEventListener, Component, ManagementNodeChangeListener {
    private static final CLogger logger = Utils.getLogger(JobQueueFacadeImpl2.class);
    private static final String LOCK_NAME = "JobQueueFacade.lock";
    private static final String ORPHAN_JOB_LOCK_NAME = "JobQueueFacade.orphanJobLock";
    private static final int LOCK_TIMEOUT = 60;

    private Map<Long, JobWrapper> wrappers = Collections.synchronizedMap(new HashMap<Long, JobWrapper>());

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;

    private volatile boolean stopped = false;
    private EventSubscriberReceipt unsubscriber;

    @Override
    public boolean handleEvent(Event e) {
        if (!(e instanceof JobEvent)) {
            return false;
        }

        JobEvent je = (JobEvent) e;
        JobWrapper jw = wrappers.get(je.getJobId());
        if (jw == null) {
            return false;
        }

        if (je.isSuccess()) {
            Object ret = je.getReturnValue() != null ? je.getReturnValue().get() : null;
            jw.success(ret);
        } else {
            jw.fail(je.getErrorCode());
        }

        return false;
    }

    @Override
    public boolean start() {
        unsubscriber = bus.subscribeEvent(this, new JobEvent());
        stopped = false;
        return true;
    }

    @Override
    public boolean stop() {
        stopped = true;
        if (unsubscriber != null) {
            unsubscriber.unsubscribeAll();
        }
        return true;
    }

    private void restartQueue(JobQueueVO qvo, String mgmtId) {
        SimpleQuery<JobQueueEntryVO> q = dbf.createQuery(JobQueueEntryVO.class);
        q.select(JobQueueEntryVO_.id, JobQueueEntryVO_.name);
        q.add(JobQueueEntryVO_.jobQueueId, SimpleQuery.Op.EQ, qvo.getId());
        q.add(JobQueueEntryVO_.issuerManagementNodeId, SimpleQuery.Op.NULL);
        List<Tuple> ts = q.listTuple();
        for (Tuple t : ts) {
            logger.debug(String.format("[Job Removed]: job[id:%s, name:%s] because its issuer management node[id:%s] became available", t.get(0), t.get(1), mgmtId));
            dbf.removeByPrimaryKey((Long) t.get(0), JobQueueEntryVO.class);
        }

        q = dbf.createQuery(JobQueueEntryVO.class);
        q.add(JobQueueEntryVO_.state, SimpleQuery.Op.IN, JobState.Pending, JobState.Processing);
        q.add(JobQueueEntryVO_.jobQueueId, SimpleQuery.Op.EQ, qvo.getId());
        q.orderBy(JobQueueEntryVO_.id, SimpleQuery.Od.ASC);
        long count = q.count();
        if (count == 0) {
            logger.debug(String.format("[JobQueue Removed]: id:%s, no Pending or Processing job remaining in this queue, remove it", qvo.getId()));
            return;
        }

        List<JobQueueEntryVO> es = q.list();
        for (JobQueueEntryVO e : es) {
            if (e.getState() == JobState.Processing && !e.isRestartable()) {
                dbf.remove(e);
                JobEvent evt = new JobEvent();
                evt.setErrorCode(err(SysErrors.MANAGEMENT_NODE_UNAVAILABLE_ERROR,
                        "management node[id:%s] becomes unavailable, job[name:%s, id:%s] is not restartable", mgmtId, e.getName(), e.getId()));
                bus.publish(evt);
                logger.debug(String.format("[Job Removed]: job[id:%s, name:%s] because it's not restartable",
                        e.getId(), e.getName()));
                continue;
            }

            logger.debug(String.format("[Job Restart]: job[id:%s, name:%s] in queue[id:%s] is restarting as its previous worker node[id:%s] became unavailable",
                    e.getId(), e.getName(), qvo.getId(), mgmtId));
            execute(qvo.getName(), qvo.getOwner(), e, new NopeReturnValueCompletion(), null);
            return;
        }
    }

    private void takeOverJobs(String mgmtId) {
        GLock lock = new GLock(ORPHAN_JOB_LOCK_NAME, LOCK_TIMEOUT);
        lock.lock();
        try {
            logger.debug(String.format("management node[id:%s] starts taking over jobs of left management node[%s]",
                    Platform.getManagementServerId(), mgmtId));
            SimpleQuery<JobQueueVO> qq = dbf.createQuery(JobQueueVO.class);
            qq.add(JobQueueVO_.workerManagementNodeId, SimpleQuery.Op.NULL);
            List<JobQueueVO> queues = qq.list();

            logger.debug(String.format("[Orphan Queue found]: management node is going to take over %s orphan queues", queues.size()));
            for (JobQueueVO queue : queues) {
                restartQueue(queue, mgmtId);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void nodeJoin(ManagementNodeInventory inv) {
    }

    @Override
    public void nodeLeft(ManagementNodeInventory inv) {
        takeOverJobs(inv.getUuid());
    }

    @Override
    public void iAmDead(ManagementNodeInventory inv) {
    }

    @Override
    public void iJoin(ManagementNodeInventory inv) {
    }

    private interface JobWrapper {
        void run();

        void success(Object ret);

        void fail(ErrorCode err);
    }
    
    public void execute(String queueName, String owner, Job job) {
        execute(queueName, owner, job, new NopeCompletion());
    }


    private <T> void execute(final String queueName, final String owner, final JobQueueEntryVO entry, final ReturnValueCompletion<T> completion, final Class<? extends T> returnType) {
        new JobWrapper() {
            private Long myJobId;

            @Transactional
            private JobQueueVO saveJob() throws IOException {
                JobQueueVO ret = null;
                String sql = "select queue from JobQueueVO queue where queue.name = :queueName";
                TypedQuery<JobQueueVO> q = dbf.getEntityManager().createQuery(sql, JobQueueVO.class);
                q.setParameter("queueName", queueName);
                JobQueueVO qvo = null;
                try {
                    qvo = q.getSingleResult();
                } catch (NoResultException|EmptyResultDataAccessException e) {
                    // no queue yet
                }

                if (qvo == null) {
                    qvo = new JobQueueVO();
                    qvo.setName(queueName);
                    qvo.setOwner(owner);
                    qvo.setWorkerManagementNodeId(Platform.getManagementServerId());
                    dbf.getEntityManager().persist(qvo);
                    dbf.getEntityManager().flush();
                    dbf.getEntityManager().refresh(qvo);
                    logger.debug(String.format("[JobQueue created] id: %s, owner: %s, queue name: %s", qvo.getId(), owner, queueName));
                    ret = qvo;
                } else if (qvo.getWorkerManagementNodeId() == null) {
                    qvo.setWorkerManagementNodeId(Platform.getManagementServerId());
                    dbf.getEntityManager().merge(qvo);
                    ret = qvo;
                }

                entry.setJobQueueId(qvo.getId());
                entry.setIssuerManagementNodeId(Platform.getManagementServerId());
                entry.setState(JobState.Pending);

                JobQueueEntryVO ne = dbf.getEntityManager().merge(entry);
                dbf.getEntityManager().flush();
                dbf.getEntityManager().refresh(ne);
                logger.debug(String.format("[Job added] job queue name: %s, job class name: %s, job id: %s", qvo.getName(), ne.getName(),
                        ne.getId()));

                myJobId = ne.getId();
                wrappers.put(myJobId, this);
                return ret;
            }

            private void jobFail(JobQueueEntryVO jvo, ErrorCode err) {
                jvo.setDoneDate(new Timestamp(new Date().getTime()));
                jvo.setState(JobState.Error);
                dbf.update(jvo);

                JobEvent evt = new JobEvent();
                evt.setJobId(jvo.getId());
                evt.setErrorCode(err);
                bus.publish(evt);
            }

            private void jobDone(JobQueueEntryVO jvo, Object ret) {
                jvo.setDoneDate(new Timestamp(new Date().getTime()));
                jvo.setState(JobState.Completed);
                dbf.update(jvo);

                JobEvent evt = new JobEvent();
                evt.setJobId(jvo.getId());
                if (ret != null) {
                    evt.setReturnValue(JsonWrapper.wrap(ret));
                }
                bus.publish(evt);
            }

            private JobQueueEntryVO findJob(JobQueueVO qvo) {
                SimpleQuery<JobQueueEntryVO> q = dbf.createQuery(JobQueueEntryVO.class);
                q.add(JobQueueEntryVO_.state, SimpleQuery.Op.EQ, JobState.Pending);
                q.add(JobQueueEntryVO_.jobQueueId, SimpleQuery.Op.EQ, qvo.getId());
                q.setLimit(1);
                q.orderBy(JobQueueEntryVO_.id, SimpleQuery.Od.ASC);
                return q.find();
            }

            private Bucket takeJob(final JobQueueVO qvo) {
                GLock lock = new GLock(LOCK_NAME, LOCK_TIMEOUT);
                lock.lock();
                try {
                    JobQueueEntryVO jobe = findJob(qvo);
                    if (jobe == null) {
                        // nothing to do, release queue
                        dbf.remove(qvo);
                        logger.debug(String.format("[JobQueue released, no pending task, delete the queue] last owner: %s, queue name: %s, queue id: %s",
                                qvo.getOwner(), qvo.getName(), qvo.getId()));
                        return null;
                    }

                    while (true) {
                        try {
                            JobContextObject ctx = SerializableHelper.readObject(jobe.getContext());
                            Job theJob = ctx.load();
                            jobe.setState(JobState.Processing);
                            jobe = dbf.updateAndRefresh(jobe);
                            return Bucket.newBucket(jobe, theJob);
                        } catch (Exception e1) {
                            ErrorCode ierr = inerr("[Job de-serialize failed, the job will be marked as Error] queue name: %s, job id: %s, %s", qvo.getName(),
                                    jobe.getId(), e1.getMessage());
                            jobFail(jobe, ierr);
                            logger.warn(ierr.getDetails(), e1);
                            jobe = findJob(qvo);
                        }
                    }
                } finally {
                    lock.unlock();
                }
            }

            @AsyncThread
            private void process(final JobQueueVO qvo) {
                if (stopped) {
                    logger.warn(String.format("[Job Facade Stopped]: stop processing job"));
                    return;
                }

                Bucket ret = takeJob(qvo);
                if (ret == null) {
                    return;
                }

                final JobQueueEntryVO e = ret.get(0);
                final Job job = ret.get(1);

                logger.debug(String.format("[Job Start] start executing job[id:%s, name:%s]", e.getId(), e.getName()));
                job.run(new ReturnValueCompletion<Object>(null) {
                    @Override
                    public void success(Object returnValue) {
                        try {
                            jobDone(e, returnValue);
                            logger.debug(String.format("[Job Success] job[id:%s, name:%s] succeed", e.getId(), e.getName()));
                        } catch (Throwable t){
                            logger.warn(String.format("unhandled exception happened when calling %s", job.getClass().getName()), t);
                            jobFail(e, inerr(t.getMessage()));
                        } finally {
                            process(qvo);
                        }
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        try {
                            jobFail(e, errorCode);
                            logger.debug(String.format("[Job Failure] job[id:%s, name:%s] failed", e.getId(), e.getName()));
                        } catch (Throwable t){
                            logger.warn(String.format("unhandled exception happened when calling %s", job.getClass().getName()), t);
                            jobFail(e, inerr(t.getMessage()));
                        } finally {
                            process(qvo);
                        }
                    }
                });
            }

            @Override
            public void run() {
                if (stopped) {
                    logger.warn(String.format("[Job Facade Stopped]: skip to run job[queueName:%s, owner:%s, name:%s]",
                            queueName, owner, entry.getName()));
                    return;
                }

                try {
                    GLock lock = new GLock(LOCK_NAME, LOCK_TIMEOUT);
                    JobQueueVO qvo = null;
                    lock.lock();
                    try {
                        qvo = saveJob();
                    } finally {
                        lock.unlock();
                    }

                    if (qvo != null) {
                        process(qvo);
                    }
                } catch (IOException e1) {
                    throw new CloudRuntimeException(String.format("unable to serialize job: %s", entry.getName()), e1);
                }
            }

            @Override
            public void success(Object ret) {
                DebugUtils.Assert(myJobId!=null, "how can myJobId be null???");
                wrappers.remove(myJobId);
                completion.success((T)ret);
            }

            @Override
            public void fail(ErrorCode err) {
                DebugUtils.Assert(myJobId!=null, "how can myJobId be null???");
                wrappers.remove(myJobId);
                completion.fail(err);
            }
        }.run();
    }

    @Override
    public <T> void execute(final String queueName, final String owner, final Job job, final ReturnValueCompletion<T> completion, final Class<? extends T> returnType) {
        try {
            JobQueueEntryVO e = new JobQueueEntryVO();
            JobContextObject ctx = new JobContextObject(job);
            byte[] bits = SerializableHelper.writeObject(ctx);
            e.setContext(bits);
            e.setRestartable(job.getClass().isAnnotationPresent(RestartableJob.class));
            e.setName(job.getClass().getName());
            execute(queueName, owner, e, completion, returnType);
        } catch (IOException e1) {
            throw new CloudRuntimeException(e1);
        }
    }

    @Override
    public void execute(String queueName, String owner, Job job, final Completion completion) {
        execute(queueName, owner, job, new ReturnValueCompletion<Object>(completion) {
            @Override
            public void success(Object returnValue) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        }, null);
    }

    @Override
    public void deleteJobQueue(String queueName) {

    }

    @Override
    public void evictOwner(String owner) {

    }

    @Override
    public List<String> listAllQueue() {
        return null;
    }

    @Override
    public List<String> listQueue(String namePattern) {
        return null;
    }

    @Override
    public long getPendingJobNumber(String queueName) {
        return 0;
    }

    @Override
    public List<String> listQueueHasPendingJob() {
        return null;
    }

    @Override
    public boolean startQueueIfPendingJob(String queueName, String owner) {
        return false;
    }

    @Override
    public boolean startQueueIfPendingJob(String queueName, String owner, boolean newThread) {
        return false;
    }
}
