package org.zstack.network.service.portforwarding

import org.zstack.network.service.portforwarding.APIGetPortForwardingAttachableVmNicsReply

doc {
	title "GetPortForwardingAttachableVmNics"

	category "portForwarding"

	desc """获取可应用端口转发规则的云主机网卡列表"""

	rest {
		request {
			url "GET /v1/port-forwarding/{ruleUuid}/vm-instances/candidate-nics"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetPortForwardingAttachableVmNicsMsg.class

			desc """获取可应用端口转发规则的云主机网卡列表"""

			params {

				column {
					name "ruleUuid"
					enclosedIn ""
					desc "规则的uuid"
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
			clz APIGetPortForwardingAttachableVmNicsReply.class
		}
	}
}