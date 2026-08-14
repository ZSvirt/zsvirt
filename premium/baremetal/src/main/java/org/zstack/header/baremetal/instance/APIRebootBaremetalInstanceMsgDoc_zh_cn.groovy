package org.zstack.header.baremetal.instance

import org.zstack.header.baremetal.instance.APIRebootBaremetalInstanceEvent

doc {
	title "RebootBaremetalInstance"

	category "baremetal.instance"

	desc """重启裸机实例"""

	rest {
		request {
			url "PUT /v1/baremetal/instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRebootBaremetalInstanceMsg.class

			desc """重启裸机实例"""

			params {

				column {
					name "uuid"
					enclosedIn "rebootBaremetalInstance"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.6.0"
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
				column {
					name "pxeBoot"
					enclosedIn "rebootBaremetalInstance"
					desc "是否通过网络PXE启动"
					location "body"
					type "Boolean"
					optional true
					since "2.6.0"
				}
			}
		}

		response {
			clz APIRebootBaremetalInstanceEvent.class
		}
	}
}