package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIUpdateLoadBalancerListenerEvent

doc {
	title "UpdateLoadBalancerListener"

	category "loadBalancer"

	desc """在这里填写API描述"""

	rest {
		request {
			url "PUT /v1/load-balancers/listeners/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateLoadBalancerListenerMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateLoadBalancerListener"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "name"
					enclosedIn "updateLoadBalancerListener"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "updateLoadBalancerListener"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIUpdateLoadBalancerListenerEvent.class
		}
	}
}