package org.zstack.header.affinitygroup

import org.zstack.header.affinitygroup.AffinityGroupInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "创建虚拟机获取可用非亲和组"

	ref {
		name "error"
		path "org.zstack.header.affinitygroup.APIGetCandidateAffinityGroupForCreatingVmReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.11.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.affinitygroup.APIGetCandidateAffinityGroupForCreatingVmReply.inventories"
		desc "亲和组清单列表"
		type "List"
		since "3.11.0"
		clz AffinityGroupInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.11.0"
	}
}
