<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : permissions-to-pe.xsl
    Created on : May 19, 2008, 3:56 PM
    Author     : ttidwell
    Description:
        This transformation is used ONLY for simulating REPLY messages from PE in answer to a REQUEST from Permissions
        It is used only by ProductResource (RESTlet service that simulates PE)
		
		EXAMPLE (will transform internal message into something like this):
		
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<message protocolVersion="1.0" sent="1300386354921" type="reply" source="PE001" id="PERM.2852384327.1300386356670.1">
    <target>
        <system>PERM001</system>
    </target>
    <reply>
        <productUpdate>
            <productData>
                <product isbn10="0764572970" isbn13="9780764572975" businessUnit="" commonWork="" pnumber="000263966" volume="0" status="N" wid="CORE.001.PROD.0000056478">
                    <lastUpdatedDate>1298974824827</lastUpdatedDate>
                    <copyrightYear>2005</copyrightYear>
                    <isbn10>0764572970</isbn10>
                    <isbn13>9780764572975</isbn13>
                    <languageSpoken>eng</languageSpoken>
                    <geographicalLocation/>
                    <targetMedium/>
                    <pnumber>000263966</pnumber>
                    <printDate>2005-03-08</printDate>
                    <productLine/>
                    <productType/>
                    <productionDate>2004-08-24</productionDate>
                    <shortTitle>Please Oh Please Can We Get a Dog?</shortTitle>
                    <sku>0764572970</sku>
                    <subjectCode1/>
                    <volume>0</volume>
                </product>
            </productData>
        </productUpdate>
    </reply>
</message>		

