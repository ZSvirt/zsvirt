package org.zstack.zwatch.mysql;

import org.zstack.core.db.Q;
import org.zstack.header.core.StaticInit;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.identity.Account;
import org.zstack.zwatch.datatype.Datapoint;
import org.zstack.zwatch.datatype.MetricQueryObject;
import org.zstack.zwatch.datatype.Namespace;
import org.zstack.zwatch.namespace.VmCountNamespace;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

public class VmMysqlNamespace extends AbstractMysqlNamespace {
    public VmMysqlNamespace(Namespace namespace) {
        super(namespace);
    }

    @StaticInit
    static void staticInit() {
        MysqlNamespace.namespacesClasses.put(VmCountNamespace.class, VmMysqlNamespace.class);
    }

    @Override
    protected List<Datapoint> doQuery(MetricQueryObject queryObject) {
        boolean allResourceReadable = Account.isAllResourcesReadable(queryObject.getAccountUuid());

        if (queryObject.getMetricName().equals(VmCountNamespace.TotalVMCount.getName())) {
            Long value;
            if (allResourceReadable) {
                value = Q.New(VmInstanceVO.class).count();
            } else {
                value = countVm(queryObject.getAccountUuid());
            }

            return transformSingleValueToDataPointList(value);
        } else if (queryObject.getMetricName().equals(VmCountNamespace.RunningVMCount.getName())) {
            Long value;
            if (allResourceReadable) {
                value = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.state, VmInstanceState.Running).count();
            } else {
                value = countVmInState(queryObject.getAccountUuid(), VmInstanceState.Running);
            }
            return transformSingleValueToDataPointList(value);
        } else if (queryObject.getMetricName().equals(VmCountNamespace.RunningVMInPercent.getName())) {
            Long total;
            Long running;
            if (allResourceReadable) {
                total = Q.New(VmInstanceVO.class).count();
                running = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.state, VmInstanceState.Running).count();
            } else {
                total = countVm(queryObject.getAccountUuid());
                running = countVmInState(queryObject.getAccountUuid(), VmInstanceState.Running);
            }

            return transformSingleValueToDataPointList(total == 0 ? 0 : ((double) running / total) * 100);
        } else if (queryObject.getMetricName().equals(VmCountNamespace.StoppedVMCount.getName())) {
            Long value;
            if (allResourceReadable) {
                value = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.state, VmInstanceState.Stopped).count();
            } else {
                value = countVmInState(queryObject.getAccountUuid(), VmInstanceState.Stopped);
            }

            return transformSingleValueToDataPointList(value);
        } else if (queryObject.getMetricName().equals(VmCountNamespace.StoppedVMInPercent.getName())) {
            Long total;
            Long stopped;
            if (allResourceReadable) {
                total = Q.New(VmInstanceVO.class).count();
                stopped = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.state, VmInstanceState.Stopped).count();
            } else {
                total = countVm(queryObject.getAccountUuid());
                stopped = countVmInState(queryObject.getAccountUuid(), VmInstanceState.Stopped);
            }

            return transformSingleValueToDataPointList(total == 0 ? 0 : ((double) stopped / total) * 100);
        } else if (queryObject.getMetricName().equals(VmCountNamespace.OtherStateVMCount.getName())) {
            Long value;
            if (allResourceReadable) {
                value = Q.New(VmInstanceVO.class).notIn(VmInstanceVO_.state, list(VmInstanceState.Running, VmInstanceState.Stopped)).count();
            } else {
                value = countVmInOtherState(queryObject.getAccountUuid());
            }

            return transformSingleValueToDataPointList(value);
        } else if (queryObject.getMetricName().equals(VmCountNamespace.OtherStateVMInPercent.getName())) {
            Long total;
            Long other;
            if (allResourceReadable) {
                total = Q.New(VmInstanceVO.class).count();
                other = Q.New(VmInstanceVO.class).notIn(VmInstanceVO_.state, list(VmInstanceState.Running, VmInstanceState.Stopped)).count();
            } else {
                total = countVm(queryObject.getAccountUuid());
                other = countVmInOtherState(queryObject.getAccountUuid());
            }

            return transformSingleValueToDataPointList(total == 0 ? 0 : ((double) other / total) * 100);
        }

        return null;
    }

    private long countVmInOtherState(String accountUuid) {
        Long count = Q.New(AccountResourceRefVO.class, VmInstanceVO.class)
                .table0()
                    .eq(AccountResourceRefVO_.accountUuid, accountUuid)
                    .eq(AccountResourceRefVO_.resourceType, VmInstanceVO.class.getSimpleName())
                    .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                    .eq(AccountResourceRefVO_.resourceUuid).table1(VmInstanceVO_.uuid)
                    .selectCount(AccountResourceRefVO_.resourceUuid)
                .table1()
                    .notIn(VmInstanceVO_.state, list(VmInstanceState.Running, VmInstanceState.Stopped))
                .find();
        return count == null ? 0L : count;
    }

    private long countVmInState(String accountUuid, VmInstanceState state) {
        Long count = Q.New(AccountResourceRefVO.class, VmInstanceVO.class)
                .table0()
                    .eq(AccountResourceRefVO_.accountUuid, accountUuid)
                    .eq(AccountResourceRefVO_.resourceType, VmInstanceVO.class.getSimpleName())
                    .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                    .eq(AccountResourceRefVO_.resourceUuid).table1(VmInstanceVO_.uuid)
                    .selectCount(AccountResourceRefVO_.resourceUuid)
                .table1()
                    .eq(VmInstanceVO_.state, state)
                .find();
        return count == null ? 0L : count;
    }

    private long countVm(String accountUuid) {
        Long count = Q.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.accountUuid, accountUuid)
                .eq(AccountResourceRefVO_.resourceType, VmInstanceVO.class.getSimpleName())
                .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                .count();
        return count == null ? 0L : count;
    }
}
