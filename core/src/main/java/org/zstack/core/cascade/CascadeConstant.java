package org.zstack.core.cascade;

import java.util.Arrays;
import java.util.List;

/**
 */
public interface CascadeConstant {
    String DELETION_CHECK_CODE = "deletion.check";
    String DELETION_DELETE_CODE = "deletion.delete";
    String DELETION_FORCE_DELETE_CODE = "deletion.forceDelete";
    String DELETION_CLEANUP_CODE = "deletion.cleanup";

    /**
     * Final removal of VM-associated resources (TPM, NvRam host files, etc.) when the VM row is expunged.
     * Invoked from {@code APIExpungeVmInstanceMsg}; not part of {@link #DELETION_CODES}.
     */
    String VM_INSTANCE_EXPUNGE_CODE = "vm.instance.expunge";

    List<String> DELETION_CODES = Arrays.asList(DELETION_CHECK_CODE, DELETION_DELETE_CODE, DELETION_FORCE_DELETE_CODE);
}
