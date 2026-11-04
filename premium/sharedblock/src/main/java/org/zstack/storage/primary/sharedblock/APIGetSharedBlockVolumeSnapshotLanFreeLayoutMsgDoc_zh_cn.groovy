package org.zstack.storage.primary.sharedblock

import org.zstack.storage.primary.sharedblock.APIGetSharedBlockVolumeSnapshotLanFreeLayoutReply

doc {
	title "GetSharedBlockVolumeSnapshotLanFreeLayout"

	category "sharedblock"

	desc """获取调用者指定的共享块存储 Volume Snapshot 完整 qcow2 链，以及各层 LV 到 LUN 的物理区间布局。接口不决定第三方备份制品是全量或增量。"""

	rest {
		request {
			url "POST /v1/primary-storage/sharedblockgroup/volume-snapshot-lan-free-layouts"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetSharedBlockVolumeSnapshotLanFreeLayoutMsg.class

			desc """snapshotUuids 只指定要查询的 Volume Snapshot，单次最多传入 100 个且不能重复。接口对每个快照返回当前完整 qcow2 backing chain。"""

			params {

				column {
					name "snapshotUuids"
					enclosedIn "params"
					desc "要查询的 Volume Snapshot UUID 列表"
					location "body"
					type "List"
					optional false
					since "1.0.1, zsv_5.2.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "1.0.1, zsv_5.2.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "1.0.1, zsv_5.2.0"
				}
			}
		}

		response {
			clz APIGetSharedBlockVolumeSnapshotLanFreeLayoutReply.class
		}
	}
}