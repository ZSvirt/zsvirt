package org.zstack.nas

import org.zstack.nas.APIDeleteNasFileSystemEvent

doc {
	title "DeleteNasFileSystem"

	category "nas.filesystem"

	desc """删除一个NAS文件系统"""

	rest {
		request {
			url "DELETE /v1/primary-storage/nas/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteNasFileSystemMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.4.0"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive 或者 Enforcing, 默认 Permissive)"
					location "query"
					type "String"
					optional true
					since "2.4.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "2.4.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "2.4.0"
				}
			}
		}

		response {
			clz APIDeleteNasFileSystemEvent.class
		}
	}
}