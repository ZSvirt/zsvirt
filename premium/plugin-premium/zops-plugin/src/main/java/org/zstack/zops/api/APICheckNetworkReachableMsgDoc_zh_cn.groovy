package org.zstack.zops.api

import org.zstack.zops.api.APICheckNetworkReachableReply

doc {
	title "CheckNetworkReachable"

	category "zops"

	desc """检查多个主机之间的网络连通性"""

	rest {
		request {
			url "GET /v1/zops/check/network"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICheckNetworkReachableMsg.class

			desc """"""

			params {

				column {
					name "sourceHostnames"
					enclosedIn ""
					desc "源指定ip地址或域名集合"
					location "query"
					type "List"
					optional true
					since "3.17.21"
				}
				column {
					name "targetHostnames"
					enclosedIn ""
					desc "目标ip地址或域名集合"
					location "query"
					type "List"
					optional false
					since "3.17.21"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.17.21"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.17.21"
				}
			}
		}

		response {
			clz APICheckNetworkReachableReply.class
		}
	}
}