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

package com.parship.roperty;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;


/**
 * Defines a value overridden for a specific domain.
 * DomainSpecificValues are ordered according to the Roperty precedence rules for resolution of domain values.
 * DomainSpecificValues can belong to a changeSet, which allows temporary changes which take precedence, when
 * the changeSet is active and are ignored, when the changeSet is not active.
 * ChangeSets are activated by the DomainResolver.
 *
 * @author mfinsterwalder
 * @since 2013-04-09 18:20
 */
public class DomainSpecificValue implements Comparable<DomainSpecificValue> {
	private final String patternStr;
	private final int ordering;
    private final String[] domains;
    private Object value;
	private final Matcher matcher;
    private String changeSet;

    public static DomainSpecificValue withChangeSet(final OrderedDomainPattern orderedDomainPattern, Object value, String changeSet, String... domainKeys) {
        return new DomainSpecificValue(orderedDomainPattern, value, changeSet, domainKeys);
    }

    public static DomainSpecificValue withoutChangeSet(final OrderedDomainPattern orderedDomainPattern, Object value, String... domainKeys) {
        return new DomainSpecificValue(orderedDomainPattern, value, domainKeys);
    }

    private DomainSpecificValue(final OrderedDomainPattern orderedDomainPattern, Object value, String changeSet, String... domainKeys) {
		this(orderedDomainPattern, value, domainKeys);
		this.changeSet = changeSet;
	}

	private DomainSpecificValue(final OrderedDomainPattern orderedDomainPattern, Object value, String... domainKeys) {
        Objects.requireNonNull(orderedDomainPattern.getDomainPattern(), "\"domainPattern\" must not be null");
        this.patternStr = orderedDomainPattern.getDomainPattern();
		if (patternStr.contains("*")) {
			matcher = new RegexMatcher(patternStr.replaceAll("\\|", "\\\\|").replaceAll("\\*", "[^|]*") + ".*");
		} else {
			matcher = new StringPrefixMatcher(orderedDomainPattern.getDomainPattern());
		}
		this.ordering = orderedDomainPattern.getOrder();
        this.domains = domainKeys;
		this.value = value;
	}

    /**
	 * Sort DomainSpecificValue in reverse order as specified by ordering, changeSet and patternStr.
	 * This ordering defines the order of resolution that Roperty uses when a key is accessed.
	 * Values with a changeSet are ordered before values without a changeSet.
	 * Values with a changeSet are ordered alphabetically with other changeSets. A value from changeSet "A_ChangeSet" is chosen
     * before a value in changeSet "B_ChangeSet".
	 * Values with the same ordering (and changeSet) are ordered by patternStr, just to define a consistent ordering.
	 */
	@Override
	public int compareTo(final DomainSpecificValue other) {
		int order = other.ordering - this.ordering;
		if (order == 0) {
			if (changeSet != null && other.changeSet != null) {
				int changeSetCompare = changeSet.compareTo(other.changeSet);
				if (changeSetCompare != 0)
					return changeSetCompare;
				else
					return patternStr.compareTo(other.patternStr);
			}
			if (changeSet != null) { // other.changeSet is null here
				return -1;
			}
			if (other.changeSet != null) { // changeSet is null here
				return 1;
			}
			return patternStr.compareTo(other.patternStr);
		}
		return order;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		DomainSpecificValue that = (DomainSpecificValue) o;

		if (ordering != that.ordering) return false;
		if (!patternStr.equals(that.patternStr)) return false;
		if (!value.equals(that.value)) return false;
		return Objects.equals(changeSet, that.changeSet);
	}

	@Override
	public int hashCode() {
		int result = patternStr.hashCode();
		result = 31 * result + ordering;
		result = 31 * result + value.hashCode();
		result = 31 * result + (changeSet != null ? changeSet.hashCode() : 0);
		return result;
	}

    @Override
    public String toString() {
        return "DomainSpecificValue{" +
            "pattern=\"" + patternStr +
            "\", ordering=" + ordering +
            (changeSet != null ? ", changeSet=\"" + changeSet + '"' : "") +
            ", value=\"" + value +
            "\", domains=" + Arrays.toString(domains) +
            "}";
    }

    public String getPatternStr() {
		return patternStr;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(final Object value) {
		this.value = value;
	}

	public boolean patternMatches(final String domainStr) {
		return matcher.matches(domainStr);
	}

	public boolean isInChangeSets(final Collection<String> activeChangeSets) {
        return changeSet == null || activeChangeSets.contains(changeSet);
    }

	public boolean changeSetIs(final String changeSet) {
		return Objects.equals(this.changeSet, changeSet);
	}

    public String getChangeSet() {
        return changeSet;
    }

    public String[] getDomains() {
        return domains;
    }

    public boolean patternMatches(Matcher matcher, DomainResolver resolver) {
        return isInChangeSets(resolver.getActiveChangeSets()) && (isDefault() || matcher.matches(patternStr));
    }

    public boolean isDefault() {
        return patternStr.length() == 0;
    }
}
