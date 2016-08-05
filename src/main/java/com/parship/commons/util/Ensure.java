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

package com.parship.commons.util;

/**
 * A simple Design-by-contract helper class to make parameter checks explicit.
 *
 * @author mfinsterwalder
 * @since 2010-10-13
 */
public class Ensure {

	/**
	 * Check that the provided string is not empty (not null, not "").
	 *
	 * @param str string to check
	 * @param parameterName name of the parameter to display in the error message
	 */
	public static void notEmpty(final CharSequence str, final String parameterName) {
		if (str == null || str.length() == 0) {
			throw new IllegalArgumentException('"' + parameterName + "\" must not be null or empty, but was: " + str);
		}
	}

	/**
	 * Check, that the given condition is true
	 *
	 * @param cond condition to evaluate
	 * @param conditionName name to display in the error message
	 */
	public static void that(final boolean cond, final String conditionName) {
		if (!cond) {
			throw new IllegalArgumentException('"' + conditionName + "\" must be true");
		}
	}
}
