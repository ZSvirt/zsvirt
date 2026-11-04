package org.zstack.twoFactorAuthentication

import org.zstack.twoFactorAuthentication.APIQueryTwoFactorAuthenticationReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryTwoFactorAuthentication"

	category "twoFactorAuthentication"

	desc """查询双因子认证密匙"""

	rest {
		request {
			url "GET /v1/twofactorauthentication/secrets"
			url "GET /v1/twofactorauthentication/secrets/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryTwoFactorAuthenticationMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryTwoFactorAuthenticationReply.class
		}
	}
}