package org.zstack.network.l2.vxlan.vxlanNetworkPool

import java.lang.Integer
import java.lang.Integer
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "vni范围清单"

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
		name "startVni"
		desc "起始VNI"
		type "Integer"
		since "0.6"
	}
	field {
		name "endVni"
		desc "结束VNI"
		type "Integer"
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
	field {
		name "l2NetworkUuid"
		desc "二层网络UUID"
		type "String"
		since "0.6"
	}
}
