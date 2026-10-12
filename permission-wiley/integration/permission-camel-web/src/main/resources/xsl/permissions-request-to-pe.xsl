<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : permissions-to-pe.xsl
    Created on : May 19, 2008, 3:56 PM
    Author     : ttidwell
    Description:
        Transforms a permissions message to a PE message.
        Used to transform PERM request messages to PE request messages
        
        EXAMPLE (will transform internal message into something like this):
         
<message protocolVersion="1.0" sent="1300384608631" type="request" source="PERM001" id="PERM.2852384327.1300384608631.3">
    <target>
        <system>PE001</system>
    </target>
    <request>
        <productUpdate dataSource="US">
            <product>
                <isbn10>0764572970</isbn10>
            </product>
        </productUpdate>
    </request>
</message>
	
	LNAGY - Not sure when we send a reply message to PE ?????        
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
		<xsl:if test="//@type = 'reply'">
			<productData>
				<xsl:call-template name="doProduct"/>
			</productData>
		</xsl:if>

		<xsl:if test="//@type = 'request'">
			<xsl:call-template name="doProduct"/>
		</xsl:if>
	</xsl:template>

	<xsl:template name="doProduct">
		<product>
<!-- lnagy - why attribute? maybe is different for reply  and request.
 I don't think  any of those parameters are allowed in the request message
For request definitely is element, not attribute
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
					<xsl:value-of select="businessUnit/code"/>
				</xsl:attribute>
			</xsl:if>
			
			<xsl:if test="commonWork">
				<xsl:attribute name="commonWork">
					<xsl:value-of select="commonWork/code"/>
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
			
 -->
			<xsl:if test="//@type = 'reply' and externalId">
				<xsl:attribute name="wid">
					<xsl:value-of select="externalId" />
				</xsl:attribute>
			</xsl:if>
			
			<!-- dataSource is attribute instead of element (for PE) -->
			<xsl:if test="//@type = 'request' and dataSource">
				<xsl:attribute name="dataSource">
					<xsl:value-of select="dataSource" />
				</xsl:attribute>
			</xsl:if>
			
			<xsl:if test="//@type = 'request' and externalId">
				<wid>
					<xsl:value-of select="externalId" />
				</wid>
			</xsl:if>

			<xsl:if test="//@type = 'request' and isbn10">
				<isbn10>
					<xsl:value-of select="isbn10" />
				</isbn10>
			</xsl:if>

			<xsl:if test="//@type = 'request' and isbn13">
				<isbn13>
					<xsl:value-of select="isbn13" />
				</isbn13>
			</xsl:if>
			
			<xsl:if test="//@type = 'request' and pnumber">
				<pnumber>
					<xsl:value-of select="pnumber" />
				</pnumber>
			</xsl:if>
			
<!-- lnagy - we need nothing else for a productUpdate
			<xsl:apply-templates/>
 -->			
		</product>
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
	
	
	<xsl:template match="subjectCode">
		<subjectCode1>
			<xsl:value-of select="./code"/>
		</subjectCode1>
	</xsl:template>
	
	<xsl:template match="targetMedium">
		<targetMedium>
			<xsl:value-of select="./code"/>
		</targetMedium>
	</xsl:template>
	
	<xsl:template match="subMedium">
		<subMedium>
			<xsl:value-of select="./code"/>
		</subMedium>
	</xsl:template>
		
	<xsl:template match="productLine">
		<productLine>
			<xsl:value-of select="./code"/>
		</productLine>
	</xsl:template>
	
	<xsl:template match="productFamily">
		<productFamily>
			<xsl:value-of select="./code"/>
		</productFamily>
	</xsl:template>
	
	<xsl:template match="productType">
		<productType>
			<xsl:value-of select="./code"/>
		</productType>
	</xsl:template>
	
	<xsl:template match="geographicalLocation">
		<geographicalLocation>
			<xsl:value-of select="./code"/>
		</geographicalLocation>
	</xsl:template>
	
	<xsl:template match="userToProducts">
		<xsl:apply-templates/>
	</xsl:template>
	
	<xsl:template match="userToProduct">
		<xsl:choose>
			<xsl:when test="user/type = 'EMPLOYEE'">
				<employee>
					<xsl:attribute name="code">
						<xsl:value-of select="./user/username"/>
					</xsl:attribute>
					
					<xsl:copy-of select="roles/role"/>
				</employee>
			</xsl:when>
			<xsl:when test="user/type = 'AUTHOR'">
				<author>
					<xsl:attribute name="code">
						<xsl:value-of select="./user/code"/>
					</xsl:attribute>
					
					<xsl:copy-of select="roles/role"/>
				</author>
			</xsl:when>
		</xsl:choose>
	</xsl:template>
	
	<!-- We're just ignoring these, they're used internally or in other ways -->
	<xsl:template match="xmlId"/>
	
	<xsl:template match="id"/>
	
	<xsl:template match="externalId"/>
	
	<xsl:template match="businessUnit"/>
	
	<xsl:template match="commonWork"/>
	
	<xsl:template match="edition"/>

	<xsl:template match="parameters"/>
	
	<xsl:template match="title"/>

	<xsl:template match="metadata"/>
	
	<xsl:template match="printings"/>
	
	<xsl:template match="printing"/>
	
	<xsl:template match="publicationStatus"/>
	<!-- // -->

	<xsl:template match="*">
		<xsl:copy-of select="."/>
	</xsl:template>
	
	<xsl:template name="convertJavaDateToPEDate">
		<xsl:param name="inDate"/>
		<xsl:value-of select="substring($inDate, 1, 10)"/>
	</xsl:template>
</xsl:stylesheet>
