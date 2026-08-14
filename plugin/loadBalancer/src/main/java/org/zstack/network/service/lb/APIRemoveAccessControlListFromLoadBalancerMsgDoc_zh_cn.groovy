package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIRemoveAccessControlListFromLoadBalancerEvent

doc {
	title "RemoveAccessControlListFromLoadBalancer"

	category "loadBalancer"

	desc """删除监听器访问控制策略"""

	rest {
		request {
			url "DELETE /v1/load-balancers/listeners/{listenerUuid}/access-control-lists"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRemoveAccessControlListFromLoadBalancerMsg.class

			desc """"""

			params {

				column {
					name "aclUuids"
					enclosedIn ""
					desc "访问控制策略组唯一标识"
					location "query"
					type "List"
					optional false
					since "3.9"
				}
				column {
					name "listenerUuid"
					enclosedIn ""
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
					location "query"
					type "List"
					optional true
					since "3.9"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.9"
				}
				column {
					name "serverGroupUuids"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIRemoveAccessControlListFromLoadBalancerEvent.class
		}
	}
}