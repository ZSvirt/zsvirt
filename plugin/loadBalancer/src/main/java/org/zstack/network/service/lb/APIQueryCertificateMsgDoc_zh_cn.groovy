package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIQueryCertificateReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryCertificate"

	category "loadBalancer"

	desc """查询证书"""

	rest {
		request {
			url "GET /v1/certificates"
			url "GET /v1/certificates/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryCertificateMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryCertificateReply.class
		}
	}
}