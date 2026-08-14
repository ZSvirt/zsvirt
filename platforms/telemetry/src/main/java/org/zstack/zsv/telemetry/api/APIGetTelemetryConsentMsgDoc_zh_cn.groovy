package org.zstack.zsv.telemetry.api

import org.zstack.zsv.telemetry.api.APIGetTelemetryConsentReply

doc {
	title "GetTelemetryConsent"

	category "telemetry"

	desc """查询 Telemetry 用户体验计划的授权状态，返回用户同意采集与上传的时间点；未授权时为 None。"""

	rest {
		request {
			url "GET /v1/telemetry/consent"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetTelemetryConsentMsg.class

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
			clz APIGetTelemetryConsentReply.class
		}
	}
}