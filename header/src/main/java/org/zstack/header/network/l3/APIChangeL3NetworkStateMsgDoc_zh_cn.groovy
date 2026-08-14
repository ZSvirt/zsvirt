package org.zstack.header.network.l3

import org.zstack.header.network.l3.APIChangeL3NetworkStateEvent

doc {
	title "改变三层网络状态(ChangeL3NetworkState)"

	category "三层网络"

	desc """改变三层网络状态"""

	rest {
		request {
			url "PUT /v1/l3-networks/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeL3NetworkStateMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "changeL3NetworkState"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "stateEvent"
					enclosedIn "changeL3NetworkState"
					desc "状态，可选 enable 与 disable"
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
			clz APIChangeL3NetworkStateEvent.class
		}
	}
}