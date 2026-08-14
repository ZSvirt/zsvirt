package org.zstack.scheduler;

import org.apache.logging.log4j.util.Strings;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.scheduler.*;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.FieldUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

/**
 * Created by MaJin on 2019/5/14.
 */

public class SchedulerJobParamCascadeUpdater {
    private static final CLogger logger = Utils.getLogger(SchedulerJobParamCascadeUpdater.class);

    private static DatabaseFacade dbf;
    private static SchedulerFacade schedulerFacade;

    private final static Map<Class<? extends SchedulerJob>, List<Field>> cascadeFields = new HashMap<>();
    private final static Map<String, Class<? extends SchedulerJob>> jobNameClassMap = new HashMap<>();
    private final static Map<String, Set<Class<? extends SchedulerJob>>> resourceTypeCascadeJobs = new HashMap<>();
    private final static Map<Class<? extends SchedulerJob>, List<Field>> requiredNotEmptyFields = new HashMap<>();

    static {
        Set<Class<? extends SchedulerJob>> jobClasses = BeanUtils.reflections.getSubTypesOf(SchedulerJob.class);
        for (Class<? extends SchedulerJob> jobClass : jobClasses) {
            if (Modifier.isAbstract(jobClass.getModifiers())) {
                continue;
            }

            jobNameClassMap.put(jobClass.getName(), jobClass);
            List<Field> fields = FieldUtils.getAnnotatedFields(CascadeUpdate.class, jobClass);
            if (fields.isEmpty()) {
                continue;
            }

            cascadeFields.put(jobClass, fields);
            for (Field field : fields) {
                CascadeUpdate a = field.getAnnotation(CascadeUpdate.class);
                field.setAccessible(true);
                resourceTypeCascadeJobs.computeIfAbsent(a.resourceType().getSimpleName(), k -> new HashSet<>()).add(jobClass);
                if (a.disableWhenEmpty()) {
                    requiredNotEmptyFields.computeIfAbsent(jobClass, k -> new ArrayList<>()).add(field);
                }
            }
        }
    }

    static void init() {
        dbf = Platform.getComponentLoader().getComponent(DatabaseFacade.class);
        schedulerFacade = Platform.getComponentLoader().getComponent(SchedulerFacade.class);
    }


    private static class GenerateJobDataResult {
        private String data;
        private boolean needDisable;

        private GenerateJobDataResult(String data, boolean needDisable) {
            this.data = data;
            this.needDisable = needDisable;
        }
    }

    static ErrorCode allowEnabled(SchedulerJob job) {
        List<Field> fields = requiredNotEmptyFields.get(job.getClass());
        if (fields == null || fields.isEmpty()) {
            return null;
        }

        for (Field field : fields) {
            if (fieldEmpty(field, job)) {
                return operr("field[%s] cannot be empty", field.getName());
            }
        }
        return null;
    }

    public static Set<String> getResourceTypeForCascadeAction() {
        return resourceTypeCascadeJobs.keySet();
    }

    public static void updateJobForResourceDeletion(String uuid, String resourceType) {
        updateJobForResourceDisabled(uuid, resourceType, null);
    }

