package com.sam.dao;

import org.hibernate.criterion.DetachedCriteria;

import java.io.Serializable;
import java.util.List;
import java.util.Map;


/**
 * Generic DAO (Data Access Object) with common methods to CRUD POJOs.
 * <p/>
 * <p>Extend this interface if you want typesafe (no casting necessary) DAO's for your
 * domain objects.
 *
 * @param <T>  a type variable
 * @param <PK> the primary key for that type
 * @author <a href="mailto:bwnoll@gmail.com">Bryan Noll</a>
 * @author jgarcia (update: added full text search + reindexing)
 */
public interface GenericDao<T, PK extends Serializable> {

    /**
     * Generic method used to get all objects of a particular type. This
     * is the same as lookup up all rows in a table.
     *
     * @return List of populated objects
     */
    List<T> getAll();

    /**
     * Gets all records without duplicates.
     * <p>Note that if you use this method, it is imperative that your model
     * classes correctly implement the hashcode/equals methods</p>
     *
     * @return List of populated objects
     */
    List<T> getAllDistinct();

    /**
     * Generic method to get an object based on class and identifier. An
     * ObjectRetrievalFailureException Runtime Exception is thrown if
     * nothing is found.
     *
     * @param id the identifier (primary key) of the object to get
     * @return a populated object
     * @see org.springframework.orm.ObjectRetrievalFailureException
     */
    T get(PK id);

    /**
     * Checks for existence of an object of type T using the id arg.
     *
     * @param id the id of the entity
     * @return - true if it exists, false if it doesn't
     */
    boolean exists(PK id);

    /**
     * Generic method to merge an object - handles both update and insert.
     *
     * @param object the object to save
     * @return the persisted object
     */
    T save(T object);

    /**
     * Generic method to save(only insert) an object - throws RuntimeException if record with same primary key exists.
     *
     * @param object the object to save
     * @return the persisted object
     */
    T insert(T object);

    /**
     * Generic method to delete an object based on class and id
     *
     * @param id the identifier (primary key) of the object to remove
     */
    void remove(PK id);

    /**
     * Generic method to delete an object
     *
     * @param object the object to remove
     */
    void remove(T object);

    /**
     * Find a list of records by using a named query
     *
     * @param queryName   query name of the named query
     * @param queryParams a map of the query names and the values
     * @return a list of the records found
     */
    List<T> findByNamedQuery(String queryName, Map<String, Object> queryParams);

    /**
     * Finds all the entities that matches the given example
     *
     * @param exampleInstance Example template
     * @return List of all entities that matches the given example template.
     */
    List<T> findByExample(final T exampleInstance);

    /**
     * Finds all the entities that matches the given example
     *
     * @param exampleInstance Example template
     * @param excludeProperty Properties that needs to be excluded for matching.
     * @return List of all entities that matches the given example template.
     */
    List<T> findByExample(final T exampleInstance, final String[] excludeProperty);

    /**
     * Finds all the entities that matches the given criteria
     *
     * @param detachedCriteria Properties that needs to be excluded for matching.
     * @return List of all entities that matches the given criteria
     */
    List<T> findByDetachedCriteria(final DetachedCriteria detachedCriteria);

    /**
     * Finds all the entities that matches the given criteria
     *
     * @param detachedCriteria Properties that needs to be excluded for matching.
     * @return List of all entities that matches the given criteria
     */
    List<T> findByDetachedCriteria(final DetachedCriteria detachedCriteria, final int firstResult, final int maxResults);

    /**
     * Flushes the current hibernate session objects to the database.
     */
    void flush();

    /**
     * Finds all the entities that matches the given example using hibernate template instead of sessionFactory
     *
     * @param exampleInstance Example template
     * @return List of all entities that matches the given example template.
     */
    public List<T> findByExampleHT(T exampleInstance);

    /**
     * Finds all the entities that matches the given example using hibernate template instead of sessionFactory
     *
     * @param exampleInstance Example template
     * @param firstResult First Result index. Use -1 to start from start
     * @param maxResults Maximum result
     * @return List of all entities that matches the given example template.
     */
    public List<T> findByExampleHT(T exampleInstance, int firstResult, int maxResults);

    Class<T> getPersistentClass();
}
