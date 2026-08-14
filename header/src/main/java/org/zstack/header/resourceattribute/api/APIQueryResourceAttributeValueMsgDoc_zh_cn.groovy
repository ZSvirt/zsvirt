package org.zstack.header.resourceattribute.api

import org.zstack.header.resourceattribute.api.APIQueryResourceAttributeValueReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryResourceAttributeValue"

	category "resourceAttribute"

	desc """查询自定义属性值"""

	rest {
		request {
			url "GET /v1/resource-attributes"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryResourceAttributeValueMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryResourceAttributeValueReply.class
		}
	}
}