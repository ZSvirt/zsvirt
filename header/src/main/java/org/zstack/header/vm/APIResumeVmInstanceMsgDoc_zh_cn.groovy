package org.zstack.header.vm

import org.zstack.header.vm.APIResumeVmInstanceEvent

doc {
	title "恢复暂停的云主机(ResumeVmInstance)"

	category "vmInstance"

	desc """恢复一个被暂停的云主机，云主机从内存中恢复运行"""

	rest {
		request {
			url "PUT /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIResumeVmInstanceMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "resumeVmInstance"
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
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
			}
		}

		response {
			clz APIResumeVmInstanceEvent.class
		}
	}
}