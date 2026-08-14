package org.zstack.header.protocol

import org.zstack.header.protocol.APICreateVRouterOspfAreaEvent

doc {
	title "CreateVRouterOspfArea"

	category "routeProtocol"

	desc """创建路由区域资源"""

	rest {
		request {
			url "POST /v1/routerArea"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateVRouterOspfAreaMsg.class

			desc """"""

			params {

				column {
					name "areaId"
					enclosedIn "params"
					desc "区域Id，区域标识"
					location "body"
					type "String"
					optional false
					since "3.4"
				}
				column {
					name "areaAuth"
					enclosedIn "params"
					desc "OSPF区域的认证方式"
					location "body"
					type "String"
					optional true
					since "3.4"
				}
				column {
					name "areaType"
					enclosedIn "params"
					desc "区域类型"
					location "body"
					type "String"
					optional true
					since "3.4"
				}
				column {
					name "password"
					enclosedIn "params"
					desc "认证方式为plaintext时的密码"
					location "body"
					type "String"
					optional true
					since "3.4"
				}
				column {
					name "keyId"
					enclosedIn "params"
					desc "认证方式为MD5时用到的keyid"
					location "body"
					type "Integer"
					optional true
					since "3.4"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "区域资源的唯一标识"
					location "body"
					type "String"
					optional true
					since "3.4"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
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
			clz APICreateVRouterOspfAreaEvent.class
		}
	}
}