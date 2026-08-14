package org.zstack.zsv.telemetry.entity



doc {

	title "Telemetry 授权状态"

	field {
		name "consentGrantedAt"
		desc "用户同意采集与上传的 UTC 时间（ISO-8601，如 2026-07-07T07:51Z）；未授权或未同意时为 None"
		type "String"
		since "zsv 5.1.0"
	}
}
