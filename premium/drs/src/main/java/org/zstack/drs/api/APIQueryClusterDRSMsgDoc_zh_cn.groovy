package org.zstack.drs.api

import org.zstack.drs.api.APIQueryClusterDRSReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryClusterDRS"

	category "drs"

	desc """查询集群DRS"""

	rest {
		request {
			url "GET /v1/clusters/drs"
			url "GET /v1/clusters/drs/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryClusterDRSMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryClusterDRSReply.class
		}
	}
}