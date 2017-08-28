/*
 * Copyright (c) 2011 BankBazaar.com, Chennai, TN, India. All rights reserved.
 * This software is the confidential and proprietary information of BankBazaar.com
 * ("Confidential Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered into with BankBazaar.
 */

package com.sam.model.converters;

import com.sam.util.DomainUtil;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * This is converter that converts a Comma Separated Value list of <code>String</code> into a List
 */
public class CSVListType extends CSVType {

    public Class returnedClass() {
        return List.class;
    }

    public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {
        String csv = rs.getString(names[0]);
        if (csv == null || !StringUtils.hasText(csv)) {
            return new ArrayList<String>();
        }
        return DomainUtil.commaDelimitedListToList(csv);
    }

    public Object assemble(Serializable cached, Object owner, SessionImplementor session) throws HibernateException {
        return cached;
    }
}