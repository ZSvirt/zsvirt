package org.zstack.header.network.l3

import org.zstack.header.network.l3.APISetL3NetworkMtuEvent

doc {
	title "SetL3NetworkMtu"

	category "network.l3"

	desc """在这里填写API描述"""

	rest {
		request {
			url "POST /v1/l3-networks/{l3NetworkUuid}/mtu"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISetL3NetworkMtuMsg.class

			desc """"""

			params {

				column {
					name "l3NetworkUuid"
					enclosedIn "params"
					desc "三层网络UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "mtu"
					enclosedIn "params"
					desc ""
					location "body"
					type "Integer"
					optional false
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APISetL3NetworkMtuEvent.class
		}
	}
}