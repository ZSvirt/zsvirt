package org.zstack.header.tpm;

import org.zstack.header.vm.VmInstanceState;

import java.util.Collections;
import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

public class TpmConstants {
    private TpmConstants() {}

    public static final String SERVICE_ID = "tpm";

    public static final List<VmInstanceState> SUPPORT_VM_STATES_FOR_TPM_OPERATION =
            Collections.unmodifiableList(list(VmInstanceState.Stopped));
}
