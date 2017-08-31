package com.sam.model.converters;

import com.sam.util.DomainUtil;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.UserType;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * This Class is responsible for converting to and fro the Java objects into XML Clob. Used as a dao type class.
 */
public class CompressedXMLUserType implements UserType {


    private static final int[] SQL_TYPES = {Types.BLOB};

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
        byte[] rawData = rs.getBytes(names[0]);

        if (rawData == null) {
            return null;
        }

        try {
            final ByteArrayInputStream bis = new ByteArrayInputStream(rawData);
            final InflaterInputStream ios = new InflaterInputStream(bis);


            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int b;
            while ((b = ios.read()) != -1) {
                bos.write(b);
            }

            final String json = new String(bos.toByteArray());
            if (!StringUtils.hasText(json)) {
                return null;
            }

            final Object output = DomainUtil.XSTREAM.fromXML(json);
            bis.close();
            bos.close();
            ios.close();
            return output;
        } catch (Exception e) {
            //Unable to decrypt. Bail out.
            throw new HibernateException("Unable to uncompress data", e);
        }

    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
        if (null == value) {
            st.setNull(index, Types.BLOB);
        } else {
            try {
                byte[] content = DomainUtil.XSTREAM.toXML(value).getBytes();

                //First zip. Compression is more effective before encryption.
                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                Deflater compresser = new Deflater(Deflater.BEST_COMPRESSION);
                final DeflaterOutputStream dos = new DeflaterOutputStream(bos, compresser);
                dos.write(content);
                dos.close();
                compresser.end();

                st.setBytes(index, bos.toByteArray());

                bos.close();
            } catch (Exception e) {
                //Can't encrypt. Bail out.
                throw new HibernateException("Unable to compress data.", e);
            }
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
