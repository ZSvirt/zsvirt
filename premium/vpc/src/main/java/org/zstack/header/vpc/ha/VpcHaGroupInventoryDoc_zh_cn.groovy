package org.zstack.header.vpc.ha

import org.zstack.header.vpc.ha.VpcHaGroupMonitorIpInventory
import org.zstack.header.vpc.ha.VpcHaGroupApplianceVmRefInventory
import org.zstack.header.vpc.ha.VpcHaGroupNetworkServiceRefInventory
import org.zstack.header.vpc.ha.VpcHaGroupVipRefInventory
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "高可用组清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.5"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "3.5"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "3.5"
	}
	ref {
		name "monitors"
		path "org.zstack.header.vpc.ha.VpcHaGroupInventory.monitors"
		desc "null"
		type "List"
		since "3.5"
		clz VpcHaGroupMonitorIpInventory.class
	}
	ref {
		name "vrRefs"
		path "org.zstack.header.vpc.ha.VpcHaGroupInventory.vrRefs"
		desc "null"
		type "List"
		since "3.5"
		clz VpcHaGroupApplianceVmRefInventory.class
	}
	ref {
		name "services"
		path "org.zstack.header.vpc.ha.VpcHaGroupInventory.services"
		desc "null"
		type "List"
		since "3.5"
		clz VpcHaGroupNetworkServiceRefInventory.class
	}
	ref {
		name "usedIps"
		path "org.zstack.header.vpc.ha.VpcHaGroupInventory.usedIps"
		desc "null"
		type "List"
		since "3.5"
		clz VpcHaGroupVipRefInventory.class
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.5"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.5"
	}
}
