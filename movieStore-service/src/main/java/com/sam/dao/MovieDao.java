package com.sam.dao;

import com.sam.generic.dao.GenericDao;
import com.sam.model.Movie;

/**
 * Created by root on 28/8/17.
 */
public interface MovieDao extends GenericDao<Movie,Long> {

    void createEntry(String filmName,String director);

}
