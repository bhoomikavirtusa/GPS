package com.wiley.sf.common.xml.bind;

import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.xml.sax.InputSource;

import com.sun.xml.bind.IDResolver;

/**
 *
 * @since  JDK 1.6, JUnit 4.10
 * @author smarkoff
 */
public class CustomIDResolverTest {

    public static void main(String [] args) throws JAXBException {
        LogFactory.getFactory().setAttribute(
            "org.apache.commons.logging.Log",
            "org.apache.commons.logging.impl.Log4JLogger");
        /*
        Logger rootLogger = Logger.getRootLogger();
        rootLogger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout(PatternLayout.TTCC_CONVERSION_PATTERN);
        rootLogger.addAppender(new ConsoleAppender(layout));
        */

        CustomIDResolverTest test = new CustomIDResolverTest();
        test.test1();
        test.test2();
    }

    public static String objectToXml(Object o) throws JAXBException {
        StringWriter writer = new StringWriter();
        JAXBContext context = JAXBContext.newInstance(o.getClass());
        // throws JAXBException
        Marshaller m = context.createMarshaller();
            // throws JAXBException
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            // throws PropertyException
        m.marshal(o, writer);
            // throws JAXBException

        return writer.toString();
    }

    public static Object xmlToObject(String xml, Class<?> clazz) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(clazz);
            // throws JAXBException
        Unmarshaller unmarshaller = context.createUnmarshaller();
            // throws JAXBException

        unmarshaller.setProperty(IDResolver.class.getName(), new CustomIDResolver());
            // throws JAXBException

        Object o = unmarshaller.unmarshal(new InputSource(new StringReader(xml)));
            // throws JAXBException
        return o;
    }

    @Test
    public void test1() throws JAXBException {
        BusinessUnit bu4 = new BusinessUnit("4", "Business Unit 4");
        BusinessUnit bu5 = new BusinessUnit("5", "Business Unit 5");
        BusinessUnit bu6 = new BusinessUnit("6", "Business Unit 6");

        DataSource ds4 = new DataSource("4", "Data Source 4");
        DataSource ds5 = new DataSource("5", "Data Source 5");
        DataSource ds6 = new DataSource("6", "Data Source 6");
        DataSource ds7 = new DataSource("7", "Data Source 7");

        ProductLine pl1 = new ProductLine("pl1", "Product Line 1");
        pl1.setBusinessUnit(bu4);
        pl1.setDataSource(ds4);

        ProductLine pl2 = new ProductLine("pl2", "Product Line 2");
        pl2.setBusinessUnit(bu5);
        pl2.setDataSource(ds7);

        ProductLine pl3 = new ProductLine("pl3", "Product Line 3");
        pl3.setBusinessUnit(bu4);
        pl3.setDataSource(ds6);

        List<ProductLine> list = new ArrayList<ProductLine>();
        list.add(pl1);
        list.add(pl2);
        list.add(pl3);

        List<BusinessUnit> buList = new ArrayList<BusinessUnit>();
        buList.add(bu4);
        buList.add(bu5);
        buList.add(bu6);

        List<DataSource> dsList = new ArrayList<DataSource>();
        dsList.add(ds4);
        //dsList.add(ds7);
        dsList.add(ds6);

        MetaData md = new MetaData();
        md.setProductLineList(list);
        md.setBusinessUnitList(buList);
        md.setDataSourceList(dsList);


        String xml = objectToXml(md);
            // throws JAXBException
        System.out.println("---- xml ----");
        System.out.println(xml);

        md = (MetaData) xmlToObject(xml, MetaData.class);  // throws JAXBException

        System.out.println("---- object.toString() ----");
        System.out.println(md);

        xml = objectToXml(md);  // throws JAXBException

        System.out.println("---- xml again ----");
        System.out.println(xml);
    }

    @Test
    public void test2() throws JAXBException {
        System.out.println("--------------------- test 2 --------------------");

        BusinessUnit bu = new BusinessUnit("3", "Business Unit 3");
        ProductLine productLine = new ProductLine("3", "Product Line 3");
        productLine.setBusinessUnit(bu);
        Product product = new Product();
        product.setBusinessUnit(bu);
        product.setProductLine(productLine);
        Message msg = new Message();
        msg.setProduct(product);
        msg.getMetaData().add(bu);
        msg.getMetaData().add(productLine);

        System.out.println("bu hashCode = " + bu.hashCode());
        System.out.println("pl hashCode = " + productLine.hashCode());


        String xml = objectToXml(msg);
                    // throws JAXBException
        System.out.println("---- xml ----");
        System.out.println(xml);

        msg = (Message) xmlToObject(xml, Message.class);  // throws JAXBException

        System.out.println("---- object.toString() ----");
        System.out.println(msg);

        xml = objectToXml(msg);  // throws JAXBException

        System.out.println("---- xml again ----");
        System.out.println(xml);
    }

    @Test
    public void setXmlID() {
        BeanWithXmlID bean = new BeanWithXmlID();
        CustomIDResolver.setXmlID(bean, "foo");
        String msg = "Expected code to be 'foo' but was [" + bean.getCode() + "].";
        assertTrue(msg, StringUtils.equals(bean.getCode(), "foo"));

        BeanWithXmlID2 bean2 = new BeanWithXmlID2();
        CustomIDResolver.setXmlID(bean2, "foo");
        msg = "Expected code to be 'foo' but was [" + bean2.getCode() + "].";
        assertTrue(msg, StringUtils.equals(bean2.getCode(), "foo"));
    }
}

