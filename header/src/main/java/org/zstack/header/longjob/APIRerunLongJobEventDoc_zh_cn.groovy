package org.zstack.header.longjob

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.longjob.LongJobInventory

doc {

	title "重新提交长任务结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.longjob.APIRerunLongJobEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 3.0.1"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.longjob.APIRerunLongJobEvent.inventory"
		desc "重新提交后的长任务清单"
		type "LongJobInventory"
		since "zsv 3.0.1"
		clz LongJobInventory.class
	}
}
