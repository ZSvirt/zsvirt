package org.zstack.header.vm

import org.zstack.header.vm.APIAttachL3NetworkToVmEvent

doc {
	title "加载网络到云主机(AttachL3NetworkToVm)"

	category "vmInstance"

	desc """动态添加一个网络到Running或者Stopped的云主机"""

	rest {
		request {
			url "POST /v1/vm-instances/{vmInstanceUuid}/l3-networks/{l3NetworkUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAttachL3NetworkToVmMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn "params"
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "l3NetworkUuid"
					enclosedIn "params"
					desc "三层网络UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "staticIp"
					enclosedIn "params"
					desc "指定分配给云主机的IP地址"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "driverType"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "zsv 3.10.0"
				}
				column {
					name "customMac"
					enclosedIn "params"
					desc "自定义网卡MAC地址"
					location "body"
					type "String"
					optional true
					since "zsv 3.10.0"
				}
				column {
					name "vmNicParams"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "zsv 3.17.0"
				}
			}
		}

		response {
			clz APIAttachL3NetworkToVmEvent.class
		}
	}
}