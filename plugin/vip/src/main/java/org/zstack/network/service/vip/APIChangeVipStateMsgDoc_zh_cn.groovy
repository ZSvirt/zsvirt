package org.zstack.network.service.vip

import org.zstack.network.service.vip.APIChangeVipStateEvent

doc {
	title "ChangeVipState"

	category "vip"

	desc """更改VIP启用状态"""

	rest {
		request {
			url "PUT /v1/vips/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeVipStateMsg.class

			desc """更改VIP启用状态"""

			params {

				column {
					name "uuid"
					enclosedIn "changeVipState"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "stateEvent"
					enclosedIn "changeVipState"
					desc "状态事件"
					location "body"
					type "String"
					optional false
					since "0.6"
					values ("enable","disable")
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
			clz APIChangeVipStateEvent.class
		}
	}
}