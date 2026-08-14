package org.zstack.network.l2.virtualSwitch.header

import org.zstack.network.l2.virtualSwitch.header.APICreateHostKernelInterfaceEvent

doc {
	title "CreateHostKernelInterface"

	category "network.l2"

	desc """创建Kernel适配器"""

	rest {
		request {
			url "POST /v1/l3-networks/kernel-interfaces"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateHostKernelInterfaceMsg.class

			desc """创建Kernel适配器"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "4.1.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "4.1.0"
				}
				column {
					name "hostUuid"
					enclosedIn "params"
					desc "物理机UUID"
					location "body"
					type "String"
					optional false
					since "4.1.0"
				}
				column {
					name "l3NetworkUuid"
					enclosedIn "params"
					desc "三层网络UUID"
					location "body"
					type "String"
					optional false
					since "4.1.0"
				}
				column {
					name "requiredIp"
					enclosedIn "params"
					desc "请求的IP"
					location "body"
					type "String"
					optional true
					since "4.1.0"
				}
				column {
					name "netmask"
					enclosedIn "params"
					desc "网络掩码"
					location "body"
					type "String"
					optional true
					since "4.1.0"
				}
				column {
					name "trafficTypes"
					enclosedIn "params"
					desc "流量类型"
					location "body"
					type "List"
					optional true
					since "4.1.0"
					values ("Management","Storage")
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "4.1.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "4.1.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.1.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.1.0"
				}
			}
		}

		response {
			clz APICreateHostKernelInterfaceEvent.class
		}
	}
}