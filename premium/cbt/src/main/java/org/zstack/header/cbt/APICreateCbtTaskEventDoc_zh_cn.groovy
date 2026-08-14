package org.zstack.header.cbt

import org.zstack.header.cbt.CbtTaskInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "创建 CBT 任务消息的返回结果"

	ref {
		name "inventory"
		path "org.zstack.header.cbt.APICreateCbtTaskEvent.inventory"
		desc "CBT 任务清单"
		type "CbtTaskInventory"
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
		path "org.zstack.header.cbt.APICreateCbtTaskEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.10.10"
		clz ErrorCode.class
	}
}
