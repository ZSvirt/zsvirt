package org.zstack.ipsec

import org.zstack.ipsec.APIRemoveRemoteCidrsFromIPsecConnectionEvent

doc {
	title "RemoveRemoteCidrsFromIPsecConnection"

	category "ipsec"

	desc """从IPsec连接删除远端网段"""

	rest {
		request {
			url "DELETE /v1/ipsec/{uuid}/remote-cidrs"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRemoveRemoteCidrsFromIPsecConnectionMsg.class

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
					name "peerCidrs"
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
			clz APIRemoveRemoteCidrsFromIPsecConnectionEvent.class
		}
	}
}