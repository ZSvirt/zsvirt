package org.zstack.network.service.portforwarding

import org.zstack.network.service.portforwarding.APIChangePortForwardingRuleStateEvent

doc {
	title "ChangePortForwardingRuleState"

	category "portForwarding"

	desc """改变端口转发规则的状态"""

	rest {
		request {
			url "PUT /v1/port-forwarding/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangePortForwardingRuleStateMsg.class

			desc """改变端口转发规则的状态"""

			params {

				column {
					name "uuid"
					enclosedIn "changePortForwardingRuleState"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "stateEvent"
					enclosedIn "changePortForwardingRuleState"
					desc "端口转发规则的状态"
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
			clz APIChangePortForwardingRuleStateEvent.class
		}
	}
}