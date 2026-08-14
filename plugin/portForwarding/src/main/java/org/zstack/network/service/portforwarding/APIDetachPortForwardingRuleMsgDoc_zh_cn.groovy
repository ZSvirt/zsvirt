package org.zstack.network.service.portforwarding

import org.zstack.network.service.portforwarding.APIDetachPortForwardingRuleEvent

doc {
	title "DetachPortForwardingRule"

	category "portForwarding"

	desc """用户可以使用DetachPortForwardingRule来从一个虚拟机网卡卸载一个规则"""

	rest {
		request {
			url "DELETE /v1/port-forwarding/{uuid}/vm-instances/nics"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDetachPortForwardingRuleMsg.class

			desc """用户可以使用DetachPortForwardingRule来从一个虚拟机网卡卸载一个规则"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIDetachPortForwardingRuleEvent.class
		}
	}
}