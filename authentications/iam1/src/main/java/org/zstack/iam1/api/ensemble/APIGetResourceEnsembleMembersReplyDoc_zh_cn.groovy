package org.zstack.iam1.api.ensemble

import org.zstack.iam1.entity.ensemble.ResourceEnsembleInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取资源组中的所有成员结果"

	ref {
		name "inventory"
		path "org.zstack.iam1.api.ensemble.APIGetResourceEnsembleMembersReply.inventory"
		desc "资源组中的所有成员结果"
		type "ResourceEnsembleInventory"
		since "4.10.0"
		clz ResourceEnsembleInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "4.10.0"
	}
	ref {
		name "error"
		path "org.zstack.iam1.api.ensemble.APIGetResourceEnsembleMembersReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.10.0"
		clz ErrorCode.class
	}
}
