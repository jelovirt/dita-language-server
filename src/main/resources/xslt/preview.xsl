<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:dita-ot="http://dita-ot.sourceforge.net/ns/201007/dita-ot"
                version="3.0"
                exclude-result-prefixes="xs dita-ot">

  <xsl:output method="html" version="5"/>

  <xsl:param name="keyrefs" as="document-node()?"/>

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
          .conkeyref:before {
            content: "📎";
          }
          .generated {
            cursor: pointer;
          }
          .label {
            text-transform: capitalize;
          }
          .note {
            margin: 1rem;
          }
          pre {
            padding: 0.5rem;
          }
          @media (prefers-color-scheme: dark) {
            pre {
              background-color: color-mix(in srgb, var(--vscode-editor-background) 90%, white);
            }
          }
          @media (prefers-color-scheme: light) {
            pre {
              background-color: color-mix(in srgb, var(--vscode-editor-background) 10%, black);
            }
          }
        </style>
      </head>
      <body>
        <xsl:apply-templates/>
      </body>
    </html>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/topic ')]">
    <xsl:apply-templates select="* except *[contains(@class, ' topic/prolog ')]"/>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/titlealts ')]">
    <xsl:apply-templates select="*[contains(@class, ' topic/navtitle ')]"/>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/titlealts ')]">
    <p>
      <b class="generated label">Navigation title: </b>
      <xsl:apply-templates/>
    </p>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/title ')]">
    <xsl:element name="h{count(ancestor::*[contains(@class, ' topic/topic ')])}">
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/shortdesc ')]">
    <p>
      <b class="generated label">Short description: </b>
      <xsl:apply-templates/>
    </p>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/body ')]">
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/pre ')]">
    <pre>
      <xsl:apply-templates/>
    </pre>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/note ')]">
    <div class="note">
      <b class="label">
        <xsl:value-of select="@type"/>
        <xsl:text>: </xsl:text>
      </b>
      <xsl:apply-templates/>
    </div>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' hi-d/b ') or
                         contains(@class, ' pr-d/parmname ')]">
    <b>
      <xsl:apply-templates/>
    </b>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' sw-d/filepath ') or
                         contains(@class, ' pr-d/codeph ')]">
    <code>
      <xsl:apply-templates/>
    </code>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/p ')]">
    <p>
      <xsl:apply-templates/>
    </p>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/ul ')]">
    <ul>
      <xsl:apply-templates/>
    </ul>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/ol ')]">
    <ol>
      <xsl:apply-templates/>
    </ol>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/li ')]">
    <li>
      <xsl:apply-templates/>
    </li>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/keyword ')]">
    <span>
      <xsl:choose>
        <xsl:when test="@keyref">
          <xsl:variable name="keyref" select="$keyrefs/keyrefs/keyref[@key = current()/@keyref]" as="element()?"/>
          <xsl:attribute name="class">keyref</xsl:attribute>
          <xsl:text>[</xsl:text>
          <xsl:value-of select="@keyref"/>
          <xsl:text>]</xsl:text>
          <xsl:choose>
            <xsl:when test="node()">
              <xsl:apply-templates/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:apply-templates select="$keyref/node()"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates/>
        </xsl:otherwise>
      </xsl:choose>
    </span>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/xref ')]">
    <a>
      <xsl:choose>
        <xsl:when test="@keyref">
          <xsl:variable name="keyref" select="$keyrefs/keyrefs/keyref[@key = current()/@keyref]" as="element()?"/>
          <xsl:attribute name="class">keyref</xsl:attribute>
          <xsl:text>[</xsl:text>
          <xsl:value-of select="@keyref"/>
          <xsl:text>]</xsl:text>
          <xsl:choose>
            <xsl:when test="node()">
              <xsl:apply-templates/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:apply-templates select="$keyref/node()"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <xsl:when test="@href">
          <xsl:attribute name="href" select="@href"/>
          <xsl:choose>
            <xsl:when test="node()">
              <xsl:apply-templates/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="@href"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:when>
      </xsl:choose>
    </a>
  </xsl:template>

</xsl:stylesheet>