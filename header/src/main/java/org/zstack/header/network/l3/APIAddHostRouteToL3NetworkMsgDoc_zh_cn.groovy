package org.zstack.header.network.l3

import org.zstack.header.network.l3.APIAddHostRouteToL3NetworkEvent

doc {
	title "AddHostRouteToL3Network"

	category "network.l3"

	desc """向三层网络添加DNS主机路由"""

	rest {
		request {
			url "POST /v1/l3-networks/{l3NetworkUuid}/hostroute"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddHostRouteToL3NetworkMsg.class

			desc """"""

			params {

				column {
					name "l3NetworkUuid"
					enclosedIn "params"
					desc "三层网络UUID"
					location "url"
					type "String"
					optional false
					since "zsv 2.3"
				}
				column {
					name "prefix"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "zsv 2.3"
				}
				column {
					name "nexthop"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "zsv 2.3"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 2.3"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 2.3"
				}
			}
		}

		response {
			clz APIAddHostRouteToL3NetworkEvent.class
		}
	}
}