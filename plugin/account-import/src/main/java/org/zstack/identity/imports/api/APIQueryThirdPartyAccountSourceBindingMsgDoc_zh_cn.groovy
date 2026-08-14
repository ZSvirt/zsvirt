package org.zstack.identity.imports.api

import org.zstack.identity.imports.api.APIQueryThirdPartyAccountSourceBindingReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryThirdPartyAccountSourceBinding"

	category "ldap"

	desc """查询第三方用户来源绑定关系"""

	rest {
		request {
			url "GET /v1/account-import/bindings"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryThirdPartyAccountSourceBindingMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryThirdPartyAccountSourceBindingReply.class
		}
	}
}