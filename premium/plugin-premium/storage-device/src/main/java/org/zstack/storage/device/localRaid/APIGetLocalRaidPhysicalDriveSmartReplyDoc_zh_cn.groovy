package org.zstack.storage.device.localRaid

import org.zstack.header.errorcode.ErrorCode
import org.zstack.storage.device.localRaid.SmartDataStruct

doc {

	title "获取raid物理结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.storage.device.localRaid.APIGetLocalRaidPhysicalDriveSmartReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.6"
		clz ErrorCode.class
	}
	ref {
		name "result"
		path "org.zstack.storage.device.localRaid.APIGetLocalRaidPhysicalDriveSmartReply.result"
		desc "null"
		type "List"
		since "3.6"
		clz SmartDataStruct.class
	}
}
