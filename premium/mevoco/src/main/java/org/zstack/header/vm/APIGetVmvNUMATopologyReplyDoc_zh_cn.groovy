package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取虚拟机 NUMA 拓扑信息的请求返回"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APIGetVmvNUMATopologyReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	field {
		name "name"
		desc "虚拟机名称"
		type "String"
		since "3.13.12"
	}
	field {
		name "uuid"
		desc "虚拟机 UUID"
		type "String"
		since "3.13.12"
	}
	field {
		name "hostUuid"
		desc "主机 UUID"
		type "String"
		since "3.13.12"
	}
	field {
		name "topology"
		desc "虚拟机 NUMA 拓扑信息"
		type "List"
		since "3.13.12"
	}
}
