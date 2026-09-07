<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : pe-to-permissions.xsl
    Created on : May 19, 2008, 2:02 PM
    Author     : ttidwell
    Description:
        Purpose of transformation follows.
        This transformer is used to process the REPLY messages received from PE 
        (like the result of  a request placed by permissions)
        
        EXAMPLE (will transform something like this into internal message):
        
<message xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="sysmsg_20100901.xsd" id="PE001.957612" type="reply" replyID="PERM.2852384327.1300384608631.3" sent="1300384615006" source="PE001" protocolVersion="1.0">
   <target>
      <system>PERM001</system>
   </target>
   <reply>
      <productUpdate>
         <productData>
            <product wid="CORE.001.PROD.0000056478" businessUnit="2" isbn10="0764572970" isbn13="9780764572975" pnumber="000263966" volume="0" status="N" dataSource="US" updateTime="1299878225565" completeRecordSet="yes">
               <title>&quot;Please Oh Please Can We Get a Dog?&quot;: Parents' Guide to Dog Ownership</title>
               <shortTitle>Please Oh Please Can We Get a Dog?</shortTitle>
               <byline>Cheryl Peterson</byline>
               <copyrightYear>2005</copyrightYear>
               <sku>0764572970</sku>
               <imprint>HW</imprint>
               <photoResearchRequired>no</photoResearchRequired>
               <publisher>WILEY</publisher>
               <audience>TRA</audience>
               <productionDate>2004-08-24</productionDate>
               <printDate>2005-03-09</printDate>
               <targetMedium>P</targetMedium>
               <trimSize>015</trimSize>
               <productLine>TH</productLine>
               <shortAuthorName>George</shortAuthorName>
			</product>
		</productUpdate>
	</reply>
</message>

Example error message:

<?xml version="1.0" encoding="UTF-8"?>
<message xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="sysmsg_20120402.xsd" id="PE001.1894630" type="reply" replyID="PERM.3434673657.1335311101795.1" sent="1335311117110" source="PE001" protocolVersion="1.0">
   <target>
      <system>PERMSM</system>
   </target>
   <reply>
      <productUpdate>
         <productData>
            <productError code="1004" queryType="wid" queryValue="CORE.002.PROD.0000002439">Unable to locate the requested product.</productError>
         </productData>
      </productUpdate>
   </reply>
