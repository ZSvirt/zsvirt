package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIChangeAccessControlListServerGroupEvent

doc {
	title "ChangeAccessControlListServerGroup"

	category "loadBalancer"

	desc """修改访问控制组绑定的后端服务器"""

	rest {
		request {
			url "PUT /v1/load-balancers/listener/acl/{aclUuid}/servergroup/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeAccessControlListServerGroupMsg.class

			desc """"""

			params {

				column {
					name "serverGroupUuids"
					enclosedIn "changeAccessControlListServerGroup"
					desc "负载均衡器服务器组uuid"
					location "body"
					type "List"
					optional false
					since "3.11.3"
				}
				column {
					name "listenerUuid"
					enclosedIn "changeAccessControlListServerGroup"
					desc "监听器唯一标识"
					location "body"
					type "String"
					optional false
					since "3.11.3"
				}
				column {
					name "aclUuid"
					enclosedIn "changeAccessControlListServerGroup"
					desc "访问控制策略组的唯一标识"
					location "url"
					type "String"
					optional false
					since "3.11.3"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.11.3"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.11.3"
				}
			}
		}

		response {
			clz APIChangeAccessControlListServerGroupEvent.class
		}
	}
}