/*
 * This file is generated by jOOQ.
 */
package me.whizvox.wsite.generated;


import javax.annotation.Generated;

import me.whizvox.wsite.generated.tables.Logins;
import me.whizvox.wsite.generated.tables.Pages;
import me.whizvox.wsite.generated.tables.UnverifiedUsers;
import me.whizvox.wsite.generated.tables.Users;
import me.whizvox.wsite.generated.tables.records.LoginsRecord;
import me.whizvox.wsite.generated.tables.records.PagesRecord;
import me.whizvox.wsite.generated.tables.records.UnverifiedUsersRecord;
import me.whizvox.wsite.generated.tables.records.UsersRecord;

import org.jooq.UniqueKey;
import org.jooq.impl.Internal;


/**
 * A class modelling foreign key relationships and constraints of tables of 
 * the <code></code> schema.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.11.5"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Keys {

    // -------------------------------------------------------------------------
    // IDENTITY definitions
    // -------------------------------------------------------------------------


    // -------------------------------------------------------------------------
    // UNIQUE and PRIMARY KEY definitions
    // -------------------------------------------------------------------------

    public static final UniqueKey<LoginsRecord> PK_LOGINS = UniqueKeys0.PK_LOGINS;
    public static final UniqueKey<LoginsRecord> SQLITE_AUTOINDEX_LOGINS_2 = UniqueKeys0.SQLITE_AUTOINDEX_LOGINS_2;
    public static final UniqueKey<PagesRecord> PK_PAGES = UniqueKeys0.PK_PAGES;
    public static final UniqueKey<UnverifiedUsersRecord> PK_UNVERIFIED_USERS = UniqueKeys0.PK_UNVERIFIED_USERS;
    public static final UniqueKey<UsersRecord> PK_USERS = UniqueKeys0.PK_USERS;
    public static final UniqueKey<UsersRecord> SQLITE_AUTOINDEX_USERS_2 = UniqueKeys0.SQLITE_AUTOINDEX_USERS_2;
    public static final UniqueKey<UsersRecord> SQLITE_AUTOINDEX_USERS_3 = UniqueKeys0.SQLITE_AUTOINDEX_USERS_3;
    public static final UniqueKey<UsersRecord> SQLITE_AUTOINDEX_USERS_4 = UniqueKeys0.SQLITE_AUTOINDEX_USERS_4;

    // -------------------------------------------------------------------------
    // FOREIGN KEY definitions
    // -------------------------------------------------------------------------


    // -------------------------------------------------------------------------
    // [#1459] distribute members to avoid static initialisers > 64kb
    // -------------------------------------------------------------------------

    private static class UniqueKeys0 {
        public static final UniqueKey<LoginsRecord> PK_LOGINS = Internal.createUniqueKey(Logins.LOGINS, "pk_logins", Logins.LOGINS.TOKEN);
        public static final UniqueKey<LoginsRecord> SQLITE_AUTOINDEX_LOGINS_2 = Internal.createUniqueKey(Logins.LOGINS, "sqlite_autoindex_logins_2", Logins.LOGINS.USER_ID, Logins.LOGINS.USER_AGENT, Logins.LOGINS.IP_ADDRESS);
        public static final UniqueKey<PagesRecord> PK_PAGES = Internal.createUniqueKey(Pages.PAGES, "pk_pages", Pages.PAGES.PATH);
        public static final UniqueKey<UnverifiedUsersRecord> PK_UNVERIFIED_USERS = Internal.createUniqueKey(UnverifiedUsers.UNVERIFIED_USERS, "pk_unverified_users", UnverifiedUsers.UNVERIFIED_USERS.TOKEN);
        public static final UniqueKey<UsersRecord> PK_USERS = Internal.createUniqueKey(Users.USERS, "pk_users", Users.USERS.ID);
        public static final UniqueKey<UsersRecord> SQLITE_AUTOINDEX_USERS_2 = Internal.createUniqueKey(Users.USERS, "sqlite_autoindex_users_2", Users.USERS.USERNAME);
        public static final UniqueKey<UsersRecord> SQLITE_AUTOINDEX_USERS_3 = Internal.createUniqueKey(Users.USERS, "sqlite_autoindex_users_3", Users.USERS.EMAIL_ADDRESS);
        public static final UniqueKey<UsersRecord> SQLITE_AUTOINDEX_USERS_4 = Internal.createUniqueKey(Users.USERS, "sqlite_autoindex_users_4", Users.USERS.PASSWORD);
    }
}
