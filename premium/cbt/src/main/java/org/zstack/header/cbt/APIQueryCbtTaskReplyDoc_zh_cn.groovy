package org.zstack.header.cbt

import org.zstack.header.cbt.CbtTaskInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询 CBT 任务的返回结果"

	ref {
		name "inventories"
		path "org.zstack.header.cbt.APIQueryCbtTaskReply.inventories"
		desc "CBT 任务清单列表"
		type "List"
		since "4.10.10"
		clz CbtTaskInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "4.10.10"
	}
	ref {
		name "error"
		path "org.zstack.header.cbt.APIQueryCbtTaskReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.10.10"
		clz ErrorCode.class
	}
}
