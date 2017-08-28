package com.sam.model.converters;

import com.sam.util.EncryptionUtil;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.UserType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * DocumentUserType is just a shorthand name for compressed encrypted file user type
 */
public class DocumentUserType implements UserType {

    private static final int[] SQL_TYPES = {Types.BLOB};
    private static final EncryptionUtil ENC_UTIL = EncryptionUtil.getInstance();

    @Override
    public int[] sqlTypes() {
        return SQL_TYPES;
    }

    @Override
    public Class returnedClass() {
        return (new byte[]{}).getClass();
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {
        byte[] rawData = rs.getBytes(names[0]);
        if (rawData == null) {
            return null;
        }
        try {
            //Decrypt.
            final byte[] data = ENC_UTIL.decrypt(rawData);

            final ByteArrayInputStream bis = new ByteArrayInputStream(data);
            final InflaterInputStream ios = new InflaterInputStream(bis);

            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int b;
            while ((b = ios.read()) != -1) {
                bos.write(b);
            }

            final byte[] output = bos.toByteArray();

            bis.close();
            bos.close();
            ios.close();
            return output;
        } catch (Exception e) {
            //Unable to decrypt. Bail out.
            throw new HibernateException("Unable to decrypt data", e);
        }
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
        if (null == value) {
            st.setNull(index, Types.BLOB);
        } else {
            try {
                byte[] fileContents = (byte[]) value;

                //First zip. Compression is more effective before encryption.
                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                Deflater compresser = new Deflater(Deflater.BEST_COMPRESSION);
                final DeflaterOutputStream dos = new DeflaterOutputStream(bos, compresser);
                dos.write(fileContents);
                dos.close();
                compresser.end();

                final byte[] result = ENC_UTIL.encrypt(bos.toByteArray());
                st.setBytes(index, result);

                bos.close();
            } catch (Exception e) {
                //Can't encrypt. Bail out.
                throw new HibernateException("Unable to compress/encrypt data.", e);
            }
        }

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
    public Object deepCopy(Object value) throws HibernateException {
        return value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable) value;
    }

    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return cached;
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }
}
