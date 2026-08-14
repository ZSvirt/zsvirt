package org.zstack.header.managementnode

doc {

	title "管理节点清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "hostName"
		desc "宿主机名称"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "joinDate"
		desc "加入时间"
		type "Timestamp"
		since "zsv 0.6"
	}
	field {
		name "heartBeat"
		desc "心跳时间"
		type "Timestamp"
		since "zsv 0.6"
	}
}
