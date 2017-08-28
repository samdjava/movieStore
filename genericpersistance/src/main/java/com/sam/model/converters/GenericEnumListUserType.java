package com.sam.model.converters;

import org.hibernate.engine.spi.SessionImplementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.HibernateException;
import org.hibernate.type.SingleColumnType;
import org.hibernate.type.TypeFactory;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;


public class GenericEnumListUserType implements UserType, ParameterizedType {
    private static final String DEFAULT_IDENTIFIER_METHOD_NAME = "name";
    private static final String DEFAULT_VALUE_OF_METHOD_NAME = "valueOf";
    private static final Logger logger = LoggerFactory.getLogger(GenericEnumListUserType.class);

    private Class<? extends Enum> enumClass;
    private Class<?> identifierType;
    private Method identifierMethod;
    private Method valueOfMethod;
    private SingleColumnType type;

    private static final int[] SQL_TYPES = {Types.VARCHAR};

    @Override
    public int[] sqlTypes() {
        return SQL_TYPES;
    }

    @Override
    public void setParameterValues(Properties parameters) {
        String enumClassName = parameters.getProperty("enumClass");
        try {
            enumClass = Class.forName(enumClassName).asSubclass(Enum.class);
        } catch (ClassNotFoundException cfne) {
            logger.error("Class Not Found " + enumClassName,cfne);
            throw new HibernateException("Enum [" + enumClassName + "] class not found", cfne);
        }

        String identifierMethodName = parameters.getProperty("identifierMethod", DEFAULT_IDENTIFIER_METHOD_NAME);

        try {
            identifierMethod = enumClass.getMethod(identifierMethodName, new Class[0]);
            identifierType = identifierMethod.getReturnType();
        } catch (Exception e) {
            throw new HibernateException("Failed to obtain identifier method", e);
        }

        type = (SingleColumnType) new TypeFactory().byClass(identifierType, parameters);

        if (type == null)
            throw new HibernateException("Unsupported identifier type " + identifierType.getName());

        String valueOfMethodName = parameters.getProperty("valueOfMethod", DEFAULT_VALUE_OF_METHOD_NAME);

        try {
            valueOfMethod = enumClass.getMethod(valueOfMethodName, new Class[]{identifierType});
        } catch (Exception e) {
            throw new HibernateException("Failed to obtain valueOf method", e);
        }
    }

    @Override
    public Class returnedClass() {
        return enumClass;
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {

        String csv = rs.getString(names[0]);

        final Set result = EnumSet.noneOf(enumClass);

        if (!rs.wasNull()) {
            try {
                final Set<String> identifiers = StringUtils.commaDelimitedListToSet(csv);

                for(String identifier : identifiers) {
                    result.add(valueOfMethod.invoke(enumClass, new Object[]{identifier}));
                }
            } catch (Exception e) {
                throw new HibernateException("Exception while invoking valueOf method '" + valueOfMethod.getName() + "' of " +
                        "enumeration class '" + enumClass + "'", e);
            }
        }

        return result;
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {

        try {
            if (null == value) {
                st.setNull(index, Types.VARCHAR);
            } else {

                Set<String> identifiers =new LinkedHashSet<String>();
                for(Object enumValue : (Collection) value) {
                    identifiers.add(String.valueOf(identifierMethod.invoke(enumValue, new Object[0])));
                }

                st.setString(index, StringUtils.collectionToCommaDelimitedString(identifiers));
            }

        } catch (Exception e) {
            throw new HibernateException("Exception while invoking identifierMethod '" + identifierMethod.getName() + "' of " +
                    "enumeration class '" + enumClass + "'", e);
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
