package com.sam.util;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import com.thoughtworks.xstream.mapper.CannotResolveClassException;
import com.thoughtworks.xstream.mapper.MapperWrapper;
import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by root on 27/8/17.
 */
public class DomainUtil {
    private static final Logger LOG = LoggerFactory.getLogger(DomainUtil.class);

    public static final Random RANDOM = new Random(System.currentTimeMillis());
    public static final XStream XSTREAM = new GracefulXStream();
    public static final XStream JSON_XSTREAM = new XStream(new JettisonMappedXmlDriver());
    protected static final Properties configProperties = new Properties();
    public static final String STAGE;
    public static final String REALM;
    public static final String MACHINE;
    public static final String SERVICE_ID;
    public static final String BB_SECTOR_LIST_CONFIG = "bbSectorList";

    private static final Boolean NDNC_RESTRICTION_ENABLED = Boolean.valueOf(getConfig("ndncRestrictionEnabled", "false"));
    private static final Boolean NDNC_TIME_RESTRICTION_ENABLED = Boolean.valueOf(getConfig("ndncTimeRestrictionEnabled", "false"));
    private static final Integer NDNC_RESTRICTION_START_HOUR = Integer.valueOf(getConfig("ndncRestrictionStartHour", "21"));
    private static final Integer NDNC_RESTRICTION_END_HOUR = Integer.valueOf(getConfig("ndncRestrictionEndHour", "9"));

    protected DomainUtil() {
//        Default Constructor Added Deliberately. No Logic here
    }

    private static final String DUMMY = "dummy";

    static {
        JSON_XSTREAM.setMode(XStream.NO_REFERENCES);
    }

    static {
        STAGE = System.getProperty("stage", "dev");
        REALM = System.getProperty("realm", DUMMY);
        MACHINE = System.getProperty("machine", DUMMY);
        SERVICE_ID = "dummy";//ConfigUtil.getConfig("serviceId");
    }
    /**
     * This is an implementation of the {@link XStream} which will not bomb upon finding any unknown element in any particular class.
     * Reference : http://jira.codehaus.org/browse/XSTR-30
     */
    private static class GracefulXStream extends XStream {

        private final Map<String, List<String>> _FIELD_NAME_MAP;

        private GracefulXStream() {
            _FIELD_NAME_MAP = new ConcurrentHashMap<String, List<String>>();
        }

        @Override
        protected MapperWrapper wrapMapper(MapperWrapper next) {

            return new MapperWrapper(next) {
                @Override
                public boolean shouldSerializeMember(Class definedIn, String fieldName) {

                    try {
                        Class studiedClass = definedIn;
                        while (studiedClass != null) {

                            final String className = studiedClass.getName();

                            if(_FIELD_NAME_MAP.get(className) == null) {
                                final List<String> names = collect(Arrays.asList(studiedClass.getDeclaredFields()), "name");
                                _FIELD_NAME_MAP.put(className, names);
                            }

                            final List<String> fieldNames = _FIELD_NAME_MAP.get(className);
                            if (fieldNames.contains(fieldName)) {
                                try {
                                    return studiedClass.getDeclaredField(fieldName).getAnnotation(XStreamOmitField.class) == null && super.shouldSerializeMember(definedIn, fieldName) && (definedIn != Object.class || realClass(fieldName) != null);
                                } catch (CannotResolveClassException e) {
                                    LOG.warn("[Graceful XSTREAM] - Unknown element handled!!", e);
                                    return false;
                                }
                            } else {
                                studiedClass = studiedClass.getSuperclass();
                            }
                        }
                        return false;
                    } catch (NoSuchFieldException e) {
                        LOG.error("[Graceful XSTREAM] - NoSuchFieldException Error Encountered ", e);
                        return true;
                    }
                }
            }

                    ;
        }
    }

    public static <T> T clone(T object) {
        //noinspection unchecked
        return object != null ? (T) XSTREAM.fromXML(XSTREAM.toXML(object)) : null;
    }

    public static Boolean variantOptionPresent(String variantOptions, String variantOption) {
        return variantOption != null && variantOptions != null && Arrays.asList(variantOptions.split(",")).contains(variantOption);
    }

    /**
     * Collects the property in the collection.
     * NOTE: This method should never return null object.
     * @param col {@link List} of {@Object}s from which the property has to be collected.
     * @param property The name of the property.
     * @param <T>
     * @param <V>
     * @return The collected property {@link List}.
     */
    public static <T, V> List<V> collect(List<T> col, String property) {

        if (col == null || col.isEmpty() || col.get(0) == null) {
            return Collections.emptyList();
        }

        final List<V> result = new ArrayList<V>(col.size());
        for (T item : col) {
            try {
                //noinspection unchecked
                if(PropertyUtils.isReadable(item, property)) {
                    result.add((V) PropertyUtils.getProperty(item, property));
                }
            } catch (Exception e) {
                LOG.error("Error while collecting the property from list. Ignoring this error and proceeding", e);
                // eat it
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static <T, V> Set<V> collectAsSet(List<T> col, String property) {

        if (col == null || col.isEmpty() || col.get(0) == null) {
            return Collections.emptySet();
        }

        final Set<V> result = new HashSet<V>(col.size());
        for (T item : col) {
            try {
                if(PropertyUtils.isReadable(item, property)) {
                    result.add((V) PropertyUtils.getProperty(item, property));
                }
                //noinspection unchecked
            } catch (Exception e) {
                LOG.debug("Error while collecting the property from list. Ignoring this error and proceeding", e);
                // eat it
            }
        }
        return Collections.unmodifiableSet(result);
    }

    public static boolean isEquals(Object a, Object b) {
        return (a == null) ? (b == null) : a.equals(b);
    }

    public static String getFormattedCalendarDate(Calendar calendar) {
        SimpleDateFormat format1 = new SimpleDateFormat("dd/MM/yyyy");
        if (calendar == null) {
            return "";
        } else {
            return format1.format(calendar.getTime());
        }
    }

    public static List<String> commaDelimitedListToList(String str) {
        List<String> list = new ArrayList<String>();
        Collections.addAll(list, StringUtils.commaDelimitedListToStringArray(str));
        return list;
    }

    public static String getConfig(final String key) {
        return key;
        //return ConfigUtil.getConfig(key);
    }

    public static String getConfig(final String key, final String defaultValue) {
        String result = getConfig(key);
        return result != null ? result : defaultValue;
    }



    public static String getServiceId() {
        return SERVICE_ID;
    }

    public static boolean isProd() {
        return "prod".equals(System.getProperty("stage", "dev"));
    }

    public static Collection<String> getBBSectorList() {
        return StringUtils.commaDelimitedListToSet(getConfig(BB_SECTOR_LIST_CONFIG, "INDIA"));
    }

    public static Boolean isNDNCRestrictionEnabled() {
        return NDNC_RESTRICTION_ENABLED;
    }

    public static Boolean isNDNCTimeRestrictionEnabled() {
        return NDNC_TIME_RESTRICTION_ENABLED;
    }

    public static Boolean isNDNCRestrictedByTime() {
        // NDNC is restricted between 2100 hrs to 0900 hrs IST by default.
        final int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return isNDNCTimeRestrictionEnabled() && (hour >= NDNC_RESTRICTION_START_HOUR || hour < NDNC_RESTRICTION_END_HOUR);
    }
}
