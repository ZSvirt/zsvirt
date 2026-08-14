package org.zstack.header.vm

import org.zstack.header.vm.APIGetImageCandidatesForVmToChangeReply

doc {
	title "GetImageCandidatesForVmToChange"

	category "mevoco"

	desc """获取用于修改云主机根云盘的候选镜像列表"""

	rest {
		request {
			url "GET /v1/vm-instances/{vmInstanceUuid}/image-candidates"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetImageCandidatesForVmToChangeMsg.class

			desc """获取用于修改云主机根云盘的候选镜像列表"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn ""
					desc "云主机UUID"
					location "url"
					type "String"
					optional true
					since "2.2"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "2.2"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "2.2"
				}
			}
		}

		response {
			clz APIGetImageCandidatesForVmToChangeReply.class
		}
	}
}