package org.zstack.storage.device.multipath

import org.zstack.storage.device.multipath.MultipathTopologyStruct
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取物理机多路径拓扑结果"

	ref {
		name "devices"
		path "org.zstack.storage.device.multipath.APIGetHostMultipathTopologyReply.devices"
		desc "结果列表"
		type "List"
		since "4.10.10"
		clz MultipathTopologyStruct.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "4.10.10"
	}
	ref {
		name "error"
		path "org.zstack.storage.device.multipath.APIGetHostMultipathTopologyReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.10.10"
		clz ErrorCode.class
	}
}
