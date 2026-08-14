package org.zstack.core.progress;

import org.apache.logging.log4j.ThreadContext;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.db.UpdateQuery;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.core.progress.APIGetTaskProgressMsg;
import org.zstack.header.core.progress.APIGetTaskProgressReply;
import org.zstack.header.core.progress.ProgressConstants;
import org.zstack.header.core.progress.TaskProgressInventory;
import org.zstack.header.core.progress.TaskProgressVO;
import org.zstack.header.core.progress.TaskProgressVO_;
import org.zstack.header.longjob.APISubmitLongJobEvent;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.AbstractBeforePublishEventInterceptor;
import org.zstack.header.message.Event;
import org.zstack.header.message.Message;
import org.zstack.header.rest.RESTFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.zstack.header.Constants.THREAD_CONTEXT_API;
import static org.zstack.header.Constants.THREAD_CONTEXT_TASK_NAME;
import static org.zstack.utils.CollectionUtils.*;

public class ActionProgressService extends AbstractService implements
        ManagementNodeReadyExtensionPoint {
    protected static final CLogger logger = Utils.getLogger(ActionProgressService.class);
    @Autowired
    private RESTFacade restFacade;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade threadFacade;
    @Autowired
    private DatabaseFacade databaseFacade;

    @Override
    @SuppressWarnings("unchecked")
    public boolean start() {
        cleanIntervalMillis = TimeUnit.SECONDS.toMillis(ProgressGlobalConfig.CLEANUP_THREAD_INTERVAL.value(Long.class));

        restFacade.registerSyncHttpCallHandler(
                ProgressConstants.PROGRESS_REPORT_PATH,
                ProgressCommands.ProgressReportCmd.class,
                cmd -> {
                    handleAgentProgress(cmd);
                    return null;
                });

        ProgressGlobalConfig.CLEANUP_THREAD_INTERVAL.installUpdateExtension(
                (oldConfig, newConfig) -> cleanIntervalMillis = TimeUnit.SECONDS.toMillis(newConfig.value(Long.class)));

        bus.installBeforePublishEventInterceptor(new AbstractBeforePublishEventInterceptor() {
            @Override
            public void beforePublishEvent(Event evt) {
                if (!(evt instanceof APIEvent) || evt instanceof APISubmitLongJobEvent) {
                    return;
                }
                markEventPublished(((APIEvent) evt).getApiId());
            }
        });

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIGetTaskProgressMsg) {
            handle((APIGetTaskProgressMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(ProgressConstants.SERVICE_ID);
    }

    @Override
    public void managementNodeReady() {
        threadFacade.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return 1L;
            }

            @Override
            public String getName() {
                return "progress-thread";
            }

            @Override
            public void run() {
                updateNowInMillisFromDB();
                flushProgressToDB();
                checkMultiMNIfNeeded();
                cleanExpiredProgressIfNeeded();
            }
        });
    }

    private void handle(final APIGetTaskProgressMsg msg) {
        APIGetTaskProgressReply reply = new APIGetTaskProgressReply();

        final List<TaskProgressInventory> subProgress = findProgressesByApiId(msg.getApiId());
        if (isEmpty(subProgress)) {
            reply.setInventories(Collections.emptyList());
        } else {
            List<TaskProgressInventory> all = new ArrayList<>(subProgress.size() + 1);
            all.add(generateMainProgress(subProgress));
            all.addAll(subProgress);
            reply.setInventories(all);
        }

        bus.reply(msg, reply);
    }

    /**
     * The main progress bar is inaccurate, mainly due to the placebo effect;
     * Actually, the UI says that there must be one main progress.
     */
    private TaskProgressInventory generateMainProgress(List<TaskProgressInventory> subProgresses) {
        long maxUpdateTime = 0L;
        double weight = 0d;

        for (TaskProgressInventory subProgress : subProgresses) {
            double totalStep = subProgress.getTotalStep();
            double currentStep = subProgress.getCurrentStep();

            if (totalStep == 0) {
                continue;
            }

            if (totalStep < currentStep) {
                currentStep = totalStep;
            }

            if (currentStep < 0) {
                currentStep = 0;
            }

            if (totalStep <= 5) {
                double scale = 5 / totalStep;
                currentStep *= scale;
            } else if (totalStep > 100) {
                 double scale = 100 / totalStep;
                currentStep *= scale;
            }

            weight += currentStep;
            maxUpdateTime = Math.max(maxUpdateTime, subProgress.getLastOpTime());
        }

        long now = nowInMillis;
        TaskProgressInventory inventory = new TaskProgressInventory();
        inventory.setApiId(subProgresses.get(0).getApiId());
        inventory.setContent("main-progress");
        inventory.setCreateTime(now);
        inventory.setLastOpTime(Math.min(now, maxUpdateTime));
        inventory.setTotalStep(10000L);

        if (isEventPublished(inventory.getApiId())) {
            inventory.setCurrentStep(10000L);
            return inventory;
        }

        long createTime = subProgresses.get(0).getCreateTime();
        double timeDelta = 1 - 10000 / ((now - createTime) + 10000d); // timeDelta always < 1
        double fakePercentage = 1 - 50 / ((weight + timeDelta * 30) + 50);
        inventory.setCurrentStep((long) Math.floor(fakePercentage * 10000));
        return inventory;
    }

    // task progress cache
    //    all progress will send to this task progress cache, and update to db together every 1s

    public static class ProgressList {
        public final String apiId;
        public List<ProgressItem> items = new ArrayList<>();
        public final Object lock = new Object();
        public boolean eventPublished = false;

        // db parameters
        public boolean localUpdated = false;
        public static final long READ_DELAY = TimeUnit.SECONDS.toMillis(2);
        public long lastTimeReadFromDB = nowInMillis;

        public ProgressList(String apiId) {
            this.apiId = apiId;
        }
    }

    /**
     * In principle, only one MN is allowed to hold a {@link TaskProgressVO}.
     * There will be no situation where multiple MNs share the same {@link TaskProgressVO#getId()}
     */
    public static class ProgressItem implements Comparable<ProgressItem> {
        public final ProgressList parent;
        /**
         * @see TaskProgressVO#getId()
         */
        public long id;
        public String content;
        public String opaqueText;
        public boolean opaqueUpdated;
        public long createTime;
        public long lastOpTime;
        public long currentStep;
        public long totalStep;

        public boolean updated;

        public ProgressItem(ProgressList parent) {
            this.parent = Objects.requireNonNull(parent);
        }

        public ProgressItem from(TaskProgressVO vo) {
            this.id = vo.getId();
            this.content = vo.getContent();
            this.opaqueText = vo.getOpaque() == null ? "{}" : vo.getOpaque();
            this.opaqueUpdated = false;
            this.createTime = vo.getCreateTime();
            this.lastOpTime = vo.getLastOpTime();
            this.currentStep = vo.getCurrentStep();
            this.totalStep = vo.getTotalStep();
            return this;
        }

        @Override
        public int compareTo(@NotNull ProgressItem o) {
            return Long.compare(this.id, o.id);
        }

        @SuppressWarnings("unchecked")
        public TaskProgressInventory toInventory() {
            TaskProgressInventory inv = new TaskProgressInventory();
            inv.setApiId(this.parent.apiId);
            inv.setContent(this.content);
            inv.setOpaque(JSONObjectUtil.toObject(this.opaqueText, Map.class));
            inv.setCreateTime(this.createTime);
            inv.setLastOpTime(this.lastOpTime);
            inv.setCurrentStep(this.currentStep);
            inv.setTotalStep(this.totalStep);
            return inv;
        }
    }

    private static final int MAX_PROGRESS_CACHE_SIZE = 64;
    private static final Map<String, ProgressList> progressCache = Collections.synchronizedMap(
            new LinkedHashMap<String, ProgressList>(MAX_PROGRESS_CACHE_SIZE, 0.9f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return this.size() > MAX_PROGRESS_CACHE_SIZE;
        }
    });

    public static volatile long nowInMillis = System.currentTimeMillis();
    public static final Object globalLock = new Object();

    private static boolean multiMN = false;
    private static long lastMultiMNCheckTime = 0L;
    private static final long MN_CHECK_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(10);

    private static long lastCleanTime = 0L;
    private static long cleanIntervalMillis = TimeUnit.MINUTES.toMillis(30);

    private void updateNowInMillisFromDB() {
        new SQLBatch() {
            @Override
            protected void scripts() {
                Query query = databaseFacade.getEntityManager().createNativeQuery("select unix_timestamp()");
                nowInMillis = ((BigInteger) query.getSingleResult()).longValue() * 1000;
            }
        }.execute();
    }

    static void putToCache(TaskProgressReporter builder) {
        if (builder.id == TaskProgressReporter.INVALID_ID) {
            applyActionProgressId(builder);
        }

        String apiId = builder.apiId;
        ProgressList list = progressCache.computeIfAbsent(apiId, ProgressList::new);

        long now = nowInMillis;
        synchronized (list.lock) {
            ProgressItem item = findOneOrNull(list.items, item0 -> item0.id == builder.id);
            if (item == null) {
                item = new ProgressItem(list);
                item.id = builder.id;
                item.content = builder.content;
                item.createTime = now;
                item.lastOpTime = now;
                item.currentStep = builder.currentStep;
                item.totalStep = builder.totalStep;

                String opaqueText = builder.opaque == null ? "{}" : JSONObjectUtil.toJsonString(builder.opaque);
                if (!opaqueText.equals(item.opaqueText)) {
                    item.opaqueText = opaqueText;
                    item.opaqueUpdated = true;
                }

                list.items.add(item);
                Collections.sort(list.items);
            } else {
                item.updated = true;
                item.content = builder.content;
                item.lastOpTime = now;
                item.currentStep = builder.currentStep;
                item.totalStep = builder.totalStep;

                String opaqueText = builder.opaque == null ? "{}" : JSONObjectUtil.toJsonString(builder.opaque);
                if (!opaqueText.equals(item.opaqueText)) {
                    item.opaqueText = opaqueText;
                    item.opaqueUpdated = true;
                }
            }

            list.localUpdated = true;
        }
    }

    private static void applyActionProgressId(TaskProgressReporter builder) {
        TaskProgressVO vo = new TaskProgressVO();
        vo.setApiId(builder.apiId);
        vo.setContent(builder.content);
        if (builder.opaque != null) {
            vo.setOpaque(JSONObjectUtil.toJsonString(builder.opaque));
        }

        long now = nowInMillis;
        vo.setCreateTime(now);
        vo.setLastOpTime(now);
        vo.setCurrentStep(builder.currentStep);
        vo.setTotalStep(builder.totalStep);

        DatabaseFacade databases = Platform.getComponentLoader().getComponent(DatabaseFacade.class);
        synchronized (globalLock) {
            vo = databases.persistAndRefresh(vo);
            builder.id = vo.getId();
        }
    }

    private static void flushProgressToDB() {
        List<ProgressList> lists;
        synchronized (progressCache) {
            lists = new ArrayList<>(progressCache.values());
        }

        for (ProgressList list : lists) {
            synchronized (list.lock) {
                if (!list.localUpdated) {
                    continue;
                }

                list.localUpdated = false;
                for (ProgressItem item : list.items) {
                    if (!item.updated) {
                        continue;
                    }

                    item.updated = false;
                    UpdateQuery eq = SQL.New(TaskProgressVO.class)
                            .eq(TaskProgressVO_.id, item.id)
                            .set(TaskProgressVO_.content, item.content)
                            .set(TaskProgressVO_.createTime, item.createTime)
                            .set(TaskProgressVO_.lastOpTime, item.lastOpTime)
                            .set(TaskProgressVO_.currentStep, item.currentStep)
                            .set(TaskProgressVO_.totalStep, item.totalStep);
                    if (item.opaqueUpdated) {
                        item.opaqueUpdated = false;
                        eq.set(TaskProgressVO_.opaque, item.opaqueText);
                    }
                    eq.update();
                }
            }
        }
    }

    public static List<TaskProgressInventory> findProgressesByApiId(String apiId) {
        if (!multiMN) {
            final ProgressList list = progressCache.get(apiId);
            if (list == null) {
                return Collections.emptyList();
            }

            synchronized (list.lock) {
                return transform(list.items, ProgressItem::toInventory);
            }
        }

        ProgressList list = progressCache.get(apiId);
        if (list == null) {
            boolean exists = Q.New(TaskProgressVO.class)
                    .eq(TaskProgressVO_.apiId, apiId)
                    .isExists();
            if (!exists) {
                return Collections.emptyList();
            }

            list = progressCache.computeIfAbsent(apiId, ProgressList::new);
        }

        long now = nowInMillis;
        boolean needReadFromDB = list.lastTimeReadFromDB + ProgressList.READ_DELAY <= now;
        if (!needReadFromDB) {
            synchronized (list.lock) {
                return transform(list.items, ProgressItem::toInventory);
            }
        }

        synchronized (list.lock) {
            List<Long> existsIds = transform(list.items, item -> item.id);

            if (existsIds.isEmpty()) {
                List<TaskProgressVO> vos = Q.New(TaskProgressVO.class)
                        .eq(TaskProgressVO_.apiId, apiId)
                        .list();
                final ProgressList finalList = list;
                list.lastTimeReadFromDB = now;
                list.items.addAll(transform(vos, vo -> new ProgressItem(finalList).from(vo)));
                Collections.sort(list.items);
                return transform(list.items, ProgressItem::toInventory);
            }

            List<TaskProgressVO> vos = Q.New(TaskProgressVO.class)
                    .eq(TaskProgressVO_.apiId, apiId)
                    .notIn(TaskProgressVO_.id, existsIds)
                    .list();
            if (!vos.isEmpty()) {
                final ProgressList finalList = list;
                list.lastTimeReadFromDB = now;
                list.items.addAll(transform(vos, vo -> new ProgressItem(finalList).from(vo)));
                Collections.sort(list.items);
            }

            return transform(list.items, ProgressItem::toInventory);
        }
    }

    private static void checkMultiMNIfNeeded() {
        long now = nowInMillis;
        if (now - lastMultiMNCheckTime < MN_CHECK_INTERVAL_MILLIS) {
            return;
        }

        lastMultiMNCheckTime = now;
        multiMN = Q.New(ManagementNodeVO.class).count() > 1;
    }

    private static void cleanExpiredProgressIfNeeded() {
        long now = nowInMillis;
        if (now - lastCleanTime < cleanIntervalMillis) {
            return;
        }

        lastCleanTime = now;
        long ttlMillis = TimeUnit.SECONDS.toMillis(ProgressGlobalConfig.PROGRESS_TTL_SECONDS.value(Long.class));
        long maxCleanTime = now - ttlMillis;

        synchronized (globalLock) {
            logger.trace("clean expired progress: maxCleanTime = " + maxCleanTime);
            SQL.New(TaskProgressVO.class)
                    .lt(TaskProgressVO_.lastOpTime, maxCleanTime)
                    .delete();
        }
    }

    public void clearCache() {
        synchronized (agentLock) {
            agentContexts.clear();
        }

        progressCache.clear();
    }

    public static void markEventPublished(String apiId) {
        ProgressList list = progressCache.get(apiId);
        if (list != null) {
            list.eventPublished = true;
        }
    }

    public static boolean isEventPublished(String apiId) {
        ProgressList list = progressCache.get(apiId);
        return list != null && list.eventPublished;
    }

    // agent progress reporter
    public static final Object agentLock = new Object();
    private static Map<String, AgentProgressContext> agentContexts = new LinkedHashMap<String, AgentProgressContext>(MAX_PROGRESS_CACHE_SIZE, 0.9f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return this.size() > MAX_PROGRESS_CACHE_SIZE;
        }
    };

    public static class AgentProgressContext implements Comparable<AgentProgressContext> {
        public String key;
        public long lastOpTime;
        public TaskProgressReporter reporter;

        @Override
        public int compareTo(@NotNull AgentProgressContext o) {
            return Long.compare(this.lastOpTime, o.lastOpTime);
        }
    }

    private static String generateAgentProgressKey(ProgressCommands.ProgressReportCmd cmd) {
        String apiId = cmd.getThreadContextMap().get(THREAD_CONTEXT_API);
        String taskName = cmd.getThreadContextMap().get(THREAD_CONTEXT_TASK_NAME);
        return String.format("%s-%s-%s",apiId, taskName, cmd.getResourceUuid());
    }

    public void handleAgentProgress(ProgressCommands.ProgressReportCmd cmd) {
        String key = generateAgentProgressKey(cmd);
        long now = nowInMillis;

        String apiId = cmd.getThreadContextMap().get(THREAD_CONTEXT_API);
        String taskName = cmd.getThreadContextMap().get(THREAD_CONTEXT_TASK_NAME);
        long currentStep;
        AgentProgressContext context;

        synchronized (agentLock) {
            context = agentContexts.get(key);

            try {
                currentStep = Long.parseLong(cmd.getProgress());
            } catch (NumberFormatException ignored) {
                currentStep = 0L;
            }

            if (context == null) {
                context = new AgentProgressContext();
                context.key = key;
                context.lastOpTime = now;
                context.reporter = taskProgress()
                        .withApiId(apiId)
                        .withContent("agent-report-task-for: " + taskName)
                        .withTotalStep(100L);

                agentContexts.put(key, context);
            } else {
                context.lastOpTime = now;
            }
        }

        if (cmd.getDetail() != null) {
            context.reporter.withOpaques(cmd.getDetail());
        }

        context.reporter.withOpaque("resource.uuid", cmd.getResourceUuid())
                .withCurrentStep(currentStep)
                .report();
    }

    // task progress reporter

    public static TaskProgressReporter taskProgress() {
        return new TaskProgressReporter();
    }

    public static String findApiId() {
        return ThreadContext.get(THREAD_CONTEXT_API);
    }

    public static void taskProgress(String content) {
        taskProgress()
                .withContent(content)
                .withCurrentStep(1L)
                .withTotalStep(1L)
                .report();
    }

    /**
     * @deprecated use {@link #taskProgress(String)} instead
     */
    @Deprecated
    public static void taskProgress(String fmt, Object...args) {
        taskProgress()
                .withContent(String.format(fmt, args))
                .withCurrentStep(1L)
                .withTotalStep(1L)
                .report();
    }

    public static void reportProgress(String content, long currentStep) {
        reportProgress(content, currentStep, 100L);
    }

    public static void reportProgress(String content, long currentStep, long totalStep) {
        taskProgress()
                .withContent(content)
                .withCurrentStep(currentStep)
                .withTotalStep(totalStep)
                .report();
    }
}
