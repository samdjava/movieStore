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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Converts a list of string to a String separated by new line character
 */
public class MultilineType implements UserType {
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
    public boolean equals(Object o, Object o1) throws HibernateException {
        return o == o1;
    }

    @Override
    public int hashCode(Object o) throws HibernateException {
        return o.hashCode();
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {
        String delimitedString = rs.getString(names[0]);
        if (delimitedString == null || !StringUtils.hasText(delimitedString)) {
            return Collections.<String>emptyList();
        }
        return Arrays.asList(StringUtils.delimitedListToStringArray(delimitedString, "\n"));
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
        if (null == value) {
            st.setNull(index, Types.CLOB);
        } else {
            st.setString(index, StringUtils.collectionToDelimitedString((Collection) value, "\n"));
        }
    }

    @Override
    public Object deepCopy(Object o) throws HibernateException {
        return o;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(Object o) throws HibernateException {
        return (Serializable) o;
    }

    @Override
    public Object assemble(Serializable cached, Object o) throws HibernateException {
        return cached;

    }

    @Override
    public Object replace(Object o, Object o1, Object o2) throws HibernateException {
        return o;
    }
}
