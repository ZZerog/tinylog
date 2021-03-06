/*
 * Copyright 2012 Martin Winandy
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.apache.log4j;

/**
 * Empty basic configurator skeleton (use tinylog for real configuration).
 */
public class BasicConfigurator {

	/** */
	protected BasicConfigurator() {
	}

	/**
	 * Do nothing (this method exists only for compatible reasons).
	 */
	public static void configure() {
		// Do nothing
	}

	/**
	 * Do nothing (this method exists only for compatible reasons).
	 */
	public static void resetConfiguration() {
		// Do nothing
	}

}
