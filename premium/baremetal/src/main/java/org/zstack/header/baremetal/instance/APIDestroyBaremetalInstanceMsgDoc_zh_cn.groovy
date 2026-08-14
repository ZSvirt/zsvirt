package org.zstack.header.baremetal.instance

import org.zstack.header.baremetal.instance.APIDestroyBaremetalInstanceEvent

doc {
	title "DestroyBaremetalInstance"

	category "baremetal.instance"

	desc """删除裸机实例"""

	rest {
		request {
			url "DELETE /v1/baremetal/instances/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDestroyBaremetalInstanceMsg.class

			desc """删除裸机实例"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.6.0"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式"
					location "query"
					type "String"
					optional true
					since "2.6.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "2.6.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "2.6.0"
				}
			}
		}

		response {
			clz APIDestroyBaremetalInstanceEvent.class
		}
	}
}