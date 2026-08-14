package org.zstack.zsv.telemetry.api

import org.zstack.zsv.telemetry.entity.TelemetrySettingView
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取 Telemetry 展示配置的结果"

	ref {
		name "inventory"
		path "org.zstack.zsv.telemetry.api.APIGetTelemetrySettingReply.inventory"
		desc "Telemetry 展示配置"
		type "TelemetrySettingView"
		since "zsv 5.1.0"
		clz TelemetrySettingView.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "zsv 5.1.0"
	}
	ref {
		name "error"
		path "org.zstack.zsv.telemetry.api.APIGetTelemetrySettingReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 5.1.0"
		clz ErrorCode.class
	}
}
