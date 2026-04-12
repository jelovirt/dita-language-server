<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:x="x"
                xmlns:dita-ot="http://dita-ot.sourceforge.net/ns/201007/dita-ot"
                version="3.0"
                exclude-result-prefixes="xs dita-ot x">

  <xsl:import href="keyref.xsl"/>
  <xsl:import href="conref.xsl"/>
  <xsl:import href="coderef.xsl"/>

  <xsl:output method="html" version="5" indent="no"/>

  <xsl:param name="keyrefs" as="document-node()?">
    <!--
    <xsl:document>
      <keyrefs>
        <keyref key="reusable-components"
          href="file:/Users/jarno.elovirta/work/github.com/dita-ot/dita-ot/src/main/docsrc/resources/reusable-components.dita"/>
        <keyref key="conref-task"
          href="file:/Users/jarno.elovirta/work/github.com/dita-ot/dita-ot/src/main/docsrc/resources/conref-task.dita"/>
      </keyrefs>
    </xsl:document>
    -->
  </xsl:param>

  <xsl:variable name="css" as="xs:string">
    a:before {
      content: "🔗";
    }

    .keyref:before {
      content: "\0023[" attr(data-keyref) "]";
      background-color: transparent
    }

    .conkeyref:before {
      content: "\00A7[" attr(data-conkeyref) "]" !important;
      background-color: transparent;
    }

    .conref:before {
      content: "\00BB[" attr(data-conref) "]";
      background-color: transparent;
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
        background-color: color-mix(in srgb, var(--vscode-editor-background) 80%, white);
      }

      .replaced, .generated, .conref, .conkeyref, [data-conref], [data-keyref] {
        background-color: color-mix(in srgb, var(--vscode-editor-background) 80%, white);
        }
    }

    @media (prefers-color-scheme: light) {
      pre {
        background-color: color-mix(in srgb, var(--vscode-editor-background) 80%, black);
      }

      .replaced, .generated, .conref, .conkeyref, [data-conref], [data-keyref] {
        background-color: color-mix(in srgb, var(--vscode-editor-background) 80%, black);
      }
    }
  </xsl:variable>

  <xsl:variable name="head" as="element()">
    <head>
      <style type="text/css">
        <xsl:value-of select="normalize-space($css)"/>
      </style>
    </head>
  </xsl:variable>

  <xsl:template match="/">
    <xsl:variable name="keyref-resolved" as="document-node()">
      <xsl:choose>
        <xsl:when test="exists($keyrefs) and //*[@keyref or @conkeyref]">
          <xsl:document>
            <xsl:apply-templates mode="keyref"/>
          </xsl:document>
        </xsl:when>
        <xsl:otherwise>
          <xsl:sequence select="/"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="conref-resolved" as="document-node()">
      <xsl:choose>
        <xsl:when test="exists($keyrefs) and $keyref-resolved//@conref">
          <xsl:document>
            <xsl:apply-templates select="$keyref-resolved/*" mode="conref"/>
          </xsl:document>
        </xsl:when>
        <xsl:otherwise>
          <xsl:sequence select="/"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="coderef-resolved" as="document-node()">
      <xsl:choose>
        <xsl:when test="false() and $conref-resolved//*[contains(@class, ' pr-d/coderef ')]">
          <xsl:document>
            <xsl:apply-templates select="$conref-resolved/*" mode="coderef"/>
          </xsl:document>
        </xsl:when>
        <xsl:otherwise>
          <xsl:sequence select="$conref-resolved"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <html>
      <xsl:sequence select="$head"/>
      <body>
        <xsl:apply-templates select="$conref-resolved/*"/>
      </body>
    </html>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/topic ')]">
    <article>
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <xsl:apply-templates select="* except *[contains(@class, ' topic/prolog ')]"/>
    </article>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/titlealts ')]">
    <xsl:apply-templates select="*[contains(@class, ' topic/navtitle ')]"/>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/navtitle ')]">
    <p>
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <xsl:apply-templates/>
    </p>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/topic ')]/*[contains(@class, ' topic/title ')]">
    <xsl:element name="h{count(ancestor::*[contains(@class, ' topic/topic ')])}">
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/shortdesc ')]">
    <p>
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <xsl:apply-templates/>
    </p>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/body ')]">
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/fig ')]">
    <figure>
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <xsl:apply-templates/>
    </figure>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/fig ')]/*[contains(@class, ' topic/title ')]">
    <figcaption>
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <xsl:apply-templates/>
    </figcaption>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/image ')]">
    <img src="{resolve-uri(@href, base-uri(.))}">
      <xsl:apply-templates select="@*" mode="common-attributes"/>
      <xsl:choose>
        <xsl:when test="*[contains(@class, ' topic/alt ')]">
          <xsl:attribute name="alt">
            <xsl:apply-templates select="*[contains(@class, ' topic/alt ')]/node()" mode="text-only"/>
          </xsl:attribute>
        </xsl:when>
        <xsl:when test="@alt">
          <xsl:attribute name="alt" select="@alt"/>
        </xsl:when>
      </xsl:choose>
    </img>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/pre ')]">
    <pre>
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <xsl:apply-templates/>
    </pre>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/note ')]">
    <div class="note">
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <b class="label">
        <xsl:value-of select="(@type/string(), 'note')[1]"/>
        <xsl:text>: </xsl:text>
      </b>
      <xsl:apply-templates/>
    </div>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' hi-d/b ') or
                         contains(@class, ' pr-d/parmname ')]" priority="10">
    <b>
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <xsl:apply-templates/>
    </b>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' sw-d/filepath ') or
                         contains(@class, ' pr-d/codeph ')]" priority="10">
    <code>
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <xsl:apply-templates/>
    </code>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/p ')]">
    <p>
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <xsl:apply-templates/>
    </p>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/section ')]">
    <section>
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <xsl:apply-templates/>
    </section>
  </xsl:template>
  
  <xsl:template match="*[contains(@class, ' topic/section ')]/*[contains(@class, ' topic/title ')]">
    <xsl:element name="h{count(ancestor::*[contains(@class, ' topic/topic ') or contains(@class, ' topic/section ')])}">
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>  

  <xsl:template match="*[contains(@class, ' topic/div ') or contains(@class, ' topic/itemgroup ')]">
    <div>
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <xsl:apply-templates/>
    </div>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/ul ')]">
    <ul>
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <xsl:apply-templates/>
    </ul>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/ol ')]">
    <ol>
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <xsl:apply-templates/>
    </ol>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/li ')]">
    <li>
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <xsl:apply-templates/>
    </li>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/keyword ') or
                         contains(@class, ' topic/ph ')]">
    <span>
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <xsl:apply-templates/>
    </span>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/xref ')]" name="xref">
    <a>
      <xsl:attribute name="href" select="resolve-uri(@href, base-uri(.))"/>
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <xsl:choose>
        <xsl:when test="node()">
          <xsl:apply-templates/>    
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="@href"/>
        </xsl:otherwise>
      </xsl:choose>
    </a>
  </xsl:template>
  
  <xsl:template match="*[contains(@class, ' topic/indexterm ')]"/>

  <!-- Common attribute templates -->

  <xsl:mode name="common-attributes" on-no-match="deep-skip"/>
  
  <xsl:template name="common-attributes">
    <xsl:param name="class" as="xs:string?"/>
    <xsl:apply-templates select="@*" mode="common-attributes"/>
    <xsl:variable name="classes" as="xs:string*">
      <xsl:apply-templates select="." mode="class"/>
    </xsl:variable>
    <xsl:if test="exists($class) or exists($classes) or exists(@outputclass)">
      <xsl:attribute name="class" select="string-join(($class, $classes, @outputclass), ' ')"/>
    </xsl:if>
  </xsl:template>

  <xsl:template match="@id" mode="common-attributes">
    <xsl:attribute name="id" select="concat(ancestor::*[contains(@class, ' topic/topic ')][1]/@id, '__', .)"/>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/topic ')]/@id" mode="common-attributes">
    <xsl:attribute name="id" select="."/>
  </xsl:template>

  <!-- Text only templates -->

  <xsl:mode name="text-only" on-no-match="text-only-copy"/>

  <!-- Class attribute templates -->
 
  <!--xsl:template name="class">
    <xsl:variable name="classes" as="xs:string*">
      <xsl:apply-templates select="." mode="class"/>
    </xsl:variable>
    <xsl:if test="exists($classes) or exists(@outputclass)">
      <xsl:attribute name="class" select="string-join(($classes, @outputclass), ' ')"/>
    </xsl:if>
  </xsl:template-->
  
  <xsl:mode name="class" on-no-match="deep-skip"/>
  
  <xsl:template match="*[@keyref]" mode="class">
    <xsl:text>keyref</xsl:text>
    <xsl:next-match/>
  </xsl:template>
  
  <xsl:template match="*[@conref]" mode="class">
    <xsl:text>conref</xsl:text>
    <xsl:next-match/>
  </xsl:template>
  
  <xsl:template match="*[@conkeyref]" mode="class">
    <xsl:text>conkeyref</xsl:text>
    <xsl:next-match/>
  </xsl:template>
  
  <!-- Prefix templates -->
  
  <xsl:mode name="prefix" on-no-match="deep-skip"/>
  
  <xsl:template match="*[@keyref]" mode="prefix">
    <xsl:attribute name="data-keyref" select="@keyref"/>
    <xsl:next-match/>
  </xsl:template>
  
  <xsl:template match="*[@conref]" mode="prefix">
    <xsl:attribute name="data-conref" select="@conref"/>
    <xsl:next-match/>
  </xsl:template>

  <xsl:template match="*[@conkeyref]" mode="prefix" priority="10">
    <xsl:attribute name="data-conkeyref" select="@conkeyref"/>
    <xsl:next-match/>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/navtitle ')]" mode="prefix">
    <b class="generated label">Navigation title: </b>
    <xsl:next-match/>
  </xsl:template>
  
  <xsl:template match="*[contains(@class, ' topic/shortdesc ')]" mode="prefix">
    <b class="generated label">Short description: </b>
    <xsl:next-match/>
  </xsl:template>
  
</xsl:stylesheet>