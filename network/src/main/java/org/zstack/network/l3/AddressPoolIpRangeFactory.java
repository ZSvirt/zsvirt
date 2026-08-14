package org.zstack.network.l3;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.network.l3.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.ForEachFunction;

public class AddressPoolIpRangeFactory implements IpRangeFactory {
    @Autowired
    DatabaseFacade dbf;
    @Autowired
    protected PluginRegistry pluginRgty;

    @Override
    public IpRangeType getType() {
        return IpRangeType.AddressPool;
    }

    @Override
    public IpRangeInventory createIpRange(IpRangeInventory ipr, APICreateMessage msg) {
        AddressPoolVO vo = new SQLBatchWithReturn<AddressPoolVO>() {
            @Override
            protected AddressPoolVO scripts() {
                AddressPoolVO vo = new AddressPoolVO();
                vo.setUuid(ipr.getUuid() == null ? Platform.getUuid() : ipr.getUuid());
                vo.setDescription(ipr.getDescription());
                vo.setEndIp(ipr.getEndIp());
                vo.setGateway(ipr.getStartIp());
                vo.setL3NetworkUuid(ipr.getL3NetworkUuid());
                vo.setName(ipr.getName());
                vo.setNetmask(ipr.getNetmask());
                vo.setStartIp(ipr.getStartIp());
                vo.setNetworkCidr(ipr.getNetworkCidr());
                vo.setAccountUuid(msg.getSession().getAccountUuid());
                vo.setIpVersion(ipr.getIpVersion());
                vo.setAddressMode(ipr.getAddressMode());
                vo.setPrefixLen(ipr.getPrefixLen());
                dbf.getEntityManager().persist(vo);
                dbf.getEntityManager().flush();
                dbf.getEntityManager().refresh(vo);

                return vo;
            }
        }.execute();

        final IpRangeInventory finalIpr = AddressPoolInventory.valueOf1(vo);
        CollectionUtils.safeForEach(pluginRgty.getExtensionList(AfterAddIpRangeExtensionPoint.class), new ForEachFunction<AfterAddIpRangeExtensionPoint>() {
            @Override
            public void run(AfterAddIpRangeExtensionPoint ext) {
                ext.afterAddIpRange(finalIpr, msg.getSystemTags());
            }
        });

        return finalIpr;
    }
}
