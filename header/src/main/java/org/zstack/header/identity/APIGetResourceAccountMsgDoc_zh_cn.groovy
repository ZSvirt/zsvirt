package org.zstack.header.identity

import org.zstack.header.identity.APIGetResourceAccountReply

doc {
	title "查看资源所属账户"

	category "identity"

	desc """输入资源的UUID可以获得该资源所属账户"""

	rest {
		request {
			url "GET /v1/resources/accounts"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetResourceAccountMsg.class

			desc """"""

			params {

				column {
					name "resourceUuids"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
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
			clz APIGetResourceAccountReply.class
		}
	}
}