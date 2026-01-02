<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:e="http://github.com/jelovirt/dita-schematron"
  xmlns="http://purl.oclc.org/dsdl/schematron" version="3.0"
  xpath-default-namespace="http://purl.oclc.org/dsdl/schematron" exclude-result-prefixes="xs e">

  <xsl:output indent="yes"/>

  <xsl:template match="/*">
    <xsl:copy>
      <xsl:apply-templates select="@* | node()"/>

      <xsl:variable name="allPhases" as="xs:string*">
        <xsl:variable name="all" as="xs:string*">
          <xsl:for-each select="@e:phases">
            <xsl:sequence select="tokenize(.)"/>
          </xsl:for-each>
        </xsl:variable>
        <xsl:sequence select="distinct-values($all)"/>
      </xsl:variable>

      <xsl:variable name="patterns" as="element()*">
        <xsl:for-each select="pattern[not(@abstract = 'true')]">
          <xsl:variable name="pattern" select="."/>
          <xsl:for-each select="
              if (exists(@e:phases)) then
                tokenize(@e:phases)
              else
                $allPhases, 'all'">
            <xsl:variable name="phase" select="."/>
            <xsl:for-each select="'1.0', '1.1', '1.2', '1.3'">
              <xsl:variable name="ditaVersion" select="."/>
              <xsl:if
                test="empty($pattern/@e:ditaVersions) or tokenize($pattern/@e:ditaVersions) = $ditaVersion">
                <pattern>
                  <xsl:copy-of select="$pattern/@id"/>
                  <xsl:attribute name="e:phase" select="concat($phase, '_', $ditaVersion)"/>
                </pattern>
              </xsl:if>
            </xsl:for-each>
          </xsl:for-each>
        </xsl:for-each>
      </xsl:variable>
      <xsl:for-each-group select="$patterns" group-by="@e:phase">
        <phase id="{current-grouping-key()}">
          <xsl:for-each select="current-group()">
            <xsl:sort select="@id"/>
            <active pattern="{@id}"/>
          </xsl:for-each>
        </phase>
      </xsl:for-each-group>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="@* | node()" priority="-10">
    <xsl:copy>
      <xsl:apply-templates select="@* | node()"/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
