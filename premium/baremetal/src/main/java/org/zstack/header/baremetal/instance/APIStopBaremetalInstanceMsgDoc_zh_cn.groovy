package org.zstack.header.baremetal.instance

import org.zstack.header.baremetal.instance.APIStopBaremetalInstanceEvent

doc {
	title "StopBaremetalInstance"

	category "baremetal.instance"

	desc """关闭裸机实例"""

	rest {
		request {
			url "PUT /v1/baremetal/instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIStopBaremetalInstanceMsg.class

			desc """关闭裸机实例"""

			params {

				column {
					name "uuid"
					enclosedIn "stopBaremetalInstance"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.6.0"
				}
				column {
					name "type"
					enclosedIn "stopBaremetalInstance"
					desc "裸机实例关闭方式"
					location "body"
					type "String"
					optional true
					since "2.6.0"
					values ("grace","cold")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "2.6.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "2.6.0"
				}
			}
		}

		response {
			clz APIStopBaremetalInstanceEvent.class
		}
	}
}