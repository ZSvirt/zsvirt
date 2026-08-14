package org.zstack.drs.api

import org.zstack.drs.api.APIQueryDRSAdviceReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryDRSAdvice"

	category "drs"

	desc """查询集群DRS调度建议"""

	rest {
		request {
			url "GET /v1/clusters/drs/advice"
			url "GET /v1/clusters/drs/advice/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryDRSAdviceMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryDRSAdviceReply.class
		}
	}
}