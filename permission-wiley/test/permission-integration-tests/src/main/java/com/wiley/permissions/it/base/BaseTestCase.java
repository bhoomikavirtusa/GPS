package com.wiley.permissions.it.base;

//import org.springframework.test.AbstractDependencyInjectionSpringContextTests;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 *
 * @author sputta
 */
public class BaseTestCase extends AbstractJUnit4SpringContextTests  {

   /**
    *
    */
   // @Override
    protected String[] getConfigLocations() {
        return new String[] { "classpath*:conf/spring/*-TEST.xml"};
    }

}
