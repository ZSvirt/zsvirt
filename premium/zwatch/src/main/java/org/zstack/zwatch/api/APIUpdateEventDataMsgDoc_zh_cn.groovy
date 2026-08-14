package org.zstack.zwatch.api

import org.zstack.zwatch.api.APIUpdateEventDataEvent

doc {
	title "UpdateEventData"

	category "zwatch"

	desc """在这里填写API描述"""

	rest {
		request {
			url "PUT /v1/zwatch/events/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateEventDataMsg.class

			desc """"""

			params {

				column {
					name "dataUuid"
					enclosedIn "updateEventData"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "dataStartTime"
					enclosedIn "updateEventData"
					desc ""
					location "body"
					type "Long"
					optional true
					since "0.6"
				}
				column {
					name "dataEndTime"
					enclosedIn "updateEventData"
					desc ""
					location "body"
					type "Long"
					optional true
					since "0.6"
				}
				column {
					name "updateMode"
					enclosedIn "updateEventData"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
					values ("OnlyOne","InRange","All")
				}
				column {
					name "readStatus"
					enclosedIn "updateEventData"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("Read","Unread")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIUpdateEventDataEvent.class
		}
	}
}