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
            content: "🔑[" attr(data-keyref) "]";
            background-color: transparent;
          }
          .conref:before {
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
            .replaced {
              background-color: color-mix(in srgb, var(--vscode-editor-background) 90%, white);
            }
          }
          @media (prefers-color-scheme: light) {
            pre {
              background-color: color-mix(in srgb, var(--vscode-editor-background) 10%, black);
            }
            .replaced {
              background-color: color-mix(in srgb, var(--vscode-editor-background) 10%, black);
            }
          }
        </style>
      </head>
      <body>
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
        <xsl:variable name="coderef-resolved" as="document-node()">
          <xsl:choose>
            <xsl:when test="$keyref-resolved//*[contains(@class, ' pr-d/coderef ')]">
              <xsl:document>
                <xsl:apply-templates select="$keyref-resolved/*" mode="coderef"/>
              </xsl:document>    
            </xsl:when>
            <xsl:otherwise>
              <xsl:sequence select="$keyref-resolved"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:variable>
        <xsl:apply-templates select="$coderef-resolved/*"/>
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

  <xsl:template match="*[contains(@class, ' topic/topic ')]/*[contains(@class, ' topic/title ')]">
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

  <xsl:template match="*[contains(@class, ' topic/fig ')]">
    <figure>
      <xsl:apply-templates/>
    </figure>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/fig ')]/*[contains(@class, ' topic/title ')]">
    <figcaption>
      <xsl:apply-templates/>
    </figcaption>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/image ')]">
    <img src="{resolve-uri(@href, base-uri(.))}">
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
      <xsl:apply-templates/>
    </pre>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/note ')]">
    <div class="note">
      <b class="label">
        <xsl:value-of select="(@type/string(), 'note')[1]"/>
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
      <xsl:if test="@keyref | @conref">
        <xsl:call-template name="class"/>
        <xsl:apply-templates select="." mode="prefix"/>
      </xsl:if>
      <xsl:apply-templates/>
    </span>
  </xsl:template>

  <xsl:template match="*[contains(@class, ' topic/xref ')]" name="xref">
    <a>
      <xsl:attribute name="href" select="resolve-uri(@href, base-uri(.))"/>
      <xsl:call-template name="class"/>
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

  <!-- Text only templates -->

  <xsl:mode name="text-only" on-no-match="text-only-copy"/>

  <!-- Class attribute templates -->
  
  <xsl:template name="class">
    <xsl:variable name="classes" as="xs:string*">
      <xsl:apply-templates select="." mode="class"/>
    </xsl:variable>
    <xsl:if test="exists($classes) or exists(@outputclass)">
      <xsl:attribute name="class" select="string-join(($classes, @outputclass), ' ')"/>
    </xsl:if>
  </xsl:template>
  
  <xsl:mode name="class" on-no-match="deep-skip"/>
  
  <xsl:template match="*[@keyref]" mode="class">
    <xsl:text>keyref</xsl:text>
    <xsl:next-match/>
  </xsl:template>
  
  <xsl:template match="*[@conref]" mode="class">
    <xsl:text>conref</xsl:text>
    <xsl:next-match/>
  </xsl:template>
  
  <!-- Prefix templates -->
  
  <xsl:mode name="prefix" on-no-match="deep-skip"/>
  
  <xsl:template match="*[@keyref]" mode="prefix">
    <xsl:attribute name="data-keyref" select="@keyref"/>
    <xsl:next-match/>
  </xsl:template>
  
  <xsl:template match="*[@conref]" mode="prefix">
    <xsl:attribute name="data-keyref" select="@keyref"/>
    <xsl:next-match/>
  </xsl:template>
  
  <!-- Key reference resolution templates -->
  
  <xsl:mode name="keyref" on-no-match="shallow-copy"/>
  
  <xsl:key name="keys" match="keyref" use="@key"/>
  
  <xsl:template match="*[@keyref]" mode="keyref">
    <xsl:variable name="key" select="key('keys', @keyref, $keyrefs)" as="element()?"/>
    <xsl:copy>
      <xsl:copy-of select="$key/@href"/>
      <xsl:apply-templates select="@*" mode="#current"/>
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
    <xsl:variable name="href" select="if (contains($key/@href, '#'))
                                      then $key/@href
                                      else concat($key/@href, '#.')"/>
    <xsl:copy>
      <xsl:attribute name="conref" select="if (contains(@conkeyref, '/'))
                                           then concat($href, '/', substring-after(@conkeyref, '/'))
                                           else $href"/>
      <xsl:apply-templates select="@*" mode="#current"/>
      <xsl:choose>
        <xsl:when test="node()">
          <xsl:apply-templates mode="#current"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates select="$key/node()" mode="#current"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:copy>
  </xsl:template>
  
  <!-- Code reference templates -->
  
  <xsl:mode name="coderef" on-no-match="shallow-copy"/>
  
  <xsl:template match="*[contains(@class, ' pr-d/coderef ')]" mode="coderef">
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