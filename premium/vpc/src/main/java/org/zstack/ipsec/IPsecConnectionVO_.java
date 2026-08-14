package org.zstack.ipsec;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by xing5 on 2016/11/8.
 */
@StaticMetamodel(IPsecConnectionVO.class)
public class IPsecConnectionVO_ extends ResourceVO_{
    public static volatile SingularAttribute<IPsecConnectionVO, String> name;
    public static volatile SingularAttribute<IPsecConnectionVO, String> description;
    public static volatile SingularAttribute<IPsecConnectionVO, String> peerAddress;
    public static volatile SingularAttribute<IPsecConnectionVO, String> authMode;
    public static volatile SingularAttribute<IPsecConnectionVO, String> authKey;
    public static volatile SingularAttribute<IPsecConnectionVO, String> vipUuid;
    public static volatile SingularAttribute<IPsecConnectionVO, IPsecState> state;
    public static volatile SingularAttribute<IPsecConnectionVO, IPSecStatus> status;
    public static volatile SingularAttribute<IPsecConnectionVO, String> ikeAuthAlgorithm;
    public static volatile SingularAttribute<IPsecConnectionVO, String> ikeEncryptionAlgorithm;
    public static volatile SingularAttribute<IPsecConnectionVO, Integer> ikeDhGroup;
    public static volatile SingularAttribute<IPsecConnectionVO, String> policyAuthAlgorithm;
    public static volatile SingularAttribute<IPsecConnectionVO, String> policyEncryptionAlgorithm;
    public static volatile SingularAttribute<IPsecConnectionVO, String> pfs;
    public static volatile SingularAttribute<IPsecConnectionVO, String> policyMode;
    public static volatile SingularAttribute<IPsecConnectionVO, String> transformProtocol;
    public static volatile SingularAttribute<IPsecConnectionVO, String> ikeVersion;
    public static volatile SingularAttribute<IPsecConnectionVO, String> idType;
    public static volatile SingularAttribute<IPsecConnectionVO, String> localId;
    public static volatile SingularAttribute<IPsecConnectionVO, String> remoteId;
    public static volatile SingularAttribute<IPsecConnectionVO, String> ikeLifeTime;
    public static volatile SingularAttribute<IPsecConnectionVO, String> lifeTime;
    public static volatile SingularAttribute<IPsecConnectionVO, Timestamp> createDate;
    public static volatile SingularAttribute<IPsecConnectionVO, Timestamp> lastOpDate;
}
