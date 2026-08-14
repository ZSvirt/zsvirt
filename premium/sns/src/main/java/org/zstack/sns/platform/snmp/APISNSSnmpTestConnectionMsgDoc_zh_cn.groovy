package org.zstack.sns.platform.snmp

import org.zstack.sns.platform.snmp.APISNSSnmpTestConnectionEvent

doc {
	title "SNSSnmpTestConnection"

	category "sns"

	desc """SNMP发送测试消息"""

	rest {
		request {
			url "POST /v1/sns/application-endpoints/snmp/test-connection"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISNSSnmpTestConnectionMsg.class

			desc """SNMP发送测试消息"""

			params {

				column {
					name "platformUuid"
					enclosedIn "params"
					desc "SNMP 平台uuid"
					location "body"
					type "String"
					optional true
					since "4.10.0"
				}
				column {
					name "endpointUuid"
					enclosedIn "params"
					desc "SNMP 端点uuid"
					location "body"
					type "String"
					optional true
					since "4.10.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.10.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.10.0"
				}
			}
		}

		response {
			clz APISNSSnmpTestConnectionEvent.class
		}
	}
}