<?xml version="1.0" encoding="utf-8"?>

<!--
    Document   : permissions-to-cms.xsl
    Created on : May 7, 2009
    Author     : smarkoff
    Description:
        Transforms a Permissions message to a proper CMS message.
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
    <xsl:output method="xml"/>

    <xsl:template match="message">
		<message>
			<xsl:for-each select="@*">
				<xsl:if test="name() != 'direction'">
					<xsl:copy/>
				</xsl:if>
			</xsl:for-each>
			
            <xsl:apply-templates />
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
        <xsl:apply-templates />
    </xsl:template>
    
    <xsl:template match="operation">
		<operation>
			 <xsl:attribute name="name"><xsl:value-of select="@operationType"/></xsl:attribute>
		     <xsl:apply-templates />
		</operation>
	</xsl:template>
    
    <xsl:template match="asset">
        <asset>
            <xsl:attribute name="wid"><xsl:value-of select="externalId"/></xsl:attribute>
            <!--  lastUpdatedDate won't exist when the asset tag is inside the UpdateAssetUse message -->
            <xsl:if test="lastUpdatedDate">
                <xsl:attribute name="updateTime"><xsl:value-of select="lastUpdatedDate"/></xsl:attribute>
                <xsl:apply-templates />
            </xsl:if>
        </asset>
    </xsl:template>
    
    <xsl:template match="copiedFrom|assetGroup">
        <xsl:element name="{name()}">
            <xsl:attribute name="wid"><xsl:value-of select="externalId"/></xsl:attribute>
        </xsl:element>
    </xsl:template>
    
    <xsl:template match="externalId|lastUpdatedDate|id">
        <!--  Don't show since already converted to attributes or are not copying to external message. -->
    </xsl:template>
    
    <xsl:template match="content">
        <content>
            <xsl:attribute name="updateTime"><xsl:value-of select="lastUpdatedDate" /></xsl:attribute>
            <xsl:attribute name="format"><xsl:value-of select="fileFormat" /></xsl:attribute>
            <xsl:attribute name="objectName"><xsl:value-of select="objectName" /></xsl:attribute>
            <xsl:attribute name="renditionType"><xsl:value-of select="renditionType/code" /></xsl:attribute>
            <xsl:value-of select="fileBlobData" />
        </content>
    </xsl:template>
    
    <xsl:template match="fileFormat|fileUploadedDate">
        <!-- Ignore these because we use them already above in fileBlobData template. -->
        <!-- Do nothing -->
    </xsl:template>
    
    <xsl:template match="ownerType|mediaType">
        <xsl:element name="{name()}">
            <xsl:value-of select="code" />
        </xsl:element>
    </xsl:template>
    
    <xsl:template match="description|creditLine|mustDisplayCredit|feeRequired|restrictedUse|royaltyFree|managed">
        <xsl:element name="{name()}">
            <xsl:value-of select="."/>
        </xsl:element>
    </xsl:template>
    
    <xsl:template match="sources">
        <sources>
            <xsl:apply-templates />
        </sources>
    </xsl:template>
    
    <xsl:template match="source">
        <source>
            <xsl:attribute name="wid"><xsl:value-of select="externalId" /></xsl:attribute>
            <xsl:element name="name"><xsl:value-of select="name" /></xsl:element>
        </source>
    </xsl:template>
    
    <xsl:template match="metadata">
        <!-- Do nothing (swallow any existing tag) -->
    </xsl:template>

    <xsl:template match="reference">
        <reference>
            <xsl:attribute name="wid"><xsl:value-of select="externalId" /></xsl:attribute>
        </reference>
    </xsl:template>

    <!-- no longer used -->
    <xsl:template match="messageError">
        <messageError>
            <xsl:copy-of select="*|@*" />
        </messageError>
    </xsl:template>

    <xsl:template match="error">
        <error>
            <xsl:copy-of select="code|text" />
            <xsl:if test="reference">
                <reference>
                    <xsl:attribute name="wid"><xsl:value-of select="reference/externalId"/></xsl:attribute>
                </reference>
            </xsl:if>
        </error>
    </xsl:template>

    <xsl:template match="assetUse">
        <assetUse>
            <xsl:attribute name="wid"><xsl:value-of select="externalId"/></xsl:attribute>
            <xsl:attribute name="updateTime"><xsl:value-of select="lastUpdatedDate"/></xsl:attribute>
            <xsl:apply-templates />
        </assetUse>
    </xsl:template>
    
    <xsl:template match="product">
        <product>
            <xsl:attribute name="wid"><xsl:value-of select="externalId"/></xsl:attribute>
        </product>
    </xsl:template>
    
    <xsl:template match="component">
        <component>
            <xsl:attribute name="wid"><xsl:value-of select="externalId"/></xsl:attribute>
            <xsl:attribute name="updateTime"><xsl:value-of select="lastUpdatedDate"/></xsl:attribute>
            <product>
                <xsl:attribute name="wid"><xsl:value-of select="commonWork/product/externalId"/></xsl:attribute>
            </product>
            <name><xsl:value-of select="name"/></name>
            <category><xsl:value-of select="category/code"/></category>
        </component>
    </xsl:template>
    
    <xsl:template match="componentList">
        <componentList>
            <xsl:apply-templates />
        </componentList>
    </xsl:template>
    
    <xsl:template match="position|manuscriptPage|finalPage|isColor|permissionComment|reuse|reuseISBN|isPickup|pickupISBN|canceled">
        <xsl:element name="{name()}">
            <xsl:value-of select="."/>
        </xsl:element>
    </xsl:template>
    
    <xsl:template match="isRequest|cameraCopyToCome|mediaReturnReq|foundOn">
        <!--  Do nothing -->
    </xsl:template>
    
    <xsl:template match="size|usage|permissionStatus">
        <xsl:element name="{name()}">
            <xsl:value-of select="code" />
        </xsl:element>
    </xsl:template>

</xsl:stylesheet>
