package org.zstack.iam1.api.accounts

import org.zstack.iam1.api.accounts.APIQueryAccountGroupReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryAccountGroup"

	category "iam1Accounts"

	desc """查询账户组"""

	rest {
		request {
			url "GET /v1/account-groups"
			url "GET /v1/account-groups/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryAccountGroupMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryAccountGroupReply.class
		}
	}
}