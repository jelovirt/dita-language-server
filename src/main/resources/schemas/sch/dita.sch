<?xml version="1.0" encoding="UTF-8"?>
<schema xmlns="http://purl.oclc.org/dsdl/schematron"
        xmlns:e="http://github.com/jelovirt/dita-schematron"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <ns uri="http://dita.oasis-open.org/architecture/2005/" prefix="ditaarch"/>
  <xsl:key name="elementsByID"
           match="*[@id][not(contains(@class, ' topic/topic '))]"
           use="concat(@id, '#', ancestor::*[contains(@class, ' topic/topic ')][1]/@id)"/>
  <!-- Required per spec -->
  <pattern id="otherrole"
           see="http://docs.oasis-open.org/dita/v1.1/OS/langspec/common/theroleattribute.html"
           e:phases="mandatory recommendation">
    <rule context="*[@role = 'other']">
      <assert test="@otherrole" role="error">
        The
        <name/>
        element with role attribute value 'other' should have otherrole attribute set.
      </assert>
    </rule>
  </pattern>
  <pattern id="othernote"
           see="http://docs.oasis-open.org/dita/v1.1/OS/langspec/common/thetypeattribute.html"
           e:phases="mandatory recommendation">
    <rule context="*[contains(@class,' topic/note ')][@type = 'other']">
      <assert test="@othertype" role="error">
        The
        <name/>
        element with type attribute value 'other' should have othertype attribute set.
      </assert>
    </rule>
  </pattern>
  <pattern id="indextermref"
           see="http://docs.oasis-open.org/dita/v1.1/OS/langspec/langref/indextermref.html"
           e:phases="mandatory recommendation">
    <rule context="*[contains(@class, ' topic/indextermref ')]">
      <report test="true()" role="error">
        The
        <name/>
        element is reserved for future use.
      </report>
    </rule>
  </pattern>
  <pattern id="collection-type_on_rel"
           see="http://docs.oasis-open.org/dita/v1.1/OS/langspec/common/topicref-atts.html"
           e:phases="mandatory recommendation">
    <rule context="*[contains(@class, ' map/reltable ')]                   | *[contains(@class, ' map/relcolspec ')]">
      <report test="@collection-type" role="error" subject="@collection-type">
        The collection-type attribute on
        <name/>
        is reserved for future use.
      </report>
    </rule>
  </pattern>
  <pattern id="keyref_attr"
           see="http://docs.oasis-open.org/dita/v1.1/OS/langspec/common/othercommon.html"
           e:ditaVersions="1.0 1.1"
           e:phases="mandatory recommendation">
    <rule context="*">
      <report test="@keyref" role="error" subject="@keyref">
        The keyref attribute on
        <name/>
        is reserved for future use.
      </report>
    </rule>
  </pattern>
  <!-- Recommended per spec -->
  <pattern id="boolean"
           see="http://docs.oasis-open.org/dita/v1.1/OS/langspec/langref/boolean.html"
           e:phases="recommendation other">
    <rule context="*[contains(@class, ' topic/boolean ')]">
      <report test="true()" diagnostics="state_element" role="warning">
        The
        <name/>
        element is deprecated.
      </report>
    </rule>
  </pattern>
  <pattern id="image_alt_attr"
           see="http://docs.oasis-open.org/dita/v1.1/OS/langspec/langref/image.html"
           e:phases="recommendation other">
    <rule context="*[contains(@class, ' topic/image ')]/@alt">
      <report test="true()" diagnostics="alt_element" role="warning">
        The alt attribute is deprecated.
      </report>
    </rule>
  </pattern>
  <pattern id="image_longdescref_attr"
           see="http://docs.oasis-open.org/dita/v1.1/OS/langspec/langref/image.html"
           e:phases="recommendation other">
    <rule context="*[contains(@class, ' topic/image ')]/@longdescref">
      <report test="true()" diagnostics="longdescref_element" role="warning">
        The longdescref attribute is deprecated.
      </report>
    </rule>
  </pattern>
  <pattern id="query_attr"
           see="http://docs.oasis-open.org/dita/v1.1/OS/langspec/langref/link.html"
           e:phases="recommendation other">
    <rule
      context="*[contains(@class, ' topic/link ')]/@query                  | *[contains(@class, ' map/topicref ')]/@query">
      <report test="true()" role="warning">
        The query attribute is deprecated. It may be removed in the future.
      </report>
    </rule>
  </pattern>
  <pattern id="role_attr_value"
           see="http://docs.oasis-open.org/dita/v1.1/OS/langspec/common/theroleattribute.html"
           e:phases="recommendation other">
    <rule
      context="*[contains(@class, ' topic/related-links ') or                       contains(@class, ' topic/link ') or                       contains(@class, ' topic/linklist ') or                       contains(@class, ' topic/linkpool ')]/@role">
      <report test=". = 'sample'" role="warning">
        The value "sample" for role attribute is deprecated.
      </report>
      <report test=". = 'external'"
              role="warning"
              diagnostics="external_scope_attribute">
        The value "external" for role attribute is deprecated.
      </report>
    </rule>
  </pattern>
  <pattern id="single_paragraph"
           see="http://docs.oasis-open.org/dita/v1.1/OS/langspec/langref/shortdesc.html"
           e:phases="recommendation other">
    <rule context="*[contains(@class, ' topic/topic ')]"
          subject="*[contains(@class, ' topic/body ')]/*[contains(@class, ' topic/p ')]">
      <report
        test="not(*[contains(@class, ' topic/shortdesc ')] | *[contains(@class, ' topic/abstract ')]) and                     count(*[contains(@class, ' topic/body ')]/*) = 1 and                     *[contains(@class, ' topic/body ')]/*[contains(@class, ' topic/p ')]"
        role="warning">
        In cases where a topic contains only one paragraph, then it is preferable to include this
        text in the shortdesc element and leave the topic body empty.
      </report>
    </rule>
  </pattern>
  <pattern id="shortdesc_length"
           see="http://docs.oasis-open.org/dita/v1.1/OS/langspec/langref/shortdesc.html"
           e:phases="recommendation other">
    <rule context="*[contains(@class, ' topic/shortdesc ')]">
      <let name="text" value="normalize-space(.)"/>
      <!-- This is a rudimentary guess that could be improved -->
      <report test="string-length($text) - string-length(translate($text, ' ', '')) &gt;= 50"
              role="warning">
        The short description should be a single, concise paragraph containing one or two sentences of no more than 50
        words.
      </report>
    </rule>
  </pattern>
  <pattern id="navtitle" e:ditaVersions="1.2" e:phases="recommendation other">
    <rule context="*[contains(@class, ' map/topicref ')]">
      <report test="@navtitle" diagnostics="navtitle_element" role="warning">
        The navtitle attribute is deprecated.
      </report>
    </rule>
  </pattern>
  <pattern id="map_title_attribute"
           e:ditaVersions="1.1 1.2"
           e:phases="recommendation other">
    <rule context="*[contains(@class, ' map/map ')]">
      <report test="@title" role="warning">
        Map can include a title element, which is preferred over the title attribute.
      </report>
    </rule>
  </pattern>
  <pattern id="topichead_navtitle"
           e:ditaVersions="1.1 1.2"
           e:phases="recommendation other">
    <rule context="*[contains(@class, ' mapgroup-d/topichead ')]">
      <assert test="@navtitle" role="warning" e:ditaVersions="1.1">
        The
        <name/>
        element should have a navtitle attrbute.
      </assert>
      <assert test="@navtitle | *[contains(@class, ' map/topicmeta ')]/*[contains(@class, ' topic/navtitle ')]"
              role="warning"
              e:ditaVersions="1.2">
        The
        <name/>
        element should have a navtitle element.
      </assert>
    </rule>
  </pattern>
  <!-- Recommended per convention -->
  <pattern id="self_nested_xref"
           see="http://www.w3.org/TR/html/#prohibitions"
           e:phases="recommendation other">
    <rule context="*[contains(@class, ' topic/xref ')]//*[contains(@class, ' topic/xref ')]">
      <report test="true()" role="warning">
        The
        <name/>
        element contains
        <name/>
        element. The results in processing are undefined.
      </report>
    </rule>
  </pattern>
  <pattern id="pre_content"
           see="http://www.w3.org/TR/html/#prohibitions"
           e:phases="recommendation other">
    <rule context="*[contains(@class, ' topic/pre ')]">
      <report test="descendant::*[contains(@class, ' topic/image ')]" role="warning">
        The
        <name/>
        contains<value-of select="name(descendant::*[contains(@class, ' topic/image ')])"/>.
        Using
        <value-of select="name(descendant::*[contains(@class, ' topic/image ')])"/>
        in this context is ill-advised.
      </report>
      <!-- XXX: Can this actually ever happen? -->
      <report test="descendant::*[contains(@class, ' topic/object ')]" role="warning">
        The
        <name/>
        contains<value-of select="name(descendant::*[contains(@class, ' topic/object ')])"/>.
        Using
        <value-of select="name(descendant::*[contains(@class, ' topic/object ')])"/>
        in this context is ill-advised.
      </report>
      <report test="descendant::*[contains(@class, ' hi-d/sup ')]" role="warning">
        The
        <name/>
        contains<value-of select="name(descendant::*[contains(@class, ' hi-d/sup ')])"/>.
        Using
        <value-of select="name(descendant::*[contains(@class, ' hi-d/sup ')])"/>
        in this context is ill-advised.
      </report>
      <report test="descendant::*[contains(@class, ' hi-d/sub ')]" role="warning">
        The
        <name/>
        contains<value-of select="name(descendant::*[contains(@class, ' hi-d/sub ')])"/>.
        Using
        <value-of select="name(descendant::*[contains(@class, ' hi-d/sub ')])"/>
        in this context is ill-advised.
      </report>
    </rule>
  </pattern>
  <pattern id="abstract_shortdesc" e:phases="recommendation other">
    <rule context="*[contains(@class, ' topic/abstract ')]">
      <assert test="*[contains(@class, ' topic/shortdesc ')]" role="warning">
        Abstract should contain at least one shortdesc element.
      </assert>
    </rule>
  </pattern>
  <!-- Authoring -->
  <pattern id="xref_in_title" e:phases="author other">
    <rule context="*[contains(@class, ' topic/title ')]">
      <report test="descendant::*[contains(@class, ' topic/xref ')]"
              diagnostics="title_links"
              role="warning">
        The
        <name/>
        contains<name path="descendant::*[contains(@class, ' topic/xref ')]"/>.
      </report>
    </rule>
  </pattern>
  <!-- Deprecated -->
  <pattern id="idless_title">
    <rule context="*[not(@id)]">
      <report test="*[contains(@class, ' topic/title ')]"
              diagnostics="link_target"
              role="warning">
        The
        <name/>
        element with a title should have an id attribute.
      </report>
    </rule>
  </pattern>
  <pattern id="required-cleanup" e:phases="author other">
    <rule context="*">
      <report test="*[contains(@class, ' topic/required-cleanup ')]" role="warning">
        A required-cleanup element is used as a placeholder for migrated elements and should not be used in documents by
        authors.
      </report>
    </rule>
  </pattern>
  <pattern id="no_topic_nesting" e:phases="author other">
    <rule context="no-topic-nesting">
      <report test="." role="warning">
        The
        <name/>
        element is not intended for direct use by authors, and has no associated output
        processing.
      </report>
    </rule>
  </pattern>
  <pattern id="tm_character"
           see="http://docs.oasis-open.org/dita/v1.1/OS/langspec/langref/tm.html"
           e:phases="author other">
    <rule context="text()">
      <report test="contains(., '™')" role="warning">
        It's preferable to use tm element instead of ™ character.
      </report>
      <report test="contains(., '℠')" role="warning">
        It's preferable to use tm element instead of ℠ character.
      </report>
      <report test="contains(., '®')" role="warning">
        It's preferable to use tm element instead of ® character.
      </report>
    </rule>
  </pattern>
  <pattern id="multiple_section_titles" e:phases="author other">
    <!-- FIXME -->
    <rule context="*[contains(@class, ' topic/section ')]">
      <report test="count(*[contains(@class, ' topic/title ')]) &gt; 1" role="warning">
        The
        <name/>
        element should only contain one title element.
      </report>
    </rule>
  </pattern>
  <!-- WAI -->
  <pattern id="no_alt_desc"
           see="http://docs.oasis-open.org/dita/v1.1/OS/langspec/langref/image.html"
           e:phases="recommendation other">
    <rule context="*[contains(@class, ' topic/image ')]">
      <assert test="@alt | alt" flag="non-WAI" role="warning">
        Alternative description should be provided for users using screen readers or text-only readers.
      </assert>
    </rule>
    <rule context="*[contains(@class, ' topic/object ')]">
      <assert test="desc" flag="non-WAI" role="warning">
        Alternative description should be provided for users using screen readers or text-only readers.
      </assert>
    </rule>
  </pattern>
  <!--Report duplicate IDs start pattern-->
  <!--
  <pattern id="checkIDs" e:phases="mandatory recommendation">
    <rule context="*[@id]">
      <let name="k"
           value="concat(@id, '#', ancestor::*[contains(@class, ' topic/topic ')][1]/@id)"/>
      <let name="countKey" value="count(key('elementsByID', $k))"/>
      <report test="$countKey &gt; 1"
              see="http://docs.oasis-open.org/dita/v1.1/OS/archspec/id.html"
              role="error">
        The id attribute value "<value-of select="@id"/>" is not unique within the topic that contains it.
      </report>
    </rule>
  </pattern>
  -->
  <!--EXM-21448 Report duplicate IDs end pattern-->
  <!-- Diagnostics -->
  <diagnostics>
    <diagnostic id="external_scope_attribute">
      Use the scope="external" attribute to indicate external links.
    </diagnostic>
    <diagnostic id="navtitle_element">
      Preferred way to specify navigation title is navtitle element.
    </diagnostic>
    <diagnostic id="state_element">
      The state element should be used instead with value attribute of "yes" or "no".
    </diagnostic>
    <diagnostic id="alt_element">
      The alt element should be used instead.
    </diagnostic>
    <diagnostic id="longdescref_element">
      The longdescref element should be used instead.
    </diagnostic>
    <diagnostic id="link_target">
      Elements with titles are candidate targets for elements level links.
    </diagnostic>
    <diagnostic id="title_links">
      Using
      <value-of select="name(descendant::*[contains(@class, ' topic/xref ')])"/>
      in this context is ill-advised
      because titles in themselves are often used as links, e.g., in table of contents and cross-references.
    </diagnostic>
  </diagnostics>
  <phase id="mandatory_1.0">
    <active pattern="checkIDs"/>
    <active pattern="collection-type_on_rel"/>
    <active pattern="indextermref"/>
    <active pattern="keyref_attr"/>
    <active pattern="othernote"/>
    <active pattern="otherrole"/>
  </phase>
  <phase id="mandatory_1.1">
    <active pattern="checkIDs"/>
    <active pattern="collection-type_on_rel"/>
    <active pattern="indextermref"/>
    <active pattern="keyref_attr"/>
    <active pattern="othernote"/>
    <active pattern="otherrole"/>
  </phase>
  <phase id="mandatory_1.2">
    <active pattern="checkIDs"/>
    <active pattern="collection-type_on_rel"/>
    <active pattern="indextermref"/>
    <active pattern="othernote"/>
    <active pattern="otherrole"/>
  </phase>
  <phase id="mandatory_1.3">
    <active pattern="checkIDs"/>
    <active pattern="collection-type_on_rel"/>
    <active pattern="indextermref"/>
    <active pattern="othernote"/>
    <active pattern="otherrole"/>
  </phase>
  <phase id="recommendation_1.0">
    <active pattern="abstract_shortdesc"/>
    <active pattern="boolean"/>
    <active pattern="checkIDs"/>
    <active pattern="collection-type_on_rel"/>
    <active pattern="image_alt_attr"/>
    <active pattern="image_longdescref_attr"/>
    <active pattern="indextermref"/>
    <active pattern="keyref_attr"/>
    <active pattern="no_alt_desc"/>
    <active pattern="othernote"/>
    <active pattern="otherrole"/>
    <active pattern="pre_content"/>
    <active pattern="query_attr"/>
    <active pattern="role_attr_value"/>
    <active pattern="self_nested_xref"/>
    <active pattern="shortdesc_length"/>
    <active pattern="single_paragraph"/>
  </phase>
  <phase id="recommendation_1.1">
    <active pattern="abstract_shortdesc"/>
    <active pattern="boolean"/>
    <active pattern="checkIDs"/>
    <active pattern="collection-type_on_rel"/>
    <active pattern="image_alt_attr"/>
    <active pattern="image_longdescref_attr"/>
    <active pattern="indextermref"/>
    <active pattern="keyref_attr"/>
    <active pattern="map_title_attribute"/>
    <active pattern="no_alt_desc"/>
    <active pattern="othernote"/>
    <active pattern="otherrole"/>
    <active pattern="pre_content"/>
    <active pattern="query_attr"/>
    <active pattern="role_attr_value"/>
    <active pattern="self_nested_xref"/>
    <active pattern="shortdesc_length"/>
    <active pattern="single_paragraph"/>
    <active pattern="topichead_navtitle"/>
  </phase>
  <phase id="recommendation_1.2">
    <active pattern="abstract_shortdesc"/>
    <active pattern="boolean"/>
    <active pattern="checkIDs"/>
    <active pattern="collection-type_on_rel"/>
    <active pattern="image_alt_attr"/>
    <active pattern="image_longdescref_attr"/>
    <active pattern="indextermref"/>
    <active pattern="map_title_attribute"/>
    <active pattern="navtitle"/>
    <active pattern="no_alt_desc"/>
    <active pattern="othernote"/>
    <active pattern="otherrole"/>
    <active pattern="pre_content"/>
    <active pattern="query_attr"/>
    <active pattern="role_attr_value"/>
    <active pattern="self_nested_xref"/>
    <active pattern="shortdesc_length"/>
    <active pattern="single_paragraph"/>
    <active pattern="topichead_navtitle"/>
  </phase>
  <phase id="recommendation_1.3">
    <active pattern="abstract_shortdesc"/>
    <active pattern="boolean"/>
    <active pattern="checkIDs"/>
    <active pattern="collection-type_on_rel"/>
    <active pattern="image_alt_attr"/>
    <active pattern="image_longdescref_attr"/>
    <active pattern="indextermref"/>
    <active pattern="no_alt_desc"/>
    <active pattern="othernote"/>
    <active pattern="otherrole"/>
    <active pattern="pre_content"/>
    <active pattern="query_attr"/>
    <active pattern="role_attr_value"/>
    <active pattern="self_nested_xref"/>
    <active pattern="shortdesc_length"/>
    <active pattern="single_paragraph"/>
  </phase>
  <phase id="all_1.0">
    <active pattern="abstract_shortdesc"/>
    <active pattern="boolean"/>
    <active pattern="checkIDs"/>
    <active pattern="collection-type_on_rel"/>
    <active pattern="idless_title"/>
    <active pattern="image_alt_attr"/>
    <active pattern="image_longdescref_attr"/>
    <active pattern="indextermref"/>
    <active pattern="keyref_attr"/>
    <active pattern="multiple_section_titles"/>
    <active pattern="no_alt_desc"/>
    <active pattern="no_topic_nesting"/>
    <active pattern="othernote"/>
    <active pattern="otherrole"/>
    <active pattern="pre_content"/>
    <active pattern="query_attr"/>
    <active pattern="required-cleanup"/>
    <active pattern="role_attr_value"/>
    <active pattern="self_nested_xref"/>
    <active pattern="shortdesc_length"/>
    <active pattern="single_paragraph"/>
    <active pattern="tm_character"/>
    <active pattern="xref_in_title"/>
  </phase>
  <phase id="all_1.1">
    <active pattern="abstract_shortdesc"/>
    <active pattern="boolean"/>
    <active pattern="checkIDs"/>
    <active pattern="collection-type_on_rel"/>
    <active pattern="idless_title"/>
    <active pattern="image_alt_attr"/>
    <active pattern="image_longdescref_attr"/>
    <active pattern="indextermref"/>
    <active pattern="keyref_attr"/>
    <active pattern="map_title_attribute"/>
    <active pattern="multiple_section_titles"/>
    <active pattern="no_alt_desc"/>
    <active pattern="no_topic_nesting"/>
    <active pattern="othernote"/>
    <active pattern="otherrole"/>
    <active pattern="pre_content"/>
    <active pattern="query_attr"/>
    <active pattern="required-cleanup"/>
    <active pattern="role_attr_value"/>
    <active pattern="self_nested_xref"/>
    <active pattern="shortdesc_length"/>
    <active pattern="single_paragraph"/>
    <active pattern="tm_character"/>
    <active pattern="topichead_navtitle"/>
    <active pattern="xref_in_title"/>
  </phase>
  <phase id="all_1.2">
    <active pattern="abstract_shortdesc"/>
    <active pattern="boolean"/>
    <active pattern="checkIDs"/>
    <active pattern="collection-type_on_rel"/>
    <active pattern="idless_title"/>
    <active pattern="image_alt_attr"/>
    <active pattern="image_longdescref_attr"/>
    <active pattern="indextermref"/>
    <active pattern="map_title_attribute"/>
    <active pattern="multiple_section_titles"/>
    <active pattern="navtitle"/>
    <active pattern="no_alt_desc"/>
    <active pattern="no_topic_nesting"/>
    <active pattern="othernote"/>
    <active pattern="otherrole"/>
    <active pattern="pre_content"/>
    <active pattern="query_attr"/>
    <active pattern="required-cleanup"/>
    <active pattern="role_attr_value"/>
    <active pattern="self_nested_xref"/>
    <active pattern="shortdesc_length"/>
    <active pattern="single_paragraph"/>
    <active pattern="tm_character"/>
    <active pattern="topichead_navtitle"/>
    <active pattern="xref_in_title"/>
  </phase>
  <phase id="all_1.3">
    <active pattern="abstract_shortdesc"/>
    <active pattern="boolean"/>
    <active pattern="checkIDs"/>
    <active pattern="collection-type_on_rel"/>
    <active pattern="idless_title"/>
    <active pattern="image_alt_attr"/>
    <active pattern="image_longdescref_attr"/>
    <active pattern="indextermref"/>
    <active pattern="multiple_section_titles"/>
    <active pattern="no_alt_desc"/>
    <active pattern="no_topic_nesting"/>
    <active pattern="othernote"/>
    <active pattern="otherrole"/>
    <active pattern="pre_content"/>
    <active pattern="query_attr"/>
    <active pattern="required-cleanup"/>
    <active pattern="role_attr_value"/>
    <active pattern="self_nested_xref"/>
    <active pattern="shortdesc_length"/>
    <active pattern="single_paragraph"/>
    <active pattern="tm_character"/>
    <active pattern="xref_in_title"/>
  </phase>
  <phase id="other_1.0">
    <active pattern="abstract_shortdesc"/>
    <active pattern="boolean"/>
    <active pattern="image_alt_attr"/>
    <active pattern="image_longdescref_attr"/>
    <active pattern="multiple_section_titles"/>
    <active pattern="no_alt_desc"/>
    <active pattern="no_topic_nesting"/>
    <active pattern="pre_content"/>
    <active pattern="query_attr"/>
    <active pattern="required-cleanup"/>
    <active pattern="role_attr_value"/>
    <active pattern="self_nested_xref"/>
    <active pattern="shortdesc_length"/>
    <active pattern="single_paragraph"/>
    <active pattern="tm_character"/>
    <active pattern="xref_in_title"/>
  </phase>
  <phase id="other_1.1">
    <active pattern="abstract_shortdesc"/>
    <active pattern="boolean"/>
    <active pattern="image_alt_attr"/>
    <active pattern="image_longdescref_attr"/>
    <active pattern="map_title_attribute"/>
    <active pattern="multiple_section_titles"/>
    <active pattern="no_alt_desc"/>
    <active pattern="no_topic_nesting"/>
    <active pattern="pre_content"/>
    <active pattern="query_attr"/>
    <active pattern="required-cleanup"/>
    <active pattern="role_attr_value"/>
    <active pattern="self_nested_xref"/>
    <active pattern="shortdesc_length"/>
    <active pattern="single_paragraph"/>
    <active pattern="tm_character"/>
    <active pattern="topichead_navtitle"/>
    <active pattern="xref_in_title"/>
  </phase>
  <phase id="other_1.2">
    <active pattern="abstract_shortdesc"/>
    <active pattern="boolean"/>
    <active pattern="image_alt_attr"/>
    <active pattern="image_longdescref_attr"/>
    <active pattern="map_title_attribute"/>
    <active pattern="multiple_section_titles"/>
    <active pattern="navtitle"/>
    <active pattern="no_alt_desc"/>
    <active pattern="no_topic_nesting"/>
    <active pattern="pre_content"/>
    <active pattern="query_attr"/>
    <active pattern="required-cleanup"/>
    <active pattern="role_attr_value"/>
    <active pattern="self_nested_xref"/>
    <active pattern="shortdesc_length"/>
    <active pattern="single_paragraph"/>
    <active pattern="tm_character"/>
    <active pattern="topichead_navtitle"/>
    <active pattern="xref_in_title"/>
  </phase>
  <phase id="other_1.3">
    <active pattern="abstract_shortdesc"/>
    <active pattern="boolean"/>
    <active pattern="image_alt_attr"/>
    <active pattern="image_longdescref_attr"/>
    <active pattern="multiple_section_titles"/>
    <active pattern="no_alt_desc"/>
    <active pattern="no_topic_nesting"/>
    <active pattern="pre_content"/>
    <active pattern="query_attr"/>
    <active pattern="required-cleanup"/>
    <active pattern="role_attr_value"/>
    <active pattern="self_nested_xref"/>
    <active pattern="shortdesc_length"/>
    <active pattern="single_paragraph"/>
    <active pattern="tm_character"/>
    <active pattern="xref_in_title"/>
  </phase>
  <phase id="author_1.0">
    <active pattern="multiple_section_titles"/>
    <active pattern="no_topic_nesting"/>
    <active pattern="required-cleanup"/>
    <active pattern="tm_character"/>
    <active pattern="xref_in_title"/>
  </phase>
  <phase id="author_1.1">
    <active pattern="multiple_section_titles"/>
    <active pattern="no_topic_nesting"/>
    <active pattern="required-cleanup"/>
    <active pattern="tm_character"/>
    <active pattern="xref_in_title"/>
  </phase>
  <phase id="author_1.2">
    <active pattern="multiple_section_titles"/>
    <active pattern="no_topic_nesting"/>
    <active pattern="required-cleanup"/>
    <active pattern="tm_character"/>
    <active pattern="xref_in_title"/>
  </phase>
  <phase id="author_1.3">
    <active pattern="multiple_section_titles"/>
    <active pattern="no_topic_nesting"/>
    <active pattern="required-cleanup"/>
    <active pattern="tm_character"/>
    <active pattern="xref_in_title"/>
  </phase>
</schema>
