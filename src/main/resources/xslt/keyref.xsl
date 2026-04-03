<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:x="x"
                xmlns:dita-ot="http://dita-ot.sourceforge.net/ns/201007/dita-ot"
                version="3.0"
                exclude-result-prefixes="xs dita-ot x">

  <xsl:output method="html" version="5"/>

  <xsl:param name="keyrefs" as="document-node()?">
    <!--
    <xsl:document>
      <keyrefs>
        <keyref key="reusable-components"
          href="file:/Users/jarno.elovirta/work/github.com/dita-ot/dita-ot/src/main/docsrc/resources/reusable-components.dita"/>
      </keyrefs>
    </xsl:document>
    -->
  </xsl:param>

  <xsl:template match="/">
    <xsl:apply-templates mode="keyref"/>
  </xsl:template>

  <xsl:mode name="keyref" on-no-match="shallow-copy"/>
  
  <xsl:key name="keys" match="keyref" use="@key"/>
  
  <xsl:template match="*[@keyref]" mode="keyref">
    <xsl:variable name="key" select="key('keys', @keyref, $keyrefs)" as="element()?"/>
    <xsl:copy>
      <xsl:copy-of select="$key/@href"/>
      <xsl:apply-templates select="@* except @keyref" mode="#current"/>
       <xsl:choose>
         <xsl:when test="node()">
           <xsl:apply-templates mode="#current"/>
         </xsl:when>
         <xsl:otherwise>
           <xsl:attribute name="outputclass" select="string-join((@outputclass, 'replaced'), ' ')"/>
           <xsl:apply-templates select="$key/node()" mode="#current"/>
         </xsl:otherwise>
       </xsl:choose>
    </xsl:copy>
  </xsl:template>
  
  <xsl:template match="*[@conkeyref]" mode="keyref">
    <xsl:variable name="keyref" select="if (contains(@conkeyref, '/'))
                                        then substring-before(@conkeyref, '/')
                                        else @conkeyref"/>
    <xsl:variable name="key" select="key('keys', $keyref, $keyrefs)" as="element()?"/>
    <xsl:choose>
      <xsl:when test="exists($key)">
        <xsl:variable name="href" select="
            if (contains($key/@href, '#'))
            then
              $key/@href
            else
              concat($key/@href, '#.')"/>
        <xsl:copy>
          <xsl:attribute name="conref" select="
              if (contains(@conkeyref, '/'))
              then
                concat($href, '/', substring-after(@conkeyref, '/'))
              else
                $href"/>
          <xsl:apply-templates select="@* except @conkeyref" mode="#current"/>
          <xsl:choose>
            <xsl:when test="node()">
              <xsl:apply-templates mode="#current"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:apply-templates select="$key/node()" mode="#current"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:copy>
      </xsl:when>
      <xsl:otherwise>
        <xsl:next-match/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
</xsl:stylesheet>