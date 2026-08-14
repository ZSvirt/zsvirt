package org.zstack.ha

import org.zstack.ha.APISetVmInstanceHaLevelEvent

doc {
	title "设置云主机高可用(SetVmInstanceHaLevel)"

	category "ha"

	desc """设置云主机高可用级别"""

	rest {
		request {
			url "POST /v1/vm-instances/{uuid}/ha-levels"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISetVmInstanceHaLevelMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "云主机的资源Uuid"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "level"
					enclosedIn "params"
					desc "云主机高可用级别，这里为NeverStop或OnHostFailure"
					location "body"
					type "String"
					optional false
					since "0.6"
					values ("NeverStop","OnHostFailure")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APISetVmInstanceHaLevelEvent.class
		}
	}
}