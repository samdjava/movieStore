package com.sam.dao.hibernate;

import com.sam.dao.GenericDao;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.*;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.orm.ObjectRetrievalFailureException;
import org.springframework.orm.hibernate5.HibernateCallback;
import org.springframework.orm.hibernate5.HibernateTemplate;
import org.springframework.orm.hibernate5.SessionFactoryUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * This class serves as the Base class for all other DAOs - namely to hold
 * common CRUD methods that they might all use. You should only need to extend
 * this class when your require custom CRUD logic.
 * <p/>
 * <p>To register this class in your Spring context file, use the following XML.
 * <pre>
 *      &lt;bean id="fooDao" class="com.sam.dao.hibernate.GenericDaoHibernate"&gt;
 *          &lt;constructor-arg value="com.sam.model.Foo"/&gt;
 *      &lt;/bean&gt;
 * </pre>
 *
 * @param <T>  a type variable
 * @param <PK> the primary key for that type
 * @author <a href="mailto:bwnoll@gmail.com">Bryan Noll</a>
 */
public class GenericDaoHibernate<T, PK extends Serializable> implements GenericDao<T, PK> {
    /**
     * Log variable for all child classes. Uses LogFactory.getLog(getClass()) from Commons Logging
     */
    protected final Log log = LogFactory.getLog(getClass());
    protected Class<T> persistentClass;
    private HibernateTemplate hibernateTemplate;
    private SessionFactory sessionFactory;

    /**
     * Constructor that takes in a class to see which type of entity to persist.
     * Use this constructor when subclassing.
     *
     * @param persistentClass the class type you'd like to persist
     */
    public GenericDaoHibernate(final Class<T> persistentClass) {
        this.persistentClass = persistentClass;
    }

    /**
     * Constructor that takes in a class and sessionFactory for easy creation of DAO.
     *
     * @param persistentClass the class type you'd like to persist
     * @param sessionFactory  the pre-configured Hibernate SessionFactory
     */
    public GenericDaoHibernate(final Class<T> persistentClass, SessionFactory sessionFactory) {
        this.persistentClass = persistentClass;
        this.sessionFactory = sessionFactory;
        this.hibernateTemplate = createHibernateTemplate(sessionFactory);
    }

    public HibernateTemplate getHibernateTemplate() {
        if (hibernateTemplate == null) {
            Assert.isTrue(sessionFactory != null);
            this.hibernateTemplate = createHibernateTemplate(sessionFactory);
        }
        return this.hibernateTemplate;
    }

    public SessionFactory getSessionFactory() {
        return this.sessionFactory;
    }

    public Session getSession() throws HibernateException {
        Session sess = getSessionFactory().getCurrentSession();
        if (sess == null) {
            sess = getSessionFactory().openSession();
        }
        return sess;
    }

