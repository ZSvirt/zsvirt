package org.zstack.drs.api

import org.zstack.header.errorcode.ErrorCode

doc {

	title "应用 DRS 建议的结果"

	field {
		name "vmMigrationActivityUuid"
		desc "虚拟机迁移活动 UUID"
		type "String"
		since "4.0.0"
	}
	field {
		name "success"
		desc "应用是否成功"
		type "boolean"
		since "4.0.0"
	}
	ref {
		name "error"
		path "org.zstack.drs.api.APIApplyDRSAdviceEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.0.0"
		clz ErrorCode.class
	}
}
