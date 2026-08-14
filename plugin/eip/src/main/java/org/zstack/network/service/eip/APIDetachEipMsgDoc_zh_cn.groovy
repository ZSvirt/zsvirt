package org.zstack.network.service.eip

import org.zstack.network.service.eip.APIDetachEipEvent

doc {
	title "解绑弹性IP(DetachEip)"

	category "弹性IP"

	desc """解绑弹性IP"""

	rest {
		request {
			url "DELETE /v1/eips/{uuid}/vm-instances/nics"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDetachEipMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "弹性IP的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIDetachEipEvent.class
		}
	}
}