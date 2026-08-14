package org.zstack.zsv.storage.api

import org.zstack.zsv.storage.entity.CephPluginConnectionView
import org.zstack.header.errorcode.ErrorCode

doc {

	title "检查环境中的 Ceph 插件结果"

	field {
		name "inventories"
		path "org.zstack.zsv.storage.api.APICheckCephPluginReply.inventories"
		desc "检查出的 Ceph 插件列表"
		type "List"
		since "4.10.7"
		clz CephPluginConnectionView.class
	}
	field {
		name "success"
		desc "检查是否成功"
		type "boolean"
		since "4.10.7"
	}
	ref {
		name "error"
		path "org.zstack.zsv.storage.api.APICheckCephPluginReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.10.7"
		clz ErrorCode.class
	}
}
