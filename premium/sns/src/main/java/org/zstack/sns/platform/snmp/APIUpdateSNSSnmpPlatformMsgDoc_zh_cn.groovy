package org.zstack.sns.platform.snmp

import org.zstack.sns.APIUpdateSNSApplicationPlatformEvent

doc {
	title "UpdateSNSSnmpPlatform"

	category "sns"

	desc """在这里填写API描述"""

	rest {
		request {
			url "PUT /v1/sns/application-platforms/snmp/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateSNSSnmpPlatformMsg.class

			desc """"""

			params {

				column {
					name "snmpAddress"
					enclosedIn "updateSNSSnmpPlatform"
					desc ""
					location "body"
					type "String"
					optional false
					since "3.17.11"
				}
				column {
					name "snmpPort"
					enclosedIn "updateSNSSnmpPlatform"
					desc ""
					location "body"
					type "Integer"
					optional false
					since "3.17.11"
				}
				column {
					name "uuid"
					enclosedIn "updateSNSSnmpPlatform"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.17.11"
				}
				column {
					name "name"
					enclosedIn "updateSNSSnmpPlatform"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "3.17.11"
				}
				column {
					name "description"
					enclosedIn "updateSNSSnmpPlatform"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.17.11"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.17.11"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.17.11"
				}
			}
		}

		response {
			clz APIUpdateSNSApplicationPlatformEvent.class
		}
	}
}