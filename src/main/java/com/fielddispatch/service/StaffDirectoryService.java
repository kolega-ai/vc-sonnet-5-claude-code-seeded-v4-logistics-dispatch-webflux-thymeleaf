package com.fielddispatch.service;

import org.springframework.stereotype.Service;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * Bridges the corporate staff directory so the dispatch console can resolve a
 * field worker's directory entry (display name and mail) when they are added
 * to a region. The directory is the authoritative HR system; the console only
 * reads from it.
 */
@Service
public class StaffDirectoryService {

    private static final String LDAP_URL = "ldap://directory.corp.local:389";
    private static final String STAFF_BASE = "ou=people,dc=corp,dc=local";

    public List<String> resolveStaff(String directoryId) throws NamingException {
        Hashtable<String, Object> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, LDAP_URL);
        DirContext ctx = new InitialDirContext(env);
        SearchControls ctl = new SearchControls();
        ctl.setSearchScope(SearchControls.SUBTREE_SCOPE);
        List<String> found = new ArrayList<>();
        try {
            NamingEnumeration<SearchResult> hits = ctx.search(STAFF_BASE, "(uid=" + directoryId + ")", ctl);
            while (hits.hasMore()) {
                SearchResult hit = hits.next();
                found.add(String.valueOf(hit.getAttributes().get("cn").get()));
            }
        } finally {
            ctx.close();
        }
        return found;
    }
}
