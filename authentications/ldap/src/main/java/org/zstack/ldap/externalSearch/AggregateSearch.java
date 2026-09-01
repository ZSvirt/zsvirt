package org.zstack.ldap.externalSearch;

import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.AggregateDirContextProcessor;
import org.zstack.ldap.driver.LdapEntryContextMapper;
import org.zstack.ldap.driver.LdapExternalSearchExtensionPoint;
import org.zstack.ldap.driver.LdapSearchedResult;
import org.zstack.ldap.entity.LdapEntryInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.naming.directory.SearchControls;
import java.util.*;
import java.util.function.Predicate;

public class AggregateSearch implements LdapExternalSearchExtensionPoint {
    private static final CLogger logger = Utils.getLogger(AggregateSearch.class);

    @Override
    public LdapSearchedResult trySearch(LdapTemplate ldapTemplate, String filter, SearchControls searchCtls, Predicate<String> resultFilter, Integer count) {
        LdapSearchedResult ldapSearchedResult = new LdapSearchedResult();

        AggregateDirContextProcessor aggregateDirContextProcessor = new AggregateDirContextProcessor();
        try {
            LdapEntryContextMapper mapper = new LdapEntryContextMapper().withResultFilter(resultFilter);
            List<LdapEntryInventory> aggregateResult = ldapTemplate.search(
                    "", filter, searchCtls, mapper, aggregateDirContextProcessor);
            aggregateResult.removeIf(Objects::isNull);

            if (count != null && aggregateResult.size() > count){
                ldapSearchedResult.setResult(aggregateResult.subList(0, count));
            } else {
                ldapSearchedResult.setResult(aggregateResult);
            }
        } catch (Exception e){
            logger.error("query ldap entry fail", e);
            ldapSearchedResult.setSuccess(false);
            ldapSearchedResult.setResult(null);
            ldapSearchedResult.setError(e.getMessage());
        }

        return ldapSearchedResult;
    }
}
