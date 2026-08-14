package org.zstack.zsv.telemetry.api

import org.zstack.zsv.telemetry.entity.TelemetryConsentView
import org.zstack.header.errorcode.ErrorCode

doc {

	title "更新 Telemetry 授权状态的结果"

	ref {
		name "inventory"
		path "org.zstack.zsv.telemetry.api.APIUpdateTelemetryConsentEvent.inventory"
		desc "更新后的 Telemetry 授权状态"
		type "TelemetryConsentView"
		since "zsv 5.1.0"
		clz TelemetryConsentView.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "zsv 5.1.0"
	}
	ref {
		name "error"
		path "org.zstack.zsv.telemetry.api.APIUpdateTelemetryConsentEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 5.1.0"
		clz ErrorCode.class
	}
}
