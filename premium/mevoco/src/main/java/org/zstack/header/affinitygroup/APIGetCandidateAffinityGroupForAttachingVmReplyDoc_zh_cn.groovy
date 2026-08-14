package org.zstack.header.affinitygroup

import org.zstack.header.affinitygroup.AffinityGroupInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取可绑定虚拟机的亲和组的请求结果"

	ref {
		name "error"
		path "org.zstack.header.affinitygroup.APIGetCandidateAffinityGroupForAttachingVmReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.10.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.affinitygroup.APIGetCandidateAffinityGroupForAttachingVmReply.inventories"
		desc "亲和组清单列表"
		type "List"
		since "3.10.0"
		clz AffinityGroupInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.10.0"
	}
}
