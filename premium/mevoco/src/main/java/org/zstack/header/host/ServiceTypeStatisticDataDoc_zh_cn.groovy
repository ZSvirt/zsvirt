package org.zstack.header.host

import java.sql.Timestamp

doc {

	title "网卡网络服务情况"

	field {
		name "interfaceUuid"
		desc ""
		type "网卡UUID"
		since "3.17.11"
	}
	field {
		name "interfaceName"
		desc "网卡名"
		type "String"
		since "3.17.11"
	}
	field {
		name "vlanId"
		desc "vlan接口ID"
		type "Integer"
		since "3.17.11"
	}
	field {
		name "serviceTypes"
		desc "网络服务类型集"
		type "List"
		since "3.17.11"
	}
	field {
		name "hostUuid"
		desc "物理机UUID"
		type "String"
		since "3.17.11"
	}
	field {
		name "hostName"
		desc "物理机名称"
		type "String"
		since "3.17.11"
	}
	field {
		name "hostIp"
		desc "物理机IP"
		type "String"
		since "3.17.11"
	}
	field {
		name "clusterUuid"
		desc "集群UUID"
		type "String"
		since "3.17.11"
	}
	field {
		name "clusterName"
		desc "集群名称"
		type "String"
		since "3.17.11"
	}
	field {
		name "zoneUuid"
		desc "区域UUID"
		type "String"
		since "3.17.11"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.17.11"
	}
}
