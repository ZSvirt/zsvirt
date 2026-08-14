package org.zstack.loginControl.api

import org.zstack.loginControl.api.APIQueryAccessControlRuleReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryAccessControlRule"

	category "loginControl"

	desc """查询IP访问控制规则请求"""

	rest {
		request {
			url "GET /v1/login-control/access-control/rules"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryAccessControlRuleMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryAccessControlRuleReply.class
		}
	}
}