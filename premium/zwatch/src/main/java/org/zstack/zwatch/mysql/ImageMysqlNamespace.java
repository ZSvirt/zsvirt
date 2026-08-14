package org.zstack.zwatch.mysql;

import org.zstack.core.db.Q;
import org.zstack.header.core.StaticInit;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.image.*;
import org.zstack.identity.Account;
import org.zstack.zwatch.datatype.Datapoint;
import org.zstack.zwatch.datatype.MetricQueryObject;
import org.zstack.zwatch.datatype.Namespace;
import org.zstack.zwatch.namespace.ImageNamespace;

import java.util.List;

public class ImageMysqlNamespace extends AbstractMysqlNamespace {
    public ImageMysqlNamespace(Namespace namespace) {
        super(namespace);
    }

    @StaticInit
    static void staticInit() {
        MysqlNamespace.namespacesClasses.put(ImageNamespace.class, ImageMysqlNamespace.class);
    }

    @Override
    protected List<Datapoint> doQuery(MetricQueryObject queryObject) {
        String accountUuid = queryObject.getAccountUuid();
        boolean allImageReadable = Account.isAllResourcesReadable(accountUuid);

        if (queryObject.getMetricName().equals(ImageNamespace.TotalImageCount.getName())) {
            Long count;
            if (allImageReadable) {
                count = Q.New(ImageVO.class).count();
            } else {
                count = countImage(accountUuid);
            }

            return transformSingleValueToDataPointList(count);
        } else if (queryObject.getMetricName().equals(ImageNamespace.ReadyImageCount.getName())) {
            Long count;
            if (allImageReadable) {
                count = Q.New(ImageVO.class).eq(ImageVO_.state, ImageState.Enabled).eq(ImageVO_.status, ImageStatus.Ready).count();
            } else {
                count = countReadyImage(accountUuid);
            }

            return transformSingleValueToDataPointList(count);
        } else if (queryObject.getMetricName().equals(ImageNamespace.ReadyImageInPercent.getName())) {
            Long total;
            Long ready;
            if (allImageReadable) {
                total = Q.New(ImageVO.class).count();
                ready = Q.New(ImageVO.class).eq(ImageVO_.state, ImageState.Enabled).eq(ImageVO_.status, ImageStatus.Ready).count();
            } else {
                total = countImage(accountUuid);
                ready = countReadyImage(accountUuid);
            }

            return transformSingleValueToDataPointList(total == 0 ? 0 : ((double) ready / total) * 100);
        } else if (queryObject.getMetricName().equals(ImageNamespace.ISOCount.getName())) {
            Long count;
            if (allImageReadable) {
                count = Q.New(ImageVO.class).eq(ImageVO_.mediaType, ImageConstant.ImageMediaType.ISO).count();
            } else {
                count = countImageInMediaType(accountUuid, ImageConstant.ImageMediaType.ISO);
            }

            return transformSingleValueToDataPointList(count);
        } else if (queryObject.getMetricName().equals(ImageNamespace.ISOInPercent.getName())) {
            Long total;
            Long iso;
            if (allImageReadable) {
                total = Q.New(ImageVO.class).count();
                iso = Q.New(ImageVO.class).eq(ImageVO_.mediaType, ImageConstant.ImageMediaType.ISO).count();
            } else {
                total = countImage(accountUuid);
                iso = countImageInMediaType(accountUuid, ImageConstant.ImageMediaType.ISO);
            }

            return transformSingleValueToDataPointList(total == 0 ? 0 : ((double) iso / total) * 100);
        } else if (queryObject.getMetricName().equals(ImageNamespace.RootVolumeTemplateCount.getName())) {
            Long count;
            if (allImageReadable) {
                count = Q.New(ImageVO.class).eq(ImageVO_.mediaType, ImageConstant.ImageMediaType.RootVolumeTemplate).count();
            } else {
                count = countImageInMediaType(accountUuid, ImageConstant.ImageMediaType.RootVolumeTemplate);
            }

            return transformSingleValueToDataPointList(count);
        } else if (queryObject.getMetricName().equals(ImageNamespace.RootVolumeTemplateInPercent.getName())) {
            Long total;
            Long root;
            if (allImageReadable) {
                total = Q.New(ImageVO.class).count();
                root = Q.New(ImageVO.class).eq(ImageVO_.mediaType, ImageConstant.ImageMediaType.RootVolumeTemplate).count();
            } else {
                total = countImage(accountUuid);
                root = countImageInMediaType(accountUuid, ImageConstant.ImageMediaType.RootVolumeTemplate);
            }

            return transformSingleValueToDataPointList(total == 0 ? 0 : ((double) root / total) * 100);
        } else if (queryObject.getMetricName().equals(ImageNamespace.DataVolumeTemplateCount.getName())) {
            Long count;
            if (allImageReadable) {
                count = Q.New(ImageVO.class).eq(ImageVO_.mediaType, ImageConstant.ImageMediaType.DataVolumeTemplate).count();
            } else {
                count = countImageInMediaType(accountUuid, ImageConstant.ImageMediaType.DataVolumeTemplate);
            }

            return transformSingleValueToDataPointList(count);
        } else if (queryObject.getMetricName().equals(ImageNamespace.DataVolumeTemplateInPercent.getName())) {
            Long total;
            Long data;
            if (allImageReadable) {
                total = Q.New(ImageVO.class).count();
                data = Q.New(ImageVO.class).eq(ImageVO_.mediaType, ImageConstant.ImageMediaType.DataVolumeTemplate).count();
            } else {
                total = countImage(accountUuid);
                data = countImageInMediaType(accountUuid, ImageConstant.ImageMediaType.DataVolumeTemplate);
            }

            return transformSingleValueToDataPointList(total == 0 ? 0 : ((double) data / total) * 100);
        }

        return null;
    }

    private long countImage(String accountUuid) {
        Long count = Q.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.accountUuid, accountUuid)
                .eq(AccountResourceRefVO_.resourceType, ImageVO.class.getSimpleName())
                .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                .count();
        return count == null ? 0L : count;
    }

    private long countReadyImage(String accountUuid) {
        Long count = Q.New(AccountResourceRefVO.class, ImageVO.class)
                .table0()
                    .eq(AccountResourceRefVO_.accountUuid, accountUuid)
                    .eq(AccountResourceRefVO_.resourceType, ImageVO.class.getSimpleName())
                    .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                    .eq(AccountResourceRefVO_.resourceUuid).table1(ImageVO_.uuid)
                    .selectCount(AccountResourceRefVO_.resourceUuid)
                .table1()
                    .eq(ImageVO_.status, ImageStatus.Ready)
                    .eq(ImageVO_.state, ImageState.Enabled)
                .find();
        return count == null ? 0L : count;
    }

    private long countImageInMediaType(String accountUuid, ImageConstant.ImageMediaType type) {
        Long count = Q.New(AccountResourceRefVO.class, ImageVO.class)
                .table0()
                    .eq(AccountResourceRefVO_.accountUuid, accountUuid)
                    .eq(AccountResourceRefVO_.resourceType, ImageVO.class.getSimpleName())
                    .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                    .eq(AccountResourceRefVO_.resourceUuid).table1(ImageVO_.uuid)
                    .selectCount(AccountResourceRefVO_.resourceUuid)
                .table1()
                    .eq(ImageVO_.mediaType, type)
                .find();
        return count == null ? 0L : count;
    }
}
