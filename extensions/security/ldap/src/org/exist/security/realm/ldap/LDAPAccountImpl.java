package org.exist.security.realm.ldap;

import org.exist.config.Configuration;
import org.exist.config.ConfigurationException;
import org.exist.config.annotation.ConfigurationClass;
import org.exist.security.Account;
import org.exist.security.Group;
import org.exist.security.PermissionDeniedException;
import org.exist.security.AbstractRealm;
import org.exist.security.internal.AccountImpl;

/**
 *
 * @author aretter
 */
@ConfigurationClass("account")
public class LDAPAccountImpl extends AccountImpl {

    public LDAPAccountImpl(AbstractRealm realm, Configuration configuration) throws ConfigurationException {
        super(realm, configuration);
    }

    public LDAPAccountImpl(AbstractRealm realm, AccountImpl from_user) throws ConfigurationException {
        super(realm, from_user);
    }

    public LDAPAccountImpl(AbstractRealm realm, int id, Account from_user) throws ConfigurationException, PermissionDeniedException {
        super(realm, id, from_user);
    }

    public LDAPAccountImpl(AbstractRealm realm, String name) throws ConfigurationException {
        super(realm, name);
    }

    public LDAPAccountImpl(AbstractRealm realm, int id, String name, String password) throws ConfigurationException {
        super(realm, id, name, password);
    }

    LDAPAccountImpl(AbstractRealm realm, Configuration config, boolean removed) throws ConfigurationException {
        super(realm, config, removed);
    }

    @Override
    public Group addGroup(Group group) throws PermissionDeniedException {
        //if(group instanceof Ldap)
        return null; //TODO
    }
}