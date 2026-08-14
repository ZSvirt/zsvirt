package org.zstack.baremetal.instance;

/**
 * Created by GuoYi on 7/5/18.
 */
public class BaremetalInstanceCommands {
    public static class NotifyDeployBeginCmd {
        public String baremetalInstanceUuid;
    }

    public static class NotifyDeployCompleteCmd {
        public String baremetalInstanceUuid;
    }

    public static class NotifyOSRunningCmd {
        public String baremetalInstanceUuid;
    }
}