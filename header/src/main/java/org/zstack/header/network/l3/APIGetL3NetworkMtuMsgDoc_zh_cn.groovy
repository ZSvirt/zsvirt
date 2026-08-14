package org.zstack.header.network.l3

import org.zstack.header.network.l3.APIGetL3NetworkMtuReply

doc {
	title "GetL3NetworkMtu"

	category "network.l3"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/l3-networks/{l3NetworkUuid}/mtu"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetL3NetworkMtuMsg.class

			desc """"""

			params {

				column {
					name "l3NetworkUuid"
					enclosedIn ""
					desc "三层网络UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIGetL3NetworkMtuReply.class
		}
	}
}