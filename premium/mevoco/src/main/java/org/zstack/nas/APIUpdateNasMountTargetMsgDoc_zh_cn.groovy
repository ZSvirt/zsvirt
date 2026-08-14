package org.zstack.nas

import org.zstack.nas.APIUpdateNasMountTargetEvent

doc {
	title "UpdateNasMountTarget"

	category "nas.filesystem"

	desc """更新NAS文件系统上的挂载点"""

	rest {
		request {
			url "PUT /v1/primary-storage/nas/mount/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateNasMountTargetMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateNasMountTarget"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.4.0"
				}
				column {
					name "name"
					enclosedIn "updateNasMountTarget"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "2.4.0"
				}
				column {
					name "description"
					enclosedIn "updateNasMountTarget"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "2.4.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "2.4.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "2.4.0"
				}
			}
		}

		response {
			clz APIUpdateNasMountTargetEvent.class
		}
	}
}