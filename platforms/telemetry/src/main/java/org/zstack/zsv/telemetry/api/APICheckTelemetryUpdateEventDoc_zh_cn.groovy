package org.zstack.zsv.telemetry.api

import org.zstack.zsv.telemetry.entity.TelemetryUpdateInfoView
import org.zstack.header.errorcode.ErrorCode

doc {

	title "检查更新的结果"

	ref {
		name "inventory"
		path "org.zstack.zsv.telemetry.api.APICheckTelemetryUpdateEvent.inventory"
		desc "检查更新返回的版本与更新说明"
		type "TelemetryUpdateInfoView"
		since "zsv 5.1.0"
		clz TelemetryUpdateInfoView.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "zsv 5.1.0"
	}
	ref {
		name "error"
		path "org.zstack.zsv.telemetry.api.APICheckTelemetryUpdateEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 5.1.0"
		clz ErrorCode.class
	}
}
