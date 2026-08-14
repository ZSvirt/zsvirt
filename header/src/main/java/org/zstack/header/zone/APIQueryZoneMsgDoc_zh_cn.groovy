package org.zstack.header.zone

import org.zstack.header.zone.APIQueryZoneReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询区域(QueryZone)"

	category "zone"

	desc """管理员可以使用QueryZone来查询区域"""

	rest {
		request {
			url "GET /v1/zones"
			url "GET /v1/zones/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryZoneMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryZoneReply.class
		}
	}
}