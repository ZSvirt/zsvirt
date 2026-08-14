package org.zstack.network.l2.virtualSwitch.header

import org.zstack.network.l2.virtualSwitch.header.HostKernelInterfaceUsedIpInventory
import java.sql.Timestamp

doc {

	title "Kernel适配器清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "4.1.0"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "4.1.0"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "4.1.0"
	}
	field {
		name "hostUuid"
		desc "物理机UUID"
		type "String"
		since "4.1.0"
	}
	field {
		name "l2NetworkUuid"
		desc "二层网络UUID"
		type "String"
		since "4.1.0"
	}
	field {
		name "l3NetworkUuid"
		desc "三层网络UUID"
		type "String"
		since "4.1.0"
	}
	ref {
		name "usedIps"
		path "org.zstack.network.l2.virtualSwitch.header.HostKernelInterfaceInventory.usedIps"
		desc "null"
		type "List"
		since "4.1.0"
		clz HostKernelInterfaceUsedIpInventory.class
	}
	field {
		name "trafficTypes"
		desc "流量类型"
		type "List"
		since "4.1.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "4.1.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "4.1.0"
	}
}
