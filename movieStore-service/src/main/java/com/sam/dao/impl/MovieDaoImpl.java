package com.sam.dao.impl;

import com.sam.dao.MovieDao;
import com.sam.generic.dao.GenericDaoHibernate;
import com.sam.model.Movie;

/**
 * Created by root on 28/8/17.
 */
public class MovieDaoImpl extends GenericDaoHibernate<Movie,Long> implements MovieDao {

    public MovieDaoImpl() {
        super(Movie.class);
    }

    public void createEntry(String filmName, String director) {

    }
}
