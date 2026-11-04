package org.zstack.ldap.driver;

import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.NameAwareAttribute;
import org.springframework.ldap.core.support.AbstractContextMapper;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.ldap.entity.LdapEntryAttributeInventory;
import org.zstack.ldap.entity.LdapEntryInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionUtils.*;

public class LdapEntryContextMapper extends AbstractContextMapper<LdapEntryInventory> {
    private static final CLogger logger = Utils.getLogger(LdapEntryContextMapper.class);

    private static final String WINDOWS_AD_USER_ACCOUNT_CONTROL = "userAccountControl";
    /**
     * see https://learn.microsoft.com/en-us/troubleshoot/windows-server/active-directory/useraccountcontrol-manipulate-account-properties
     */
    private static final int CONTROL_FLAG_DISABLE = 0x0002;

    private Predicate<String> resultFilter;

    public LdapEntryContextMapper withResultFilter(Predicate<String> resultFilter) {
        this.resultFilter = resultFilter;
        return this;
    }

    @Override
    public LdapEntryInventory doMapFromContext(DirContextOperations ctx) {
        if (resultFilter != null && !resultFilter.test(ctx.getNameInNamespace())){
            return null;
        }
        return buildEntry(ctx);
    }

    private LdapEntryInventory buildEntry(DirContextOperations ctx) {
        LdapEntryInventory result = new LdapEntryInventory();
        result.setDn(ctx.getNameInNamespace());

        List<LdapEntryAttributeInventory> list = new ArrayList<>();
        result.setAttributes(list);

        Attributes attributes = ctx.getAttributes();
        NamingEnumeration<?> it = attributes.getAll();
        try {
            while (it.hasMore()){
                final LdapEntryAttributeInventory inventory = buildAttribute(it.next());
                if (inventory != null) {
                    list.add(inventory);
                }
            }
        } catch (javax.naming.NamingException e){
            logger.error("query ldap entry attributes fail", e.getCause());
            throw new OperationFailureException(operr("query ldap entry fail, %s", e.toString()));
        }

        fillUserState(result);
        return result;
    }

    private LdapEntryAttributeInventory buildAttribute(Object attribute) {
        if (attribute instanceof NameAwareAttribute) {
            final NameAwareAttribute nameAwareAttribute = (NameAwareAttribute) attribute;
            LdapEntryAttributeInventory inventory = new LdapEntryAttributeInventory();
            inventory.setId(nameAwareAttribute.getID());
            inventory.setOrderMatters(nameAwareAttribute.isOrdered());

            List<Object> values = new ArrayList<>();
            for (Object o : nameAwareAttribute) {
                values.add(o);
            }
            inventory.setValues(values);

            return inventory;
        }
        logger.debug("skip buildAttribute from class: " + attribute.getClass().getName());
        return null;
    }

    private void fillUserState(LdapEntryInventory inventory) {
        LdapEntryAttributeInventory attribute = findOneOrNull(inventory.getAttributes(),
                item -> WINDOWS_AD_USER_ACCOUNT_CONTROL.equals(item.getId()));
        if (attribute == null) {
            inventory.setEnable(true);
            return;
        }

        try {
            int flag = Integer.parseInt(attribute.getValues().get(0).toString());
            inventory.setEnable((flag & CONTROL_FLAG_DISABLE) == 0);
        } catch (RuntimeException e) {
            logger.info(String.format("failed to parse attribute %s=%s for ldap entry[dn=%s]: %s",
                    WINDOWS_AD_USER_ACCOUNT_CONTROL, attribute.getValues(), inventory.getDn(), e.getMessage()));
            inventory.setEnable(true);
        }
    }
}
