package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIAddAccessControlListToLoadBalancerEvent

doc {
	title "AddAccessControlListToLoadBalancer"

	category "loadBalancer"

	desc """添加监听器的访问控制策略"""

	rest {
		request {
			url "POST /v1/load-balancers/listeners/{listenerUuid}/access-control-lists"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddAccessControlListToLoadBalancerMsg.class

			desc """"""

			params {

				column {
					name "aclUuids"
					enclosedIn "params"
					desc "访问控制策略组的唯一标识"
					location "body"
					type "List"
					optional false
					since "3.9"
				}
				column {
					name "aclType"
					enclosedIn "params"
					desc "访问控制策略类型"
					location "body"
					type "String"
					optional false
					since "3.9"
					values ("white","black","redirect")
				}
				column {
					name "listenerUuid"
					enclosedIn "params"
					desc "监听器唯一标识"
					location "url"
					type "String"
					optional false
					since "3.9"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.9"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.9"
				}
				column {
					name "serverGroupUuids"
					enclosedIn "params"
					desc "负载均衡器服务器组uuid"
					location "body"
					type "List"
					optional true
					since "3.11.3"
				}
			}
		}

		response {
			clz APIAddAccessControlListToLoadBalancerEvent.class
		}
	}
}