package org.zstack.sns.platform.microsoftteams

import org.zstack.sns.platform.microsoftteams.APIQuerySNSMicrosoftTeamsEndpointReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QuerySNSMicrosoftTeamsEndpoint"

	category "sns"

	desc """查询微软Teams接收端"""

	rest {
		request {
			url "GET /v1/sns/application-endpoints/microsoft-teams"
			url "GET /v1/sns/application-endpoints/microsoft-teams/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQuerySNSMicrosoftTeamsEndpointMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQuerySNSMicrosoftTeamsEndpointReply.class
		}
	}
}