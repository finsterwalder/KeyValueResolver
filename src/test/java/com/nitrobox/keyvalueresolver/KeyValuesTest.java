/*
 * KeyValueResolver - An dynamic Key-Value Store
 * Copyright (C) 2022 Nitrobox GmbH
 *
 * This Software is a fork of Roperty - An advanced property 
 * management and retrival system
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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @author finsterwalder
 * @since 2013-04-02 22:32
 */
class KeyValuesTest {

    private final KeyValues keyValues = new KeyValues("key", new DefaultDomainSpecificValueFactory(), null);

    private DomainResolver resolver = new DomainResolver() {
        @Override
        public String getDomainValue(final String domain) {
            return domain;
        }

        @Override
        public Collection<String> getActiveChangeSets() {
            return new ArrayList<>();
        }
    };

    @Test
    void descriptionIsNeverNullButIsTheEmptyString() {
        KeyValues keyValues = new KeyValues("key", new DefaultDomainSpecificValueFactory(), null);
        assertThat(keyValues.getDescription()).isEmpty();
    }

    @Test
    void domainSpecificValuesCanBeInitialized() {
        KeyValues keyValues = new KeyValues("key", new DefaultDomainSpecificValueFactory(), null, List.of(
                DomainSpecificValue.withoutChangeSet("value", "dom1"),
                DomainSpecificValue.withoutChangeSet("value", "dom1", "dom2")));
        assertThat(keyValues.getDomainSpecificValues()).hasSize(2);
    }
    
    @Test
    void toStringEmpty() {
        assertThat(keyValues).hasToString("KeyValues{\n\tdescription=\"\"\n" +
                "}");
    }

    @Test
    void toStringFilled() {
        keyValues.setDescription("description");
        keyValues.put("text", "domain1", "domain2");
        assertThat(keyValues).hasToString("KeyValues{\n" +
                "\tdescription=\"description\"\n" +
                "\tDomainSpecificValue{pattern=\"domain1|domain2|\", ordering=7, value=\"text\"}\n" +
                "}");
    }

    @Test
    void gettingFromAnEmptyKeyValuesGivesNull() {
        assertThat((String) keyValues.get(singletonList("dom1"), null, resolver)).isNull();
    }

    @Test
    void whenNoPatternMatchesTheDefaultValueIsReturned() {
        keyValues.put("value", "domain");
        assertThat(keyValues.get(singletonList("x1"), "default", resolver)).isEqualTo("default");
    }

    @Test
    void whenNoPatternMatchesButADefaultIsStoredItIsReturnedAndNotTheProvidedDefault() {
        keyValues.put("text");
        assertThat(keyValues.get(singletonList("x1"), "default", resolver)).isEqualTo("text");
    }

    @Test
    void whenAPatternMatchesItIsReturnedAndNotTheDefault() {
        keyValues.put("def");
        keyValues.put("text", "domain");
        assertThat(keyValues.get(singletonList("domain"), "default", resolver)).isEqualTo("text");
    }

    @Test
    void whenNoValuesAreDefinedGettingAllDomainSpecificValuesGivesAnEmptySet() {
        Set<DomainSpecificValue> domainSpecificValues = keyValues.getDomainSpecificValues();
        assertThat(domainSpecificValues).isEmpty();
    }

    @Test
    void gettingAllDomainSpecificValuesGivesSetInLongestMatchFirstOrder() {
        keyValues.put("value1", "dom1");
        keyValues.put("value2", "dom1", "dom2");
        keyValues.put("value*", "*", "dom2");
        Set<DomainSpecificValue> domainSpecificValues = keyValues.getDomainSpecificValues();
        assertThat(domainSpecificValues).hasSize(3);
        Iterator<DomainSpecificValue> iterator = domainSpecificValues.iterator();
        DomainSpecificValue value = iterator.next();
        assertThat((String) value.getValue()).isEqualTo("value2");
        assertThat(value.getPattern()).isEqualTo("dom1|dom2|");
        value = iterator.next();
        assertThat((String) value.getValue()).isEqualTo("value*");
        assertThat(value.getPattern()).isEqualTo("*|dom2|");
        value = iterator.next();
        assertThat((String) value.getValue()).isEqualTo("value1");
        assertThat(value.getPattern()).isEqualTo("dom1|");
    }

    @Test
    void wildcardsAlsoMatchNullDomainValues() {
        DomainResolver resolverMock = mock(DomainResolver.class);
        when(resolverMock.getDomainValue("dom1")).thenReturn(null);
        when(resolverMock.getDomainValue("dom2")).thenReturn(null);
        when(resolverMock.getDomainValue("dom3")).thenReturn("domVal3");
        keyValues.put("value", "*", "*", "domVal3");
        assertThat(keyValues.get(asList("dom1", "dom2", "dom3"), "default", resolverMock)).isEqualTo("value");
    }

