package org.zstack.zwatch.alarm

import org.zstack.zwatch.alarm.APIUpdateSubscribeEventEvent

doc {
	title "UpdateSubscribeEvent"

	category "zwatch.alarm"

	desc """修改事件报警器"""

	rest {
		request {
			url "PUT /v1/zwatch/events/subscriptions/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateSubscribeEventMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateSubscribeEvent"
					desc "事件报警器UUID"
					location "url"
					type "String"
					optional false
					since "3.8"
				}
				column {
					name "emergencyLevel"
					enclosedIn "updateSubscribeEvent"
					desc "报警等级"
					location "body"
					type "String"
					optional true
					since "3.8"
					values ("Emergent","Important","Normal")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.8"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.8"
				}
				column {
					name "name"
					enclosedIn "updateSubscribeEvent"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIUpdateSubscribeEventEvent.class
		}
	}
}