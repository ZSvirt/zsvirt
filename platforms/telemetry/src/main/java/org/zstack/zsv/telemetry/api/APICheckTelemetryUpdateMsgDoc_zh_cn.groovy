package org.zstack.zsv.telemetry.api

import org.zstack.zsv.telemetry.api.APICheckTelemetryUpdateEvent

doc {
	title "CheckTelemetryUpdate"

	category "telemetry"

	desc """检查产品更新：向 Telemetry Cloud 提交阉割版检查更新数据并返回版本与中英文更新说明。不依赖用户体验计划授权状态；License 过期时仍可调用。"""

	rest {
		request {
			url "PUT /v1/telemetry/updates/check"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICheckTelemetryUpdateMsg.class

			desc """"""

			params {

				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 5.1.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 5.1.0"
				}
			}
		}

		response {
			clz APICheckTelemetryUpdateEvent.class
		}
	}
}