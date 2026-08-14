package org.zstack.storage.device.localRaid

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.ErrorCode

doc {

	title "在这里输入结构的名称"

	ref {
		name "error"
		path "org.zstack.storage.device.localRaid.APISelfTestLocalRaidEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.6"
		clz ErrorCode.class
	}
	field {
		name "result"
		desc ""
		type "String"
		since "3.6"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "3.6"
	}
}
