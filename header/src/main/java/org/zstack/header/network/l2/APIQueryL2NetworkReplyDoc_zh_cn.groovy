package org.zstack.header.network.l2

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.network.l2.L2NetworkInventory

doc {

	title "二层网络清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.network.l2.APIQueryL2NetworkReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.network.l2.APIQueryL2NetworkReply.inventories"
		desc "null"
		type "List"
		since "zsv 0.6"
		clz L2NetworkInventory.class
	}
}
