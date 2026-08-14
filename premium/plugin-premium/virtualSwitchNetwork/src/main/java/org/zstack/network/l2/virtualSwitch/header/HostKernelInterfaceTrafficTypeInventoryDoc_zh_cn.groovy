package org.zstack.network.l2.virtualSwitch.header

import org.zstack.network.l2.virtualSwitch.header.HostKernelInterfaceTrafficType
import java.sql.Timestamp

doc {

	title "Kernel适配器的流量类型清单"

	field {
		name "hostKernelInterfaceUuid"
		desc "Kernel适配器的UUID"
		type "String"
		since "4.1.0"
	}
	ref {
		name "trafficType"
		path "org.zstack.network.l2.virtualSwitch.header.HostKernelInterfaceTrafficTypeInventory.trafficType"
		desc "流量类型"
		type "HostKernelInterfaceTrafficType"
		since "4.1.0"
		clz HostKernelInterfaceTrafficType.class
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
