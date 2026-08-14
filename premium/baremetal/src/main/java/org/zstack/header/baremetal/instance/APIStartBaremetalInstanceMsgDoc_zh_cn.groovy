package org.zstack.header.baremetal.instance

import org.zstack.header.baremetal.instance.APIStartBaremetalInstanceEvent

doc {
	title "StartBaremetalInstance"

	category "baremetal.instance"

	desc """启动裸机实例"""

	rest {
		request {
			url "PUT /v1/baremetal/instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIStartBaremetalInstanceMsg.class

			desc """启动裸机实例"""

			params {

				column {
					name "uuid"
					enclosedIn "startBaremetalInstance"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.6.0"
				}
				column {
					name "pxeBoot"
					enclosedIn "startBaremetalInstance"
					desc "是否通过网络PXE启动"
					location "body"
					type "Boolean"
					optional true
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
			}
		}

		response {
			clz APIStartBaremetalInstanceEvent.class
		}
	}
}