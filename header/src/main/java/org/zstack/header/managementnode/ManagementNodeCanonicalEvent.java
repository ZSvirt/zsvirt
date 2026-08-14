package org.zstack.header.managementnode;

import org.zstack.header.message.NeedJsonSchema;

/**
 * Created by xing5 on 2016/7/1.
 */
public class ManagementNodeCanonicalEvent {
    public static final String NODE_LIFECYCLE_PATH = "/managementnode/lifecycle";
    public static final String NODE_TEMPORAL_REGRESSION_PATH = "/managementnode/temporal/regression";

    public enum LifeCycle {
        NodeJoin,
        NodeLeft
    }

    public static class ManagementNodeTemporalRegressionData {
        private String nodeUuid;
        private String hostname;

        public String getNodeUuid() {
            return nodeUuid;
        }

        public void setNodeUuid(String nodeUuid) {
            this.nodeUuid = nodeUuid;
        }

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }
    }

    @NeedJsonSchema
    public static class ManagementNodeLifeCycleData {
        private String nodeUuid;
        private String lifeCycle;
        private ManagementNodeInventory inventory;

        public String getNodeUuid() {
            return nodeUuid;
        }

        public void setNodeUuid(String nodeUuid) {
            this.nodeUuid = nodeUuid;
        }

        public String getLifeCycle() {
            return lifeCycle;
        }

        public void setLifeCycle(String lifeCycle) {
            this.lifeCycle = lifeCycle;
        }

        public ManagementNodeInventory getInventory() {
            return inventory;
        }

        public void setInventory(ManagementNodeInventory inventory) {
            this.inventory = inventory;
        }
    }
}
