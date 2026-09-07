package com.wiley.permissions.common.transformer;

public class NamespacePrefixMapper extends com.sun.xml.bind.marshaller.NamespacePrefixMapper { 

	public static final String NAMESPACE = "http://schemas.wiley.org/permission";

	public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) { 
      
         if (namespaceUri.equals(NAMESPACE)) { 
                 return "msg"; 
         } 
          
         return suggestion; 
     } 
}