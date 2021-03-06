/*
 * This file is generated by jOOQ.
 */
package me.whizvox.wsite.generated.tables;


import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Generated;

import me.whizvox.wsite.generated.DefaultSchema;
import me.whizvox.wsite.generated.Indexes;
import me.whizvox.wsite.generated.Keys;
import me.whizvox.wsite.generated.tables.records.LoginsRecord;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.11.5"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Logins extends TableImpl<LoginsRecord> {

    private static final long serialVersionUID = -1532409808;

    /**
     * The reference instance of <code>logins</code>
     */
    public static final Logins LOGINS = new Logins();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<LoginsRecord> getRecordType() {
        return LoginsRecord.class;
    }

    /**
     * The column <code>logins.token</code>.
     */
    public final TableField<LoginsRecord, String> TOKEN = createField("token", org.jooq.impl.SQLDataType.CHAR(24), this, "");

    /**
     * The column <code>logins.user_id</code>.
     */
    public final TableField<LoginsRecord, String> USER_ID = createField("user_id", org.jooq.impl.SQLDataType.CHAR(36).nullable(false), this, "");

    /**
     * The column <code>logins.user_agent</code>.
     */
    public final TableField<LoginsRecord, String> USER_AGENT = createField("user_agent", org.jooq.impl.SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>logins.ip_address</code>.
     */
    public final TableField<LoginsRecord, String> IP_ADDRESS = createField("ip_address", org.jooq.impl.SQLDataType.VARCHAR(50).nullable(false), this, "");

    /**
     * The column <code>logins.expiration_date</code>.
     */
    public final TableField<LoginsRecord, Timestamp> EXPIRATION_DATE = createField("expiration_date", org.jooq.impl.SQLDataType.TIMESTAMP.nullable(false), this, "");

    /**
     * Create a <code>logins</code> table reference
     */
    public Logins() {
        this(DSL.name("logins"), null);
    }

    /**
     * Create an aliased <code>logins</code> table reference
     */
    public Logins(String alias) {
        this(DSL.name(alias), LOGINS);
    }

    /**
     * Create an aliased <code>logins</code> table reference
     */
    public Logins(Name alias) {
        this(alias, LOGINS);
    }

    private Logins(Name alias, Table<LoginsRecord> aliased) {
        this(alias, aliased, null);
    }

    private Logins(Name alias, Table<LoginsRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""));
    }

    public <O extends Record> Logins(Table<O> child, ForeignKey<O, LoginsRecord> key) {
        super(child, key, LOGINS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Schema getSchema() {
        return DefaultSchema.DEFAULT_SCHEMA;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.SQLITE_AUTOINDEX_LOGINS_1, Indexes.SQLITE_AUTOINDEX_LOGINS_2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UniqueKey<LoginsRecord> getPrimaryKey() {
        return Keys.PK_LOGINS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UniqueKey<LoginsRecord>> getKeys() {
        return Arrays.<UniqueKey<LoginsRecord>>asList(Keys.PK_LOGINS, Keys.SQLITE_AUTOINDEX_LOGINS_2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Logins as(String alias) {
        return new Logins(DSL.name(alias), this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Logins as(Name alias) {
        return new Logins(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public Logins rename(String name) {
        return new Logins(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Logins rename(Name name) {
        return new Logins(name, null);
    }
}
