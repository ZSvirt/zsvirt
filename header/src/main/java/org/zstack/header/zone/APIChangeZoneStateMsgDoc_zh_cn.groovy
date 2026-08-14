package org.zstack.header.zone

import org.zstack.header.zone.APIChangeZoneStateEvent

doc {
	title "改变区域的可用状态(ChangeZoneState)"

	category "zone"

	desc """改变区域的可用状态"""

	rest {
		request {
			url "PUT /v1/zones/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeZoneStateMsg.class

			desc """改变区域的可用状态"""

			params {

				column {
					name "uuid"
					enclosedIn "changeZoneState"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "stateEvent"
					enclosedIn "changeZoneState"
					desc "状态触发事件"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
					values ("enable","disable")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIChangeZoneStateEvent.class
		}
	}
}