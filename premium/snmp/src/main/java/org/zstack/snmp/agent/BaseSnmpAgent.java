package org.zstack.snmp.agent;

import org.snmp4j.MessageDispatcherImpl;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.agent.*;
import org.snmp4j.agent.mo.MOAccessImpl;
import org.snmp4j.agent.mo.MOScalar;
import org.snmp4j.agent.mo.snmp.SnmpCommunityMIB;
import org.snmp4j.agent.mo.snmp.SnmpTargetMIB;
import org.snmp4j.agent.mo.snmp.StorageType;
import org.snmp4j.agent.mo.snmp.VacmMIB;
import org.snmp4j.agent.security.MutableVACM;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.*;
import org.snmp4j.smi.*;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.snmp.SnmpConstants;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @Author : jingwang
 * @create 2023/7/17 5:40 PM
 */
public abstract class BaseSnmpAgent implements SnmpAgent {
    private final static CLogger logger = Utils.getLogger(BaseSnmpAgent.class);

    protected CommandProcessor agent;
    protected byte[] localEngineID;
    protected List<TransportMapping<? extends Address>> transportMappings;
    protected MessageDispatcherImpl dispatcher;
    protected MOServer server;
    protected Snmp session;
    protected SnmpCommunityMIB snmpCommunityMIB;
    protected SnmpTargetMIB snmpTargetMIB;
    protected VacmMIB vacmMIB;
    protected MPv3 mpv3;
    protected USM usm;
    protected SnmpAgentState agentState = SnmpAgentState.STATE_CREATED;

    protected Set<UsmUser> usmUsers = new HashSet<>();
    protected Set<String> readCommunities = new HashSet<>();

    protected void registerManagedObject(ManagedObject mo) {
        try {
            server.register(mo, null);
        } catch (DuplicateRegistrationException ignored) {
            logger.warn("Snmp agent register duplicate mo[%s]");
        }
    }

    protected void initMOServer() {
        server = new DefaultMOServer();
        server.addContext(new OctetString());
    }

    protected void registerNecessarySnmpMIBs() throws DuplicateRegistrationException {
        snmpTargetMIB.registerMOs(server, null);
        snmpCommunityMIB.registerMOs(server, null);
        vacmMIB.registerMOs(server, null);
    }

    protected void addViews(VacmMIB vacm) {
        vacm.addGroup(SecurityModel.SECURITY_MODEL_SNMPv1,
                new OctetString(SnmpConstants.DEFAULT_SNMP_SECURITY_NAME),
                new OctetString(SnmpConstants.DEFAULT_SNMP_V1_V2_GROUP_NAME),
                StorageType.nonVolatile);

        vacm.addGroup(SecurityModel.SECURITY_MODEL_SNMPv2c,
                new OctetString(SnmpConstants.DEFAULT_SNMP_SECURITY_NAME),
                new OctetString(SnmpConstants.DEFAULT_SNMP_V1_V2_GROUP_NAME),
                StorageType.nonVolatile);

        vacm.addAccess(new OctetString(SnmpConstants.DEFAULT_SNMP_V1_V2_GROUP_NAME),
                new OctetString(),
                SecurityModel.SECURITY_MODEL_ANY, SecurityLevel.NOAUTH_NOPRIV,
                MutableVACM.VACM_MATCH_EXACT, new OctetString(SnmpConstants.DEFAULT_SNMP_FULL_READ_VIEW_NAME),
                new OctetString(""), new OctetString(""), StorageType.nonVolatile);

        vacm.addAccess(new OctetString(SnmpConstants.DEFAULT_SNMP_V3_GROUP_NOAUTH_NOPRIV_NAME),
                new OctetString(),
                SecurityModel.SECURITY_MODEL_USM, SecurityLevel.NOAUTH_NOPRIV,
                MutableVACM.VACM_MATCH_EXACT, new OctetString(SnmpConstants.DEFAULT_SNMP_FULL_READ_VIEW_NAME),
                new OctetString(), new OctetString(), StorageType.nonVolatile);

        vacm.addAccess(new OctetString(SnmpConstants.DEFAULT_SNMP_V3_GROUP_AUTH_NOPRIV_NAME),
                new OctetString(),
                SecurityModel.SECURITY_MODEL_USM, SecurityLevel.AUTH_NOPRIV,
                MutableVACM.VACM_MATCH_EXACT, new OctetString(SnmpConstants.DEFAULT_SNMP_FULL_READ_VIEW_NAME),
                new OctetString(), new OctetString(), StorageType.nonVolatile);

        vacm.addAccess(new OctetString(SnmpConstants.DEFAULT_SNMP_V3_GROUP_AUTH_PRIV_NAME),
                new OctetString(),
                SecurityModel.SECURITY_MODEL_USM, SecurityLevel.AUTH_PRIV,
                MutableVACM.VACM_MATCH_EXACT, new OctetString(SnmpConstants.DEFAULT_SNMP_FULL_READ_VIEW_NAME),
                new OctetString(), new OctetString(), StorageType.nonVolatile);

        vacm.addViewTreeFamily(new OctetString(SnmpConstants.DEFAULT_SNMP_FULL_READ_VIEW_NAME),
                new OID(SnmpConstants.DEFAULT_SNMP_FULL_READ_VIEW_OID),
                new OctetString(), VacmMIB.vacmViewIncluded,
                StorageType.nonVolatile);
    }



