package org.zstack.ha.sns;

import org.zstack.core.asyncbatch.While;
import org.zstack.core.progress.TaskTracker;
import org.zstack.core.thread.GroupedConsumeQueue;
import org.zstack.ha.HaGlobalConfig;
import org.zstack.ha.HostCheckAliveTaskTracker;
import org.zstack.ha.VmHaTaskTracker;
import org.zstack.header.Component;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.NopeWhileDoneCompletion;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

public class HaProgressNotification implements Component {
    private static final CLogger logger = Utils.getLogger(HaProgressNotification.class);

    private List<String> registerTaskName = Arrays.asList(VmHaTaskTracker.TASK_NAME, HostCheckAliveTaskTracker.TASK_NAME);

    private TaskPublisher publisher = new TaskPublisher();
    private GroupedConsumeQueue<VmHaTaskTracker.Task> vmProgressQueue;

    private boolean enabled;

    @Override
    public boolean start() {
        enabled = HaGlobalConfig.HA_NOTIFICATION_TIMELINESS.value(Integer.class) >= 0;
        configVmProgressNotification();
        registerTaskName.forEach(name -> TaskTracker.registerConsumer(name, this::handleTask));

        HaGlobalConfig.HA_NOTIFICATION_TIMELINESS.installUpdateExtension((oldConfig, newConfig) -> {
            vmProgressQueue.setMaxDelayedTime(newConfig.value(Integer.class));
            enabled = HaGlobalConfig.HA_NOTIFICATION_TIMELINESS.value(Integer.class) >= 0;
        });
        return true;
    }

    @Override
    public boolean stop() {
        return false;
    }

    private void configVmProgressNotification() {
        vmProgressQueue = new GroupedConsumeQueue<VmHaTaskTracker.Task>(HaGlobalConfig.HA_NOTIFICATION_TIMELINESS.value(Integer.class)) {
            @Override
            protected void consume(List<VmHaTaskTracker.Task> items) {
                if (items.size() == 1) {
                    publisher.notify(items.get(0));
                    return;
                }

                boolean useless = items.get(0).parameters.get(VmHaTaskTracker.PARAM_PROCESS) == VmHaTaskTracker.Process.starting &&
                        items.get(items.size() - 1).parameters.get(VmHaTaskTracker.PARAM_PROCESS) == VmHaTaskTracker.Process.failure;
                if (useless) {
                    logger.debug(String.format("[haQueue] abandon a useless queue[vmUuid:%s, process: %s]",
                            items.get(0).resourceUuid, items.stream().map(t -> t.parameters.get(VmHaTaskTracker.PARAM_PROCESS)).collect(Collectors.toList())));
                    return;
                }

                new While<>(items).each((item, compl) -> publisher.notify(item, new NoErrorCompletion() {
                    @Override
                    public void done() {
                        compl.done();
                    }
                })).run(new NopeWhileDoneCompletion());
            }

            @Override
            protected String getGroupId(VmHaTaskTracker.Task item) {
                return (String) item.parameters.getOrDefault(VmHaTaskTracker.FIRE_ID, "");
            }
        };
        vmProgressQueue.start();
    }

    public void handleTask(TaskTracker.Task task) {
        if (!enabled) {
            return;
        }

        if (!task.taskName.equals(VmHaTaskTracker.TASK_NAME)) {
            publisher.notify(task);
            return;
        }

        VmHaTaskTracker.Process process = (VmHaTaskTracker.Process) task.parameters.get(VmHaTaskTracker.PARAM_PROCESS);
        if (!process.isReadOnly()) {
            vmProgressQueue.offer(task);
        }
    }
}
