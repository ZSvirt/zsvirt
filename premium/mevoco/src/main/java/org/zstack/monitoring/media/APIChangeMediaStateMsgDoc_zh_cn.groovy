package org.zstack.monitoring.media

import org.zstack.monitoring.media.APIChangeMediaStateEvent

doc {
	title "ChangeMediaState"

	category "monitoring"

	desc """改变媒体状态"""

	rest {
		request {
			url "PUT /v1/media/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeMediaStateMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "changeMediaState"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "stateEvent"
					enclosedIn "changeMediaState"
					desc "状态事件，enable/disable"
					location "body"
					type "String"
					optional false
					since "0.6"
					values ("enable","disable")
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
			clz APIChangeMediaStateEvent.class
		}
	}
}