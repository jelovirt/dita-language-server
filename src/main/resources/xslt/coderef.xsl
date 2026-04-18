<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:x="x"
                version="3.0"
                exclude-result-prefixes="xs x">

  <xsl:template match="/">
    <xsl:apply-templates mode="coderef"/>
  </xsl:template>

  <xsl:mode name="coderef" on-no-match="shallow-copy"/>

  <xsl:template match="*[contains-token(@class, 'pr-d/coderef')]" mode="coderef">
    <xsl:choose>
      <xsl:when test="doc-available(@href)">
        <xsl:value-of select="unparsed-text(@href)"/>
      </xsl:when>
      <xsl:otherwise>
        <span class="coderef">
          <xsl:text>[</xsl:text>
          <xsl:value-of select="@href"/>
          <xsl:text>]</xsl:text>
        </span>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>