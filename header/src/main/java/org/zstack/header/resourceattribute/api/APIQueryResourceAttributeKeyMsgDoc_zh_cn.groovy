package org.zstack.header.resourceattribute.api

import org.zstack.header.resourceattribute.api.APIQueryResourceAttributeKeyReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryResourceAttributeKey"

	category "resourceAttribute"

	desc """查询自定义属性键"""

	rest {
		request {
			url "GET /v1/resource-attributes/keys"
			url "GET /v1/resource-attributes/keys/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryResourceAttributeKeyMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryResourceAttributeKeyReply.class
		}
	}
}