package com.sam.dao.impl;

import com.sam.dao.MovieDao;
import com.sam.dao.hibernate.GenericDaoHibernate;
import com.sam.model.Movie;

/**
 * Created by root on 28/8/17.
 */
public class MovieDaoImpl extends GenericDaoHibernate<Movie,Long> implements MovieDao {

    public MovieDaoImpl() {
        super(Movie.class);
    }



}
