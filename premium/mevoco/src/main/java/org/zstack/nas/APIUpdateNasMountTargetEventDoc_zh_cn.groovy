package org.zstack.nas

import org.zstack.header.errorcode.ErrorCode
import org.zstack.nas.NasMountTargetInventory

doc {

	title "更新后的NAS文件系统挂载点"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.nas.APIUpdateNasMountTargetEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.4.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.nas.APIUpdateNasMountTargetEvent.inventory"
		desc "更新后的NAS文件系统挂载点"
		type "NasMountTargetInventory"
		since "2.4.0"
		clz NasMountTargetInventory.class
	}
}
