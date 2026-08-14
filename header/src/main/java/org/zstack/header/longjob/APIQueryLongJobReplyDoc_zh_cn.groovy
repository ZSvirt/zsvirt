package org.zstack.header.longjob

import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询所得长任务"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.longjob.APIQueryLongJobReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 2.2.4"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.longjob.APIQueryLongJobReply.inventories"
		desc "长任务列表"
		type "List"
		since "zsv 2.2.4"
		clz LongJobInventory.class
	}
}
