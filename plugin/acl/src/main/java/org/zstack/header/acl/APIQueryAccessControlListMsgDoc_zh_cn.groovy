package org.zstack.header.acl

import org.zstack.header.acl.APIQueryAccessControlListReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryAccessControlList"

	category "acl"

	desc """查询访问控制策略组"""

	rest {
		request {
			url "GET /v1/access-control-lists"
			url "GET /v1/access-control-lists/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryAccessControlListMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryAccessControlListReply.class
		}
	}
}