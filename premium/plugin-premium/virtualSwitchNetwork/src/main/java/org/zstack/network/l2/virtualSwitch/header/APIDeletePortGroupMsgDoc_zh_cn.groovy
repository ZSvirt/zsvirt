package org.zstack.network.l2.virtualSwitch.header

import org.zstack.network.l2.virtualSwitch.header.APIDeletePortGroupEvent

doc {
	title "删除端口组(DeletePortGroup)"

	category "network.l2"

	desc """删除端口组"""

	rest {
		request {
			url "DELETE /v1/l3-networks/port-group/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeletePortGroupMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "4.2.0"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "query"
					type "String"
					optional true
					since "4.2.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "4.2.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "4.2.0"
				}
			}
		}

		response {
			clz APIDeletePortGroupEvent.class
		}
	}
}