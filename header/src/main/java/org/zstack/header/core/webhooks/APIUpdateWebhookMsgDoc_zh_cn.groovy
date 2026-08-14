package org.zstack.header.core.webhooks

import org.zstack.header.core.webhooks.APIUpdateWebhookEvent

doc {
	title "UpdateWebhook"

	category "webhook"

	desc """在这里填写API描述"""

	rest {
		request {
			url "PUT /v1/web-hooks/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateWebhookMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateWebhook"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "name"
					enclosedIn "updateWebhook"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "description"
					enclosedIn "updateWebhook"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "url"
					enclosedIn "updateWebhook"
					desc ""
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "type"
					enclosedIn "updateWebhook"
					desc ""
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "opaque"
					enclosedIn "updateWebhook"
					desc ""
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIUpdateWebhookEvent.class
		}
	}
}