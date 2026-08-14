package org.zstack.network.service.portforwarding

import org.zstack.network.service.portforwarding.APIAttachPortForwardingRuleEvent

doc {
	title "AttachPortForwardingRule"

	category "portForwarding"

	desc """用户可以使用AttachPortForwardingRule来挂载一个规则到虚拟机网卡上"""

	rest {
		request {
			url "POST /v1/port-forwarding/{ruleUuid}/vm-instances/nics/{vmNicUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAttachPortForwardingRuleMsg.class

			desc """用户可以使用AttachPortForwardingRule来挂载一个规则到虚拟机网卡上"""

			params {

				column {
					name "ruleUuid"
					enclosedIn "params"
					desc "规则的uuid"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "vmNicUuid"
					enclosedIn "params"
					desc "云主机网卡UUID"
					location "url"
					type "String"
					optional false
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
			clz APIAttachPortForwardingRuleEvent.class
		}
	}
}