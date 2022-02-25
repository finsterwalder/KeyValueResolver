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

/**
 * @author finsterwalder
 * @since 2013-09-13 10:49
 */
public class StringPrefixMatcher implements Matcher {

    private final String prefixString;

    public StringPrefixMatcher(final String prefixString) {
        this.prefixString = prefixString;
    }

    @Override
    public boolean matches(String domainString) {
        return prefixString.equals(domainString.substring(0, Math.min(domainString.length(), prefixString.length())));
    }
}
