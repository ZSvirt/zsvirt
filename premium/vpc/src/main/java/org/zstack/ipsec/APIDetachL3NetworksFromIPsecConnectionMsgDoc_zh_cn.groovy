package org.zstack.ipsec

import org.zstack.ipsec.APIDetachL3NetworksFromIPsecConnectionEvent

doc {
	title "DetachL3NetworksFromIPsecConnection"

	category "ipsec"

	desc """从IPsec连接删除三层网路"""

	rest {
		request {
			url "DELETE /v1/ipsec/{uuid}/l3networks"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDetachL3NetworksFromIPsecConnectionMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "l3NetworkUuids"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional false
					since "2.3"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "2.3"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "2.3"
				}
			}
		}

		response {
			clz APIDetachL3NetworksFromIPsecConnectionEvent.class
		}
	}
}