package org.zstack.header.storage.snapshot

import org.zstack.header.storage.snapshot.APICreateVolumesSnapshotEvent

doc {
    title "CreateVolumesSnapshot"

    category "mevoco"

    desc """在这里填写API描述"""

    rest {
        request {
			url "POST /v1/volumes/volume-snapshots"

			header (Authorization: 'OAuth the-session-uuid')

            clz APICreateVolumesSnapshotMsg.class

            desc """"""
            
			params {

				column {
					name "volumeUuids"
					enclosedIn "params"
					desc ""
					location "body"
					type "List"
					optional false
					since "0.6"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APICreateVolumesSnapshotEvent.class
        }
    }
}