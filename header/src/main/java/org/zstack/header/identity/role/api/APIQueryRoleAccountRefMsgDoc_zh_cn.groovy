package org.zstack.header.identity.role.api

import org.zstack.header.identity.role.api.APIQueryRoleAccountRefReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryRoleAccountRef"

	category "rbac"

	desc """查询角色账户关系"""

	rest {
		request {
			url "GET /v1/identities/role-account-refs"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryRoleAccountRefMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryRoleAccountRefReply.class
		}
	}
}