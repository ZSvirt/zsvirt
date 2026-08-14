package org.zstack.zwatch.api

import org.zstack.header.errorcode.ErrorCode
import org.zstack.zwatch.datatype.EventData

doc {

	title "获取事件返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.zwatch.api.APIGetEventDataReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.3"
		clz ErrorCode.class
	}
	ref {
		name "events"
		path "org.zstack.zwatch.api.APIGetEventDataReply.events"
		desc "null"
		type "List"
		since "2.3"
		clz EventData.class
	}
}