    @Test
    void callingGetWithAnEmptyDomainListDoesNotUseTheResolver() {
        assertThat(keyValues.<String>get(emptyList(), null, null)).isNull();
        keyValues.put("val");
        assertThat((String) keyValues.get(emptyList(), null, null)).isEqualTo("val");
    }

    @Test
    void callingGetWithDomainsButWithoutAResolverGivesNullPointerException() {
        assertThrows(IllegalArgumentException.class, () -> keyValues.get(asList("dom1", "dom2"), null, null));
    }

    @Test
    void getAWildcardOverriddenValueIsReturnedByBestMatch() {
        keyValues.put("value_1", "*", "*", "domain3");
        keyValues.put("value_2", "domain1", "*", "domain3");
        assertThat((String) keyValues.get(asList("domain1", "domain2", "domain3"), null, resolver)).isEqualTo("value_2");
    }

    @Test
    void getAWildcardOverriddenValueIsReturnedWhenAllDomainsMatch() {
        keyValues.put("other value", "aaa", "*", "domain3");
        keyValues.put("value", "domain1", "*", "domain3");
        assertThat((String) keyValues.get(asList("domain1", "domain2", "domain3"), null, resolver)).isEqualTo("value");
    }

    @Test
    void domainValuesMustNotContainPipe() {
        DomainResolver resolverMock = mock(DomainResolver.class);
        when(resolverMock.getDomainValue("x1")).thenReturn("abc|def");
        assertThrows(IllegalArgumentException.class, () -> keyValues.get(singletonList("x1"), null, resolverMock));
    }

    @Test
    void resolvingToNullMatchesEmptyStringAndThatNeverMatchesSoTheRestOfTheDomainsAreIgnored() {
        resolver = mock(DomainResolver.class);
        when(resolver.getDomainValue("domain1")).thenReturn("domain1");
        when(resolver.getDomainValue("domain2")).thenReturn(null);
        when(resolver.getDomainValue("domain3")).thenReturn("domain3");
        keyValues.put("value");
        keyValues.put("overridden1", "domain1");
        keyValues.put("overridden2", "domain1", "domain2");
        keyValues.put("overridden3", "domain1", "domain2", "domain3");
        String value = keyValues.get(asList("domain1", "domain2", "domain3"), null, resolver);
        assertThat(value).isEqualTo("overridden1");
    }

    @Test
    void newValuesAreCreatedThroughTheSuppliedFactory() {
        DefaultDomainSpecificValueFactory factoryMock = mock(DefaultDomainSpecificValueFactory.class);
        keyValues.setDomainSpecificValueFactory(factoryMock);
        String value = "value";
        when(factoryMock.create(value, null)).thenReturn(DomainSpecificValue.withoutChangeSet(value));
        keyValues.put(value);
        verify(factoryMock).create(value, null);
    }

    @Test
    void domainsWithTheSamePrefixReturnTheCorrectValue() {
        keyValues.put("valuePrefix", "dom1", "prefix");
        keyValues.put("value1", "dom1", "prefixDom2");
        assertThat((String) keyValues.get(asList("dom1", "prefix"), null, resolver)).isEqualTo("valuePrefix");
        assertThat((String) keyValues.get(asList("dom1", "prefixDom2"), null, resolver)).isEqualTo("value1");
    }

    @Test
    void copyWithResolverGivesOnlyValuesMatchingResolverSpecificTrumpsWildcard() {
        keyValues.put("value_1", "*", "*", "domain3");
        keyValues.put("value_2", "domain1", "*", "domain3");
        final KeyValues copy = keyValues.copy(asList("dom1", "dom2", "dom3"), new MapBackedDomainResolver().set("dom1", "domain1"));
        assertThat(copy.getDomainSpecificValues()).containsExactlyInAnyOrder(
                DomainSpecificValue.withoutChangeSet("value_2", "domain1", "*", "domain3")
        );
    }

    @Test
    void copyWithResolverGivesOnlyValuesMatchingResolverAllWildcardedThenAllAreReturned() {
        keyValues.put("value_1", "*", "*", "domain3");
        keyValues.put("value_2", "*", "domain2", "domain3");
        final KeyValues copy = keyValues.copy(asList("dom1", "dom2", "dom3"), new MapBackedDomainResolver().set("dom1", "domain1"));
        assertThat(copy.getDomainSpecificValues()).containsExactlyInAnyOrder(
                DomainSpecificValue.withoutChangeSet("value_1", "*", "*", "domain3"),
                DomainSpecificValue.withoutChangeSet("value_2", "*", "domain2", "domain3")
        );
    }

