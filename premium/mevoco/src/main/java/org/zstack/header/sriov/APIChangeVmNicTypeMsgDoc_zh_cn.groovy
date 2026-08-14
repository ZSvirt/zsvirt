package org.zstack.header.sriov

import org.zstack.header.sriov.APIChangeVmNicTypeEvent

doc {
	title "ChangeVmNicType"

	category "sriov"

	desc """修改云主机网卡类型"""

	rest {
		request {
			url "PUT /v1/vm-instances/nics/{vmNicUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeVmNicTypeMsg.class

			desc """修改云主机网卡类型"""

			params {

				column {
					name "vmNicUuid"
					enclosedIn "changeVmNicType"
					desc "云主机网卡UUID"
					location "url"
					type "String"
					optional false
					since "3.9.0"
				}
				column {
					name "vmNicType"
					enclosedIn "changeVmNicType"
					desc "云主机网卡类型"
					location "body"
					type "String"
					optional false
					since "3.9.0"
					values ("VNIC")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.9.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.9.0"
				}
			}
		}

		response {
			clz APIChangeVmNicTypeEvent.class
		}
	}
}