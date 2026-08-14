package org.zstack.zwatch.api

import org.zstack.header.errorcode.ErrorCode
import org.zstack.zwatch.datatype.AlarmDataV2

doc {

	title "报警器历史记录返回结构"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.zwatch.api.APIGetAlarmDataReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.3"
		clz ErrorCode.class
	}
	ref {
		name "histories"
		path "org.zstack.zwatch.api.APIGetAlarmDataReply.histories"
		desc "历史纪录列表"
		type "List"
		since "2.3"
		clz AlarmDataV2.class
	}
}
