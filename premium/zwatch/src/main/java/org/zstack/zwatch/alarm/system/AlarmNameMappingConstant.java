package org.zstack.zwatch.alarm.system;

import org.zstack.zwatch.namespace.HostNamespace;
import org.zstack.zwatch.namespace.VRouterNamespace;
import org.zstack.zwatch.namespace.VmNamespace;

public class AlarmNameMappingConstant {
    static class ActiveAlarm {
        public static final String HOST_CPU_AVERAGE_USED_UTILIZATION = String.format("Active-%s-%s", HostNamespace.NAME, HostNamespace.CPUAverageUsedUtilization.getName());
        public static final String HOST_CPU_AVERAGE_USED_UTILIZATION_CN = "主机平均CPU使用率";
        public static final String HOST_DISK_ALL_USED_CAPACITY_IN_PERCENT = String.format("Active-%s-%s", HostNamespace.NAME, HostNamespace.DiskAllUsedCapacityInPercent.getName());
        public static final String HOST_DISK_ALL_USED_CAPACITY_IN_PERCENT_CN = "主机全部磁盘已使用容量百分比";
        public static final String HOST_MEMORY_USED_IN_PERCENT = String.format("Active-%s-%s", HostNamespace.NAME, HostNamespace.MemoryUsedInPercent.getName());
        public static final String HOST_MEMORY_USED_IN_PERCENT_CN = "主机内存使用百分比";
        public static final String VM_CPU_AVERAGE_USED_UTILIZATION = String.format("Active-%s-%s", VmNamespace.NAME, VmNamespace.CPUAverageUsedUtilization.getName());
        public static final String VM_CPU_AVERAGE_USED_UTILIZATION_CN = "虚拟机平均CPU使用率";
        public static final String VM_DISK_ALL_USED_CAPACITY_IN_PERCENT = String.format("Active-%s-%s", VmNamespace.NAME, VmNamespace.DiskAllUsedCapacityInPercent.getName());
        public static final String VM_DISK_ALL_USED_CAPACITY_IN_PERCENT_CN = "虚拟机全部硬盘已使用容量百分比(需安装VMTools)";
        public static final String VM_MEMORY_USED_IN_PERCENT = String.format("Active-%s-%s", VmNamespace.NAME, VmNamespace.MemoryUsedInPercent.getName());
        public static final String VM_MEMORY_USED_IN_PERCENT_CN = "虚拟机内存已用百分比";
        public static final String VM_OPERATING_SYSTEM_CPU_AVERAGE_USED_UTILIZATION = String.format("Active-%s-%s", VmNamespace.NAME, VmNamespace.OperatingSystemCPUAverageUsedUtilization.getName());
        public static final String VM_OPERATING_SYSTEM_CPU_AVERAGE_USED_UTILIZATION_CN = "虚拟机CPU平均使用率(需安装VMTools)";
        public static final String VM_OPERATING_SYSTEM_MEMORY_USED_PERCENT = String.format("Active-%s-%s", VmNamespace.NAME, VmNamespace.OperatingSystemMemoryUsedPercent.getName());
        public static final String VM_OPERATING_SYSTEM_MEMORY_USED_PERCENT_CN = "虚拟机内存已用百分比(需安装VMTools)";
        public static final String VROUTER_CPU_AVERAGE_USED_UTILIZATION = String.format("Active-%s-%s", VRouterNamespace.NAME, VRouterNamespace.VRouterCPUAverageUsedUtilization.getName());
        public static final String VROUTER_CPU_AVERAGE_USED_UTILIZATION_CN = "VPC路由器平均CPU使用率";
        public static final String VROUTER_DISK_ALL_USED_CAPACITY_IN_PERCENT = String.format("Active-%s-%s", VRouterNamespace.NAME, VRouterNamespace.VRouterDiskAllUsedCapacityInPercent.getName());
        public static final String VROUTER_DISK_ALL_USED_CAPACITY_IN_PERCENT_CN = "VPC路由器全部硬盘已使用容量百分比";
        public static final String VROUTER_MEMORY_USED_PERCENT = String.format("Active-%s-%s", VRouterNamespace.NAME, VRouterNamespace.VRouterMemoryUsedPercent.getName());
        public static final String VROUTER_MEMORY_USED_PERCENT_CN = "VPC路由器内存已用百分比";
    }

    static class VCenter {
        public static final String VCENTER_HOST_WRONG_DATE_TIME_NAME = "vCenter Host Time Abnormal";
        public static final String VCENTER_HOST_WRONG_DATE_TIME_NAME_CN = "vCenter主机时间异常";

        public static final String VCENTER_RESOURCE_EVENT_NAME = "vCenter Event Message";
        public static final String VCENTER_RESOURCE_EVENT_NAME_CN = "vCenter事件消息";
    }

    static class VM {
        public static final String VM_STATE_CHANGED_ON_HOST_NAME = "VM State Changed On Host";
        public static final String VM_STATE_CHANGED_ON_HOST_NAME_CN = "虚拟机在主机上的状态发生变化";
    }

    static class Scheduler {
        public static final String SCHEDULER_JOB_GROUP_EXECUTED_RESULT_NAME = "Job Failed";
        public static final String SCHEDULER_JOB_GROUP_EXECUTED_RESULT_NAME_CN = "任务结果失败";
    }

    static class IAM2Project {
        public static final String IAM2_PROJEC_RETIRE_NAME = "Project Has Been Recycled";
        public static final String IAM2_PROJEC_RETIRE_NAME_CN = "项目已回收";
    }
}
