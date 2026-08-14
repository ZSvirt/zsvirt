package org.zstack.network.service.eip

import org.zstack.network.service.eip.APIDeleteEipEvent

doc {
	title "删除弹性IP(DeleteEip)"

	category "弹性IP"

	desc """删除弹性IP"""

	rest {
		request {
			url "DELETE /v1/eips/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteEipMsg.class

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
					name "deleteMode"
					enclosedIn ""
					desc "删除模式"
					location "query"
					type "String"
					optional true
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
			clz APIDeleteEipEvent.class
		}
	}
}