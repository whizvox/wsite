/*
 * This file is generated by jOOQ.
 */
package me.whizvox.wsite.generated.tables.records;


import java.sql.Timestamp;

import javax.annotation.Generated;

import me.whizvox.wsite.generated.tables.Users;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record6;
import org.jooq.Row6;
import org.jooq.impl.UpdatableRecordImpl;


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
public class UsersRecord extends UpdatableRecordImpl<UsersRecord> implements Record6<String, String, String, String, Boolean, Timestamp> {

    private static final long serialVersionUID = -1009633950;

    /**
     * Setter for <code>users.id</code>.
     */
    public void setId(String value) {
        set(0, value);
    }

    /**
     * Getter for <code>users.id</code>.
     */
    public String getId() {
        return (String) get(0);
    }

    /**
     * Setter for <code>users.username</code>.
     */
    public void setUsername(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>users.username</code>.
     */
    public String getUsername() {
        return (String) get(1);
    }

    /**
     * Setter for <code>users.email_address</code>.
     */
    public void setEmailAddress(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>users.email_address</code>.
     */
    public String getEmailAddress() {
        return (String) get(2);
    }

    /**
     * Setter for <code>users.password</code>.
     */
    public void setPassword(String value) {
        set(3, value);
    }

    /**
     * Getter for <code>users.password</code>.
     */
    public String getPassword() {
        return (String) get(3);
    }

    /**
     * Setter for <code>users.operator</code>.
     */
    public void setOperator(Boolean value) {
        set(4, value);
    }

    /**
     * Getter for <code>users.operator</code>.
     */
    public Boolean getOperator() {
        return (Boolean) get(4);
    }

    /**
     * Setter for <code>users.when_created</code>.
     */
    public void setWhenCreated(Timestamp value) {
        set(5, value);
    }

    /**
     * Getter for <code>users.when_created</code>.
     */
    public Timestamp getWhenCreated() {
        return (Timestamp) get(5);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Record1<String> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record6 type implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Row6<String, String, String, String, Boolean, Timestamp> fieldsRow() {
        return (Row6) super.fieldsRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Row6<String, String, String, String, Boolean, Timestamp> valuesRow() {
        return (Row6) super.valuesRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field1() {
        return Users.USERS.ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field2() {
        return Users.USERS.USERNAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field3() {
        return Users.USERS.EMAIL_ADDRESS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field4() {
        return Users.USERS.PASSWORD;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Boolean> field5() {
        return Users.USERS.OPERATOR;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Timestamp> field6() {
        return Users.USERS.WHEN_CREATED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String component1() {
        return getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String component2() {
        return getUsername();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String component3() {
        return getEmailAddress();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String component4() {
        return getPassword();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean component5() {
        return getOperator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timestamp component6() {
        return getWhenCreated();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value1() {
        return getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value2() {
        return getUsername();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value3() {
        return getEmailAddress();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value4() {
        return getPassword();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean value5() {
        return getOperator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timestamp value6() {
        return getWhenCreated();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UsersRecord value1(String value) {
        setId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UsersRecord value2(String value) {
        setUsername(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UsersRecord value3(String value) {
        setEmailAddress(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UsersRecord value4(String value) {
        setPassword(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UsersRecord value5(Boolean value) {
        setOperator(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UsersRecord value6(Timestamp value) {
        setWhenCreated(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UsersRecord values(String value1, String value2, String value3, String value4, Boolean value5, Timestamp value6) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached UsersRecord
     */
    public UsersRecord() {
        super(Users.USERS);
    }

    /**
     * Create a detached, initialised UsersRecord
     */
    public UsersRecord(String id, String username, String emailAddress, String password, Boolean operator, Timestamp whenCreated) {
        super(Users.USERS);

        set(0, id);
        set(1, username);
        set(2, emailAddress);
        set(3, password);
        set(4, operator);
        set(5, whenCreated);
    }
}
