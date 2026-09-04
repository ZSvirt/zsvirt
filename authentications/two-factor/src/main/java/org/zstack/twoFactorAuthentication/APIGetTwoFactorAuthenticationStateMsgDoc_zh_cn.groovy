package org.zstack.twoFactorAuthentication

import org.zstack.twoFactorAuthentication.APIGetTwoFactorAuthenticationStateReply

doc {
	title "GetTwoFactorAuthenticationState"

	category "twoFactorAuthentication"

	desc """获取双因子认证状态"""

	rest {
		request {
			url "GET /v1/twofactorauthentication/state"



			clz APIGetTwoFactorAuthenticationStateMsg.class

			desc """"""

			params {

				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "4.10.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "4.10.0"
				}
			}
		}

		response {
			clz APIGetTwoFactorAuthenticationStateReply.class
		}
	}
}