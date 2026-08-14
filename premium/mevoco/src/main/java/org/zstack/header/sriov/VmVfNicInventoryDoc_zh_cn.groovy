package org.zstack.header.sriov

import java.lang.Integer
import org.zstack.header.network.l3.UsedIpInventory
import java.sql.Timestamp

doc {

	title "云主机VF网卡资源清单"

	field {
		name "pciDeviceUuid"
		desc "网卡PCI地址"
		type "String"
		since "4.10.0"
	}
	field {
		name "haState"
		desc "高可用状态"
		type "String"
		since "4.10.0"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "4.10.0"
	}
	field {
		name "vmInstanceUuid"
		desc "云主机UUID"
		type "String"
		since "4.10.0"
	}
	field {
		name "l3NetworkUuid"
		desc "三层网络UUID"
		type "String"
		since "4.10.0"
	}
	field {
		name "ip"
		desc ""
		type "String"
		since "4.10.0"
	}
	field {
		name "mac"
		desc ""
		type "String"
		since "4.10.0"
	}
	field {
		name "hypervisorType"
		desc ""
		type "String"
		since "4.10.0"
	}
	field {
		name "netmask"
		desc ""
		type "String"
		since "4.10.0"
	}
	field {
		name "gateway"
		desc ""
		type "String"
		since "4.10.0"
	}
	field {
		name "metaData"
		desc ""
		type "String"
		since "4.10.0"
	}
	field {
		name "ipVersion"
		desc ""
		type "Integer"
		since "4.10.0"
	}
	field {
		name "driverType"
		desc ""
		type "String"
		since "4.10.0"
	}
	ref {
		name "usedIps"
		path "org.zstack.header.sriov.VmVfNicInventory.usedIps"
		desc "null"
		type "List"
		since "4.10.0"
		clz UsedIpInventory.class
	}
	field {
		name "internalName"
		desc ""
		type "String"
		since "4.10.0"
	}
	field {
		name "deviceId"
		desc ""
		type "Integer"
		since "4.10.0"
	}
	field {
		name "type"
		desc ""
		type "String"
		since "4.10.0"
	}
	field {
		name "state"
		desc ""
		type "String"
		since "4.10.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "4.10.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "4.10.0"
	}
}
