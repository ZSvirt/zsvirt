package org.zstack.drs.api

import org.zstack.drs.api.HostLoad
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取集群 DRS 详情的结果"

	ref {
		name "hostLoadOverThreshold"
		path "org.zstack.drs.api.APIGetClusterDRSStatusReply.hostLoadOverThreshold"
		desc "超过阈值的主机列表"
		type "List"
		since "4.0.0"
		clz HostLoad.class
	}
	field {
		name "success"
		desc "获取是否成功"
		type "boolean"
		since "4.0.0"
	}
	ref {
		name "error"
		path "org.zstack.drs.api.APIGetClusterDRSStatusReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.0.0"
		clz ErrorCode.class
	}
}
