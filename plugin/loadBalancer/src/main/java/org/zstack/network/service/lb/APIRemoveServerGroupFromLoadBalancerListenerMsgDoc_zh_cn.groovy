package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIRemoveServerGroupFromLoadBalancerListenerEvent

doc {
	title "RemoveServerGroupFromLoadBalancerListener"

	category "loadBalancer"

	desc """在这里填写API描述"""

	rest {
		request {
			url "DELETE /v1/load-balancers/listeners/{listenerUuid}/servergroups"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRemoveServerGroupFromLoadBalancerListenerMsg.class

			desc """"""

			params {

				column {
					name "serverGroupUuid"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "listenerUuid"
					enclosedIn ""
					desc ""
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
			clz APIRemoveServerGroupFromLoadBalancerListenerEvent.class
		}
	}
}