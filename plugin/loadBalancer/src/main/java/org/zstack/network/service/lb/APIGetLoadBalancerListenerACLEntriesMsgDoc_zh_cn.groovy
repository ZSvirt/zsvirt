package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIGetLoadBalancerListenerACLEntriesReply

doc {
	title "GetLoadBalancerListenerACLEntries"

	category "loadBalancer"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/load-balancers/listeners/access-control-lists/entries"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetLoadBalancerListenerACLEntriesMsg.class

			desc """"""

			params {

				column {
					name "listenerUuids"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "type"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
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
			clz APIGetLoadBalancerListenerACLEntriesReply.class
		}
	}
}