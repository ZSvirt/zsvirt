package org.zstack.storage.primary.allocate;

import org.zstack.core.db.Q;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.storage.primary.allocate.AbstractPrimaryStorageAllocatorFlow;
import org.zstack.header.storage.primary.allocate.PrimaryStorageCandidate;

import java.util.List;

public class PrimaryStorageCandidateProducerFlow extends AbstractPrimaryStorageAllocatorFlow {
    @Override
    public void allocate() {
        List<String> uuids = spec.getCandidatePrimaryStorageUuids();
        List<PrimaryStorageVO> vos = uuids.isEmpty()
                ? Q.New(PrimaryStorageVO.class).list()
                : Q.New(PrimaryStorageVO.class).in(PrimaryStorageVO_.uuid, uuids).list();

        for (PrimaryStorageVO vo : vos) {
            candidates.add(new PrimaryStorageCandidate(vo));
        }
        next();
    }
}