    public static void updateJobForResourceDisabled(String uuid, String resourceType, List<Class<? extends SchedulerJob>> jobTypes) {
        Set<Class<? extends SchedulerJob>> jobClasses = resourceTypeCascadeJobs.get(resourceType);
        if (jobTypes != null) {
            jobClasses.retainAll(jobTypes);
        }

        if (jobClasses.isEmpty()) {
            return;
        }

        List<String> jobClassNames = jobClasses.stream().map(Class::getName).collect(Collectors.toList());
        List<SchedulerJobVO> toUpdateJob = Q.New(SchedulerJobVO.class)
                .in(SchedulerJobVO_.jobClassName, jobClassNames)
                .like(SchedulerJobVO_.jobData, "%" + uuid + "%")
                .list();

        List<String> updatedJobUuids = new ArrayList<>();
        List<String> toDisableJobUuids = new ArrayList<>();
        for (SchedulerJobVO jobVO : toUpdateJob) {
            String oldData = jobVO.getJobData();
            GenerateJobDataResult result = generateJobDescData(jobVO, uuid, resourceType);

            if (result.needDisable) {
                jobVO.setJobData(result.data);
                toDisableJobUuids.add(jobVO.getUuid());
            } else if (!result.data.equals(oldData)) {
                jobVO.setJobData(result.data);
                updatedJobUuids.add(jobVO.getUuid());
            }
        }
        dbf.updateCollection(toUpdateJob);

        List<String> updatedGroupUuids = new ArrayList<>();
        List<String> toDisableGroupUuids = new ArrayList<>();
        List<SchedulerJobGroupVO> toUpdateJobGroup = Q.New(SchedulerJobGroupVO.class)
                .in(SchedulerJobGroupVO_.jobClassName, jobClassNames)
                .like(SchedulerJobGroupVO_.jobData, "%" + uuid + "%")
                .list();

        for (SchedulerJobGroupVO groupVO : toUpdateJobGroup) {
            String oldData = groupVO.getJobData();
            GenerateJobDataResult result = generateJobDescData(groupVO, uuid, resourceType);
            if (result.needDisable) {
                groupVO.setJobData(result.data);
                toDisableGroupUuids.add(groupVO.getUuid());
            } else if (!result.data.equals(oldData)) {
                groupVO.setJobData(result.data);
                updatedGroupUuids.add(groupVO.getUuid());
            }
        }
        dbf.updateCollection(toUpdateJobGroup);

        logger.debug(String.format("updated %d schedulerJob(s) and %d schedulerJobGroup(s) for unused resource[uuid:%s, type:%s]",
                updatedJobUuids.size(), updatedGroupUuids.size(), uuid, resourceType));
        logger.debug(String.format("disable %d schedulerJob(s) and %d schedulerJobGroup(s) for unused resource[uuid:%s, type:%s]",
                toDisableJobUuids.size(), toDisableGroupUuids.size(), uuid, resourceType));
        schedulerFacade.handleJobUpdated(updatedJobUuids, updatedGroupUuids, false);
        schedulerFacade.handleJobUpdated(toDisableJobUuids, toDisableGroupUuids, true);
    }

    private static GenerateJobDataResult generateJobDescData(SchedulerJobDesc jobDesc, String toRemoveUuid, String resourceType) {
        Class<? extends SchedulerJob> jobClass = jobNameClassMap.get(jobDesc.getJobClassName());
        SchedulerJob job = JSONObjectUtil.toObject(jobDesc.getJobData(), jobClass);
        List<Field> toUpdateFields = cascadeFields.get(jobClass);

        boolean needDisable = false;
        for (Field f : toUpdateFields) {
            CascadeUpdate annotation = f.getAnnotation(CascadeUpdate.class);
            if (!annotation.resourceType().getSimpleName().equals(resourceType)) {
                continue;
            }
            removeFromField(f, job, toRemoveUuid);
            if (annotation.disableWhenEmpty() && fieldEmpty(f, job)) {
                needDisable = true;
            }
        }

        return new GenerateJobDataResult(JSONObjectUtil.toJsonString(job), needDisable);
    }

    private static void removeFromField(Field field, SchedulerJob job, String toRemoveUuid) {
        try {
            if (Collection.class.isAssignableFrom(field.getType())) {
                Collection c = (Collection) field.get(job);
                c.remove(toRemoveUuid);
            } else if (String.class.isAssignableFrom(field.getType()) && toRemoveUuid.equals(field.get(job))) {
                field.set(job, null);
            }
        } catch (IllegalAccessException e) {
            logger.error("cannot set field", e);
        }
    }

    private static boolean fieldEmpty(Field field, SchedulerJob job) {
        try {
            if (Collection.class.isAssignableFrom(field.getType())) {
                Collection c = (Collection) field.get(job);
                return c.isEmpty();
            } else if (String.class.isAssignableFrom(field.getType())) {
                return Strings.isEmpty((String) field.get(job));
            }
        } catch (IllegalAccessException e) {
            logger.error("cannot set field", e);
        }

        return false;
    }
}
