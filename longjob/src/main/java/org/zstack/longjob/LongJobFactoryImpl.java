package org.zstack.longjob;

import org.zstack.header.Component;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.longjob.LongJob;
import org.zstack.header.longjob.LongJobFor;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.ObjectUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import static org.zstack.core.Platform.operr;

/**
 * Created by GuoYi on 11/14/17.
 */
public class LongJobFactoryImpl implements LongJobFactory, Component {
    private static final CLogger logger = Utils.getLogger(LongJobFactoryImpl.class);
    /**
     * Key:LongJobName
     */
    private TreeMap<String, LongJob> allLongJob = new TreeMap<>();
    private TreeMap<String, String> fullJobName = new TreeMap<>();

    private Set<String> notSupportCancelJobType = new HashSet<>();
    private Set<String> notSupportResumeJobType = new HashSet<>();
    private Set<String> notSupportCleanJobType = new HashSet<>();

    @Override
    public LongJob getLongJob(String jobName) {
        LongJob job = allLongJob.get(jobName);
        if (null == job) {
            throw new OperationFailureException(operr("%s has no corresponding longjob", jobName));
        }
        return ObjectUtils.newAndCopy(job, job.getClass());
    }

    @Override
    public boolean start() {
        LongJob job = null;
        Set<Class<?>> longJobClasses = BeanUtils.reflections.getTypesAnnotatedWith(LongJobFor.class);
        for (Class it : longJobClasses) {
            LongJobFor at = (LongJobFor) it.getAnnotation(LongJobFor.class);
            try {
                job = (LongJob) it.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
            if (null == job) {
                logger.warn(String.format("[LongJob] class name [%s] but get LongJob instance is null ", at.getClass().getSimpleName()));
                continue;
            }
            logger.debug(String.format("[LongJob] collect class [%s]", job.getClass().getSimpleName()));

            String jobName = at.value().getSimpleName();

            LongJob old = allLongJob.get(jobName);
            if (old != null) {
                throw new CloudRuntimeException(String.format("[LongJob] duplicate LongJob[name: %s] and LongJob[name: %s] for %s",
                        it.getCanonicalName(), old.getClass().getCanonicalName(), jobName));
            }

            allLongJob.put(jobName, job);
            fullJobName.put(jobName, at.value().getName());

            checkBehaviorSupported(jobName, job);
        }
        return true;
    }

    @Override
    public boolean supportCancel(String jobName) {
        return !notSupportCancelJobType.contains(jobName);
    }

    @Override
    public boolean supportResume(String jobName) {
        return !notSupportResumeJobType.contains(jobName);
    }

    @Override
    public boolean supportClean(String jobName) {
        return !notSupportCleanJobType.contains(jobName);
    }

    private void checkBehaviorSupported(String jobName, LongJob job) {
        for (Method method : job.getClass().getMethods()) {
            if (method.getName().equals("cancel") && method.isDefault()) {
                notSupportCancelJobType.add(jobName);
            }

            if (method.getName().equals("resume") && method.isDefault()) {
                notSupportResumeJobType.add(jobName);
            }

            if (method.getName().equals("clean") && method.isDefault()) {
                notSupportCleanJobType.add(jobName);
            }

        }
    }

    @Override
    public TreeMap<String, String> getFullJobName() {
        return fullJobName;
    }

    @Override
    public boolean stop() {
        allLongJob.clear();
        return true;
    }
}
