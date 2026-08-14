package org.zstack.header.baremetal.instance

import org.zstack.header.baremetal.instance.APIExpungeBaremetalInstanceEvent

doc {
	title "ExpungeBaremetalInstance"

	category "baremetal.instance"

	desc """彻底删除裸机实例"""

	rest {
		request {
			url "PUT /v1/baremetal/instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIExpungeBaremetalInstanceMsg.class

			desc """删除裸机实例"""

			params {

				column {
					name "uuid"
					enclosedIn "expungeBaremetalInstance"
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
			}
		}

		response {
			clz APIExpungeBaremetalInstanceEvent.class
		}
	}
}