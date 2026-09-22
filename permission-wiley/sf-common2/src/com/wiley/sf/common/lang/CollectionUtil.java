package com.wiley.sf.common.lang;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides methods to deal with Collections that are provided by the
 * Java JDK or the Apache Commons-Collections library.
 *
 * @since    JDK 1.6
 * @version  $Id: CollectionUtil.java,v 1.1 2012-07-19 20:02:12 smarkoff Exp $
 * @author   Steve Markoff
 */
public class CollectionUtil {

   /**
    *
    * @param list  Must be non-null
    * @param targetSize  Must be at least 1
    */
   public static <T extends Object> List<List<T>> split(List<T> list, int targetSize) {
       ArgUtil.notLess1(targetSize, "targetSize");

       List<List<T>> lists = new ArrayList<List<T>>();
       for (int i = 0; i < list.size(); i += targetSize) {
           lists.add(list.subList(i, Math.min(i + targetSize, list.size())));
       }
       return lists;
   }

   /**
    * There is no reason to ever create an instance of this class
    * since all methods are static.
    */
   private CollectionUtil() { }
}
