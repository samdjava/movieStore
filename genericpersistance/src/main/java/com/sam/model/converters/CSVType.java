/*
 * Copyright (c) 2010 BankBazaar.com, Chennai, TN, India. All rights reserved.
 * This software is the confidential and proprietary information of BankBazaar.com
 * ("Confidential Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered into with BankBazaar.
 */

package com.sam.model.converters;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.UserType;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * This is converter that converts a Comma Separated Value list of <code>String</code> into a ordered set by sorting the result.
 * For example, if the string is "B,A,C,E,D" the result will be a set with the order "A","B","C","D","E".
 */
public class CSVType implements UserType {

    private static final int[] SQL_TYPES = {Types.CLOB};

    @Override
    public int[] sqlTypes() {
        return SQL_TYPES;
    }

    @Override
    public Class returnedClass() {
        return List.class;
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {

        String csv = rs.getString(names[0]);
        if (csv == null || !StringUtils.hasText(csv)) {
            return new HashSet<String>(); // Empty set should not be TreeSet
        }

        return StringUtils.commaDelimitedListToSet(csv);
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
        if (null == value) {
            st.setNull(index, Types.CLOB);
        } else {
            List<String> trimmedValues = new ArrayList<String>();
            //noinspection unchecked
            for (String val : (Collection<String>) value) {
                trimmedValues.add(val.trim());
            }
            st.setString(index, StringUtils.collectionToCommaDelimitedString(trimmedValues));
        }
    }

    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return cached;
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        return value;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable) value;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        return x == y;
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        return x.hashCode();
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }

}