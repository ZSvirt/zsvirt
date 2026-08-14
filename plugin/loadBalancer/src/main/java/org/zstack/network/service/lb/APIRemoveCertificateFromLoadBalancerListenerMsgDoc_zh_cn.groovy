package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIRemoveCertificateFromLoadBalancerListenerEvent

doc {
	title "RemoveCertificateFromLoadBalancerListener"

	category "loadBalancer"

	desc """从负载均衡移除证书"""

	rest {
		request {
			url "DELETE /v1/load-balancers/listeners/{listenerUuid}/certificate"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRemoveCertificateFromLoadBalancerListenerMsg.class

			desc """"""

			params {

				column {
					name "certificateUuid"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "listenerUuid"
					enclosedIn ""
					desc ""
					location "url"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "2.3"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "2.3"
				}
			}
		}

		response {
			clz APIRemoveCertificateFromLoadBalancerListenerEvent.class
		}
	}
}