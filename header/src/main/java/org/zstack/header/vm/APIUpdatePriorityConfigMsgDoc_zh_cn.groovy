package org.zstack.header.vm

import org.zstack.header.vm.APIUpdatePriorityConfigEvent

doc {
	title "UpdatePriorityConfig"

	category "vmInstance"

	desc """更改云主机优先级配置"""

	rest {
		request {
			url "PUT /v1/vm-priority-config/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdatePriorityConfigMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updatePriorityConfig"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 3.7"
				}
				column {
					name "cpuShares"
					enclosedIn "updatePriorityConfig"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "zsv 3.7"
				}
				column {
					name "oomScoreAdj"
					enclosedIn "updatePriorityConfig"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "zsv 3.7"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 3.7"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 3.7"
				}
			}
		}

		response {
			clz APIUpdatePriorityConfigEvent.class
		}
	}
}