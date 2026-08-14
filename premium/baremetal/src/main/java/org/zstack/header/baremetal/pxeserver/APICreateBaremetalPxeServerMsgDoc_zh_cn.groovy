package org.zstack.header.baremetal.pxeserver

import org.zstack.header.baremetal.pxeserver.APICreateBaremetalPxeServerEvent

doc {
	title "CreateBaremetalPxeServer"

	category "baremetal.pxeserver"

	desc """创建PXE服务"""

	rest {
		request {
			url "POST /v1/baremetal/pxeservers"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateBaremetalPxeServerMsg.class

			desc """创建PXE服务"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "2.6.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "2.6.0"
				}
				column {
					name "dhcpInterface"
					enclosedIn "params"
					desc "DHCP请求监听网卡"
					location "body"
					type "String"
					optional false
					since "2.6.0"
				}
				column {
					name "dhcpRangeBegin"
					enclosedIn "params"
					desc "DHCP地址范围起始"
					location "body"
					type "String"
					optional true
					since "2.6.0"
				}
				column {
					name "dhcpRangeEnd"
					enclosedIn "params"
					desc "DHCP地址范围终止"
					location "body"
					type "String"
					optional true
					since "2.6.0"
				}
				column {
					name "dhcpRangeNetmask"
					enclosedIn "params"
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
					name "hostname"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "sshUsername"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "sshPassword"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "sshPort"
					enclosedIn "params"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "0.6"
				}
				column {
					name "storagePath"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
			}
		}

		response {
			clz APICreateBaremetalPxeServerEvent.class
		}
	}
}