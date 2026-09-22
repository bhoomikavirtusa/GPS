package com.wiley.sf.common.xml.bind;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import com.wiley.sf.common.lang.ArgUtil;

/**
 * XmlAdapter for use with JAXB.
 * The strings "true", "yes", and "1" are treated as true (case-insensitive).
 * All other strings (including null) are treated as false.
 * For the reverse conversion the default is to convert true to "true"
 * but this can be overwritten using the static method setDefaultBooleanType
 * and/or the non-empty constructor.
 *
 * @since  JDK 1.6
 * @author smarkoff
 */
public class BooleanXmlAdapter extends XmlAdapter<String, Boolean> {

    public enum BooleanType { TRUE_FALSE, YES_NO, ONE_ZERO };

    private static BooleanType defaultBooleanType = BooleanType.TRUE_FALSE;

    /**
     * To be called by a Spring config, InitServlet,
     * or some other initialization mechanism.
     *
     * @param booleanType  Must be non-null
     */
    public static void setDefaultBooleanType(BooleanType booleanType) {
        ArgUtil.notNull(booleanType, "booleanType");
        BooleanXmlAdapter.defaultBooleanType = booleanType;
    }

    private final BooleanType booleanType;

    /**
     * Create an instance that uses the defaultBooleanType.
     */
    public BooleanXmlAdapter() {
        booleanType = defaultBooleanType;
    }

    /**
     * Create an instance that may be different from the default
     * BooleanType.
     *
     * @param booleanType  Must be non-null
     */
    public BooleanXmlAdapter(BooleanType booleanType) {
        ArgUtil.notNull(booleanType, "booleanType");
        this.booleanType = booleanType;
    }

    @Override
    public Boolean unmarshal(String b) throws Exception {
        return "true".equalsIgnoreCase(b) || "yes".equalsIgnoreCase(b) || "1".equalsIgnoreCase(b);
    }

    @Override
    public String marshal(Boolean b) throws Exception {
        if (booleanType == BooleanType.TRUE_FALSE) {
            return b == null ? "false" : (b ? "true" : "false");
        }
        else if (booleanType == BooleanType.YES_NO) {
            return b == null ? "no" : (b ? "yes" : "no");
        }
        else {  // ONE_ZERO
            return b == null ? "0" : (b ? "1" : "0");
        }
    }
}
