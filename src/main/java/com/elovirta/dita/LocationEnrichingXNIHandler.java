package com.elovirta.dita;

import java.util.Objects;
import javax.xml.XMLConstants;
import org.apache.xerces.impl.Constants;
import org.apache.xerces.xni.*;
import org.apache.xerces.xni.parser.*;
import org.xml.sax.*;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.ext.Locator2Impl;
import org.xml.sax.helpers.AttributesImpl;

public class LocationEnrichingXNIHandler implements XMLDocumentHandler {

  public static final String LOC_NAMESPACE = "http://www.elovirta.com/dita/location";
  public static final String LOC_PREFIX = "loc";

  private final ContentHandler contentHandler;
  private XMLLocator locator;
  private Locator2Impl saxLocator;

  public LocationEnrichingXNIHandler(ContentHandler contentHandler) {
    this.contentHandler = contentHandler;
    this.saxLocator = new Locator2Impl();
  }

  @Override
  public void startDocument(
      XMLLocator locator, String encoding, NamespaceContext namespaceContext, Augmentations augs)
      throws XNIException {
    this.locator = locator;

    try {
      contentHandler.setDocumentLocator(saxLocator);
      contentHandler.startDocument();
      // Declare the location namespace
      contentHandler.startPrefixMapping(LOC_PREFIX, LOC_NAMESPACE);
    } catch (SAXException e) {
      throw new XNIException(e);
    }
  }

  @Override
  public void startElement(QName element, XMLAttributes attributes, Augmentations augs)
      throws XNIException {
    try {
      // Update SAX locator with current position
      if (locator != null) {
        saxLocator.setLineNumber(locator.getLineNumber());
        saxLocator.setColumnNumber(locator.getColumnNumber());
      }

      // Create enriched attributes with location info
      AttributesImpl enrichedAttrs = new AttributesImpl();

      // Add location for the element itself
      if (locator != null) {
        String elemLocation = locator.getLineNumber() + ":" + locator.getColumnNumber();
        enrichedAttrs.addAttribute(
            LOC_NAMESPACE, "elem", LOC_PREFIX + ":elem", "CDATA", elemLocation);
      }

      // Add original attributes
      for (int i = 0; i < attributes.getLength(); i++) {
        String uri = Objects.requireNonNullElse(attributes.getURI(i), XMLConstants.NULL_NS_URI);
        String localName = attributes.getLocalName(i);
        String qName = attributes.getQName(i);
        String type = attributes.getType(i);
        String value = attributes.getValue(i);

        enrichedAttrs.addAttribute(uri, localName, qName, type, value);

        // Try to get location info for this attribute
        String location = getAttributeLocation(attributes, i, augs);

        if (location != null) {
          String locAttrName = LOC_PREFIX + ":attr-" + localName;
          enrichedAttrs.addAttribute(
              LOC_NAMESPACE, "attr-" + localName, locAttrName, "CDATA", location);
        }
      }

      // Fire SAX event with enriched attributes
      String uri = Objects.requireNonNullElse(element.uri, XMLConstants.NULL_NS_URI);
      String localName = element.localpart;
      String qName = element.rawname;
      contentHandler.startElement(uri, localName, qName, enrichedAttrs);
    } catch (SAXException e) {
      throw new XNIException(e);
    }
  }

  @Override
  public void endElement(QName element, Augmentations augs) throws XNIException {
    try {
      String uri = Objects.requireNonNullElse(element.uri, XMLConstants.NULL_NS_URI);
      String localName = element.localpart;
      String qName = element.rawname;

      contentHandler.endElement(uri, localName, qName);
    } catch (SAXException e) {
      throw new XNIException(e);
    }
  }

  @Override
  public void characters(XMLString text, Augmentations augs) throws XNIException {
    try {
      contentHandler.characters(text.ch, text.offset, text.length);
    } catch (SAXException e) {
      throw new XNIException(e);
    }
  }

  @Override
  public void endDocument(Augmentations augs) throws XNIException {
    try {
      contentHandler.endPrefixMapping(LOC_PREFIX);
      contentHandler.endDocument();
    } catch (SAXException e) {
      throw new XNIException(e);
    }
  }

  /**
   * Extract location information for a specific attribute. This is tricky because Xerces doesn't
   * always provide per-attribute locations.
   */
  private String getAttributeLocation(
      XMLAttributes attributes, int index, Augmentations elementAugs) {
    // Try to get attribute-specific augmentations
    Augmentations attrAugs = attributes.getAugmentations(index);

    if (attrAugs != null) {
      // Look for location info in augmentations
      // The key might vary depending on Xerces version
      Object locInfo = attrAugs.getItem(Constants.ATTRIBUTE_DECLARED);

      // You may need to explore what's actually in the augmentations
      // This is where it gets implementation-specific

      // For now, we'll fall back to element location
    }

    // Fallback: use element location (not perfect but better than nothing)
    if (locator != null) {
      return locator.getLineNumber() + ":" + locator.getColumnNumber();
    }

    return null;
  }

  // Minimal implementations of other required methods

  @Override
  public void xmlDecl(String version, String encoding, String standalone, Augmentations augs)
      throws XNIException {
    // Optional: handle XML declaration
  }

  @Override
  public void doctypeDecl(String rootElement, String publicId, String systemId, Augmentations augs)
      throws XNIException {
    // Optional: handle DOCTYPE
  }

  @Override
  public void comment(XMLString text, Augmentations augs) throws XNIException {
    // Optional: handle comments
    if (contentHandler instanceof LexicalHandler) {
      try {
        ((LexicalHandler) contentHandler).comment(text.ch, text.offset, text.length);
      } catch (SAXException e) {
        throw new XNIException(e);
      }
    }
  }

  @Override
  public void processingInstruction(String target, XMLString data, Augmentations augs)
      throws XNIException {
    try {
      contentHandler.processingInstruction(target, data.toString());
    } catch (SAXException e) {
      throw new XNIException(e);
    }
  }

  @Override
  public void ignorableWhitespace(XMLString text, Augmentations augs) throws XNIException {
    try {
      contentHandler.ignorableWhitespace(text.ch, text.offset, text.length);
    } catch (SAXException e) {
      throw new XNIException(e);
    }
  }

  @Override
  public void startGeneralEntity(
      String name, XMLResourceIdentifier identifier, String encoding, Augmentations augs)
      throws XNIException {
    // Entity handling if needed
  }

  @Override
  public void endGeneralEntity(String name, Augmentations augs) throws XNIException {
    // Entity handling if needed
  }

  @Override
  public void startCDATA(Augmentations augs) throws XNIException {
    if (contentHandler instanceof LexicalHandler) {
      try {
        ((LexicalHandler) contentHandler).startCDATA();
      } catch (SAXException e) {
        throw new XNIException(e);
      }
    }
  }

  @Override
  public void endCDATA(Augmentations augs) throws XNIException {
    if (contentHandler instanceof LexicalHandler) {
      try {
        ((LexicalHandler) contentHandler).endCDATA();
      } catch (SAXException e) {
        throw new XNIException(e);
      }
    }
  }

  @Override
  public void textDecl(String version, String encoding, Augmentations augs) throws XNIException {
    // Text declaration in external entities
  }

  @Override
  public void emptyElement(QName element, XMLAttributes attributes, Augmentations augs)
      throws XNIException {
    // Empty elements - treat as start + end
    startElement(element, attributes, augs);
    endElement(element, augs);
  }

  @Override
  public void setDocumentSource(XMLDocumentSource source) {
    // Chain setup if needed
  }

  @Override
  public XMLDocumentSource getDocumentSource() {
    return null;
  }
}
