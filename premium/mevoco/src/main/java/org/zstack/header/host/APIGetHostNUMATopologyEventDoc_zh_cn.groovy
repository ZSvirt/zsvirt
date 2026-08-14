package org.zstack.header.host

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.host.HostNUMANode

doc {

	title "主机 NUMA 拓扑信息"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.13.12"
	}
	ref {
		name "error"
		path "org.zstack.header.host.APIGetHostNUMATopologyEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.13.12"
		clz ErrorCode.class
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "3.13.12"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.13.12"
	}
	ref {
		name "topology"
		path "org.zstack.header.host.APIGetHostNUMATopologyEvent.topology"
		desc "主机 NUMA 拓扑信息"
		type "Map"
		since "3.13.12"
		clz HostNUMANode.class
	}
}
