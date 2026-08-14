package org.zstack.header.core.webhooks

import org.zstack.header.core.webhooks.APIQueryWebhookReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryWebhook"

	category "webhook"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/web-hooks"
			url "GET /v1/web-hooks/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryWebhookMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryWebhookReply.class
		}
	}
}