class BeanWithXmlID {
    private int id;
    private String code;
    private String name;

    public int getId() { return id; }
    public void setId(int i) { this.id = i; }

    @XmlID
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

class BeanWithXmlID2 {
    private int id;
    @XmlID
    private String code;
    private String name;

    public int getId() { return id; }
    public void setId(int i) { this.id = i; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
class Message {
    @XmlElement
    private Product product;

    @XmlElementWrapper(name="metadata")
    @XmlElements(
        {
            @XmlElement(name="businessUnit", type=BusinessUnit.class),
            @XmlElement(name="productLine", type=ProductLine.class),
        }
    )
    private Set<Object> metaData = new HashSet<Object>();

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Set<Object> getMetaData() {
        return metaData;
    }

    public void setMetaData(Set<Object> metaData) {
        this.metaData = metaData;
    }

    @Override
    public String toString() {
        return "product = \r\n" + product + ", metaData = \r\n" + metaData;
    }
}

@XmlAccessorType(XmlAccessType.NONE)
class Product {

    @XmlElement
    @XmlIDREF
    private BusinessUnit businessUnit;

    @XmlElement
    @XmlIDREF
    private ProductLine productLine;


    public BusinessUnit getBusinessUnit() {
        return businessUnit;
    }

    public void setBusinessUnit(BusinessUnit bu) {
        this.businessUnit = bu;
    }

    public ProductLine getProductLine() {
        return productLine;
    }

    public void setProductLine(ProductLine pl) {
        this.productLine = pl;
    }

    @Override
    public String toString() {
        return "productLine = " + productLine + ",\r\nbusinessUnit = " + businessUnit;
    }
}

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
class MetaData {

    @XmlElementWrapper(name = "businessUnitList")
    @XmlElement(name = "businessUnit")
    private List<BusinessUnit> businessUnitList;

    @XmlElementWrapper(name = "productLineList")
    @XmlElement(name = "productLine")
    private List<ProductLine> productLineList;

    @XmlElementWrapper(name = "dataSourceList")
    @XmlElement(name = "dataSource")
    private List<DataSource> dataSourceList;

    public List<BusinessUnit> getBusinessUnitList() {
        return businessUnitList;
    }

    public void setBusinessUnitList(List<BusinessUnit> list) {
        this.businessUnitList = list;
    }

    public List<ProductLine> getProductLineList() {
        return productLineList;
    }

    public void setProductLineList(List<ProductLine> list) {
        this.productLineList = list;
    }

    public List<DataSource> getDataSourceList() {
        return dataSourceList;
    }

    public void setDataSourceList(List<DataSource> list) {
        this.dataSourceList = list;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (productLineList == null) {
            sb.append("MetaData: productLineList is null\r\n");
        }
        else {
            sb.append("MetaData: productLineList.size() = " + productLineList.size());
            sb.append("\r\n");
            int i = 1;
            for (ProductLine pl : productLineList) {
                sb.append("ProductLine " + i + ": " + pl);
                sb.append("\r\n");
                i++;
            }
        }

        if (businessUnitList == null) {
            sb.append("MetaData: businessUnitList is null\r\n");
        }
        else {
            sb.append("MetaData: businessUnitList.size() = " + businessUnitList.size());
            sb.append("\r\n");
            int i = 1;
            for (BusinessUnit bu : businessUnitList) {
                sb.append("BusinessUnit " + i + ": " + bu);
                sb.append("\r\n");
                i++;
            }
        }

        if (dataSourceList == null) {
            sb.append("MetaData: dataSourceList is null\r\n");
        }
        else {
            sb.append("MetaData: dataSourceList.size() = " + dataSourceList.size());
            sb.append("\r\n");
            int i = 1;
            for (DataSource ds : dataSourceList) {
                sb.append("DataSource " + i + ": " + ds);
                sb.append("\r\n");
                i++;
            }
        }

        return sb.toString();
    }
}

@XmlAccessorType(XmlAccessType.NONE)
class ProductLine {

    @XmlID
    @XmlAttribute
    private String code;

    @XmlElement
    private String name;

    @XmlElement
    @XmlIDREF
    private BusinessUnit businessUnit;

    @XmlElement
    @XmlIDREF
    private DataSource dataSource;


    public ProductLine() { }

    public ProductLine(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BusinessUnit getBusinessUnit() {
        return businessUnit;
    }

    public void setBusinessUnit(BusinessUnit bu) {
        this.businessUnit = bu;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource ds) {
        this.dataSource = ds;
    }

    @Override
    public String toString() {
        return "code = " + code
            + ", name = " + name
            + ", businessUnit = " + businessUnit
            + ", dataSource = " + dataSource;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((code == null) ? 0 : code.hashCode());

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        // Important to use "instance of"
        //if (getClass() != obj.getClass()) return false;
        if (!(obj instanceof ProductLine)) return false;
        ProductLine other = (ProductLine) obj;
        if (code == null) {
            if (other.code != null) return false;
        }
        else if (!code.equals(other.code)) return false;

        return true;
    }
}

@XmlAccessorType(XmlAccessType.NONE)
class BusinessUnit {
    @XmlID
    private String code;

    @XmlElement
    private String name;

    public BusinessUnit() { }

    public BusinessUnit(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() { return code; }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() { return name; }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "code = " + code
        + ", name = " + name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((code == null) ? 0 : code.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        //if (getClass() != obj.getClass()) return false;
        if (!(obj instanceof BusinessUnit)) return false;
        BusinessUnit other = (BusinessUnit) obj;
        if (code == null) {
            if (other.code != null) return false;
        }
        else if (!code.equals(other.code)) return false;
        return true;
    }
}

@XmlAccessorType(XmlAccessType.NONE)
class DataSource {
    @XmlID
    private String code;

    @XmlElement
    private String name;

    public DataSource() { }

    public DataSource(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() { return code; }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() { return name; }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "code = " + code
        + ", name = " + name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((code == null) ? 0 : code.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        //if (getClass() != obj.getClass()) return false;
        if (!(obj instanceof DataSource)) return false;
        DataSource other = (DataSource) obj;
        if (code == null) {
            if (other.code != null) return false;
        }
        else if (!code.equals(other.code)) return false;
        return true;
    }
}
