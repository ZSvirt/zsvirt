package org.zstack.network.l2.vxlan.vxlanNetwork

import java.lang.Integer
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "二层VXLAN网络清单"

	field {
		name "vni"
		desc "Vni号"
		type "Integer"
		since "0.6"
	}
	field {
		name "poolUuid"
		desc "VXLAN资源池UUID"
		type "String"
		since "0.6"
	}
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
		name "zoneUuid"
		desc "区域UUID"
		type "String"
		since "0.6"
	}
	field {
		name "physicalInterface"
		desc "物理网卡"
		type "String"
		since "0.6"
	}
	field {
		name "type"
		desc "二层网络类型"
		type "String"
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
		name "attachedClusterUuids"
		desc "挂载集群的UUID列表"
		type "List"
		since "0.6"
	}
}
