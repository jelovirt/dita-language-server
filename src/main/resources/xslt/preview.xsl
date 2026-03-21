<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:dita-ot="http://dita-ot.sourceforge.net/ns/201007/dita-ot"
                version="3.0"
                exclude-result-prefixes="xs dita-ot">

  <xsl:output method="html" version="5"/>

  <xsl:template match="/">
    <html>
      <head>
        <style type="text/css">
          a:before {
            content: "🔗";
          }
          .keyref:before {
            content: "🔑";
          }
          .generated {
            cursor: pointer;
          }
        </style>
      </head>
      <body>
        <xsl:apply-templates/>
      </body>
    </html>
  </xsl:template>


  <xsl:template match="topic">
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="title">
    <xsl:element name="h{count(ancestor::topic)}">
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>

  <xsl:template match="shortdesc">
    <p><b class="generated">Short description</b>: <xsl:apply-templates/></p>
  </xsl:template>

  <xsl:template match="body">
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="b | i | em | strong | p | ol | ul | li">
    <xsl:element name="{local-name()}">
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>

  <xsl:template match="xref">
    <a>
      <xsl:choose>
        <xsl:when test="@keyref">
          <xsl:attribute name="class">keyref</xsl:attribute>
          <xsl:text>[</xsl:text>
          <xsl:value-of select="@keyref"/>
          <xsl:text>]</xsl:text>
        </xsl:when>
        <xsl:when test="@href">
          <xsl:attribute name="href" select="@href"/>
        </xsl:when>
      </xsl:choose>
      <xsl:apply-templates/>
    </a>
  </xsl:template>


</xsl:stylesheet>