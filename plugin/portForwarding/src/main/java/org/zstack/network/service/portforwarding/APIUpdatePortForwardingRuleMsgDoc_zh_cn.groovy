package org.zstack.network.service.portforwarding

import org.zstack.network.service.portforwarding.APIUpdatePortForwardingRuleEvent

doc {
	title "UpdatePortForwardingRule"

	category "portForwarding"

	desc """更新端口转发规则"""

	rest {
		request {
			url "PUT /v1/port-forwarding/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdatePortForwardingRuleMsg.class

			desc """更新端口转发规则"""

			params {

				column {
					name "uuid"
					enclosedIn "updatePortForwardingRule"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "name"
					enclosedIn "updatePortForwardingRule"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "updatePortForwardingRule"
					desc "资源的详细描述"
					location "body"
					type "String"
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
			clz APIUpdatePortForwardingRuleEvent.class
		}
	}
}