    @Test
    void copyWithResolverGivesOnlyValuesMatchingResolverMoreSpecificTrumpsWildcard() {
        keyValues.put("value_1", "domain1", "*", "domain3");
        keyValues.put("value_2", "*", "domain2", "domain3");
        final KeyValues copy = keyValues.copy(asList("dom1", "dom2", "dom3"), new MapBackedDomainResolver()
                .set("dom1", "domain1")
                .set("dom2", "domain2"));
        assertThat(copy.getDomainSpecificValues()).containsExactlyInAnyOrder(
                DomainSpecificValue.withoutChangeSet("value_2", "*", "domain2", "domain3")
        );
    }

    @Test
    void copyWithResolverGivesOnlyValuesMatchingResolverNoDomainsMatchGivesDefault() {
        keyValues.put("value_*");
        keyValues.put("value_1", "*", "*", "domain3");
        keyValues.put("value_2", "*", "domain2", "domain3");
        final KeyValues copy = keyValues.copy(asList("dom1", "dom2", "dom3"), new MapBackedDomainResolver()
                .set("dom1", "domain_X1")
                .set("dom2", "domain_X2")
                .set("dom3", "domain_X3"));
        assertThat(copy.getDomainSpecificValues()).containsExactlyInAnyOrder(
                DomainSpecificValue.withoutChangeSet("value_*")
        );
    }

    @Test
    void copyWithResolverGivesOnlyValuesMatchingResolverWithoutPrefix() {
        keyValues.put("default");
        keyValues.put("value_1", "domain1");
        keyValues.put("value_2", "domain1", "domain2");
        keyValues.put("value_3", "domain1", "*", "domain3");
        keyValues.put("value_4", "domain1", "domain2", "domain3");
        final KeyValues copy = keyValues.copy(asList("dom1", "dom2", "dom3"), new MapBackedDomainResolver().set("dom2", "domain2"));
        assertThat(copy.getDomainSpecificValues()).containsExactlyInAnyOrder(
                DomainSpecificValue.withoutChangeSet("value_2", "domain1", "domain2"),
                DomainSpecificValue.withoutChangeSet("value_4", "domain1", "domain2", "domain3")
        );
    }

    @Test
    void copyWithMultipleDomainValues() {
        keyValues.put("value_1", "domain1");
        keyValues.put("value_2", "domain1", "domain2");
        keyValues.put("value_3", "domain1", "domainX");
        keyValues.put("value_4", "domain1", "domainY");
        keyValues.put("value_5", "domain1", "*");
        keyValues.put("value_6", "domainA", "*");
        keyValues.put("value_7", "domainA", "domainY");
        final KeyValues copy = keyValues.copy(asList("dom1", "dom2"), new MapBackedDomainResolver().set("dom2", "domain2|domainX|*"));
        assertThat(copy.getDomainSpecificValues()).containsExactlyInAnyOrder(
                DomainSpecificValue.withoutChangeSet("value_2", "domain1", "domain2"),
                DomainSpecificValue.withoutChangeSet("value_3", "domain1", "domainX"),
                DomainSpecificValue.withoutChangeSet("value_5", "domain1", "*"),
                DomainSpecificValue.withoutChangeSet("value_6", "domainA", "*")
        );
    }

    @Test
    void copyWithMultipleDomainResolvers() {
        keyValues.put("value_1", "domain1");
        keyValues.put("value_2", "domain1", "domain2");
        keyValues.put("value_3", "domain1", "domainX");
        keyValues.put("value_4", "domain1", "domainY");
        keyValues.put("value_5", "domain1", "*");
        keyValues.put("value_6", "domainA", "*");
        keyValues.put("value_7", "domainA", "domainY");
        final KeyValues copy = keyValues.copy(asList("dom1", "dom2"),
                new MapBackedDomainResolver().set("dom2", "domain2"),
                new MapBackedDomainResolver().set("dom1", "domain1").set("dom2", "domainX"),
                new MapBackedDomainResolver().set("dom1", "domainNone").set("dom2", "domainX")
        );
        assertThat(copy.getDomainSpecificValues()).containsExactlyInAnyOrder(
                DomainSpecificValue.withoutChangeSet("value_2", "domain1", "domain2"),
                DomainSpecificValue.withoutChangeSet("value_3", "domain1", "domainX")
        );
    }
}
