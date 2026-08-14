package org.zstack.header.image

import org.zstack.header.image.APIGetCandidateBackupStorageForCreatingImageReply

doc {
	title "GetCandidateBackupStorageForCreatingImage"

	category "host.allocator"

	desc """获取创建镜像的备份存储候选"""

	rest {
		request {
			url "GET /v1/images/candidate-backup-storage"
			url "GET /v1/images/volumes/{volumeUuid}/candidate-backup-storage"
			url "GET /v1/images/volume-snapshots/{volumeSnapshotUuid}/candidate-backup-storage"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetCandidateBackupStorageForCreatingImageMsg.class

			desc """"""

			params {

				column {
					name "volumeUuid"
					enclosedIn ""
					desc "云盘UUID，注意：volumeUuid 和 volumeSnapshotUuid 二选一"
					location "query"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "volumeSnapshotUuid"
					enclosedIn ""
					desc "云盘快照UUID，注意：volumeUuid 和 volumeSnapshotUuid 二选一"
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
			clz APIGetCandidateBackupStorageForCreatingImageReply.class
		}
	}
}