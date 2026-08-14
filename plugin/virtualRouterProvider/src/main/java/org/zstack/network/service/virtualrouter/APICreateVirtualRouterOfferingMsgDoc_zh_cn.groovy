package org.zstack.network.service.virtualrouter

import org.zstack.header.configuration.APICreateInstanceOfferingEvent

doc {
	title "创建虚拟路由器规格(CreateVirtualRouterOffering)"

	category "虚拟路由器"

	desc """创建虚拟路由器规格"""

	rest {
		request {
			url "POST /v1/instance-offerings/virtual-routers"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateVirtualRouterOfferingMsg.class

			desc """"""

			params {

				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "managementNetworkUuid"
					enclosedIn "params"
					desc "管理L3网络UUID"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "imageUuid"
					enclosedIn "params"
					desc "镜像UUID"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "publicNetworkUuid"
					enclosedIn "params"
					desc "公有L3网络UUID"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "isDefault"
					enclosedIn "params"
					desc "默认"
					location "body"
					type "Boolean"
					optional true
					since "0.6"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "cpuNum"
					enclosedIn "params"
					desc "CPU数量"
					location "body"
					type "int"
					optional false
					since "0.6"
				}
				column {
					name "cpuSpeed"
					enclosedIn "params"
					desc ""
					location "body"
					type "int"
					optional false
					since "0.6"
				}
				column {
					name "memorySize"
					enclosedIn "params"
					desc "内存大小"
					location "body"
					type "long"
					optional false
					since "0.6"
				}
				column {
					name "allocatorStrategy"
					enclosedIn "params"
					desc "分配策略"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "sortKey"
					enclosedIn "params"
					desc "排序主键"
					location "body"
					type "int"
					optional true
					since "0.6"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "类型"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
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
					name "reservedMemorySize"
					enclosedIn "params"
					desc ""
					location "body"
					type "long"
					optional true
					since "3.17.21"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.17.21"
				}
			}
		}

		response {
			clz APICreateInstanceOfferingEvent.class
		}
	}
}