    protected void initMessageDispatcher() {
        dispatcher = new MessageDispatcherImpl();
        mpv3 = new MPv3(agent.getContextEngineID().getValue());
        // not support engine boot count
        usm = new USM(SecurityProtocols.getInstance(),
                agent.getContextEngineID(),
                0);
        SecurityModels.getInstance().addSecurityModel(usm);
        SecurityProtocols.getInstance().addDefaultProtocols();
        SecurityProtocols.getInstance().addPrivacyProtocol(new Priv3DES());
        dispatcher.addMessageProcessingModel(new MPv1());
        dispatcher.addMessageProcessingModel(new MPv2c());
        dispatcher.addMessageProcessingModel(mpv3);
        initSnmpSession();
    }

    protected void initSnmpSession() {
        session = new Snmp(dispatcher);
        for (TransportMapping<? extends Address> transportMapping : transportMappings) {
            try {
                session.addTransportMapping(transportMapping);
            } catch (Exception ex) {
                throw new CloudRuntimeException("Failed to initialize transport mapping '" +
                        transportMapping + "' with: " + ex.getMessage());
            }
        }
    }

    protected void finishInit() {
        agent.addMOServer(server);
        agent.setCoexistenceProvider(snmpCommunityMIB);
        agent.setVacm(vacmMIB);
        dispatcher.addCommandResponder(agent);
        agentState = SnmpAgentState.STATE_INIT_FINISHED;
    }

    @Override
    public boolean addReadCommunity(String readCommunity) {
        snmpCommunityMIB.addSnmpCommunityEntry(
                new OctetString(readCommunity),
                new OctetString(readCommunity),
                new OctetString(SnmpConstants.DEFAULT_SNMP_SECURITY_NAME),
                new OctetString(localEngineID),
                new OctetString(),
                new OctetString(),
                StorageType.nonVolatile);
        readCommunities.add(readCommunity);
        return true;
    }

    @Override
    public boolean addUsmUser(UsmUser user) {
        if (user.getPrivacyProtocol() != null) {
            vacmMIB.addGroup(
                    SecurityModel.SECURITY_MODEL_USM,
                    user.getSecurityName(),
                    new OctetString(SnmpConstants.DEFAULT_SNMP_V3_GROUP_AUTH_PRIV_NAME),
                    StorageType.nonVolatile
            );
        } else if (user.getAuthenticationProtocol() != null) {
            vacmMIB.addGroup(
                    SecurityModel.SECURITY_MODEL_USM,
                    user.getSecurityName(),
                    new OctetString(SnmpConstants.DEFAULT_SNMP_V3_GROUP_AUTH_NOPRIV_NAME),
                    StorageType.nonVolatile
            );
        } else {
            vacmMIB.addGroup(
                    SecurityModel.SECURITY_MODEL_USM,
                    user.getSecurityName(),
                    new OctetString(SnmpConstants.DEFAULT_SNMP_V3_GROUP_NOAUTH_NOPRIV_NAME),
                    StorageType.nonVolatile
            );
        }
        usm.addUser(user.getSecurityName(), agent.getContextEngineID(), user);
        usmUsers.add(user);
        return true;
    }

    @Override
    public boolean removeReadCommunity(String readCommunity) {
        if (readCommunities.contains(readCommunity)) {
            return snmpCommunityMIB.removeSnmpCommuntiyEntry(new OctetString(readCommunity));
        }
        return true;
    }

    @Override
    public boolean removeUsmUser(String userName) {
        if (usmUsers.stream().anyMatch(usmUser -> usmUser.getSecurityName().toString().equals(userName))) {
            return !usm.removeAllUsers(new OctetString(userName)).isEmpty();
        }
        return true;
    }

    @Override
    public boolean clearReadCommunity() {
        for (String readCommunity : readCommunities) {
            removeReadCommunity(readCommunity);
        }
        readCommunities.clear();
        return true;
    }

    @Override
    public boolean clearUsmUser() {
        usm.removeAllUsers();
        usmUsers.clear();
        return true;
    }
}
