package org.zstack.header.host

import org.zstack.header.host.APIPowerOffHostEvent

doc {
	title "PowerOffHost"

	category "host"

	desc """在这里填写API描述"""

	rest {
		request {
			url "PUT /v1/hosts/power-off/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIPowerOffHostMsg.class

			desc """"""

			params {

				column {
					name "adminPassword"
					enclosedIn "powerOffHost"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "hostUuids"
					enclosedIn "powerOffHost"
					desc ""
					location "body"
					type "List"
					optional false
					since "0.6"
				}
				column {
					name "waitTaskCompleted"
					enclosedIn "powerOffHost"
					desc ""
					location "body"
					type "boolean"
					optional true
					since "0.6"
				}
				column {
					name "maxWaitTime"
					enclosedIn "powerOffHost"
					desc ""
					location "body"
					type "Long"
					optional true
					since "0.6"
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
			clz APIPowerOffHostEvent.class
		}
	}
}