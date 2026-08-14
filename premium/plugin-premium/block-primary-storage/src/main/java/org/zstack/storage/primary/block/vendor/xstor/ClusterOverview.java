package org.zstack.storage.primary.block.vendor.xstor;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/8/10 10:21
 */
public class ClusterOverview {
    public Integer active_alarms_num;
    public String cluster_data_state;
    public String cluster_healthy_state;
    public String cluster_running_state;

    public String getCluster_data_state() {
        return cluster_data_state;
    }

    public Integer getActive_alarms_num() {
        return active_alarms_num;
    }

    public String getCluster_healthy_state() {
        return cluster_healthy_state;
    }

    public String getCluster_running_state() {
        return cluster_running_state;
    }

    public Boolean isClusterHealth() {
        if (cluster_data_state == null || cluster_healthy_state == null || cluster_running_state == null) {
            return false;
        } else if (cluster_running_state.equals("SYSTEM_RUNNING") || cluster_running_state.equals("SYSTEM_UPGRADING")) {
            return true;
        } else {
            return false;
        }
    }
}
