<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:dita-ot="http://dita-ot.sourceforge.net/ns/201007/dita-ot"
                version="3.0"
                exclude-result-prefixes="xs dita-ot">

  <xsl:variable name="root-base-uri" as="xs:anyURI" select="base-uri()"/>

  <xsl:template match="*[contains(@class, ' mapgroup-d/mapref ')] | mapref">
    <xsl:message>Found mapref <xsl:value-of select="@href"/> (<xsl:value-of select="base-uri()"/>)</xsl:message>
    <xsl:variable name="submap-uri" select="resolve-uri(@href, base-uri())"/>
    <xsl:variable name="submap" as="document-node()" select="doc($submap-uri)"/>
    <xsl:apply-templates select="$submap/*/*"/>
  </xsl:template>

  <xsl:template match="@href">
    <xsl:attribute name="{name()}" select="dita-ot:relativize($root-base-uri, resolve-uri(., base-uri()))"/>
  </xsl:template>

  <xsl:template match="@* | node()" priority="-10">
    <xsl:copy>
      <xsl:apply-templates select="@* | node()"/>
    </xsl:copy>
  </xsl:template>

  <!-- uri-utils.xsl -->

  <xsl:function name="dita-ot:relativize" as="xs:anyURI">
    <xsl:param name="base" as="xs:anyURI"/>
    <xsl:param name="uri" as="xs:anyURI"/>

    <xsl:variable name="b-scheme" select="substring-before($base, ':')" as="xs:string"/>
    <xsl:variable name="u-scheme" select="substring-before($uri, ':')" as="xs:string"/>
    <xsl:choose>
      <xsl:when test="$b-scheme ne $u-scheme">
        <xsl:sequence select="$uri"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:variable name="b" select="tokenize(substring-after($base, ':'), '/')" as="xs:string+"/>
        <xsl:variable name="u" select="tokenize(substring-after($uri, ':'), '/')" as="xs:string+"/>
        <xsl:sequence select="dita-ot:relativize.strip-and-prefix($b, $u)"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:function>

  <xsl:function name="dita-ot:relativize.strip-and-prefix" as="xs:anyURI">
    <xsl:param name="a" as="xs:string+"/>
    <xsl:param name="b" as="xs:string+"/>
    <xsl:choose>
      <xsl:when test="$a[1] = $b[1]">
        <xsl:sequence select="dita-ot:relativize.strip-and-prefix($a[position() ne 1], $b[position() ne 1])"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:variable name="res" as="xs:string+">
          <xsl:for-each select="$a[position() ne 1]">../</xsl:for-each>
          <xsl:value-of select="$b" separator="/"/>
        </xsl:variable>
        <xsl:sequence select="xs:anyURI(string-join($res, ''))"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:function>

</xsl:stylesheet>