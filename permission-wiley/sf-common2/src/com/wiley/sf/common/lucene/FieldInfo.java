package com.wiley.sf.common.lucene;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.wiley.sf.common.lang.ArgUtil;

/**
 * Holder for field information from an index.
 * See IndexReadManager.readIndexedFieldInfo() and readStoredFieldInfo().
 *
 * @since  JDK 1.6, Lucene 4.7
 * @author smarkoff
 */
public class FieldInfo {
    private final String name;
    private int count = 0;
    private final List<String> values = new ArrayList<String>();

    /**
     * @param name  Must be non-blank
     */
    public FieldInfo(String name) {
        ArgUtil.notBlank(name, "name");
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void increaseCount() {
        count++;
    }

    public int getCount() {
        return count;
    }

    public List<String> getValues() {
        return values;
    }

    public String getValuesString() {
        StringBuilder sb = new StringBuilder("[");
        sb.append(StringUtils.join(values, ", "));
        if (count > values.size()) {
            sb.append(", ...");
        }
        sb.append("]");
        return sb.toString();
    }
}