-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="xml" indent="no" standalone="yes"/>
	
	<xsl:strip-space elements="*"/>
	
	<xsl:template match="message">
		<message>
			<xsl:for-each select="@*">
				<!--  TODO: convert permissions replyId to PE replyID (smarkoff) -->
				<!-- lnagy - for now just ignore the replyId because the validation fails for request message in PE  -->
				<xsl:if test="name(.) != 'direction' and name(.) != 'replyId' ">
					<xsl:copy/>
				</xsl:if>
			</xsl:for-each>

			<xsl:apply-templates/>
		</message>
	</xsl:template>
	
	<xsl:template match="targets">
		<target>
			<xsl:apply-templates/>
		</target>
	</xsl:template>
	
	<xsl:template match="target">
		<system>
			<xsl:value-of select="."/>
		</system>
	</xsl:template>
	
	<xsl:template match="operations">
		<xsl:element name="{/message/@type}">
			<xsl:apply-templates/>
		</xsl:element>
	</xsl:template>
	
	<xsl:template match="operation">
		<xsl:element name="{@operationType}">
			<xsl:for-each select="parameters/parameter">
				<xsl:attribute name="{@name}"><xsl:value-of select="@value"/></xsl:attribute>
			</xsl:for-each>
			<xsl:apply-templates/>
		</xsl:element>
	</xsl:template>
	
	<xsl:template match="items">
		<xsl:apply-templates/>
	</xsl:template>
	
	<xsl:template match="product">
		<productData>
			<xsl:call-template name="doProduct"/>
		</productData>
	</xsl:template>

	<xsl:template name="doProduct">
		<product>

			<xsl:if test="externalId">
				<xsl:attribute name="wid">
					<xsl:value-of select="externalId"/>
				</xsl:attribute>
			</xsl:if>

			<xsl:if test="isbn10">
				<xsl:attribute name="isbn10">
					<xsl:value-of select="isbn10"/>
				</xsl:attribute>
			</xsl:if>

			<xsl:if test="isbn13">
				<xsl:attribute name="isbn13">
					<xsl:value-of select="isbn13"/>
				</xsl:attribute>
			</xsl:if>
			
			<xsl:if test="editor">
				<xsl:attribute name="editor">
					<xsl:value-of select="editor"/>
				</xsl:attribute>
			</xsl:if>
			
			<xsl:if test="businessUnit">
				<xsl:attribute name="businessUnit">
					<xsl:value-of select="businessUnit"/>
				</xsl:attribute>
			</xsl:if>
			
			<xsl:if test="pnumber">
				<xsl:attribute name="pnumber">
					<xsl:value-of select="pnumber"/>
				</xsl:attribute>
			</xsl:if>

			<xsl:if test="volume">
				<xsl:attribute name="volume">
					<xsl:value-of select="volume"/>
				</xsl:attribute>
			</xsl:if>

			<xsl:if test="publicationStatus">
				<xsl:attribute name="status">
					<xsl:value-of select="publicationStatus/code"/>
				</xsl:attribute>
			</xsl:if>

			<xsl:if test="edition">
				<xsl:attribute name="editionWid">
					<xsl:value-of select="edition/externalId"/>
				</xsl:attribute>
			</xsl:if>
			
			<xsl:apply-templates/>
		</product>

		<xsl:call-template name="masterListUpdate">
			<xsl:with-param name="masterListUpdate" select="//metadata" />
		</xsl:call-template>		
		
	</xsl:template>
	
	<xsl:template match="productionDate">
		<xsl:variable name="ourDate" select="."/>
		
		<productionDate>
			<xsl:call-template name="convertJavaDateToPEDate">
				<xsl:with-param name="inDate">
					<xsl:value-of select="$ourDate"/>
				</xsl:with-param>
			</xsl:call-template>
		</productionDate>
	</xsl:template>
	
	<xsl:template match="printDate">
		<xsl:variable name="ourDate" select="."/>
		
		<printDate>
			<xsl:call-template name="convertJavaDateToPEDate">
				<xsl:with-param name="inDate">
					<xsl:value-of select="$ourDate"/>
				</xsl:with-param>
			</xsl:call-template>
		</printDate>
	</xsl:template>
	
	<xsl:template match="consolidatedReleaseDate">
		<xsl:variable name="ourDate" select="."/>
		
		<consolidatedReleaseDate>
			<xsl:call-template name="convertJavaDateToPEDate">
				<xsl:with-param name="inDate">
					<xsl:value-of select="$ourDate"/>
				</xsl:with-param>
			</xsl:call-template>
		</consolidatedReleaseDate>
	</xsl:template>
	
	<xsl:template match="userToProducts">
		<xsl:apply-templates/>
	</xsl:template>
	
	<xsl:template match="userToProduct">
		<xsl:choose>
			<xsl:when test="role != 'A'">
				<employee>
					<xsl:attribute name="code">
						<xsl:value-of select="user"/>
					</xsl:attribute>
					
					<xsl:copy-of select="role"/>
				</employee>
			</xsl:when>
			<xsl:when test="role = 'A'">
				<author>
					<xsl:attribute name="code">
						<xsl:value-of select="user"/>
					</xsl:attribute>
					
					<xsl:copy-of select="role"/>
				</author>
			</xsl:when>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template match="printings">
		<printings>
			<xsl:for-each select="./printing">
				<printing>
					<xsl:attribute name="number">
						<xsl:value-of select="number"/>
					</xsl:attribute>				
					<distributionCenter><xsl:value-of select="distributionCenter"/></distributionCenter>
					<poNumber><xsl:value-of select="poNumber"/></poNumber>
					<poStatus><xsl:value-of select="poStatus"/></poStatus>
					<poDate>
						<xsl:call-template name="convertJavaDateToPEDate">
							<xsl:with-param name="inDate"><xsl:value-of select="poDate"/></xsl:with-param>
						</xsl:call-template>
					</poDate>
					<orderQuantity><xsl:value-of select="orderQuantity"/></orderQuantity>
					<expectedDeliveryDate>
						<xsl:call-template name="convertJavaDateToPEDate">
							<xsl:with-param name="inDate"><xsl:value-of select="expectedDeliveryDate"/></xsl:with-param>
						</xsl:call-template>
					</expectedDeliveryDate>
					<receivedDate>
						<xsl:call-template name="convertJavaDateToPEDate">
							<xsl:with-param name="inDate"><xsl:value-of select="receivedDate"/></xsl:with-param>
						</xsl:call-template>					
					</receivedDate>
					<receivedQuantity><xsl:value-of select="receivedQuantity"/></receivedQuantity>
				</printing>
			</xsl:for-each>
		</printings>
	</xsl:template>
		
	<!-- We're just ignoring these, they're used internally or in other ways -->
	<xsl:template match="xmlId"/>
	
	<xsl:template match="id"/>
	
	<xsl:template match="externalId"/>
	
	<xsl:template match="businessUnit"/>
	
	<xsl:template match="parameters"/>
	
	<xsl:template match="publicationStatus"/>	
	
	<xsl:template match="metadata"/>	
	
	<xsl:template name="masterListUpdate">
		<xsl:param name="masterListUpdate" />
		<masterListUpdate>
		    <xsl:apply-templates select="$masterListUpdate/businessUnit"/>
		    <xsl:apply-templates select="$masterListUpdate/commonWork"/>
		    <xsl:apply-templates select="$masterListUpdate/geographicalLocation"/>
		    <xsl:apply-templates select="$masterListUpdate/productFamily"/>
		    <xsl:apply-templates select="$masterListUpdate/productType"/>
		    <xsl:apply-templates select="$masterListUpdate/productLine"/>
		    <xsl:apply-templates select="$masterListUpdate/subjectCode"/>
		    <xsl:apply-templates select="$masterListUpdate/medium"/>
		    <xsl:apply-templates select="$masterListUpdate/subMedium"/>
		    <xsl:apply-templates select="$masterListUpdate/productEdition"/>
		    <xsl:call-template name="roles"/>
		    <xsl:call-template name="authors"/>
		     <xsl:call-template name="employees"/>

            <masterListObject name="AuthorRole">
               <authorRole code="A">
                  <name>Author</name>
               </authorRole>
            </masterListObject>
		    
		</masterListUpdate>
	</xsl:template>	
	
	<!-- // -->
	<xsl:template name="roles">
         <masterListObject name="EmployeeRole">
			<xsl:for-each select="//product/userToProducts/userToProduct/role">
				<employeeRole>
					<xsl:attribute name="code">
						<xsl:value-of select="."/>
					</xsl:attribute>   		
					<name><xsl:value-of select="."/></name>		
                </employeeRole>
			</xsl:for-each>
         </masterListObject>
	</xsl:template>

	<xsl:template name="authors">
         <masterListObject name="Author">
			<xsl:for-each select="//product/userToProducts/userToProduct">
				<xsl:choose>
					<xsl:when test="role = 'A'">
		                  <author>
		                     <authorInfo>
								<xsl:attribute name="code">
									<xsl:value-of select="user"/>
								</xsl:attribute>		                     
		                        <authorName>
		                           <firstName>TEST</firstName>
		                           <lastName>TEST</lastName>
		                        </authorName>
		                      </authorInfo>
		                  </author>
					</xsl:when>
				</xsl:choose>
			</xsl:for-each>
         </masterListObject>
	</xsl:template>

	<xsl:template name="employees">
         <masterListObject name="Employee">
			<xsl:for-each select="//product/userToProducts/userToProduct">
				<xsl:choose>
					<xsl:when test="role != 'A'">
		                  <employee>
		                     <employeeInfo>
								<xsl:attribute name="code">
									<xsl:value-of select="user"/>
								</xsl:attribute>		                     
		                        <employeeName>
		                           <firstName>TEST</firstName>
		                           <lastName>TEST</lastName>
		                        </employeeName>
		                     </employeeInfo>
		                  </employee>
					</xsl:when>
				</xsl:choose>
			</xsl:for-each>
         </masterListObject>
	</xsl:template>
	
	<xsl:template match="metadata/businessUnit">
         <masterListObject name="BusinessUnit">
            <businessUnit>
				<xsl:attribute name="code">
					<xsl:value-of select="code"/>
				</xsl:attribute>            	
               <xsl:copy-of select="name"/>
            </businessUnit>
         </masterListObject>
	</xsl:template>
	
	<xsl:template match="metadata/commonWork">
         <masterListObject name="CommonWork">
            <commonWork>
				<xsl:attribute name="code">
					<xsl:value-of select="code"/>
				</xsl:attribute>            	
               <xsl:copy-of select="name"/>
            </commonWork>
         </masterListObject>
	</xsl:template>

	<xsl:template match="metadata/geographicalLocation">
         <masterListObject name="GeographicalLocation">
            <geographicalLocation>
				<xsl:attribute name="code">
					<xsl:value-of select="code"/>
				</xsl:attribute>            	
               <xsl:copy-of select="name"/>
            </geographicalLocation>
         </masterListObject>
	</xsl:template>

	<xsl:template match="metadata/productFamily">
         <masterListObject name="ProductFamily">
            <productFamily>
				<xsl:attribute name="familyCode">
					<xsl:value-of select="code"/>
				</xsl:attribute>            	
               <xsl:copy-of select="name"/>
            </productFamily>
         </masterListObject>
	</xsl:template>

	<xsl:template match="metadata/productType">
         <masterListObject name="ProductType">
            <productType>
				<xsl:attribute name="code">
					<xsl:value-of select="code"/>
				</xsl:attribute>            	
               <xsl:copy-of select="name"/>
            </productType>
         </masterListObject>
	</xsl:template>
	
	<xsl:template match="metadata/productLine">
         <masterListObject name="ProductLine">
            <productLine>
				<xsl:attribute name="code">
					<xsl:value-of select="code"/>
				</xsl:attribute>            	
				<xsl:attribute name="businessUnit">
					<xsl:value-of select="businessUnit"/>
				</xsl:attribute>            	
               <xsl:copy-of select="name"/>
            </productLine>
         </masterListObject>
	</xsl:template>

	<xsl:template match="metadata/subjectCode">
         <masterListObject name="SubjectCode">
            <subjectCode>
				<xsl:attribute name="code">
					<xsl:value-of select="code"/>
				</xsl:attribute>            	
               <xsl:copy-of select="name"/>
               <xsl:copy-of select="shortName"/>
            </subjectCode>
         </masterListObject>
	</xsl:template>
	
	
	<xsl:template match="metadata/medium">
         <masterListObject name="TargetMedium">
            <targetMedium>
				<xsl:attribute name="code">
					<xsl:value-of select="code"/>
				</xsl:attribute>            	
               <xsl:copy-of select="name"/>
            </targetMedium>
         </masterListObject>
	</xsl:template>
	
	<xsl:template match="metadata/subMedium">
         <masterListObject name="SubMedium">
            <subMedium>
				<xsl:attribute name="code">
					<xsl:value-of select="code"/>
				</xsl:attribute>            	
               <xsl:copy-of select="name"/>
            </subMedium>
         </masterListObject>
	</xsl:template>

	<xsl:template match="metadata/productEdition">
         <masterListObject name="Edition">
            <edition>
				<xsl:attribute name="wid">
					<xsl:value-of select="externalId"/>
				</xsl:attribute>            	
				<xsl:attribute name="productFamily">
					<xsl:value-of select="productFamily"/>
				</xsl:attribute>            	
               <xsl:copy-of select="editionNumber"/>
               <xsl:copy-of select="productLine"/>
               <xsl:copy-of select="name"/>
            </edition>
         </masterListObject>
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


	<xsl:template match="*">
		<xsl:copy-of select="."/>
	</xsl:template>
	
	<xsl:template name="convertJavaDateToPEDate">
		<xsl:param name="inDate"/>
		<xsl:value-of select="substring($inDate, 1, 10)"/>
	</xsl:template>
</xsl:stylesheet>
