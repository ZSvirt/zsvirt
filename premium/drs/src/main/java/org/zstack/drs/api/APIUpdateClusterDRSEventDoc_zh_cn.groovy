package org.zstack.drs.api

import org.zstack.drs.entity.ClusterDRSInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "更新集群DRS的结果"

	ref {
		name "inventory"
		path "org.zstack.drs.api.APIUpdateClusterDRSEvent.inventory"
		desc "集群DRS"
		type "ClusterDRSInventory"
		since "4.0.0"
		clz ClusterDRSInventory.class
	}
	field {
		name "success"
		desc "更新是否成功"
		type "boolean"
		since "4.0.0"
	}
	ref {
		name "error"
		path "org.zstack.drs.api.APIUpdateClusterDRSEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.0.0"
		clz ErrorCode.class
	}
}
