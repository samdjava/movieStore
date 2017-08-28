package com.sam.model.converters;

import com.sam.util.DomainUtil;
import com.sam.util.EncryptionUtil;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * This class extends from XMLUserType which serializes Java Objects to json string to be stored in database.
 * This class adds a layer of security by encrypting the resulting string.
 */
public class EncryptedXMLUserType extends XMLUserType {
    private final static EncryptionUtil encUtil = EncryptionUtil.getInstance();

    private static final int[] SQL_TYPES = {Types.BLOB};

    @Override
    public int[] sqlTypes() {
        return SQL_TYPES;
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {
        byte[] data = rs.getBytes(names[0]);
        if (data == null || data.length == 0) {
            return null;
        }
        try {
            return DomainUtil.XSTREAM.fromXML(new String(encUtil.decrypt(data)));
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
        if (null == value) {
            st.setNull(index, Types.BLOB);
        } else {
            try {
                st.setBytes(index, encUtil.encrypt(DomainUtil.XSTREAM.toXML(value).getBytes()));
            } catch (Exception e) {
                throw new SQLException(e);
            }
        }
    }
}
