package org.zstack.header.volume

import org.zstack.header.volume.APICreateDataVolumeFromVolumeSnapshotEvent

doc {
	title "CreateDataVolumeFromVolumeSnapshot"

	category "volume"

	desc """从快照创建一个云盘"""

	rest {
		request {
			url "POST /v1/volumes/data/from/volume-snapshots/{volumeSnapshotUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateDataVolumeFromVolumeSnapshotMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "云盘名称"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "云盘的详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "volumeSnapshotUuid"
					enclosedIn "params"
					desc "云盘快照UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "primaryStorageUuid"
					enclosedIn "params"
					desc "主存储UUID"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源的Uuid"
					location "body"
					type "String"
					optional true
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
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "zsv 3.4.0"
				}
			}
		}

		response {
			clz APICreateDataVolumeFromVolumeSnapshotEvent.class
		}
	}
}