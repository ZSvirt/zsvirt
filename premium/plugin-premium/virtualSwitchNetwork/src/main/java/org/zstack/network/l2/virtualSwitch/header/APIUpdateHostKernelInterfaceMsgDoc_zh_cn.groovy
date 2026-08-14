package org.zstack.network.l2.virtualSwitch.header

import org.zstack.network.l2.virtualSwitch.header.APIUpdateHostKernelInterfaceEvent

doc {
	title "UpdateHostKernelInterface"

	category "network.l2"

	desc """更新Kernel适配器"""

	rest {
		request {
			url "PUT /v1/l3-networks/kernel-interfaces/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateHostKernelInterfaceMsg.class

			desc """更新Kernel适配器"""

			params {

				column {
					name "uuid"
					enclosedIn "updateHostKernelInterface"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.11.0"
				}
				column {
					name "name"
					enclosedIn "updateHostKernelInterface"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "3.11.0"
				}
				column {
					name "description"
					enclosedIn "updateHostKernelInterface"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.11.0"
				}
				column {
					name "requiredIp"
					enclosedIn "updateHostKernelInterface"
					desc "请求的IP"
					location "body"
					type "String"
					optional true
					since "3.11.0"
				}
				column {
					name "netmask"
					enclosedIn "updateHostKernelInterface"
					desc "网络掩码"
					location "body"
					type "String"
					optional true
					since "3.11.0"
				}
				column {
					name "trafficTypes"
					enclosedIn "updateHostKernelInterface"
					desc "流量类型"
					location "body"
					type "List"
					optional true
					since "3.11.0"
					values ("Management")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.11.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.11.0"
				}
			}
		}

		response {
			clz APIUpdateHostKernelInterfaceEvent.class
		}
	}
}