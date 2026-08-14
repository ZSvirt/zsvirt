package org.zstack.header.storage.snapshot

import org.zstack.header.storage.snapshot.APIDeleteVolumeSnapshotEvent

doc {
	title "删除云盘快照(DeleteVolumeSnapshot)"

	category "snapshot.volume"

	desc """删除云盘快照"""

	rest {
		request {
			url "DELETE /v1/volume-snapshots/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteVolumeSnapshotMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive 或者 Enforcing, 默认 Permissive)"
					location "query"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "direction"
					enclosedIn ""
					desc "数据合并方向。pull：向前合并；commit：向后合并；auto：自动选择最优合并方向"
					location "query"
					type "String"
					optional true
					since "zsv 4.10.6"
				}
				column {
					name "scope"
					enclosedIn ""
					desc "数据合并方式。single：仅合并单个快照；chain：合并整个快照链；auto：自动判断最佳合并范围"
					location "query"
					type "String"
					optional true
					since "zsv 4.10.6"
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
			clz APIDeleteVolumeSnapshotEvent.class
		}
	}
}