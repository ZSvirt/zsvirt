package org.zstack.header.vm

import org.zstack.header.vm.APISetVmDnsEvent

doc {
	title "SetVmDns"

	category "vmInstance"

	desc """设置云主机DNS"""

	rest {
		request {
			url "PUT /v1/vm-instances/{vmInstanceUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISetVmDnsMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn "setVmDns"
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "zsv 4.10.10"
				}
				column {
					name "vmNicUuid"
					enclosedIn "setVmDns"
					desc "云主机网卡UUID"
					location "body"
					type "String"
					optional true
					since "zsv 4.10.10"
				}
				column {
					name "dnsList"
					enclosedIn "setVmDns"
					desc "DNS列表"
					location "body"
					type "List"
					optional false
					since "zsv 4.10.10"
				}
				column {
					name "ipVersion"
					enclosedIn "setVmDns"
					desc "ip协议号"
					location "body"
					type "Integer"
					optional true
					since "zsv 4.10.10"
					values ("4","6")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 4.10.10"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 4.10.10"
				}
			}
		}

		response {
			clz APISetVmDnsEvent.class
		}
	}
}