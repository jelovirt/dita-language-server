<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:resolved="resolved"
                xmlns:x="x"
                version="3.0"
                exclude-result-prefixes="xs x">

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

    .keyref:not(.keyref__resolved):before {
      content: "\0023[" attr(data-keyref) "]";
      padding-right: 0.25rem;
    }

    .keyref__resolved:before {
      content: "\0023";
      padding-right: 0.25rem;
    }

    .conkeyref:not(.conkeyref__resolved):before {
      content: "\00A7[" attr(data-conkeyref) "]" !important;
      padding-right: 0.25rem;
    }

    .conkeyref__resolved:before {
      content: "\00A7" !important;
      padding-right: 0.25rem;
    }

    .conref:not(.conref__resolved):before {
      content: "\00BB[" attr(data-conref) "]";
      padding-right: 0.25rem;
    }

    .conref__resolved:before {
      content: "\00BB";
      padding-right: 0.25rem;
    }

    .shortdesc:before {
      content: "Short description: ";
      font-weight: bold;
      text-transform: capitalize;
    }

    .navtitle:before {
      content: "Navigation title: ";
      font-weight: bold;
      text-transform: capitalize;
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
        background-color: color-mix(in srgb, var(--vscode-editor-background) 90%, pink);
      }

      .replaced, .generated, .conref, .conkeyref,
      .shortdesc:before, .navtitle:before,
      [data-resolved-conref], [data-resolved-keyref], [data-resolved-conkeyref] {
        background-color: color-mix(in srgb, var(--vscode-editor-background) 80%, white);
      }
    }

    @media (prefers-color-scheme: light) {
      pre {
        background-color: color-mix(in srgb, var(--vscode-editor-background) 90%, black);
      }

      .replaced, .generated, .conref, .conkeyref,
      .shortdesc:before, .navtitle:before,
      [data-resolved-conref], [data-resolved-keyref], [data-resolved-conkeyref] {
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
        <xsl:when test="false() and $conref-resolved//*[contains-token(@class, 'pr-d/coderef')]">
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

  <xsl:template match="*[contains-token(@class, 'topic/topic')]">
    <article>
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <xsl:apply-templates select="* except *[contains-token(@class, 'topic/prolog')]"/>
    </article>
  </xsl:template>

  <xsl:template match="*[contains-token(@class, 'topic/titlealts')]">
    <xsl:apply-templates select="*[contains-token(@class, 'topic/navtitle')]"/>
  </xsl:template>

  <xsl:template match="*[contains-token(@class, 'topic/navtitle')]">
    <p>
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <xsl:apply-templates/>
    </p>
  </xsl:template>

  <xsl:template match="*[contains-token(@class, 'topic/topic')]/*[contains-token(@class, 'topic/title')]">
    <xsl:element name="h{count(ancestor::*[contains-token(@class, 'topic/topic')])}">
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>

  <xsl:template match="*[contains-token(@class, 'topic/shortdesc')]">
    <p>
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <xsl:apply-templates/>
    </p>
  </xsl:template>

  <xsl:template match="*[contains-token(@class, 'topic/body')]">
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="*[contains-token(@class, 'topic/fig')]">
    <figure>
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <xsl:apply-templates/>
    </figure>
  </xsl:template>

  <xsl:template match="*[contains-token(@class, 'topic/fig')]/*[contains-token(@class, 'topic/title')]">
    <figcaption>
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <xsl:apply-templates/>
    </figcaption>
  </xsl:template>

  <xsl:template match="*[contains-token(@class, 'topic/image')]">
    <img src="{resolve-uri(@href, base-uri(.))}">
      <xsl:apply-templates select="@*" mode="common-attributes"/>
      <xsl:choose>
        <xsl:when test="*[contains-token(@class, 'topic/alt')]">
          <xsl:attribute name="alt">
            <xsl:apply-templates select="*[contains-token(@class, 'topic/alt')]/node()" mode="text-only"/>
          </xsl:attribute>
        </xsl:when>
        <xsl:when test="@alt">
          <xsl:attribute name="alt" select="@alt"/>
        </xsl:when>
      </xsl:choose>
    </img>
  </xsl:template>

  <xsl:template match="*[contains-token(@class, 'topic/pre')]">
    <pre>
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <xsl:apply-templates/>
    </pre>
  </xsl:template>

  <xsl:template match="*[contains-token(@class, 'topic/note')]">
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

  <xsl:template match="*[contains-token(@class, 'hi-d/b') or
                         contains-token(@class, 'pr-d/parmname')]" priority="10">
    <b>
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <xsl:apply-templates/>
    </b>
  </xsl:template>

  <xsl:template match="*[contains-token(@class, 'sw-d/filepath') or
                         contains-token(@class, 'pr-d/codeph')]" priority="10">
    <code>
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <xsl:apply-templates/>
    </code>
  </xsl:template>

  <xsl:template match="*[contains-token(@class, 'topic/p')]">
    <p>
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <xsl:apply-templates/>
    </p>
  </xsl:template>

  <xsl:template match="*[contains-token(@class, 'topic/section')]">
    <section>
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <xsl:apply-templates/>
    </section>
  </xsl:template>

  <xsl:template match="*[contains-token(@class, 'topic/section')]/*[contains-token(@class, 'topic/title')]">
    <xsl:element name="h{count(ancestor::*[contains-token(@class, 'topic/topic') or contains-token(@class, 'topic/section')])}">
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>

  <xsl:template match="*[contains-token(@class, 'topic/div') or contains-token(@class, 'topic/itemgroup')]">
    <div>
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <xsl:apply-templates/>
    </div>
  </xsl:template>

  <xsl:template match="*[contains-token(@class, 'topic/ul')]">
    <ul>
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <xsl:apply-templates/>
    </ul>
  </xsl:template>

  <xsl:template match="*[contains-token(@class, 'topic/ol')]">
    <ol>
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <xsl:apply-templates/>
    </ol>
  </xsl:template>

  <xsl:template match="*[contains-token(@class, 'topic/li')]">
    <li>
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <xsl:apply-templates/>
    </li>
  </xsl:template>

  <xsl:template match="*[contains-token(@class, 'topic/keyword') or
                         contains-token(@class, 'topic/ph')]">
    <span>
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <xsl:apply-templates/>
    </span>
  </xsl:template>

  <xsl:template match="*[contains-token(@class, 'topic/xref')]" name="xref">
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

  <xsl:template match="*[contains-token(@class, 'topic/indexterm')]"/>

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
    <xsl:attribute name="id" select="concat(ancestor::*[contains-token(@class, 'topic/topic')][1]/@id, '__', .)"/>
  </xsl:template>

  <xsl:template match="*[contains-token(@class, 'topic/topic')]/@id" mode="common-attributes">
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

  <xsl:template match="*[@keyref | @resolved:keyref]" mode="class">
    <xsl:text>keyref</xsl:text>
    <xsl:if test="@resolved:keyref">keyref__resolved</xsl:if>
    <xsl:next-match/>
  </xsl:template>

  <xsl:template match="*[@conref | @resolved:conref]" mode="class">
    <xsl:text>conref</xsl:text>
    <xsl:if test="@resolved:conref">conref__resolved</xsl:if>
    <xsl:next-match/>
  </xsl:template>

  <xsl:template match="*[@conkeyref | @resolved:conkeyref]" mode="class">
    <xsl:text>conkeyref</xsl:text>
    <xsl:if test="@resolved:conkeyref">conkeyref__resolved</xsl:if>
    <xsl:next-match/>
  </xsl:template>

  <xsl:template match="*[contains-token(@class, 'topic/navtitle')]" mode="class">
    <xsl:text>navtitle</xsl:text>
    <xsl:next-match/>
  </xsl:template>

  <xsl:template match="*[contains-token(@class, 'topic/shortdesc')]" mode="class">
    <xsl:text>shortdesc</xsl:text>
    <xsl:next-match/>
  </xsl:template>


  <!-- Prefix templates -->

  <xsl:mode name="prefix" on-no-match="deep-skip"/>

  <xsl:template match="*[@keyref]" mode="prefix">
    <xsl:attribute name="data-keyref" select="@keyref"/>
    <xsl:next-match/>
  </xsl:template>

  <xsl:template match="*[@resolved:keyref]" mode="prefix">
    <xsl:attribute name="data-resolved-keyref" select="@resolved:keyref"/>
    <xsl:next-match/>
  </xsl:template>

  <xsl:template match="*[@conref]" mode="prefix">
    <xsl:attribute name="data-conref" select="@conref"/>
    <xsl:next-match/>
  </xsl:template>

  <xsl:template match="*[@resolved:conref]" mode="prefix">
    <xsl:attribute name="data-resolved-conref" select="@resolved:conref"/>
    <xsl:next-match/>
  </xsl:template>

  <xsl:template match="*[@conkeyref]" mode="prefix" priority="10">
    <xsl:attribute name="data-conkeyref" select="@conkeyref"/>
    <xsl:next-match/>
  </xsl:template>

  <xsl:template match="*[@resolved:conkeyref]" mode="prefix" priority="10">
    <xsl:attribute name="data-resolved-conkeyref" select="@resolved:conkeyref"/>
    <xsl:next-match/>
  </xsl:template>

  <!--
  <xsl:template match="*[contains-token(@class, 'topic/navtitle')]" mode="prefix">
    <b class="generated label">Navigation title:</b>
    <xsl:next-match/>
  </xsl:template>

  <xsl:template match="*[contains-token(@class, 'topic/shortdesc')]" mode="prefix">
    <b class="generated label">Short description:</b>
    <xsl:next-match/>
  </xsl:template>
  -->

</xsl:stylesheet>