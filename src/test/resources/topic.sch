<?xml version="1.0" encoding="UTF-8"?>
<sch:schema xmlns:sch="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2"
    xmlns:loc="loc:"
    xmlns:sqf="http://www.schematron-quickfix.com/validator/process">
    <sch:pattern>
        <sch:rule context="pre" role="warn" subject="@loc:elem" >
            <sch:assert test="false()">Error message.</sch:assert>
        </sch:rule>
    </sch:pattern>
</sch:schema>