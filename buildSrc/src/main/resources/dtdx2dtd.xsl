<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="3.0">

  <xsl:strip-space elements="*"/>

  <xsl:output method="text"/>

  <xsl:mode name="flat"/>
    
  <xsl:template match="comment" mode="flat"/>
    
  <xsl:template match="@* | dtd | elementDecl | attlist | attributeDecl | enumeration" mode="flat">
    <xsl:copy>
      <xsl:apply-templates select="@* | *" mode="#current"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="internalEntityDecl | externalEntityDecl | unparsedEntityDecl | notationDecl | conditional[@type = 'IGNORE'] | ignoredCharacters" mode="flat">
  <!--
    <xsl:copy>
      <xsl:apply-templates select="@* | *" mode="#current"/>
    </xsl:copy>
    -->
  </xsl:template>
  
  <xsl:template match="group[group and empty(group[2])]" mode="flat">
    <xsl:apply-templates mode="#current"/>
  </xsl:template>
  
  <xsl:template match="contentModel | any | empty | group | pcdata | element | separator | occurrence" mode="flat">
    <xsl:copy>
      <xsl:apply-templates select="@* | *" mode="#current"/>
    </xsl:copy>
  </xsl:template>
  
  <xsl:template match="*" mode="flat">
    <xsl:apply-templates mode="#current"/>
  </xsl:template>

  <xsl:template match="/">
    <xsl:variable name="flat" as="document-node()">
      <xsl:document>
        <xsl:apply-templates select="*" mode="flat"/>
      </xsl:document>
    </xsl:variable>
    <xsl:apply-templates select="$flat/*"/>
  </xsl:template>
  
  <xsl:template match="/dtd">
    <xsl:if test="@sysid">
      <xsl:comment>Generated from
        <xsl:value-of select="@sysid"/>
      </xsl:comment>
      <xsl:text>
</xsl:text>
    </xsl:if>
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="elementDecl">
    <xsl:text>&lt;!ELEMENT </xsl:text>
    <xsl:value-of select="@ename"/>
    <xsl:text> </xsl:text>
    <xsl:value-of select="@model"/>
    <xsl:text>&gt;
</xsl:text>
  </xsl:template>

  <xsl:template match="attlist">
    <xsl:text>&lt;!ATTLIST </xsl:text>
    <xsl:value-of select="@ename"/>
    <xsl:text> </xsl:text>
    <xsl:for-each select="descendant::attributeDecl">
      <xsl:if test="position() gt 1">
        <xsl:text>
</xsl:text>
      </xsl:if>
      <xsl:apply-templates select="."/>
    </xsl:for-each>
    <xsl:text>&gt;
</xsl:text>
  </xsl:template>

  <xsl:template match="attributeDecl">
    <xsl:value-of select="@aname"/>
    <xsl:text> </xsl:text>
    <xsl:value-of select="@atype"/>
    <xsl:if test="@atype = 'NOTATION'">
      <xsl:text> (</xsl:text>
      <xsl:for-each select="enumeration">
        <xsl:if test="position() gt 1">|</xsl:if>
        <xsl:value-of select="@value"/>
      </xsl:for-each>
      <xsl:text>)</xsl:text>
    </xsl:if>
    <xsl:choose>
      <xsl:when test="@required"> #REQUIRED</xsl:when>
      <xsl:when test="@fixed"> #FIXED</xsl:when>
      <xsl:when test="not(@default)"> #IMPLIED</xsl:when>
    </xsl:choose>
    <xsl:if test="@default">
      <xsl:text> "</xsl:text>
      <xsl:call-template name="escape">
        <xsl:with-param name="s">
          <xsl:value-of select="@default"/>
        </xsl:with-param>
      </xsl:call-template>
      <xsl:text>"</xsl:text>
    </xsl:if>
  </xsl:template>

  <xsl:template match="internalEntityDecl[not(contains(@name, '%'))]">
    <xsl:text>&lt;!ENTITY </xsl:text>
    <xsl:value-of select="@name"/>
    <xsl:text> "</xsl:text>
    <xsl:call-template name="escape">
      <xsl:with-param name="s">
        <xsl:call-template name="escape">
          <xsl:with-param name="s">
            <xsl:value-of select="normalize-space(@value)"/>
          </xsl:with-param>
        </xsl:call-template>
      </xsl:with-param>
      <xsl:with-param name="c">%</xsl:with-param>
      <xsl:with-param name="C">&amp;#37;</xsl:with-param>
    </xsl:call-template>
    <xsl:text>"&gt;
