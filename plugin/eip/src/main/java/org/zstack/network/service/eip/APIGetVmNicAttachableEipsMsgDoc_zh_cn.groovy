package org.zstack.network.service.eip

import org.zstack.network.service.eip.APIGetVmNicAttachableEipsReply

doc {
	title "GetVmNicAttachableEips"

	category "eip"

	desc """获取云主机网卡可挂载弹性IP"""

	rest {
		request {
			url "GET /v1/vm-instances/nics/{vmNicUuid}/candidate-eips"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVmNicAttachableEipsMsg.class

			desc """"""

			params {

				column {
					name "vmNicUuid"
					enclosedIn ""
					desc "云主机网卡UUID"
					location "url"
					type "String"
					optional false
					since "3.13.18"
				}
				column {
					name "ipVersion"
					enclosedIn ""
					desc ""
					location "query"
					type "Integer"
					optional true
					since "3.13.18"
					values ("4","6")
				}
				column {
					name "limit"
					enclosedIn ""
					desc ""
					location "query"
					type "Integer"
					optional true
					since "3.13.18"
				}
				column {
					name "start"
					enclosedIn ""
					desc ""
					location "query"
					type "Integer"
					optional true
					since "3.13.18"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.13.18"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.13.18"
				}
			}
		}

		response {
			clz APIGetVmNicAttachableEipsReply.class
		}
	}
}