package org.zstack.ha;

import org.zstack.core.Platform;
import org.zstack.core.progress.TaskTracker;
import org.zstack.header.host.HostVO;

public class HostCheckAliveTaskTracker extends TaskTracker {
    public static final String TASK_NAME = "HostCheckAlive";

    public static final String PARAM_PROCESS = "process";
    public static final String PROCESS_ID = "processId";

    private final String progressId = Platform.getUuid();

    public enum Process {
        judgeDead,
        judgeAlive,
        checking;

        static Process mapFrom(HostCheckResult.HostScanResult stage){
            return stage.isAlive() ? judgeAlive : judgeDead;
        }
    }

    public HostCheckAliveTaskTracker(String hostUuid) {
        super(hostUuid);
    }

    @Override
    protected String getResourceType() {
        return HostVO.class.getSimpleName();
    }

    @Override
    protected String getTaskName() {
        return TASK_NAME;
    }

    public TaskTracker track(Process process, String info){
        param(PROCESS_ID, progressId);
        param(PARAM_PROCESS, process.toString());
        track(info);
        return this;
    }
}
