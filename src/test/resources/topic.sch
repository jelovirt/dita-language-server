<?xml version="1.0" encoding="UTF-8"?>
<sch:schema xmlns:sch="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2" xmlns:loc="loc:"
    xmlns:sqf="http://www.schematron-quickfix.com/validator/process">
    <sch:ns uri="http://dita.oasis-open.org/architecture/2005/" prefix="ditaarch"/>
    <sch:pattern>
        <sch:rule context="body" role="warn">
            <sch:assert test="false()">Error message pre.</sch:assert>
        </sch:rule>
        <sch:rule context="@id" role="warn">
            <sch:assert test="false()">Error message @id.</sch:assert>
        </sch:rule>
        <sch:rule context="@ditaarch:DITAArchVersion" role="warn">
            <sch:assert test="false()">Error message ditaarch:DITAArchVersion.</sch:assert>
        </sch:rule>
    </sch:pattern>
</sch:schema>
