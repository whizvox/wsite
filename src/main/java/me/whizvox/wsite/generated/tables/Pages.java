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
import me.whizvox.wsite.generated.tables.records.PagesRecord;

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
public class Pages extends TableImpl<PagesRecord> {

    private static final long serialVersionUID = -1085746939;

    /**
     * The reference instance of <code>pages</code>
     */
    public static final Pages PAGES = new Pages();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<PagesRecord> getRecordType() {
        return PagesRecord.class;
    }

    /**
     * The column <code>pages.path</code>.
     */
    public final TableField<PagesRecord, String> PATH = createField("path", org.jooq.impl.SQLDataType.VARCHAR(255), this, "");

    /**
     * The column <code>pages.title</code>.
     */
    public final TableField<PagesRecord, String> TITLE = createField("title", org.jooq.impl.SQLDataType.VARCHAR(1000).nullable(false), this, "");

    /**
     * The column <code>pages.contents</code>.
     */
    public final TableField<PagesRecord, String> CONTENTS = createField("contents", org.jooq.impl.SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column <code>pages.syntax</code>.
     */
    public final TableField<PagesRecord, String> SYNTAX = createField("syntax", org.jooq.impl.SQLDataType.VARCHAR(30).nullable(false), this, "");

    /**
     * The column <code>pages.published</code>.
     */
    public final TableField<PagesRecord, Timestamp> PUBLISHED = createField("published", org.jooq.impl.SQLDataType.TIMESTAMP.nullable(false), this, "");

    /**
     * The column <code>pages.last_edited</code>.
     */
    public final TableField<PagesRecord, Timestamp> LAST_EDITED = createField("last_edited", org.jooq.impl.SQLDataType.TIMESTAMP, this, "");

    /**
     * Create a <code>pages</code> table reference
     */
    public Pages() {
        this(DSL.name("pages"), null);
    }

    /**
     * Create an aliased <code>pages</code> table reference
     */
    public Pages(String alias) {
        this(DSL.name(alias), PAGES);
    }

    /**
     * Create an aliased <code>pages</code> table reference
     */
    public Pages(Name alias) {
        this(alias, PAGES);
    }

    private Pages(Name alias, Table<PagesRecord> aliased) {
        this(alias, aliased, null);
    }

    private Pages(Name alias, Table<PagesRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""));
    }

    public <O extends Record> Pages(Table<O> child, ForeignKey<O, PagesRecord> key) {
        super(child, key, PAGES);
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
        return Arrays.<Index>asList(Indexes.SQLITE_AUTOINDEX_PAGES_1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UniqueKey<PagesRecord> getPrimaryKey() {
        return Keys.PK_PAGES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UniqueKey<PagesRecord>> getKeys() {
        return Arrays.<UniqueKey<PagesRecord>>asList(Keys.PK_PAGES);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Pages as(String alias) {
        return new Pages(DSL.name(alias), this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Pages as(Name alias) {
        return new Pages(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public Pages rename(String name) {
        return new Pages(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Pages rename(Name name) {
        return new Pages(name, null);
    }
}
