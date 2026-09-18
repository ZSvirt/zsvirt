package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.image.ImageInventory

doc {

	title "用于修改云主机根云盘的候选镜像列表"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv_0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APIGetImageCandidatesForVmToChangeReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv_2.2"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.vm.APIGetImageCandidatesForVmToChangeReply.inventories"
		desc "用于修改云主机根云盘的候选镜像列表"
		type "List"
		since "zsv_2.2"
		clz ImageInventory.class
	}
}
