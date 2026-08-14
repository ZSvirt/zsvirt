package org.zstack.header.host

import java.lang.Long
import java.lang.Integer
import org.zstack.header.host.HwMonitorStatus
import java.sql.Timestamp

doc {

	title "在这里输入结构的名称"

	field {
		name "zoneUuid"
		desc "区域UUID"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "clusterUuid"
		desc "集群UUID"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "managementIp"
		desc ""
		type "String"
		since "zsv 0.6"
	}
	field {
		name "hypervisorType"
		desc ""
		type "String"
		since "zsv 0.6"
	}
	field {
		name "state"
		desc ""
		type "String"
		since "zsv 0.6"
	}
	field {
		name "status"
		desc ""
		type "String"
		since "zsv 0.6"
	}
	field {
		name "totalCpuCapacity"
		desc ""
		type "Long"
		since "zsv 0.6"
	}
	field {
		name "availableCpuCapacity"
		desc ""
		type "Long"
		since "zsv 0.6"
	}
	field {
		name "cpuSockets"
		desc "物理CPU插槽数量"
		type "Integer"
		since "zsv 0.6"
	}
	field {
		name "totalMemoryCapacity"
		desc ""
		type "Long"
		since "zsv 0.6"
	}
	field {
		name "availableMemoryCapacity"
		desc ""
		type "Long"
		since "zsv 0.6"
	}
	field {
		name "cpuNum"
		desc "逻辑CPU数量"
		type "Integer"
		since "zsv 0.6"
	}
	field {
		name "ipmiAddress"
		desc "IPMI地址"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "ipmiUsername"
		desc "IPMI用户名"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "ipmiPort"
		desc "IPMI端口"
		type "Integer"
		since "zsv 0.6"
	}
	field {
		name "ipmiPowerStatus"
		desc "IPMI电源状态"
		type "String"
		since "zsv 0.6"
	}
	ref {
		name "cpuStatus"
		path "org.zstack.header.host.HostInventory.cpuStatus"
		desc "CPU状态"
		type "HwMonitorStatus"
		since "zsv 4.10.0"
		clz HwMonitorStatus.class
	}
	ref {
		name "memoryStatus"
		path "org.zstack.header.host.HostInventory.memoryStatus"
		desc "内存状态"
		type "HwMonitorStatus"
		since "zsv 4.10.0"
		clz HwMonitorStatus.class
	}
	ref {
		name "diskStatus"
		path "org.zstack.header.host.HostInventory.diskStatus"
		desc "磁盘状态"
		type "HwMonitorStatus"
		since "zsv 4.10.0"
		clz HwMonitorStatus.class
	}
	ref {
		name "nicStatus"
		path "org.zstack.header.host.HostInventory.nicStatus"
		desc "网卡状态"
		type "HwMonitorStatus"
		since "zsv 4.10.0"
		clz HwMonitorStatus.class
	}
	ref {
		name "gpuStatus"
		path "org.zstack.header.host.HostInventory.gpuStatus"
		desc "GPU状态"
		type "HwMonitorStatus"
		since "zsv 4.10.0"
		clz HwMonitorStatus.class
	}
	ref {
		name "powerSupplyStatus"
		path "org.zstack.header.host.HostInventory.powerSupplyStatus"
		desc "电源状态"
		type "HwMonitorStatus"
		since "zsv 4.10.0"
		clz HwMonitorStatus.class
	}
	ref {
		name "fanStatus"
		path "org.zstack.header.host.HostInventory.fanStatus"
		desc "风扇状态"
		type "HwMonitorStatus"
		since "zsv 4.10.0"
		clz HwMonitorStatus.class
	}
	ref {
		name "raidStatus"
		path "org.zstack.header.host.HostInventory.raidStatus"
		desc "RAID状态"
		type "HwMonitorStatus"
		since "zsv 4.10.0"
		clz HwMonitorStatus.class
	}
	ref {
		name "temperatureStatus"
		path "org.zstack.header.host.HostInventory.temperatureStatus"
		desc "温度状态"
		type "HwMonitorStatus"
		since "zsv 4.10.0"
		clz HwMonitorStatus.class
	}
	field {
		name "architecture"
		desc ""
		type "String"
		since "zsv 0.6"
	}
	field {
		name "nqn"
		desc "NVMe 限定名称"
		type "String"
		since "zsv 4.10.6"
	}
	field {
		name "hostname"
		desc "主机名"
		type "String"
		since "zsv 4.10.20"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv 0.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv 0.6"
	}
}
