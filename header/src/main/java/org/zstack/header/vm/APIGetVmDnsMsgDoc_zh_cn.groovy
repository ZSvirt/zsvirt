package org.zstack.header.vm

import org.zstack.header.vm.APIGetVmDnsReply

doc {
	title "GetVmDns"

	category "vmInstance"

	desc """获取云主机的DNS信息"""

	rest {
		request {
			url "GET /v1/vm-instances/{vmInstanceUuid}/dns"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVmDnsMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn ""
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "zsv 4.10.10"
				}
				column {
					name "vmNicUuid"
					enclosedIn ""
					desc "云主机网卡UUID"
					location "query"
					type "String"
					optional true
					since "zsv 4.10.10"
				}
				column {
					name "ipVersion"
					enclosedIn ""
					desc "ip协议号"
					location "query"
					type "Integer"
					optional true
					since "zsv 4.10.10"
					values ("4","6")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 4.10.10"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 4.10.10"
				}
			}
		}

		response {
			clz APIGetVmDnsReply.class
		}
	}
}