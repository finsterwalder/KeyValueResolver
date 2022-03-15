/*
 * Roperty - An advanced property management and retrival system
 * Copyright (C) 2013 PARSHIP GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nitrobox.keyvalueresolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;


/**
 * A collection of domain specifically overridden values for a single key. The different DomainSpecificValues are queried according to their
 * ordering and changeSet.
 *
 * @author finsterwalder
 * @see DomainSpecificValue
 * @since 2013-03-26 09:18
 */
public class KeyValues {

    private static final String DOMAIN_SEPARATOR = "|";
    private final String key;
    private String description;
    private final Set<DomainSpecificValue> domainSpecificValues = new ConcurrentSkipListSet<>();
    private DomainSpecificValueFactory domainSpecificValueFactory;

    public KeyValues(String key, final DomainSpecificValueFactory domainSpecificValueFactory) {
        this(key, domainSpecificValueFactory, null);
    }

    public KeyValues(String key, final DomainSpecificValueFactory domainSpecificValueFactory, String description) {
        this.key = key;
        this.domainSpecificValueFactory = domainSpecificValueFactory;
        this.description = description;
    }

    public DomainSpecificValue put(Object value, List<String> domainValues) {
        return putWithChangeSet(null, value, domainValues);
    }

    public DomainSpecificValue put(Object value, String... domainValues) {
        return putWithChangeSet(null, value, domainValues);
    }

    public DomainSpecificValue putWithChangeSet(final String changeSet, final Object value, final List<String> domainValues) {
        return putWithChangeSet(changeSet, value, domainValues.toArray(new String[domainValues.size()]));
    }

    public DomainSpecificValue putWithChangeSet(final String changeSet, final Object value, final String... domainValues) {
        Objects.requireNonNull(domainValues, "Domain key parts may no be null");
        for (int i = 0; i < domainValues.length; i++) {
            if (domainValues[i] == null || domainValues[i].trim().length() == 0) {
                domainValues[i] = "*";
            }
        }
        return addOrChangeDomainSpecificValue(changeSet, value, domainValues);
    }

    private DomainSpecificValue addOrChangeDomainSpecificValue(final String changeSet, final Object value, final String... domainValues) {
        DomainSpecificValue domainSpecificValue = domainSpecificValueFactory.create(value, changeSet, domainValues);
        if (domainSpecificValues.contains(domainSpecificValue)) {
            domainSpecificValues.stream()
                .filter(dsv -> dsv.compareTo(domainSpecificValue) == 0)
                .forEach(dsv -> dsv.setValue(domainSpecificValue.getValue()));
        } else {
            domainSpecificValues.add(domainSpecificValue);
        }
        return domainSpecificValue;
    }

    public <T> T get(Iterable<String> domains, T defaultValue, final DomainResolver resolver) {
        Objects.requireNonNull(domains, "\"domains\" must not be null");
        Iterator<String> domainsIterator = domains.iterator();
        if (domainsIterator.hasNext() && resolver == null) {
            throw new IllegalArgumentException("If a domain is specified, the domain resolver must not be null");
        }
        String domainStr = buildDomain(domains, resolver);
        for (DomainSpecificValue domainSpecificValue : domainSpecificValues) {
            if ((resolver == null || domainSpecificValue.isInChangeSets(resolver.getActiveChangeSets()))
                && domainSpecificValue.patternMatches(domainStr)) {
                return (T) domainSpecificValue.getValue();
            }
        }
        return defaultValue;
    }

    private static String buildDomain(final Iterable<String> domains, final DomainResolver resolver) {
        StringBuilder builder = new StringBuilder();
        for (String domain : domains) {
            String domainValue = resolver.getDomainValue(domain);
            if (domainValue == null) {
                domainValue = "";
            }
            if (domainValue.contains(DOMAIN_SEPARATOR)) {
                throw new IllegalArgumentException("domainValues may not contain '" + DOMAIN_SEPARATOR + '\'');
            }
            builder.append(domainValue).append(DOMAIN_SEPARATOR);
        }
        return builder.toString();
    }

    public String getDescription() {
        return description == null ? "" : description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("KeyValues{\n\tdescription=\"");
        builder.append(getDescription()).append("\"\n");
        for (DomainSpecificValue entry : domainSpecificValues) {
            builder.append('\t').append(entry).append('\n');
        }
        builder.append('}');
        return builder.toString();
    }

