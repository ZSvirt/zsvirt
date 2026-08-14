package org.zstack.header.storage.primary

import org.zstack.header.errorcode.ErrorCode

doc {

	title "发现集群物理机上未受当前平台管控的主存储返回"

	field {
		name "inventories"
		desc "发现的主存储清单列表"
		type "List"
		since "zsv 5.0.0"
	}
	field {
		name "success"
		desc "操作是否成功"
		type "boolean"
		since "zsv 5.0.0"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.primary.APIDiscoverStrangePrimaryStorageReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 5.0.0"
		clz ErrorCode.class
	}
}
