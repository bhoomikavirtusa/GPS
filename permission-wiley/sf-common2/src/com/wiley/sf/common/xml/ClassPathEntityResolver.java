package com.wiley.sf.common.xml;

import java.io.InputStream;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import com.wiley.sf.common.lang.ArgUtil;

/**
 * Entity Resolver that can do one of two things:
 * 1) Ignore system and/or public id specified in XML and forces validation
 * from a specified source in the classpath.
 * 2) Look at system id and map to a source in the classpath.
 *
 * @since   JDK 1.5, Xerces 2.9.0
 * @version 12/22/2007
 * @author  Steve Markoff
 */
public class ClassPathEntityResolver implements EntityResolver {

    private final String classpath;

    /**
     * classpath should be of the form "/package/classname" or "/package/".
     * (sub-packages can used also: ex. "/package/subpackage/classname")
     * If a classname (really the name of a DTD or XML Schema file)is
     * specified then this will always be used. If just a package is specified
     * then this package will be used to map system id's to files.
     *
     * @param classpath  Must be non-null and non-blank
     */
    public ClassPathEntityResolver(String classpath) {
        this.classpath = classpath;
        ArgUtil.notBlank(classpath, "classpath");
    }

    /**
     * Implements EntityResolver interface.
     */
    public InputSource resolveEntity(String publicId, String systemId) {
        String fullpath = classpath;
        if (classpath.endsWith("/")) {
            int index = systemId.lastIndexOf('/');
            if (index == -1)  fullpath = classpath + systemId;
            else  fullpath = classpath + systemId.substring(index + 1);
        }

        //System.out.println("resolveEntity(): publicId [" + publicId
        //    + "] systemId [" + systemId + "] fullpath [" + fullpath + "]");
        InputStream validateStream = XMLUtil.class.getResourceAsStream(fullpath);
        if (validateStream == null) {
            throw new RuntimeException("Resource for \"" + fullpath + "\" not found.");
        }
        return new InputSource(validateStream);
    }
}
