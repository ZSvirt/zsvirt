package org.zstack.header.cbt

import org.zstack.header.cbt.APIDeleteCbtTaskEvent

doc {
	title "DeleteCbtTask"

	category "cbt"

	desc """删除CBT任务"""

	rest {
		request {
			url "DELETE /v1/cbt-task/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteCbtTaskMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "4.10.10"
				}
				column {
					name "force"
					enclosedIn ""
					desc ""
					location "query"
					type "boolean"
					optional true
					since "4.10.10"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "query"
					type "String"
					optional true
					since "4.10.10"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "4.10.10"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "4.10.10"
				}
			}
		}

		response {
			clz APIDeleteCbtTaskEvent.class
		}
	}
}