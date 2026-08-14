package org.zstack.core.keyvalue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.keyvalue.KeyValueEntity;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.serializable.SerializableHelper;

import javax.persistence.Query;
import java.io.IOException;
import java.util.List;

/**
 */
public class KeyValueFacadeImpl implements KeyValueFacade {
    @Autowired
    private DatabaseFacade dbf;

    @Override
    @Transactional
    public void persist(KeyValueEntity entity) {
        KeyValueBinaryVO bvo = new KeyValueBinaryVO();
        bvo.setUuid(entity.getUuid());
        try {
            bvo.setContents(SerializableHelper.writeObject(entity));
        } catch (IOException e) {
            throw new CloudRuntimeException(e);
        }
        dbf.getEntityManager().persist(bvo);

        List<KeyValueStruct> structs = new KeyValueSerializer().build(entity);
        for (KeyValueStruct struct : structs) {
            KeyValueVO vo = new KeyValueVO();
            vo.setClassName(entity.getClass().getName());
            vo.setUuid(entity.getUuid());
            vo.setEntityKey(struct.getKey());
            vo.setEntityValue(struct.getValue());
            vo.setValueType(struct.getType().getName());
            dbf.getEntityManager().persist(vo);
        }
    }

    @Override
    @Transactional
    public void update(KeyValueEntity entity) {
        delete(entity.getUuid());
        persist(entity);
    }

    @Override
    @Transactional
    public void delete(String uuid) {
        String sql = "delete from KeyValueVO vo where vo.uuid = :uuid";
        Query q = dbf.getEntityManager().createQuery(sql);
        q.setParameter("uuid", uuid);
        q.executeUpdate();

        sql = "delete from KeyValueBinaryVO vo where vo.uuid = :uuid";
        q = dbf.getEntityManager().createQuery(sql);
        q.setParameter("uuid", uuid);
        q.executeUpdate();
    }

    @Override
    public void delete(KeyValueEntity entity) {
        delete(entity.getUuid());
    }

    @Override
    @Transactional
    public <T> T find(String uuid) {
        KeyValueBinaryVO bvo = dbf.getEntityManager().find(KeyValueBinaryVO.class, uuid);
        try {
            return SerializableHelper.readObject(bvo.getContents());
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }
}
