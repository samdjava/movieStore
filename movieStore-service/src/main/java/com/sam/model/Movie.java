package com.sam.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by root on 28/8/17.
 */
@Entity
@Table(name = "movie")
public class Movie {
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "filmName")
    String filmName;

    @Column(name = "director")
    String director;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFilmName() {
        return filmName;
    }

    public void setFilmName(String filmName) {
        this.filmName = filmName;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    @Override
    public String toString() {
        return "Movie{" +
                "id=" + id +
                ", filmName='" + filmName + '\'' +
                ", director='" + director + '\'' +
                '}';
    }
}
