package org.zstack.header.network.l2

import org.zstack.header.network.l2.L2NetworkInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "从物理机上卸载二层网络结果"

	ref {
		name "inventory"
		path "org.zstack.header.network.l2.APIDetachL2NetworkFromHostEvent.inventory"
		desc "二层网络清单"
		type "L2NetworkInventory"
		since "zsv 3.18.0"
		clz L2NetworkInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "zsv 3.18.0"
	}
	ref {
		name "error"
		path "org.zstack.header.network.l2.APIDetachL2NetworkFromHostEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 3.18.0"
		clz ErrorCode.class
	}
}
