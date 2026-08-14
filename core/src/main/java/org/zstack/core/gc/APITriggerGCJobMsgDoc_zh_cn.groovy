package org.zstack.core.gc

import org.zstack.core.gc.APITriggerGCJobEvent

doc {
	title "TriggerGCJob"

	category "gc"

	desc """在这里填写API描述"""

	rest {
		request {
			url "PUT /v1/gc-jobs/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APITriggerGCJobMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "triggerGCJob"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APITriggerGCJobEvent.class
		}
	}
}