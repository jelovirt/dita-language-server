<?xml version="1.0" encoding="UTF-8"?>
<!--
This file is part of the DITA Open Toolkit project.

Copyright 2016 Jarno Elovirta

See the accompanying LICENSE file for applicable license.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:dita2html="http://dita-ot.sourceforge.net/ns/200801/dita2html"
                xmlns:dita-ot="http://dita-ot.sourceforge.net/ns/201007/dita-ot"
                xmlns:table="http://dita-ot.sourceforge.net/ns/201007/dita-ot/table"
                xmlns:simpletable="http://dita-ot.sourceforge.net/ns/201007/dita-ot/simpletable"
                version="3.0"
                exclude-result-prefixes="xs dita2html dita-ot table simpletable">
  
  <xsl:template name="dita2html:simpletable-cols">
    <xsl:variable name="col-count" as="xs:integer"
                  select="max(
                    for $row
                    in *[contains-token(@class, 'topic/sthead')] | *[contains-token(@class, 'topic/strow')]
                    return count($row/*[contains-token(@class, 'topic/stentry')])
                  )"/>
    <xsl:variable name="widths" select="tokenize(normalize-space(translate(@relcolwidth, '*', '')), '\s+')"/>
    <xsl:variable name="col-widths" as="xs:double*"
                  select="for $width
                          in $widths
                          return if ($width castable as xs:double)
                                 then xs:double($width)
                                 else xs:double(1),
                          for $gen
                          in 1 to ($col-count - count($widths))
                          return xs:double(1)"/>
    <xsl:variable name="col-widths-sum" select="sum($col-widths)"/>
    <colgroup>
      <xsl:for-each select="$col-widths">
        <col style="width:{(. div $col-widths-sum) * 100}%"/>
      </xsl:for-each>
    </colgroup>
  </xsl:template>

  <xsl:template match="*[contains-token(@class, 'topic/simpletable')]
                        [empty(*[contains-token(@class, 'topic/strow')]/*[contains-token(@class, 'topic/stentry')])]"
                priority="10"/>
  
  <xsl:template match="*[contains-token(@class, 'topic/strow') or contains-token(@class, 'topic/sthead')][empty(*[contains-token(@class, 'topic/stentry')])]" priority="10"/>

  <xsl:template match="*[contains-token(@class, 'topic/simpletable')]">
<!--    <xsl:apply-templates select="*[contains-token(@class, 'ditaot-d/ditaval-startprop')]" mode="out-of-line"/>-->

<!--    <xsl:call-template name="spec-title"/>-->
    <table>
<!--      <xsl:apply-templates select="." mode="table:common"/>-->
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="prefix"/>
      <xsl:apply-templates select="*[contains-token(@class, 'topic/title')]"/>
      <xsl:call-template name="dita2html:simpletable-cols"/>
      <xsl:apply-templates select="*[contains-token(@class, 'topic/sthead')]"/>
      <xsl:apply-templates select="." mode="generate-table-header"/>

      <tbody>
        <xsl:apply-templates select="*[contains-token(@class, 'topic/strow')]"/>
      </tbody>
    </table>

<!--    <xsl:apply-templates select="*[contains-token(@class, 'ditaot-d/ditaval-endprop')]" mode="out-of-line"/>-->
  </xsl:template>

  <xsl:template match="*[contains-token(@class, 'topic/simpletable')]" mode="prefix">
    <xsl:attribute name="border">1</xsl:attribute>
    <xsl:next-match/>
  </xsl:template>

  <xsl:template match="*[contains-token(@class, 'topic/simpletable')]" mode="class">
    <xsl:if test="@frame">
      <xsl:value-of select="concat('frame__', @frame)"/>
    </xsl:if>
    <xsl:next-match/>
  </xsl:template>

  <xsl:template match="*[contains-token(@class, 'topic/simpletable')]" mode="css-class">
    <xsl:apply-templates select="@frame, @expanse, @scale" mode="#current"/>
  </xsl:template>

  <xsl:template match="*[contains-token(@class, 'topic/simpletable')]/*[contains-token(@class, 'topic/title')]">
    <caption>
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates/>
    </caption>
  </xsl:template>

  <xsl:template match="*[contains-token(@class, 'topic/strow')]" name="topic.strow">
    <tr>
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates/>
    </tr>
  </xsl:template>

  <xsl:template match="*[contains-token(@class, 'topic/sthead')]" name="topic.sthead">
    <thead>
      <xsl:call-template name="common-attributes"/>
      <tr>
        <xsl:apply-templates/>
      </tr>
    </thead>
  </xsl:template>

  <xsl:template match="*[simpletable:is-body-entry(.)]" name="topic.stentry">
    <td>
      <xsl:apply-templates select="." mode="simpletable:entry"/>
    </td>
  </xsl:template>

  <xsl:template match="*[simpletable:is-head-entry(.)]
                     | *[simpletable:is-body-entry(.)][simpletable:is-keycol-entry(.) or @scope]"
                priority="2">
    <th>
      <xsl:call-template name="common-attributes"/>
      <xsl:apply-templates select="." mode="simpletable:entry"/>
    </th>
  </xsl:template>

  <xsl:template match="*[contains-token(@class, 'topic/stentry')]" mode="simpletable:entry">
<!--    <xsl:apply-templates select="." mode="table:common"/>-->
    <xsl:apply-templates select="." mode="simpletable:headers"/>
    <xsl:apply-templates select="@colspan | @rowspan | @scope"/>
    <xsl:choose>
      <xsl:when test="* | text() | processing-instruction()">
        <xsl:apply-templates/>
      </xsl:when>
      <xsl:when test="@specentry">
        <xsl:apply-templates select="@specentry"/>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="@colspan | @rowspan | @scope">
    <xsl:copy/>
  </xsl:template>
  
  <xsl:template match="*[simpletable:is-head-entry(.)]" mode="simpletable:headers" as="attribute()*">
    <xsl:attribute name="scope" select="'col'"/>
<!--    <xsl:attribute name="id" select="dita-ot:generate-html-id(.)"/>-->
  </xsl:template>

  <xsl:template match="*[simpletable:is-body-entry(.)]" mode="simpletable:headers" as="attribute()*">
    <xsl:if test="simpletable:is-keycol-entry(.)">
      <xsl:attribute name="scope" select="'row'"/>
    </xsl:if>
    <xsl:variable name="xs" as="xs:integer+"
                  select="if (@colspan)
                          then xs:integer(@dita-ot:x) to (xs:integer(@dita-ot:x) + xs:integer(@colspan) - 1)
                          else xs:integer(@dita-ot:x)"/>
    <xsl:variable name="stentry" as="element()*"
                  select="../../*[contains-token(@class, 'topic/sthead')]/*[contains-token(@class, 'topic/stentry')]
                                                                       [xs:integer(@dita-ot:x) = $xs]"/>
<!--    <xsl:if test="exists($stentry)">-->
<!--      <xsl:variable name="ids" as="xs:string*" select="$stentry/dita-ot:generate-html-id(.)" />-->
<!--      <xsl:attribute name="headers" select="$ids" separator=" "/>-->
<!--    </xsl:if>-->
  </xsl:template>

  <xsl:template match="*[contains-token(@class, 'topic/simpletable')]" mode="generate-table-header" priority="10">
    <xsl:variable name="gen" as="element(gen)">
      <!--
      Generated header needs to be wrapped in gen element to allow correct
      language detection.
      -->
      <gen>
        <xsl:copy-of select="ancestor-or-self::*[@xml:lang][1]/@xml:lang"/>
        <xsl:next-match/>
      </gen>
    </xsl:variable>
    
    <xsl:apply-templates select="$gen/*"/>
  </xsl:template>

  <xsl:function name="simpletable:is-body-entry" as="xs:boolean">
    <xsl:param name="el" as="element()"/>

    <xsl:sequence select="
      contains-token($el/@class, 'topic/stentry') and contains-token($el/../@class, 'topic/strow')
    "/>
  </xsl:function>

  <xsl:function name="simpletable:is-head-entry" as="xs:boolean">
    <xsl:param name="el" as="element()"/>

    <xsl:sequence select="
      contains-token($el/@class, 'topic/stentry') and contains-token($el/../@class, 'topic/sthead')
    "/>
  </xsl:function>

  <xsl:function name="simpletable:get-current-table" as="element()">
    <xsl:param name="node" as="node()"/>

    <xsl:sequence select="
      $node/ancestor-or-self::*[contains-token(@class, 'topic/simpletable')][1]
    "/>
  </xsl:function>

  <xsl:function name="simpletable:is-keycol-entry" as="xs:boolean">
    <xsl:param name="entry" as="element()"/>

    <xsl:variable name="keycol" as="xs:integer?"
                  select="simpletable:get-current-table($entry)/@keycol/xs:integer(.)"/>

    <xsl:sequence select="$keycol = $entry/@dita-ot:x/xs:integer(.)"/>
  </xsl:function>

</xsl:stylesheet>
