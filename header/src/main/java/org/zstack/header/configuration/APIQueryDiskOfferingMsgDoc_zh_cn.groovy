package org.zstack.header.configuration

import org.zstack.header.configuration.APIQueryDiskOfferingReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryDiskOffering"

	category "configuration"

	desc """查询云盘规格"""

	rest {
		request {
			url "GET /v1/disk-offerings"
			url "GET /v1/disk-offerings/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryDiskOfferingMsg.class

			desc """查询云盘规格"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryDiskOfferingReply.class
		}
	}
}