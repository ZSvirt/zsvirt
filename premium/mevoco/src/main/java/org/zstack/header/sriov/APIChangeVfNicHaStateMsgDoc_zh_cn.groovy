package org.zstack.header.sriov

import org.zstack.header.sriov.APIChangeVfNicHaStateEvent

doc {
	title "ChangeVfNicHaState"

	category "sriov"

	desc """修改云主机VF网卡高可用状态"""

	rest {
		request {
			url "PUT /v1/vm-instances/nics/{vfNicUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeVfNicHaStateMsg.class

			desc """修改云主机VF网卡高可用状态"""

			params {

				column {
					name "vfNicUuid"
					enclosedIn "changeVfNicHaState"
					desc "VF网卡UUID"
					location "url"
					type "String"
					optional false
					since "4.10.0"
				}
				column {
					name "haState"
					enclosedIn "changeVfNicHaState"
					desc "高可用状态"
					location "body"
					type "String"
					optional false
					since "4.10.0"
					values ("Enabled","Disabled")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.10.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.10.0"
				}
			}
		}

		response {
			clz APIChangeVfNicHaStateEvent.class
		}
	}
}