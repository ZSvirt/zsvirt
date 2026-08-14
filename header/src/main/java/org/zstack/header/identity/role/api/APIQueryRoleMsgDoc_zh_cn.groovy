package org.zstack.header.identity.role.api

import org.zstack.header.identity.role.api.APIQueryRoleReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryRole"

	category "rbac"

	desc """查询角色"""

	rest {
		request {
			url "GET /v1/identities/roles"
			url "GET /v1/identities/roles/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryRoleMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryRoleReply.class
		}
	}
}