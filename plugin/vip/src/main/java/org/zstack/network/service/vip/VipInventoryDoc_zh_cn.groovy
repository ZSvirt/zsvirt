package org.zstack.network.service.vip

import java.lang.Integer
import org.zstack.network.service.vip.VipNetworkServicesRefInventory
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "虚拟IP清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "0.6"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "0.6"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "0.6"
	}
	field {
		name "l3NetworkUuid"
		desc "三层网络UUID"
		type "String"
		since "0.6"
	}
	field {
		name "ip"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "state"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "gateway"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "netmask"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "prefixLen"
		desc ""
		type "Integer"
		since "0.6"
	}
	field {
		name "serviceProvider"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "peerL3NetworkUuids"
		desc ""
		type "List"
		since "0.6"
	}
	ref {
		name "servicesRefs"
		path "org.zstack.network.service.vip.VipInventory.servicesRefs"
		desc "null"
		type "List"
		since "0.6"
		clz VipNetworkServicesRefInventory.class
	}
	field {
		name "useFor"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "system"
		desc ""
		type "boolean"
		since "0.6"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "0.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "0.6"
	}
}
