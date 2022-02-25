/*
 * Roperty - An advanced property management and retrieval system
 * Copyright (C) 2013 PARSHIP GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nitrobox.keyvalueresolver;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author finsterwalder
 * @since 2013-06-07 16:05
 */
public class KeyValueResolverFactoriesTest {

    final DomainSpecificValueFactory domainSpecificValueFactoryMock = mock(DomainSpecificValueFactory.class);
    final String key = "key";
    final String defaultValue = "default";

    @BeforeEach
    void before() {
        when(domainSpecificValueFactoryMock.create(defaultValue, null, new String[0])).thenReturn(
            DomainSpecificValue.withoutChangeSet(defaultValue));
    }

    @Test
    void factoriesAreUsedToCreateObjectsViaFactoryProvider() {
        KeyValueResolverImpl r = new KeyValueResolverImpl(new Persistence() {
            final Map<String, KeyValues> stringKeyValuesHashMap = new HashMap<>();

            @Override
            public KeyValues load(final String key, final DomainSpecificValueFactory domainSpecificValueFactory) {
                return stringKeyValuesHashMap.get(key);
            }

            @Override
            public Collection<KeyValues> loadAll(final DomainSpecificValueFactory domainSpecificValueFactory) {
                return stringKeyValuesHashMap.values();
            }

            @Override
            public Collection<KeyValues> reload(final Collection<KeyValues> keyValuesMap,
                final DomainSpecificValueFactory domainSpecificValueFactory) {
                return keyValuesMap;
            }

            @Override
            public void store(final String key, final KeyValues keyValues, final DomainSpecificValue domainSpecificValue) {
                stringKeyValuesHashMap.put(key, keyValues);
            }

            @Override
            public void remove(final String key) {
            }

            @Override
            public void remove(final String key, final DomainSpecificValue domainSpecificValue) {
            }
        }, domainSpecificValueFactoryMock);
        checkFactoryAccess(r);
    }

    @Test
    void factoriesAreUsedToCreateObjectsViaSet() {
        KeyValueResolverImpl r = new KeyValueResolverImpl();
        r.setDomainSpecificValueFactory(domainSpecificValueFactoryMock);
        checkFactoryAccess(r);
    }

    private void checkFactoryAccess(final KeyValueResolver r) {
        r.getOrDefine(key, defaultValue, new MapBackedDomainResolver());
        verify(domainSpecificValueFactoryMock).create(defaultValue, null, new String[0]);
    }
}
