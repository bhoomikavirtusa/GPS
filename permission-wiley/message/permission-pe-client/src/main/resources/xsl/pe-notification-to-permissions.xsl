<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : request-pe-to-permissions.xsl
    Created on : Jan 3, 2011
    Author     : lnagy
    Description:
        Split the pe-to-permissions into 2 files - one for requests|notifications, the other for reply messages
        This transformer is used to convert the notification messages from PE
        
        EXAMPLE (will transform something like this into internal message):
        
<?xml version="1.0" encoding="UTF-8"?>
<message xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="sysmsg_20090427.xsd" id="PE001.598336" type="notification" sent="1272910686577" source="PE001" protocolVersion="1.0">
   <notification>
      <productUpdate>
         <product wid="CORE.001.PROD.0000234204" businessUnit="2" status="I" dataSource="US" completeRecordSet="no" updateTime="1272910652459"></product>
      </productUpdate>
   </notification>
</message>
        
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="xml" indent="yes" standalone="yes"/>
	
	<xsl:strip-space elements="*"/>

	<xsl:variable name="messageType" select="//message/@type"/>

	<xsl:template match="message">
		<message direction="INCOMING">
			<xsl:for-each select="@*">
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
	
	<xsl:template match="notification">
		<xsl:apply-templates/>
	</xsl:template>

	<xsl:template match="masterListUpdate">
		<operations>
			<operation operationType="masterListUpdate">
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
	</xsl:template>

	<!-- this template is used by productUpdate notifications -->
	<xsl:template match="product">
		<updateProductNotificationMessage>
			<!-- fields that should always be in the message -->
			<externalId><xsl:value-of select="@wid" /></externalId>
			<title><xsl:value-of select="title" /></title>
			<lastUpdatedDate><xsl:value-of select="@updateTime" /></lastUpdatedDate>

			<!-- fields that may be in the message -->
			
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
					
					<xsl:when test="name() = 'dataSource'">
						<dataSource><xsl:value-of select="."/></dataSource>
					</xsl:when>
					
					<xsl:when test="name() = 'status'">
						<status><xsl:value-of select="."/></status>
					</xsl:when>
					
					<xsl:when test="name() = 'businessUnit'">
						<businessUnit><xsl:value-of select="."/></businessUnit>
					</xsl:when>
				</xsl:choose>
			</xsl:for-each>
			
			<!-- note using copy-of is fine for optional elements -->
			<xsl:copy-of select="commonWork" />
			<xsl:copy-of select="shortAuthorName" />
			<xsl:copy-of select="copyrightYear" />
			<xsl:copy-of select="editionNumber" />
			<xsl:copy-of select="previousEditionWID" />
			<xsl:copy-of select="nextEditionWID" />
			<xsl:copy-of select="targetMedium" />
			
			<xsl:for-each select="author">
				<authorCode><xsl:value-of select="@code" /></authorCode>
			</xsl:for-each>
		</updateProductNotificationMessage>
	</xsl:template>
	
	<xsl:template name="masterListUpdate">
		<xsl:param name="masterListUpdate" />
		<metadata>
			<xsl:apply-templates select="masterList"/>
		</metadata>
	</xsl:template>

	<xsl:template match="masterList">
	    <masterList>
			<xsl:copy-of select="@*" />
		</masterList>
	</xsl:template>
	
</xsl:stylesheet>