    public Set<DomainSpecificValue> getDomainSpecificValues() {
        return Collections.unmodifiableSet(domainSpecificValues);
    }

    public void setDomainSpecificValues(Collection<DomainSpecificValue> values) {
        domainSpecificValues.clear();
        domainSpecificValues.addAll(values);
    }

    public void setDomainSpecificValueFactory(final DomainSpecificValueFactory domainSpecificValueFactory) {
        this.domainSpecificValueFactory = domainSpecificValueFactory;
    }

    public <T> T getDefaultValue() {
        return (T) domainSpecificValues.stream()
            .filter(DomainSpecificValue::isDefault)
            .map(DomainSpecificValue::getValue)
            .findAny()
            .orElse(null);
    }

    public DomainSpecificValue remove(final String changeSet, final String[] domainValues) {
        StringBuilder builder = new StringBuilder(domainValues.length * 8);
        for (String domainValue : domainValues) {
            builder.append(domainValue).append(DOMAIN_SEPARATOR);
        }
        final String pattern = builder.toString();

        Iterator<DomainSpecificValue> iterator = domainSpecificValues.iterator();
        while (iterator.hasNext()) {
            DomainSpecificValue value = iterator.next();
            if (value.changeSetIs(changeSet) && pattern.equals(value.getPattern())) {
                iterator.remove();
                return value;
            }
        }
        return null;
    }

    public Collection<DomainSpecificValue> removeChangeSet(final String changeSet) {
        Collection<DomainSpecificValue> removedValues = new ArrayList<>(domainSpecificValues.size());
        Iterator<DomainSpecificValue> iterator = domainSpecificValues.iterator();
        while (iterator.hasNext()) {
            DomainSpecificValue value = iterator.next();
            if (value.changeSetIs(changeSet)) {
                removedValues.add(value);
                iterator.remove();
            }
        }
        return removedValues;
    }

    public String getKey() {
        return key;
    }

    public KeyValues copy(List<String> domains, DomainResolver resolver) {
        KeyValues result = new KeyValues(key, domainSpecificValueFactory, description);
        result.domainSpecificValues.addAll(findMatchingValues(domains, resolver));
        return result;
    }

    private Collection<DomainSpecificValue> findMatchingValues(List<String> domains, DomainResolver resolver) {
        Matcher matcher = buildMatcher(domains, resolver);

        final Map<String, DomainSpecificValue> dvPatternMap = new HashMap<>();
        domainSpecificValues.stream()
            .filter(val -> val.patternMatches(matcher, resolver))
            .forEach(newDomainValue -> dvPatternMap.compute(newDomainValue.getPattern(), (k, existingDomainValue) -> {
                if (existingDomainValue == null) {
                    return newDomainValue;
                }
                if (existingDomainValue.noChangeSet()) {
                    return newDomainValue;
                }
                if (newDomainValue.noChangeSet()) {
                    return existingDomainValue;
                }
                return existingDomainValue.compareChangeSet(newDomainValue) < 0 ? existingDomainValue : newDomainValue;
            }));
        return dvPatternMap.values();
    }

    private static RegexMatcher buildMatcher(final Iterable<String> domains, final DomainResolver resolver) {
        StringBuilder builder = new StringBuilder();
        for (String domain : domains) {
            String domainValue = resolver.getDomainValue(domain);
            if (domainValue == null || domainValue.equals("*")) {
                domainValue = "[^|]*";
            } else if (domainValue.contains(DOMAIN_SEPARATOR)) {
                throw new IllegalArgumentException("domainValues may not contain '" + DOMAIN_SEPARATOR + '\'');
            }
            builder.append(domainValue).append("\\|");
        }
        while (builder.length() >= 7 && builder.lastIndexOf("[^|]*\\|") == builder.length() - 7) {
            builder.delete(builder.length() - 7, builder.length());
        }
        builder.append(".*");
        return new RegexMatcher(builder.toString());
    }

    public void removeAll(List<String> domains, DomainResolver resolver) {
        this.domainSpecificValues.removeAll(findMatchingValues(domains, resolver));
    }
}
