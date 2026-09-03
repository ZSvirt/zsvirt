package org.zstack.sso.header

import org.zstack.sso.header.APIGetSSOClientReply

doc {
	title "GetSSOClient"

	category "sso"

	desc """获取 SSO 客户端"""

	rest {
		request {
			url "GET /v1/get/sso/client"



			clz APIGetSSOClientMsg.class

			desc """"""

			params {

				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "4.3.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "4.3.0"
				}
			}
		}

		response {
			clz APIGetSSOClientReply.class
		}
	}
}