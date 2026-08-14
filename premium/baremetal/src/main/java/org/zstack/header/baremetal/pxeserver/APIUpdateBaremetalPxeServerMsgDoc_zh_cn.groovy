package org.zstack.header.baremetal.pxeserver

import org.zstack.header.baremetal.pxeserver.APIUpdateBaremetalPxeServerEvent

doc {
	title "UpdateBaremetalPxeServer"

	category "baremetal.pxeserver"

	desc """更新PXE服务"""

	rest {
		request {
			url "PUT /v1/baremetal/pxeservers/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateBaremetalPxeServerMsg.class

			desc """更新PXE服务"""

			params {

				column {
					name "uuid"
					enclosedIn "updateBaremetalPxeServer"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.6.0"
				}
				column {
					name "name"
					enclosedIn "updateBaremetalPxeServer"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "2.6.0"
				}
				column {
					name "description"
					enclosedIn "updateBaremetalPxeServer"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "2.6.0"
				}
				column {
					name "dhcpInterface"
					enclosedIn "updateBaremetalPxeServer"
					desc "DHCP请求监听网卡"
					location "body"
					type "String"
					optional true
					since "2.6.0"
				}
				column {
					name "dhcpRangeBegin"
					enclosedIn "updateBaremetalPxeServer"
					desc "DHCP地址范围起始"
					location "body"
					type "String"
					optional true
					since "2.6.0"
				}
				column {
					name "dhcpRangeEnd"
					enclosedIn "updateBaremetalPxeServer"
					desc "DHCP地址范围终止"
					location "body"
					type "String"
					optional true
					since "2.6.0"
				}
				column {
					name "dhcpRangeNetmask"
					enclosedIn "updateBaremetalPxeServer"
					desc "DHCP地址范围掩码"
					location "body"
					type "String"
					optional true
					since "2.6.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "2.6.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "2.6.0"
				}
			}
		}

		response {
			clz APIUpdateBaremetalPxeServerEvent.class
		}
	}
}