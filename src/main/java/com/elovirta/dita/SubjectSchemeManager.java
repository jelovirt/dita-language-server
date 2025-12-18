package com.elovirta.dita;

import static com.elovirta.dita.Utils.*;
import static net.sf.saxon.s9api.streams.Steps.*;

import java.net.URI;
import java.util.*;
import java.util.function.Predicate;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.streams.Steps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubjectSchemeManager {
  private static final Logger logger = LoggerFactory.getLogger(SubjectSchemeManager.class);

  private static final String KEYS_ATTR = "keys";
  private static final String KEYREF_ATTR = "keyref";
  private static final String ATTRIBUTE_NAME_NAME = "name";
  private static final QName ATTRIBUTE_QNAME_KEYREF = QName.fromClarkName(KEYREF_ATTR);
  private static final QName ATTRIBUTE_QNAME_KEYS = QName.fromClarkName(KEYS_ATTR);
  private static final QName ATTRIBUTE_QNAME_NAME = QName.fromClarkName(ATTRIBUTE_NAME_NAME);
  private static final String ANY_ELEMENT = "*";

  private volatile Map<String, SubjectDefinition> subjectDefinitions = Collections.emptyMap();
  private final Map<QName, Map<String, Set<SubjectDefinition>>> bindingMap = new HashMap<>();
  private final Map<QName, Map<String, Set<String>>> validValuesMap = new HashMap<>();
  private final Map<QName, Map<String, String>> defaultValueMap = new HashMap<>();

  public void read(URI uri, XdmNode map) {
    logger.info("Read subject scheme definitions {}", uri);
    subjectDefinitions = getSubjectDefinition(map);

    map.select(descendant(SUBJECTSCHEME_ENUMERATIONDEF))
        .forEach(
            enumerationDef -> {
              processEnumerationDef(subjectDefinitions, enumerationDef);
            });
    logger.info("subjectDefinitions: " + subjectDefinitions);
    logger.info("bindingMap: " + bindingMap);
    logger.info("validValuesMap: " + validValuesMap);
    logger.info("defaultValueMap: " + defaultValueMap);
  }

  public void processEnumerationDef(
      final Map<String, SubjectDefinition> subjectDefinitions, final XdmNode enumerationDef) {
    final String elementName =
        enumerationDef
            .select(
                Steps.child(SUBJECTSCHEME_ELEMENTDEF)
                    .then(
                        Steps.attribute(ATTRIBUTE_NAME_NAME)
                            .where(Predicate.not(isEmptyAttribute()))))
            .findFirst()
            .map(XdmItem::getStringValue)
            .orElse(ANY_ELEMENT);

    final Optional<XdmNode> attributeDefElement =
        enumerationDef.select(Steps.child(SUBJECTSCHEME_ATTRIBUTEDEF).first()).findFirst();
    final QName attributeName =
        attributeDefElement
            .map(child -> child.getAttributeValue(ATTRIBUTE_QNAME_NAME))
            .filter(name -> name != null && !name.isEmpty())
            .map(QName::fromClarkName)
            .orElse(null);
    if (attributeDefElement.isPresent()) {
      bindingMap.computeIfAbsent(attributeName, k -> new HashMap<>());
    }

    enumerationDef
        .select(Steps.child(SUBJECTSCHEME_DEFAULTSUBJECT))
        .map(child -> child.getAttributeValue(ATTRIBUTE_QNAME_KEYREF))
        .filter(keyref -> keyref != null && !keyref.isEmpty())
        .findFirst()
        .ifPresent(
            keyValue -> {
              final Map<String, String> S =
                  defaultValueMap.getOrDefault(attributeName, new HashMap<>());
              S.put(elementName, keyValue);
              defaultValueMap.put(attributeName, S);
            });

    for (XdmNode child : enumerationDef.children(SUBJECTSCHEME_SUBJECTDEF)) {
      final List<String> keyValues =
          Optional.ofNullable(child.getAttributeValue(ATTRIBUTE_QNAME_KEYREF))
              .filter(Predicate.not(String::isBlank))
              .or(() -> Optional.ofNullable(child.getAttributeValue(ATTRIBUTE_QNAME_KEYS)))
              .map(String::trim)
              .filter(Predicate.not(String::isEmpty))
              .map(value -> Arrays.asList(value.split("\\s+")))
              .orElse(List.of());
      if (!subjectDefinitions.isEmpty() && !keyValues.isEmpty()) {
        for (String keyValue : keyValues) {
          final SubjectDefinition subTree = subjectDefinitions.get(keyValue);
          if (subTree != null) {
            final Map<String, Set<SubjectDefinition>> S =
                bindingMap.getOrDefault(attributeName, new HashMap<>());
            final Set<SubjectDefinition> A = S.getOrDefault(elementName, new HashSet<>());
            if (!A.contains(subTree)) {
              if (attributeName != null) {
                putValuePairsIntoMap(subTree, elementName, attributeName, keyValue);
              }
            }
            A.add(subTree);
            S.put(elementName, A);
            bindingMap.put(attributeName, S);
          }
        }
      }
    }
  }

  /**
   * Populate valid values map
   *
   * @param subtree subject scheme definition element
   * @param elementName element name
   * @param attName attribute name
   * @param category enumeration category name
   */
  private void putValuePairsIntoMap(
      final SubjectDefinition subtree,
      final String elementName,
      final QName attName,
      final String category) {
    final Map<String, Set<String>> valueMap = validValuesMap.getOrDefault(attName, new HashMap<>());
    final Set<String> valueSet = valueMap.getOrDefault(elementName, new HashSet<>());
    subtree.flatten().stream()
        .flatMap(child -> child.keys().stream())
        .filter(key -> !key.equals(category))
        .forEach(valueSet::add);
    valueMap.put(elementName, valueSet);
    validValuesMap.put(attName, valueMap);
  }

  public Map<String, SubjectDefinition> getSubjectDefinition(final XdmNode schemeRoot) {
    final List<SubjectDefinition> subjectDefinitions = readSubjectDefinitions(schemeRoot);
    final Map<String, SubjectDefinition> buf = new HashMap<>();
    getSubjectDefinition(subjectDefinitions, buf);
    return Collections.unmodifiableMap(buf);
  }

  private void getSubjectDefinition(
      List<SubjectDefinition> subjectDefinitions, Map<String, SubjectDefinition> buf) {
    for (SubjectDefinition subjectDefinition : subjectDefinitions) {
      for (String key : subjectDefinition.keys()) {
        buf.putIfAbsent(key, subjectDefinition);
      }
      getSubjectDefinition(subjectDefinition.children(), buf);
    }
  }

  private List<SubjectDefinition> readSubjectDefinitions(final XdmNode elem) {
    final List<SubjectDefinition> res = new ArrayList<>();
    readSubjectDefinitions(elem, res);
    return Collections.unmodifiableList(res);
  }

  private void readSubjectDefinitions(final XdmNode elem, List<SubjectDefinition> buf) {
    for (XdmNode child : elem.children()) {
      if (SUBJECTSCHEME_SUBJECTDEF.test(child)) {
        //        String keyref = child.getAttributeValue(ATTRIBUTE_QNAME_KEYREF);
        //        if (keyref == null || keyref.isEmpty()) {
        //          keyref = null;
        //        }
        final List<SubjectDefinition> childBuf = new ArrayList<>();
        readSubjectDefinitions(child, childBuf);
        final SubjectDefinition res =
            new SubjectDefinition(Set.copyOf(getKeyValues(child)), child, null, childBuf);
        buf.add(res);
      } else {
        readSubjectDefinitions(child, buf);
      }
    }
  }

  private static Set<String> getKeyValues(XdmNode child) {
    var value = child.getAttributeValue(ATTRIBUTE_QNAME_KEYS);
    if (value == null) {
      return Set.of();
    }
    value = value.trim();
    if (value.isEmpty()) {
      return Set.of();
    }
    return Set.of(value.split("\\s+"));
  }

  public Set<String> values(QName attributeName, String elementName) {
    var elements = validValuesMap.getOrDefault(attributeName, Collections.emptyMap());
    logger.info("elements: " + elements);
    return elements.getOrDefault(
        elementName, elements.getOrDefault(ANY_ELEMENT, Collections.emptySet()));
  }

  public boolean containsKey(String key) {
    return subjectDefinitions.containsKey(key);
  }

  public Set<Map.Entry<String, SubjectDefinition>> keys() {
    return subjectDefinitions.entrySet();
  }

  public record SubjectDefinition(
      //          URI mapUri,
      Set<String> keys, XdmNode definition, String navtitle, List<SubjectDefinition> children) {
    //    public Location location() {
    //      return new Location(
    //          mapUri().toString(),
    //          Utils.parseRange(this.definition().getAttributeValue(new QName(LOC_NAMESPACE,
    // "elem"))));
    //    }
    public List<SubjectDefinition> flatten() {
      final List<SubjectDefinition> res = new ArrayList<>();
      flatten(this, res);
      return res;
    }

    private void flatten(SubjectDefinition def, final List<SubjectDefinition> buf) {
      buf.add(def);
      for (SubjectDefinition child : def.children) {
        flatten(child, buf);
      }
    }
  }
}
