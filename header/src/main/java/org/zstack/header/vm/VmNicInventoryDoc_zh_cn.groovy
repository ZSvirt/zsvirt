package org.zstack.header.vm

import java.lang.Integer
import org.zstack.header.network.l3.UsedIpInventory
import java.sql.Timestamp

doc {

	title "云主机网卡清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "zsv 3.17.13"
	}
	field {
		name "vmInstanceUuid"
		desc "云主机UUID"
		type "String"
		since "zsv 3.17.13"
	}
	field {
		name "l3NetworkUuid"
		desc "三层网络UUID"
		type "String"
		since "zsv 3.17.13"
	}
	field {
		name "ip"
		desc ""
		type "String"
		since "zsv 3.17.13"
	}
	field {
		name "mac"
		desc ""
		type "String"
		since "zsv 3.17.13"
	}
	field {
		name "hypervisorType"
		desc ""
		type "String"
		since "zsv 3.17.13"
	}
	field {
		name "netmask"
		desc ""
		type "String"
		since "zsv 3.17.13"
	}
	field {
		name "gateway"
		desc ""
		type "String"
		since "zsv 3.17.13"
	}
	field {
		name "metaData"
		desc ""
		type "String"
		since "zsv 3.17.13"
	}
	field {
		name "ipVersion"
		desc ""
		type "Integer"
		since "zsv 3.17.13"
	}
	field {
		name "driverType"
		desc ""
		type "String"
		since "zsv 3.17.13"
	}
	ref {
		name "usedIps"
		path "org.zstack.header.vm.VmNicInventory.usedIps"
		desc "null"
		type "List"
		since "zsv 3.17.13"
		clz UsedIpInventory.class
	}
	field {
		name "internalName"
		desc ""
		type "String"
		since "zsv 3.17.13"
	}
	field {
		name "deviceId"
		desc ""
		type "Integer"
		since "zsv 3.17.13"
	}
	field {
		name "type"
		desc ""
		type "String"
		since "zsv 3.17.13"
	}
	field {
		name "state"
		desc "网卡状态"
		type "String"
		since "zsv 3.17.13"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv 3.17.13"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv 3.17.13"
	}
}
