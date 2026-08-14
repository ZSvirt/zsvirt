package org.zstack.header.host



doc {

	title "物理机IPMI信息"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
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
		type "int"
		since "zsv 0.6"
	}
	field {
		name "ipmiPowerStatus"
		desc "IPMI电源状态"
		type "String"
		since "zsv 0.6"
	}
}
