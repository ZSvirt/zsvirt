package org.zstack.ha;

import org.zstack.compute.vm.VmSchedHistoryRecorder;
import org.zstack.core.Platform;
import org.zstack.core.progress.TaskTracker;
import org.zstack.header.vm.VmInstanceVO;

import java.util.Collections;

public class VmHaTaskTracker extends TaskTracker {
    public static final String TASK_NAME = VmSchedHistoryRecorder.TYPE_HA;

    public static final String PARAM_PROCESS = "haProcess";
    public static final String FIRE_ID = "fireId";

    private String fireId = Platform.getUuid();

    public enum Process {
        success(false),
        failure(false),
        checking(true),
        starting(false),
        noNeed(true);

        private boolean readOnly;

        Process(boolean readOnly) {
            this.readOnly = readOnly;
        }

        public boolean isReadOnly() {
            return readOnly;
        }
    }

    public VmHaTaskTracker(String resourceUuid) {
        super(resourceUuid);
    }

    @Override
    protected String getResourceType() {
        return VmInstanceVO.class.getSimpleName();
    }

    @Override
    protected String getTaskName() {
        return TASK_NAME;
    }

    public VmHaTaskTracker track(Process process, String info){
        param(FIRE_ID, fireId);
        track(info, Collections.singletonMap(PARAM_PROCESS, process));
        return this;
    }
}
