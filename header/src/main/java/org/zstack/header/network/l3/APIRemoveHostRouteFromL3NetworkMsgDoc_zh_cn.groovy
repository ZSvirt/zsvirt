package org.zstack.header.network.l3

import org.zstack.header.network.l3.APIRemoveHostRouteFromL3NetworkEvent

doc {
	title "RemoveHostRouteFromL3Network"

	category "network.l3"

	desc """从三层网络移除主机路由"""

	rest {
		request {
			url "DELETE /v1/l3-networks/{l3NetworkUuid}/hostroute"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRemoveHostRouteFromL3NetworkMsg.class

			desc """"""

			params {

				column {
					name "l3NetworkUuid"
					enclosedIn ""
					desc "三层网络UUID"
					location "url"
					type "String"
					optional false
					since "zsv 2.3"
				}
				column {
					name "prefix"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional false
					since "zsv 2.3"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "zsv 2.3"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "zsv 2.3"
				}
			}
		}

		response {
			clz APIRemoveHostRouteFromL3NetworkEvent.class
		}
	}
}