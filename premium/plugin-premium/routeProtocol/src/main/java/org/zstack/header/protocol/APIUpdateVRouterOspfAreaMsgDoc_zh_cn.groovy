package org.zstack.header.protocol

import org.zstack.header.protocol.APIUpdateVRouterOspfAreaEvent

doc {
	title "UpdateVRouterOspfArea"

	category "routeProtocol"

	desc """更改OSPF的路由区域属性"""

	rest {
		request {
			url "PUT /v1/routerArea/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateVRouterOspfAreaMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateVRouterOspfArea"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.4"
				}
				column {
					name "areaAuth"
					enclosedIn "updateVRouterOspfArea"
					desc "区域认证方式"
					location "body"
					type "String"
					optional true
					since "3.4"
				}
				column {
					name "areaType"
					enclosedIn "updateVRouterOspfArea"
					desc "区域类型"
					location "body"
					type "String"
					optional true
					since "3.4"
				}
				column {
					name "password"
					enclosedIn "updateVRouterOspfArea"
					desc "认证需要的password"
					location "body"
					type "String"
					optional true
					since "3.4"
				}
				column {
					name "keyId"
					enclosedIn "updateVRouterOspfArea"
					desc "认证需要的KeyID"
					location "body"
					type "Integer"
					optional true
					since "3.4"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.4"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.4"
				}
			}
		}

		response {
			clz APIUpdateVRouterOspfAreaEvent.class
		}
	}
}