    @Autowired(required = true)
    @Required
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        this.hibernateTemplate = createHibernateTemplate(sessionFactory);
    }

    @SuppressWarnings("unchecked")
    public List<T> getAll() {
        return hibernateTemplate.loadAll(this.persistentClass);
    }

    @SuppressWarnings("unchecked")
    public List<T> getAllDistinct() {
        return new ArrayList<T>(getAll());
    }

    @SuppressWarnings("unchecked")
    public T get(PK id) {
        T entity = (T) getHibernateTemplate().get(this.persistentClass, id);

        if (entity == null) {
            log.warn("Uh oh, '" + this.persistentClass + "' object with id '" + id + "' not found...");
            throw new ObjectRetrievalFailureException(this.persistentClass, id);
        }

        return entity;
    }

    @SuppressWarnings("unchecked")
    public boolean exists(final PK id) {
        final String identifierKey = sessionFactory.getClassMetadata(this.persistentClass).getIdentifierPropertyName();

        if(StringUtils.hasText(identifierKey)) {
            // Identifier Key may not be available in case the persistent class has multiple keys
            return getHibernateTemplate().executeWithNativeSession(new HibernateCallback<Boolean>() {
                
                public Boolean doInHibernate(Session session) throws HibernateException {
                    return session.createCriteria(persistentClass)
                            .add(Restrictions.eq(identifierKey, id))
                            .setProjection(Projections.id())
                            .uniqueResult() != null;
                }
            });
        }

        T entity = (T) getHibernateTemplate().get(this.persistentClass, id);
        return entity != null;
    }

    
    public T insert(T object) {
        return (T) executeUpdateOperation(object, Operation.SAVE);
    }

    @SuppressWarnings("unchecked")
    public T save(T object) {
        return (T) executeUpdateOperation(object, Operation.MERGE);
    }

    
    public void remove(PK id) {
        executeUpdateOperation(this.get(id), Operation.DELETE);
    }

    
    public void remove(T object) {
        executeUpdateOperation(object, Operation.DELETE);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    
    public List<T> findByNamedQuery(String queryName, Map<String, Object> queryParams) {
        String[] params = new String[queryParams.size()];
        Object[] values = new Object[queryParams.size()];

        int index = 0;
        for (String s : queryParams.keySet()) {
            params[index] = s;
            values[index++] = queryParams.get(s);
        }

        return (List<T>) getHibernateTemplate().findByNamedQueryAndNamedParam(queryName, params, values);
    }

    @SuppressWarnings("unchecked")
    public List<T> findByExample(final T exampleInstance) {
        return getHibernateTemplate().executeWithNativeSession(new HibernateCallback<List<T>>() {
            
            public List<T> doInHibernate(Session session) throws HibernateException {
                Criteria crit = session.createCriteria(persistentClass);
                Example example = Example.create(exampleInstance);
                crit.add(example);
                return crit.list();
            }
        });
    }

    @SuppressWarnings("unchecked")
    public List<T> findByExample(final T exampleInstance, final String[] excludeProperty) {
        return getHibernateTemplate().executeWithNativeSession(new HibernateCallback<List<T>>() {
            
            public List<T> doInHibernate(Session session) throws HibernateException {
                Criteria crit = session.createCriteria(persistentClass);
                Example example = Example.create(exampleInstance);
                for (String exclude : excludeProperty) {
                    example.excludeProperty(exclude);
                }
                crit.add(example);
                return crit.list();
            }
        });
    }

    @SuppressWarnings("unchecked")
    public List<T> findByDetachedCriteria(final DetachedCriteria detachedCriteria) {
        return new ArrayList<T>((Collection<? extends T>) getHibernateTemplate().findByCriteria(detachedCriteria));
    }

    @SuppressWarnings("unchecked")
    public List<T> findByDetachedCriteria(final DetachedCriteria detachedCriteria, final int firstResult, final int maxResults) {
        return new ArrayList<T>((Collection<? extends T>) getHibernateTemplate().findByCriteria(detachedCriteria, firstResult, maxResults));
    }

    @SuppressWarnings("unchecked")
    public List<T> findByExampleHT(T exampleInstance) {
        return getHibernateTemplate().findByExample(exampleInstance);
    }

    @SuppressWarnings("unchecked")
    public List<T> findByExampleHT(T exampleInstance, int firstResult, int maxResults) {
        return getHibernateTemplate().findByExample(exampleInstance, firstResult, maxResults);
    }

    
    public void flush() {
        this.getHibernateTemplate().flush();
    }

    public Class<T> getPersistentClass() {
        return persistentClass;
    }

    private HibernateTemplate createHibernateTemplate(SessionFactory sessionFactory){
        return new HibernateTemplate(sessionFactory);
    }

    private Object executeUpdateOperation(Object object, Operation operation){
        Object returnObj;
        Session session = null;
        boolean isNew = false;
        try {
            session = this.getSessionFactory().getCurrentSession();
        } catch (HibernateException var13) {
            log.debug("Could not retrieve pre-bound Hibernate session", var13);
        }

        if(session == null) {
            session = this.getSessionFactory().openSession();
            session.setFlushMode(FlushMode.MANUAL);
            isNew = true;
        }

        try {
            if(operation == Operation.MERGE){
                returnObj = session.merge(object);
            }else if(operation == Operation.SAVE){
                returnObj = session.save(object);
            }else if (operation == Operation.DELETE){
                session.delete(object);
                returnObj = Void.TYPE;
            }else {
                throw new RuntimeException("Unkown operation:"+operation);
            }

            if(isNew){
                session.flush();
            }
        } catch (Exception e) {
            log.warn(String.format("Unable to perform: %s on entity: %s, Error: %s ", operation, object, e.getMessage()));
            throw e;
        } finally {
            if(isNew) {
                SessionFactoryUtils.closeSession(session);
            }
        }
        return returnObj;
    }

    private static enum Operation{
        MERGE, SAVE, DELETE
    }
}
