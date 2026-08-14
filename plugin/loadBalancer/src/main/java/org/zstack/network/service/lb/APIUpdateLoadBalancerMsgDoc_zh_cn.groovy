package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIUpdateLoadBalancerEvent

doc {
	title "UpdateLoadBalancer"

	category "未知类别"

	desc """在这里填写API描述"""

	rest {
		request {
			url "PUT /v1/load-balancers/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateLoadBalancerMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateLoadBalancer"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "name"
					enclosedIn "updateLoadBalancer"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "updateLoadBalancer"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "resourceUuid"
					enclosedIn "updateLoadBalancer"
					desc ""
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
				column {
					name "tagUuids"
					enclosedIn "updateLoadBalancer"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
			}
		}

		response {
			clz APIUpdateLoadBalancerEvent.class
		}
	}
}