package org.zstack.core.aspect;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.core.encrypt.EncryptAfterSaveDbRecordExtensionPoint;
import org.zstack.header.core.encrypt.EncryptColumn;
import org.zstack.header.core.encrypt.IntegrityVerificationResourceFactory;
import org.zstack.header.vo.ResourceVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.EntityManager;
import java.lang.reflect.Field;

/**
 * @Author: DaoDao
 * @Date: 2021/12/2
 */
public aspect EncryptColumnAspect {
     private static final CLogger logger = Utils.getLogger(EncryptColumnAspect.class);

    @Autowired
    protected PluginRegistry pluginRegistry;

     after(EntityManager mgr, Object entity) : call(void EntityManager+.persist(Object))
           && target(mgr)
           && args(entity) {
         Field[] fields = entity.getClass().getDeclaredFields();
         for (IntegrityVerificationResourceFactory f : pluginRegistry.getExtensionList(IntegrityVerificationResourceFactory.class)) {
             if (entity.getClass().getSimpleName().equals(f.getResourceType())) {
                 f.doIntegrityAfterSaveDbRecord(entity);
                 break;
             }
         }

         for (Field field : fields) {
             if (field.getAnnotation(EncryptColumn.class) != null) {
                 ResourceVO resourceVO = (ResourceVO) entity;
                 pluginRegistry.getExtensionList(EncryptAfterSaveDbRecordExtensionPoint.class).forEach(point -> point.encryptAfterSaveDbRecord(resourceVO));
                 break;
             }
         }
     }

    after(EntityManager mgr, Object entity) : call(* EntityManager+.merge(Object))
            && target(mgr)
            && args(entity) {
        Field[] fields = entity.getClass().getDeclaredFields();
        for (IntegrityVerificationResourceFactory f : pluginRegistry.getExtensionList(IntegrityVerificationResourceFactory.class)) {
            if (entity.getClass().getSimpleName().equals(f.getResourceType())) {
                f.doIntegrityAfterUpdateDbRecord(entity);
                break;
            }
        }

        for (Field field : fields) {
            if (field.getAnnotation(EncryptColumn.class) != null) {
                ResourceVO resourceVO = (ResourceVO) entity;
                pluginRegistry.getExtensionList(EncryptAfterSaveDbRecordExtensionPoint.class).forEach(point -> point.encryptAfterUpdateDbRecord(resourceVO));
                break;
            }
        }
    }

}
