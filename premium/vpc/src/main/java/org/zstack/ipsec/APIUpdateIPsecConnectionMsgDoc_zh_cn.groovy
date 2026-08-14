package org.zstack.ipsec

import org.zstack.ipsec.APIUpdateIPsecConnectionEvent

doc {
	title "UpdateIPsecConnection"

	category "ipsec"

	desc """更新IPSec连接的信息"""

	rest {
		request {
			url "PUT /v1/ipsec/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateIPsecConnectionMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateIPsecConnection"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "name"
					enclosedIn "updateIPsecConnection"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "updateIPsecConnection"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIUpdateIPsecConnectionEvent.class
		}
	}
}