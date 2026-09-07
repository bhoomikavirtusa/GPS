<?xml version="1.0" encoding="utf-8"?>

<!--
    Document   : cms-to-permissions.xsl
    Created on : May 11, 2009
    Author     : smarkoff
    Description:
        Transforms a CMS message to a proper Permissions message.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
	<xsl:output method="xml"/>
	
	<xsl:template match="message">
		<message direction="INCOMING">
			<xsl:for-each select="@*">
				<xsl:copy/>
			</xsl:for-each>
			
			<xsl:for-each select="target">
			    <xsl:call-template name="target" />
			</xsl:for-each>
			
			<operations>
			    <xsl:for-each select="operation">
			        <xsl:call-template name="operation" />
			    </xsl:for-each>
			</operations>
			
			<!-- no longer used -->
			<xsl:for-each select="messageError">
			    <xsl:call-template name="messageError" />
			</xsl:for-each>
		</message>
    </xsl:template>
    
	<xsl:template name="target">
		<targets>
			<xsl:apply-templates/>
		</targets>
	</xsl:template>
	
	<xsl:template match="system">
		<target>
			<xsl:value-of select="."/>
		</target>
	</xsl:template>
    
    <xsl:template name="operation">
        <operation>
            <xsl:attribute name="operationType"><xsl:value-of select="@name" /></xsl:attribute>
            <items>
                <xsl:apply-templates />
            </items>
        </operation>
    </xsl:template>
    
    <xsl:template match="asset">
        <asset>
            <!--  updateTime won't exist when the asset tag is inside the UpdateAssetUse message -->
            <xsl:if test="@updateTime">
                <lastUpdatedDate><xsl:value-of select="@updateTime" /></lastUpdatedDate>
            </xsl:if>
            <externalId><xsl:value-of select="@wid" /></externalId>
            <xsl:apply-templates />
        </asset>
    </xsl:template>
    
    <xsl:template match="description|creditLine|mustDisplayCredit|feeRequired|restrictedUse|royaltyFree|managed">
        <xsl:element name="{name()}">
            <xsl:value-of select="."/>
        </xsl:element>
    </xsl:template>
    
    <xsl:template match="ownerType|mediaType">
        <xsl:element name="{name()}">
            <code><xsl:value-of select="." /></code>
        </xsl:element>
    </xsl:template>
    
    <xsl:template match="copiedFrom|assetGroup">
        <!-- Protect against receiving an empty tag by first checking for wid -->
        <xsl:if test="@wid">
            <xsl:element name="{name()}">
                <externalId><xsl:value-of select="@wid"/></externalId>
            </xsl:element>
        </xsl:if>
    </xsl:template>
    
    <xsl:template match="sources">
        <sources>
            <xsl:apply-templates />
        </sources>
    </xsl:template>
    
    <xsl:template match="source">
        <source>
            <externalId><xsl:value-of select="@wid" /></externalId>
            <role><xsl:value-of select="@role" /></role>
            <name><xsl:value-of select="name" /></name>
        </source>
    </xsl:template>
    
    <xsl:template match="content">
        <content>
            <lastUpdatedDate><xsl:value-of select="@updateTime" /></lastUpdatedDate>
            <fileBlobData><xsl:value-of select="." /></fileBlobData>
            <fileFormat><xsl:value-of select="@format" /></fileFormat>
            <objectName><xsl:value-of select="@objectName" /></objectName>
            <renditionType>
                <code><xsl:value-of select="@renditionType" /></code>
            </renditionType>
        </content>
    </xsl:template>
    
    <xsl:template match="reference">
        <reference>
            <externalId><xsl:value-of select="@wid" /></externalId>
        </reference>
    </xsl:template>
    
    <!-- no longer used -->
    <xsl:template name="messageError">
        <messageError>
            <xsl:copy-of select="*|@*" />
        </messageError>
    </xsl:template>
    
    <xsl:template match="error">
        <error>
            <xsl:copy-of select="code|text" />
            <xsl:if test="reference">
                <reference>
                    <externalId><xsl:value-of select="reference/@wid" /></externalId>
                </reference>
            </xsl:if>
        </error>
    </xsl:template>
    
    <xsl:template match="assetUse">
        <assetUse>
            <lastUpdatedDate><xsl:value-of select="@updateTime" /></lastUpdatedDate>
            <externalId><xsl:value-of select="@wid" /></externalId>
            <xsl:apply-templates />
        </assetUse>
    </xsl:template>

    <xsl:template match="product">
        <product>
            <externalId><xsl:value-of select="@wid" /></externalId>
        </product>
    </xsl:template>
    
    <xsl:template match="componentList">
        <componentList>
            <xsl:apply-templates />
        </componentList>
    </xsl:template>

    <xsl:template match="component">
        <component>
            <lastUpdatedDate><xsl:value-of select="@updateTime" /></lastUpdatedDate>
            <externalId><xsl:value-of select="@wid" /></externalId>
            <name><xsl:value-of select="name" /></name>
            <!-- If use IDREFs on Component for Category and Product, works ok for object-to-xml
                 but not the other way - so don't do using IDREF. -->
            <category><code><xsl:value-of select="category" /></code></category>
            <commonWork><product><externalId><xsl:value-of select="product/@wid" /></externalId></product></commonWork>
        </component>
    </xsl:template>
    
    <xsl:template match="position|manuscriptPage|finalPage|isColor|reuse|reuseISBN|isPickup|pickupISBN|canceled|permissionComment">
        <xsl:element name="{name()}">
            <xsl:value-of select="."/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="size|usage|permissionStatus">
        <xsl:element name="{name()}">
            <code><xsl:value-of select="." /></code>
        </xsl:element>
    </xsl:template>
    
</xsl:stylesheet>
