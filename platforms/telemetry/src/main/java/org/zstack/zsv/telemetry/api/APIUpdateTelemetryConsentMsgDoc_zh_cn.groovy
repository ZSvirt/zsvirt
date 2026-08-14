package org.zstack.zsv.telemetry.api

import org.zstack.zsv.telemetry.api.APIUpdateTelemetryConsentEvent

doc {
	title "UpdateTelemetryConsent"

	category "telemetry"

	desc """开启或关闭 Telemetry 用户体验计划。开启时须勾选同意条款（agreedToTerms=true），并写入当前 UTC 同意时间；关闭时将授权时间重置为 None。"""

	rest {
		request {
			url "PUT /v1/telemetry/consent"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateTelemetryConsentMsg.class

			desc """"""

			params {

				column {
					name "action"
					enclosedIn "updateTelemetryConsent"
					desc "授权操作，Enabled 表示开启，Disabled 表示关闭"
					location "body"
					type "String"
					optional false
					since "zsv 5.1.0"
					values ("Enabled","Disabled")
				}
				column {
					name "agreedToTerms"
					enclosedIn "updateTelemetryConsent"
					desc "是否同意用户体验计划条款；action 为 Enabled 时须为 true"
					location "body"
					type "Boolean"
					optional true
					since "zsv 5.1.0"
				}
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
			clz APIUpdateTelemetryConsentEvent.class
		}
	}
}