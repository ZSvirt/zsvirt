package org.zstack.sso.header

import org.zstack.sso.header.APIGetOAuth2TokenReply

doc {
	title "GetOAuth2Token"

	category "sso"

	desc """获取 OAuth2 Token"""

	rest {
		request {
			url "GET /v1/get/oauth2/token"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetOAuth2TokenMsg.class

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
			clz APIGetOAuth2TokenReply.class
		}
	}
}