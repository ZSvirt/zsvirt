package org.zstack.network.service.virtualrouter

import org.zstack.header.configuration.APIUpdateInstanceOfferingEvent

doc {
	title "更新虚拟路由器规格(UpdateVirtualRouterOffering)"

	category "虚拟路由器"

	desc """更新虚拟路由器规格"""

	rest {
		request {
			url "PUT /v1/instance-offerings/virtual-routers/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateVirtualRouterOfferingMsg.class

			desc """"""

			params {

				column {
					name "isDefault"
					enclosedIn "updateVirtualRouterOffering"
					desc "默认"
					location "body"
					type "Boolean"
					optional true
					since "0.6"
				}
				column {
					name "imageUuid"
					enclosedIn "updateVirtualRouterOffering"
					desc "镜像UUID"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "uuid"
					enclosedIn "updateVirtualRouterOffering"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "name"
					enclosedIn "updateVirtualRouterOffering"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "updateVirtualRouterOffering"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "allocatorStrategy"
					enclosedIn "updateVirtualRouterOffering"
					desc ""
					location "body"
					type "String"
					optional true
					since "2.3.1"
				}
			}
		}

		response {
			clz APIUpdateInstanceOfferingEvent.class
		}
	}
}