package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIAddCertificateToLoadBalancerListenerEvent

doc {
	title "AddCertificateToLoadBalancerListener"

	category "loadBalancer"

	desc """添加证书到负载均衡"""

	rest {
		request {
			url "POST /v1/load-balancers/listeners/{listenerUuid}/certificate"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddCertificateToLoadBalancerListenerMsg.class

			desc """"""

			params {

				column {
					name "certificateUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "listenerUuid"
					enclosedIn "params"
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
					location "body"
					type "List"
					optional true
					since "2.3"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "2.3"
				}
			}
		}

		response {
			clz APIAddCertificateToLoadBalancerListenerEvent.class
		}
	}
}