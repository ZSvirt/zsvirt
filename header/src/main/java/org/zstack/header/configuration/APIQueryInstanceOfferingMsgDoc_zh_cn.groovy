package org.zstack.header.configuration

import org.zstack.header.configuration.APIQueryInstanceOfferingReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryInstanceOffering"

	category "configuration"

	desc """查询云主机规格"""

	rest {
		request {
			url "GET /v1/instance-offerings"
			url "GET /v1/instance-offerings/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryInstanceOfferingMsg.class

			desc """查询云主机规格"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryInstanceOfferingReply.class
		}
	}
}