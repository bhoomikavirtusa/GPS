<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : request-pe-to-permissions.xsl
    Created on : Jan 3, 2011
    Author     : lnagy
    Description:
        This transformation is used ONLY for processing REQUEST messages from Permissions 
        It is used only by ProductResource (RESTlet service that simulates PE)
		
		EXAMPLE (will transform something like this into the internal message):
		
<message protocolVersion="1.0" sent="1300299002378" type="request" source="PERM001" id="PERM.2852384327.1300299002379.2">
    <target>
        <system>PE001</system>
    </target>
    <request>
        <productUpdate dataSource="US">
            <product>
                <wid>CORE.001.PROD.0000056478</wid>
            </product>
        </productUpdate>
    </request>
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
	
	<xsl:template match="request">
		<xsl:apply-templates/>
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

	<!-- this template is used by productUpdate notifications or requests -->
	<xsl:template match="product">
		<product>
			<xsl:if test="isbn10">
				<isbn10><xsl:value-of select="isbn10"/></isbn10>
			</xsl:if>
			<xsl:if test="isbn13">
				<isbn13><xsl:value-of select="isbn13"/></isbn13>
			</xsl:if>
			<xsl:if test="wid">
				<externalId><xsl:value-of select="wid"/></externalId>
			</xsl:if>
		</product>
	</xsl:template>
	
	<xsl:template match="masterList">
	    <masterList>
			<xsl:copy-of select="@*" />
		</masterList>
	</xsl:template>
	
</xsl:stylesheet>
