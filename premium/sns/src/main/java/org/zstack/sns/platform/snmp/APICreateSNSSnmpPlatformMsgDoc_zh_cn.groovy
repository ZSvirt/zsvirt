package org.zstack.sns.platform.snmp

import org.zstack.sns.APICreateSNSApplicationPlatformEvent

doc {
	title "CreateSNSSnmpPlatform"

	category "sns"

	desc """在这里填写API描述"""

	rest {
		request {
			url "POST /v1/sns/application-platforms/snmp"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateSNSSnmpPlatformMsg.class

			desc """"""

			params {

				column {
					name "snmpAddress"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "3.17.11"
				}
				column {
					name "snmpPort"
					enclosedIn "params"
					desc ""
					location "body"
					type "Integer"
					optional false
					since "3.17.11"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "3.17.11"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.17.11"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "3.17.11"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
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
			clz APICreateSNSApplicationPlatformEvent.class
		}
	}
}