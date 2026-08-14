package org.zstack.header.cbt

import org.zstack.header.cbt.APIEnableCbtTaskEvent

doc {
	title "EnableCbtTask"

	category "cbt"

	desc """启动CBT任务"""

	rest {
		request {
			url "POST /v1/cbt-task/enable/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIEnableCbtTaskMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "4.10.10"
				}
				column {
					name "bitmapName"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "4.10.10"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.10.10"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.10.10"
				}
			}
		}

		response {
			clz APIEnableCbtTaskEvent.class
		}
	}
}