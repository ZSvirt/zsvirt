package org.zstack.header.vm

import org.zstack.header.vm.APIFstrimVmEvent

doc {
	title "FstrimVm"

	category "vmInstance"

	desc """回收云主机磁盘空间"""

	rest {
		request {
			url "POST /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIFstrimVmMsg.class

			desc """回收云主机磁盘空间"""

			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "zsv 3.17.21"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.17.21"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.17.21"
				}
			}
		}

		response {
			clz APIFstrimVmEvent.class
		}
	}
}