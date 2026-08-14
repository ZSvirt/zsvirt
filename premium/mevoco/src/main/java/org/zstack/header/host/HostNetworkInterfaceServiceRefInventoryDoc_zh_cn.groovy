package org.zstack.header.host

import java.lang.Integer
import org.zstack.header.host.HostNetworkInterfaceServiceType
import java.sql.Timestamp

doc {

	title "物理网卡服务类型详细信息"

	field {
		name "interfaceUuid"
		desc "物理网口Uuid"
		type "String"
		since "3.17.11"
	}
	field {
		name "vlanId"
		desc "vlan子接口id"
		type "Integer"
		since "3.17.11"
	}
	ref {
		name "serviceType"
		path "org.zstack.header.host.HostNetworkInterfaceServiceRefInventory.serviceType"
		desc "网路服务类型"
		type "HostNetworkInterfaceServiceType"
		since "3.17.11"
		clz HostNetworkInterfaceServiceType.class
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.17.11"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.17.11"
	}
}