</message>     
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="xml" indent="yes" standalone="yes"/>
	
	<xsl:strip-space elements="*"/>

	<xsl:variable name="messageType" select="//message/@type"/>

	<xsl:template match="message">
		<message direction="INCOMING">
			<xsl:for-each select="@*">
			<!--  TODO: convert PE replyID to Permissions replyId (smarkoff) -->
				<xsl:copy/>
			</xsl:for-each>

			<xsl:apply-templates/>
		</message>
	</xsl:template>
	
	<xsl:template match="target">
		<targets>
			<xsl:apply-templates/>
		</targets>
	</xsl:template>
	
	<xsl:template match="system">
		<target><xsl:value-of select="."/></target>
	</xsl:template>
	
	<xsl:template match="reply">
		<xsl:apply-templates/>
	</xsl:template>

	<xsl:template match="masterListUpdate">
		<operations>
			<operation operationType="masterListUpdate">
				<parameters>
					<xsl:element name="parameter">
						<xsl:attribute name="name">name</xsl:attribute>
						<xsl:attribute name="value">
							<xsl:value-of select="masterListObject/@name"/>
						</xsl:attribute>
					</xsl:element>
					<xsl:element name="parameter">
						<xsl:attribute name="name">dataSource</xsl:attribute>
						<xsl:attribute name="value">
							<xsl:value-of select="masterListObject/@dataSource"/>
						</xsl:attribute>
					</xsl:element>
					<xsl:element name="parameter">
						<xsl:attribute name="name">updateTime</xsl:attribute>
						<xsl:attribute name="value">
							<xsl:value-of select="masterListObject/@updateTime"/>
						</xsl:attribute>
					</xsl:element>
					<!-- We don't need these last two but might as well copy them over. -->
					<xsl:element name="parameter">
						<xsl:attribute name="name">completeRecordSet</xsl:attribute>
						<xsl:attribute name="value">
							<xsl:value-of select="masterListObject/@completeRecordSet"/>
						</xsl:attribute>
					</xsl:element>
					<xsl:element name="parameter">
						<xsl:attribute name="name">recordCount</xsl:attribute>
						<xsl:attribute name="value">
							<xsl:value-of select="masterListObject/@recordCount"/>
						</xsl:attribute>
					</xsl:element>
				</parameters>
			</operation>
		</operations>
		<xsl:call-template name="masterListUpdate">
			<xsl:with-param name="masterListUpdate" select="//masterListUpdate" />
		</xsl:call-template>
	</xsl:template>

	<xsl:template match="productUpdate">
		<operations>
			<operation operationType="productUpdate">
				<items>
					<xsl:apply-templates/>
				</items>
			</operation>
		</operations>
		<xsl:call-template name="masterListUpdate">
			<xsl:with-param name="masterListUpdate" select="//masterListUpdate" />
		</xsl:call-template>
	</xsl:template>
	
	<xsl:template match="productData">
		<xsl:apply-templates select="product" />
		<xsl:apply-templates select="productError" />
	</xsl:template>
	
	<!--  smarkoff: This is different than the CMS error format, and maybe needs to be so we don't lose track of the fact
	that this is a product reply message (CMS errors are at the operation level).
	-->
	<xsl:template match="productError">
		<productError>
			<code><xsl:value-of select="@code" /></code>
			<text><xsl:value-of select="." /></text>
			<queryType><xsl:value-of select="@queryType" /></queryType>
			<queryValue><xsl:value-of select="@queryValue" /></queryValue>
		</productError>
	</xsl:template>
	
	<!-- this template is used by productUpdate - full product data (a Reply message to a request or notification) -->
	<xsl:template match="product">
		<xsl:variable name="productId" select="@wid"/>
		<product>
			<!--  required attributes -->
			<externalId><xsl:value-of select="@wid" /></externalId>
			<businessUnit><xsl:value-of select="@businessUnit" /></businessUnit>
			<dataSource><xsl:value-of select="@dataSource" /></dataSource>
			<lastUpdatedDate><xsl:value-of select="@updateTime" /></lastUpdatedDate>
			<completeRecordSet><xsl:value-of select="@completeRecordSet" /></completeRecordSet>
			
			<!-- optional attributes -->
			<xsl:for-each select="@*">
				<xsl:choose>
					<xsl:when test="name() = 'isbn10'">
						<isbn10><xsl:value-of select="."/></isbn10>
					</xsl:when>
					
					<xsl:when test="name() = 'isbn13'">
						<isbn13><xsl:value-of select="."/></isbn13>
					</xsl:when>
											
					<xsl:when test="name() = 'pnumber'">
						<pnumber><xsl:value-of select="."/></pnumber>
					</xsl:when>
					
					<xsl:when test="name() = 'volume'">
						<volume><xsl:value-of select="."/></volume>
					</xsl:when>
					
					<xsl:when test="name() = 'status'">
						<publicationStatus>
							<code><xsl:value-of select="."/></code>
						</publicationStatus>
					</xsl:when>
					
					<xsl:when test="name() = 'ppc'">
						<processCode><xsl:value-of select="."/></processCode>
					</xsl:when>
					
					<xsl:when test="name() = 'editionWid'">
						<edition><xsl:value-of select="."/></edition>
					</xsl:when>
				</xsl:choose>
			</xsl:for-each>
			
			<!-- note using copy-of is fine for optional elements -->
			<xsl:copy-of select="commonWork" />
			
			<xsl:copy-of select="sku" />
			<xsl:copy-of select="title" />
			<xsl:copy-of select="shortTitle" />
			<xsl:copy-of select="shortAuthorName" />
			<xsl:copy-of select="languageSpoken" />
			<xsl:copy-of select="copyrightYear" />
			<xsl:copy-of select="editionNumber" />
			<xsl:copy-of select="previousEditionWID" />
			<xsl:copy-of select="nextEditionWID" />

			<xsl:apply-templates select="productionDate"/>
			<xsl:apply-templates select="printDate"/>
			<xsl:apply-templates select="consolidatedReleaseDate"/>
			<xsl:apply-templates select="productionEndDate" />

			<xsl:copy-of select="productFamily"/>
			<xsl:copy-of select="productType"/>
			<xsl:copy-of select="productLine"/>
			
			<xsl:apply-templates select="subjectCode1"/>
			
			<xsl:copy-of select="editor"/>
			<xsl:copy-of select="geographicalLocation"/>
			<xsl:copy-of select="targetMedium"/>
			<xsl:copy-of select="subMedium"/>
			<xsl:copy-of select="discountGroupCode" />
			<xsl:copy-of select="discountSubGroupCode" />
			<xsl:copy-of select="componentFlag" />
			
			<xsl:apply-templates select="printings"/>
		
			<xsl:copy-of select="photoIllusTotalCount"/>
			<xsl:copy-of select="productPriority"/>
				
			<userToProducts>
				<xsl:for-each select="author">
					<userToProduct>
						<user><xsl:value-of select="@code"/></user>
						<product><xsl:value-of select="$productId"/></product>
						<role>
							<code><xsl:value-of select="role" /></code>
							<roleType>AUTHOR</roleType>
						</role>
					</userToProduct>
				</xsl:for-each>

				<xsl:for-each select="employee">
					<userToProduct>
						<user><xsl:value-of select="@code"/></user>
						<product><xsl:value-of select="$productId"/></product>
						<role>
							<code><xsl:value-of select="role" /></code>
							<roleType>EMPLOYEE</roleType>
						</role>
					</userToProduct>
				</xsl:for-each>
			</userToProducts>
			
			<bundles>
				<xsl:for-each select="bundleCode">
					<bundle><xsl:value-of select="." /></bundle>
				</xsl:for-each>
			</bundles>
			
			<!-- titleRelations is actually 0 or 1 (not many) -->
			<xsl:for-each select="titleRelations">
				<relations>
					<xsl:for-each select="relation">
						<relation>
							<code><xsl:value-of select="@code" /></code>
							<relatedWid><xsl:value-of select="." /></relatedWid>
						</relation>
					</xsl:for-each>
				</relations>
			</xsl:for-each>
		</product>
	</xsl:template>

	<xsl:template name="masterListUpdate">
		<xsl:param name="masterListUpdate" />
		<!-- for notification and reply messages -->
		<metadata>
		    <xsl:apply-templates select="$masterListUpdate/masterListObject[@name = 'Author']/author/authorInfo"/>
		    <xsl:apply-templates select="$masterListUpdate/masterListObject[@name = 'AuthorRole']/authorRole"/>
			<xsl:apply-templates select="$masterListUpdate/masterListObject[@name = 'BusinessUnit']/businessUnit"/>
			<xsl:apply-templates select="$masterListUpdate/masterListObject[@name = 'CommonWork']/commonWork"/>
			<xsl:apply-templates select="$masterListUpdate/masterListObject[@name = 'Edition']/edition"/>
			<xsl:apply-templates select="$masterListUpdate/masterListObject[@name = 'Employee']/employee/employeeInfo"/>
			<xsl:apply-templates select="$masterListUpdate/masterListObject[@name = 'EmployeeRole']/employeeRole"/>
			<xsl:apply-templates select="$masterListUpdate/masterListObject[@name = 'GeographicalLocation']/geographicalLocation"/>
			<xsl:apply-templates select="$masterListUpdate/masterListObject[@name = 'ProductFamily']/productFamily"/>
			<xsl:apply-templates select="$masterListUpdate/masterListObject[@name = 'ProductLine']/productLine"/>
			<xsl:apply-templates select="$masterListUpdate/masterListObject[@name = 'Editor']/editor"/>
			<xsl:apply-templates select="$masterListUpdate/masterListObject[@name = 'ProductType']/productType"/>
			<xsl:apply-templates select="$masterListUpdate/masterListObject[@name = 'RelationCode']/relationCode"/>
			<xsl:apply-templates select="$masterListUpdate/masterListObject[@name = 'SubjectCode']/subjectCode"/>
			<xsl:apply-templates select="$masterListUpdate/masterListObject[@name = 'SubMedium']/subMedium"/>
			<xsl:apply-templates select="$masterListUpdate/masterListObject[@name = 'TargetMedium']/targetMedium"/>
			<xsl:apply-templates select="$masterListUpdate/masterListObject[@name = 'Bundle']/bundle"/>

			<xsl:apply-templates select="masterList"/>
		</metadata>
	</xsl:template>

	<xsl:template match="masterList">
	    <masterList>
			<xsl:copy-of select="@*" />
		</masterList>
	</xsl:template>
	
	<xsl:template match="businessUnit">
		<businessUnit>
			<code><xsl:value-of select="@code"/></code>
			<xsl:copy-of select="name"/>
		</businessUnit>
	</xsl:template>
	
	<xsl:template match="commonWork">
		<commonWork>
			<code><xsl:value-of select="@code"/></code>
			<xsl:copy-of select="name"/>
		</commonWork>
	</xsl:template>
	
	<xsl:template match="geographicalLocation">
		<geographicalLocation>
			<code><xsl:value-of select="@code"/></code>
			<xsl:copy-of select="name"/>
		</geographicalLocation>
	</xsl:template>
	
	<xsl:template match="editor">
		<editor>
			<code><xsl:value-of select="@code"/></code>
			<dataSource><xsl:value-of select="../@dataSource" /></dataSource>
			<xsl:copy-of select="name"/>
		</editor>
	</xsl:template>
	
	<xsl:template match="productFamily">
		<productFamily>
			<code><xsl:value-of select="@familyCode"/></code>
			<xsl:copy-of select="name"/>
		</productFamily>
	</xsl:template>

	<xsl:template match="productType">
		<productType>
			<code><xsl:value-of select="@code"/></code>
			<xsl:copy-of select="name"/>
		</productType>
	</xsl:template>

	<xsl:template match="productLine">
		<productLine>
			<code><xsl:value-of select="@code" /></code>
			<businessUnit><xsl:value-of select="@businessUnit" /></businessUnit>
			<xsl:copy-of select="name" />
			<dataSource><xsl:value-of select="../@dataSource" /></dataSource>
		</productLine>
	</xsl:template>
	
	<xsl:template match="relationCode">
		<relationCode>
			<code><xsl:value-of select="@code"/></code>
			<xsl:copy-of select="name"/>
		</relationCode>
	</xsl:template>

	<xsl:template match="subjectCode">
		<subjectCode>
			<code><xsl:value-of select="@code"/></code>
			<xsl:copy-of select="name"/>
			<xsl:copy-of select="shortName"/>
			<dataSource><xsl:value-of select="../@dataSource" /></dataSource>
		</subjectCode>
	</xsl:template>

	<xsl:template match="targetMedium">
		<medium>
			<code><xsl:value-of select="@code"/></code>
			<xsl:copy-of select="name"/>
		</medium>
	</xsl:template>
	
	<xsl:template match="bundle">
		<bundle>
			<code><xsl:value-of select="@code"/></code>
			<xsl:copy-of select="name"/>
		</bundle>
	</xsl:template>

	<xsl:template match="subMedium">
		<subMedium>
			<code><xsl:value-of select="@code"/></code>
			<xsl:copy-of select="name"/>
			<dataSource><xsl:value-of select="../@dataSource" /></dataSource>
		</subMedium>
	</xsl:template>

	<xsl:template match="edition">
		<productEdition>
			<externalId><xsl:value-of select="@wid"/></externalId>
			<editionNumber><xsl:value-of select="editionNumber"/></editionNumber>
			<productFamily><xsl:value-of select="@productFamily"/></productFamily>
			<productLine><xsl:value-of select="productLine"/></productLine>
			<xsl:copy-of select="name"/>
		</productEdition>
	</xsl:template>

	<xsl:template match="authorRole">
		<role>
		    <roleType>AUTHOR</roleType>
			<code><xsl:value-of select="@code"/></code>
			<description><xsl:value-of select="name"/></description>
		</role>
	</xsl:template>
	
	<xsl:template match="employeeRole">
		<role>
		    <roleType>EMPLOYEE</roleType>
			<code><xsl:value-of select="@code"/></code>
			<description><xsl:value-of select="name"/></description>
		</role>
	</xsl:template>
	
	<xsl:template match="authorInfo">
		<user>
			<type>AUTHOR</type>
			<code><xsl:value-of select="@code"/></code>
			
			<!--
				For some reason, some author's don't have first names.
				Our assumption is that it's because they're organizations
				or something along those lines.
			-->
			<xsl:choose>
				<xsl:when  test="authorName/firstName">
					<xsl:copy-of select="authorName/firstName"/>
				</xsl:when>
				
				<xsl:otherwise>
					<firstName><xsl:value-of select="authorName/lastName"/></firstName>
				</xsl:otherwise>
			</xsl:choose>
			
			<xsl:copy-of select="authorName/lastName"/>
		</user>
	</xsl:template>
	
	<xsl:template match="employeeInfo">
		<user>
			<type>EMPLOYEE</type>
			<code><xsl:value-of select="@code"/></code>
			<username><xsl:value-of select="@code"/></username>
			<xsl:copy-of select="employeeName/firstName"/>
			<xsl:copy-of select="employeeName/lastName"/>
		</user>
	</xsl:template>
	
	<xsl:template match="productionDate">
		<productionDate>
			<xsl:call-template name="convertPEDateToJavaDate">
				<xsl:with-param name="inDate"><xsl:value-of select="."/></xsl:with-param>
			</xsl:call-template>
		</productionDate>
	</xsl:template>

	<xsl:template match="printDate">
		<printDate>
			<xsl:call-template name="convertPEDateToJavaDate">
				<xsl:with-param name="inDate"><xsl:value-of select="."/></xsl:with-param>
			</xsl:call-template>
		</printDate>
	</xsl:template>
	
	<xsl:template match="consolidatedReleaseDate">
		<consolidatedReleaseDate>
			<xsl:call-template name="convertPEDateToJavaDate">
				<xsl:with-param name="inDate"><xsl:value-of select="."/></xsl:with-param>
			</xsl:call-template>
		</consolidatedReleaseDate>
	</xsl:template>
	
	<xsl:template match="productionEndDate">
		<transmittalDate>
			<xsl:call-template name="convertPEDateToJavaDate">
				<xsl:with-param name="inDate"><xsl:value-of select="."/></xsl:with-param>
			</xsl:call-template>
		</transmittalDate>
	</xsl:template>

	<xsl:template match="subjectCode1">
		<subjectCode><xsl:value-of select="."/></subjectCode>
	</xsl:template>

	<xsl:template name="convertPEDateToJavaDate">
		<xsl:param name="inDate"/>
		<xsl:value-of select="concat($inDate, 'T00:00:00.000-07:00')"/>
	</xsl:template>
	
	<xsl:template match="printings">
		<printings>
			<xsl:for-each select="./printing">
				<printing>
					<number><xsl:value-of select="@number"/></number>
					<distributionCenter><xsl:value-of select="distributionCenter"/></distributionCenter>
					<poNumber><xsl:value-of select="poNumber"/></poNumber>
					<poStatus><xsl:value-of select="poStatus"/></poStatus>
					<poDate>
						<xsl:call-template name="convertPEDateToJavaDate">
							<xsl:with-param name="inDate"><xsl:value-of select="poDate"/></xsl:with-param>
						</xsl:call-template>
					</poDate>
					<orderQuantity><xsl:value-of select="orderQuantity"/></orderQuantity>
					<expectedDeliveryDate>
						<xsl:call-template name="convertPEDateToJavaDate">
							<xsl:with-param name="inDate"><xsl:value-of select="expectedDeliveryDate"/></xsl:with-param>
						</xsl:call-template>
					</expectedDeliveryDate>
					<receivedDate>
						<xsl:call-template name="convertPEDateToJavaDate">
							<xsl:with-param name="inDate"><xsl:value-of select="receivedDate"/></xsl:with-param>
						</xsl:call-template>					
					</receivedDate>
					<receivedQuantity><xsl:value-of select="receivedQuantity"/></receivedQuantity>
				</printing>
			</xsl:for-each>
		</printings>
	</xsl:template>
</xsl:stylesheet>
