package org.zstack.header.vm

import org.zstack.header.vm.APIAttachL3NetworkToVmNicEvent

doc {
	title "AttachL3NetworkToVmNic"

	category "vmInstance"

	desc """动态添加一个网络到网卡"""

	rest {
		request {
			url "POST /v1/nics/{vmNicUuid}/l3-networks/{l3NetworkUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAttachL3NetworkToVmNicMsg.class

			desc """"""

			params {

				column {
					name "vmNicUuid"
					enclosedIn "params"
					desc "云主机网卡UUID"
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
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIAttachL3NetworkToVmNicEvent.class
		}
	}
}