package org.zstack.network.securitygroup

import java.sql.Timestamp

doc {

	title "网卡的安全策略"

	field {
		name "vmNicUuid"
		desc "云主机网卡UUID"
		type "String"
		since "3.17.21"
	}
	field {
		name "ingressPolicy"
		desc "网卡入方向安全策略"
		type "String"
		since "3.17.21"
	}
	field {
		name "egressPolicy"
		desc "网卡出方向安全策略"
		type "String"
		since "3.17.21"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.17.21"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.17.21"
	}
}
