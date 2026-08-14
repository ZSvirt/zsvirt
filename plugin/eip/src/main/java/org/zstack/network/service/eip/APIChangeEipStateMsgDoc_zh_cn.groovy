package org.zstack.network.service.eip

import org.zstack.network.service.eip.APIChangeEipStateEvent

doc {
	title "更改弹性IP状态(ChangeEipState)"

	category "弹性IP"

	desc """更改弹性IP状态"""

	rest {
		request {
			url "PUT /v1/eips/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeEipStateMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "changeEipState"
					desc "弹性IP的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "stateEvent"
					enclosedIn "changeEipState"
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
			clz APIChangeEipStateEvent.class
		}
	}
}