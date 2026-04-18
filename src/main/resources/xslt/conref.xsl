<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:resolved="resolved"
                xmlns:x="x"
                version="3.0"
                exclude-result-prefixes="xs x">

  <xsl:template match="/">
    <xsl:apply-templates mode="conref"/>
  </xsl:template>

  <xsl:mode name="conref" on-no-match="shallow-copy"/>

  <xsl:function name="x:parse-uri" as="xs:string+">
    <xsl:param name="uri" as="xs:string"/>
    <xsl:choose>
      <xsl:when test="contains($uri, '#')">
        <xsl:value-of select="substring-before($uri, '#')"/>
        <xsl:variable name="fragment" select="substring-after($uri, '#')"/>
        <xsl:choose>
          <xsl:when test="contains($fragment, '/')">
            <xsl:value-of select="substring-before($fragment, '/')"/>
            <xsl:value-of select="substring-after($fragment, '/')"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="$fragment"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$uri"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:function>

  <xsl:template match="*[@conref]" mode="conref">
    <xsl:variable name="conref" select="@conref"/>
    <xsl:variable name="conkeyref" select="@resolved:conkeyref"/>
    <xsl:variable name="tokens" select="x:parse-uri(@conref)" as="xs:string+"/>
    <xsl:variable name="target-doc" as="document-node()?"
                  select="document(resolve-uri($tokens[1], base-uri(.)))"/>

    <xsl:variable name="topic" as="element()?"
                  select="if (exists($target-doc))
                          then if ($tokens[2] = '.')
                               then $target-doc//*[contains-token(@class, 'topic/topic')][1]
                               else $target-doc//*[contains-token(@class, 'topic/topic')][@id = $tokens[2]]
                          else ()"/>

    <xsl:variable name="element" as="element()?"
                  select="if (exists($topic))
                          then if (exists($tokens[3]))
                               then $topic//*[@id = $tokens[3]]
                               else $topic
                          else ()"/>

    <xsl:choose>
      <xsl:when test="exists($element)">
        <xsl:for-each select="$element">
          <xsl:copy>
            <xsl:copy-of select="@* except $conref"/>
            <xsl:attribute name="resolved:conref" select="$conref"/>
            <!-- XXX: don't resolve conref recursively -->
            <xsl:copy-of select="node()"/>
          </xsl:copy>
        </xsl:for-each>
      </xsl:when>
      <xsl:otherwise>
        <xsl:next-match/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>