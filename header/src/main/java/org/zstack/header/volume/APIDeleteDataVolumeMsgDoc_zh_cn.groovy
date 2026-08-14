package org.zstack.header.volume

import org.zstack.header.volume.APIDeleteDataVolumeEvent

doc {
	title "DeleteDataVolume"

	category "volume"

	desc """删除云盘"""

	rest {
		request {
			url "DELETE /v1/volumes/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteDataVolumeMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "云盘的资源Uuid"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式：Permissive(删除前检查)或Enforcing(强行删除)"
					location "query"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIDeleteDataVolumeEvent.class
		}
	}
}