</xsl:text>
  </xsl:template>

  <xsl:template match="externalEntityDecl[not(contains(@name, '%'))]">
    <xsl:text>&lt;!ENTITY </xsl:text>
    <xsl:value-of select="@name"/>
    <xsl:choose>
      <xsl:when test="@pubid">
        <xsl:text> PUBLIC "</xsl:text>
        <xsl:value-of select="@pubid"/>
        <xsl:text>"</xsl:text>
      </xsl:when>
      <xsl:otherwise>SYSTEM</xsl:otherwise>
    </xsl:choose>
    <xsl:text> "</xsl:text>
    <xsl:value-of select="@sysid"/>
    <xsl:text>"&gt;
</xsl:text>
  </xsl:template>

  <xsl:template match="unparsedEntityDecl">
    <xsl:text>&lt;!ENTITY </xsl:text>
    <xsl:value-of select="@name"/>
    <xsl:choose>
      <xsl:when test="@pubid">
        <xsl:text> PUBLIC "</xsl:text>
        <xsl:value-of select="@pubid"/>
        <xsl:text>"</xsl:text>
      </xsl:when>
      <xsl:otherwise>SYSTEM</xsl:otherwise>
    </xsl:choose>
    <xsl:if test="@sysid">
      <xsl:text> "</xsl:text>
      <xsl:value-of select="@sysid"/>
      <xsl:text>"</xsl:text>
    </xsl:if>
    <xsl:text> NDATA </xsl:text>
    <xsl:value-of select="@notation"/>
    <xsl:text>&gt;
</xsl:text>
  </xsl:template>

  <xsl:template match="notationDecl">
    <xsl:text>&lt;!NOTATION </xsl:text>
    <xsl:value-of select="@name"/>
    <xsl:choose>
      <xsl:when test="@pubid">
        <xsl:text> PUBLIC "</xsl:text>
        <xsl:value-of select="@pubid"/>
        <xsl:text>"</xsl:text>
      </xsl:when>
      <xsl:otherwise>SYSTEM</xsl:otherwise>
    </xsl:choose>
    <xsl:if test="@sysid">
      <xsl:text> "</xsl:text>
      <xsl:value-of select="@sysid"/>
      <xsl:text>"</xsl:text>
    </xsl:if>
    <xsl:text>&gt;
</xsl:text>
  </xsl:template>

  <xsl:template match="conditional">
    <xsl:text>&lt;![</xsl:text>
    <xsl:value-of select="@type"/>
    <xsl:text>[</xsl:text>
    <xsl:if test="@type = 'INCLUDE'">
      <xsl:text>
</xsl:text>
    </xsl:if>
    <xsl:apply-templates/>
    <xsl:text>]]&gt;
</xsl:text>
  </xsl:template>

  <xsl:template match="ignoredCharacters"/>

  <xsl:template match="processingInstruction">
    <xsl:text>&lt;?</xsl:text>
    <xsl:value-of select="@target"/>
    <xsl:if test="@data">
      <xsl:text> </xsl:text>
      <xsl:value-of select="@data"/>
    </xsl:if>
    <xsl:text>?&gt;
</xsl:text>
  </xsl:template>

  <xsl:template match="contentModel"/>

  <xsl:template match="*" priority="-10">
    <xsl:message terminate="yes" select="name()"/>
  </xsl:template>

  <xsl:template name="escape">
    <xsl:param name="s"/>
    <xsl:param name="c">"</xsl:param>
    <xsl:param name="C">&amp;#34;</xsl:param>
    <xsl:if test="string-length($s) gt 0">
      <xsl:choose>
        <xsl:when test="contains($s, $c)">
          <xsl:value-of select="substring-before($s, $c)"/>
          <xsl:value-of select="$C"/>
          <xsl:call-template name="escape">
            <xsl:with-param name="s">
              <xsl:value-of select="substring(substring-after($s, $c), 2)"/>
            </xsl:with-param>
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$s"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
