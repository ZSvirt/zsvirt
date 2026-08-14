package org.zstack.iam1.api;

import org.zstack.core.db.Q;
import org.zstack.header.description.PackageDescription;
import org.zstack.header.identity.APIRevokeResourceSharingMsg;
import org.zstack.header.image.ImageVO;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l3.IpRangeVO;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.rest.SDKPackage;
import org.zstack.header.search.SearchConstant;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.iam1.api.accounts.APIGetResourceInAccountGroupMsg;
import org.zstack.iam1.api.accounts.APIQueryAccountGroupMsg;
import org.zstack.iam1.api.accounts.APIShareResourceToGroupMsg;
import org.zstack.iam1.entity.accounts.AccountGroupVO;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SDKPackage(packageName="org.zstack.sdk.iam1")
public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "iam1";
    }

    {
        permissionBuilder()
                .communityAvailable()
                .zsvProAvailable()
                .build();

        resourceEnsembleContributorBuilder()
                .resourceWithCustomizeFindingMethods(VolumeVO.class, this::findVolumeByVm, this::findVmByVolume)
                .contributeTo(VmInstanceVO.class)
                .build();

        resourceEnsembleContributorBuilder()
                .resource(IpRangeVO.class)
                .contributeTo(L3NetworkVO.class)
                .build();

        resourceEnsembleContributorBuilder()
                .contributeTo(ImageVO.class)
                .build();

        resourceEnsembleContributorBuilder()
                .contributeTo(L2NetworkVO.class)
                .build();

        roleBuilder()
                .uuid("cbf0b13efdb24d4196493ce9fbd9aac6")
                .name("account-group")
                .actions("org.zstack.iam1.api.accounts.*")
                .build();

        roleContributorBuilder()
                .roleName("identity")
                .actions("org.zstack.iam1.api.ensemble.*")
                .actions(
                        APIShareResourceToGroupMsg.class,
                        APIRevokeResourceSharingMsg.class,
                        APIGetResourceInAccountGroupMsg.class
                )
                .build();

        globalReadableResourceBuilder()
                .resources(AccountGroupVO.class)
                .build();

        apis()
                .inPackage("org.zstack.iam1.api.accounts")
                .toService("iam1Accounts")
                .build();

        apis()
                .inPackage("org.zstack.iam1.api.ensemble")
                .toService("iam1Ensemble")
                .build();

        apis()
                .api(APIQueryAccountGroupMsg.class)
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
    }

    private void findVolumeByVm(Map<String, List<String>> vmVolumeMap) {
        final List<Tuple> tuples = Q.New(VolumeVO.class)
                .in(VolumeVO_.vmInstanceUuid, vmVolumeMap.keySet())
                .eq(VolumeVO_.isShareable, false)
                .select(VolumeVO_.uuid, VolumeVO_.vmInstanceUuid, VolumeVO_.lastVmInstanceUuid)
                .listTuple();
        tuples.addAll(Q.New(VolumeVO.class)
                .in(VolumeVO_.lastVmInstanceUuid, vmVolumeMap.keySet())
                .eq(VolumeVO_.isShareable, false)
                .select(VolumeVO_.uuid, VolumeVO_.vmInstanceUuid, VolumeVO_.lastVmInstanceUuid)
                .listTuple());

        Map<String, Tuple> volumeTupleMap = new HashMap<>();
        for (Tuple tuple : tuples) {
            volumeTupleMap.put(tuple.get(0, String.class), tuple);
        }

        for (Map.Entry<String, Tuple> entry : volumeTupleMap.entrySet()) {
            String volumeUuid = entry.getKey();
            String vmUuid = entry.getValue().get(1, String.class);
            vmUuid = vmUuid == null ? entry.getValue().get(2, String.class) : vmUuid;

            vmVolumeMap.compute(vmUuid, (key, list) -> {
                if (list == null) {
                    list = new ArrayList<>();
                }
                list.add(volumeUuid);
                return list;
            });
        }
    }

    private void findVmByVolume(Map<String, String> volumeVmMap) {
        final List<Tuple> tuples = Q.New(VolumeVO.class)
                .in(VolumeVO_.uuid, volumeVmMap.keySet())
                .eq(VolumeVO_.isShareable, false)
                .select(VolumeVO_.uuid, VolumeVO_.vmInstanceUuid, VolumeVO_.lastVmInstanceUuid)
                .listTuple();
        for (Tuple tuple : tuples) {
            String volumeUuid = tuple.get(0, String.class);
            String vmUuid = tuple.get(1, String.class);
            vmUuid = vmUuid == null ? tuple.get(2, String.class) : vmUuid;
            if (vmUuid == null) {
                continue;
            }

            volumeVmMap.put(volumeUuid, vmUuid);
        }
    }
}