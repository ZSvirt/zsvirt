package org.zstack.zsv.telemetry.api

import org.zstack.zsv.telemetry.api.APIGetTelemetrySettingReply

doc {
	title "GetTelemetrySetting"

	category "telemetry"

	desc """查询 Telemetry 用户体验计划的展示配置，包括说明文案 i18n key 与隐私政策 URL。"""

	rest {
		request {
			url "GET /v1/telemetry/settings"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetTelemetrySettingMsg.class

			desc """"""

			params {

				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 5.1.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 5.1.0"
				}
			}
		}

		response {
			clz APIGetTelemetrySettingReply.class
		}
	}
}