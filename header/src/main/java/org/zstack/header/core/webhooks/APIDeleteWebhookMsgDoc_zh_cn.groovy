package org.zstack.header.core.webhooks

import org.zstack.header.core.webhooks.APIDeleteWebhookEvent

doc {
	title "DeleteWebhook"

	category "webhook"

	desc """在这里填写API描述"""

	rest {
		request {
			url "DELETE /v1/web-hooks/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteWebhookMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIDeleteWebhookEvent.class
		}
	}
}