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

package org.pmw.tinylog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Pattern;

import org.pmw.tinylog.labelers.Labeler;
import org.pmw.tinylog.policies.Policy;
import org.pmw.tinylog.writers.LoggingWriter;

/**
 * Loads configuration for {@link org.pmw.tinylog.Logger Logger} from properties.
 */
final class PropertiesLoader {

	/**
	 * Name of property for logging level
	 */
	static final String LEVEL_PROPERTY = "tinylog.level";

	/**
	 * Name of property for format pattern
	 */
	static final String FORMAT_PROPERTY = "tinylog.format";

	/**
	 * Name of property for locale
	 */
	static final String LOCALE_PROPERTY = "tinylog.locale";

	/**
	 * Name of property for max stack trace elements
	 */
	static final String STACKTRACE_PROPERTY = "tinylog.stacktrace";

	/**
	 * Name of property for writer
	 */
	static final String WRITER_PROPERTY = "tinylog.writer";

	/**
	 * Name of property for writing thread
	 */
	static final String WRITING_THREAD_PROPERTY = "tinylog.writingthread";

	/**
	 * Name of property for thread to observe by writing thread
	 */
	static final String WRITING_THREAD_OBSERVE_PROPERTY = WRITING_THREAD_PROPERTY + ".observe";

	/**
	 * Name of property for priority by writing thread
	 */
	static final String WRITING_THREAD_PRIORITY_PROPERTY = WRITING_THREAD_PROPERTY + ".priority";

	/**
	 * Prefix for path to services
	 */
	static final String SERVICES_PREFIX = "META-INF/services/";

	/**
	 * Name prefix of properties for custom logging levels
	 */
	static final String CUSTOM_LEVEL_PREFIX = LEVEL_PROPERTY + "@";

	private PropertiesLoader() {
	}

	/**
	 * Load configuration from properties.
	 * 
	 * @param properties
	 *            Properties with configuration
	 * @return A new configurator
	 */
	static Configurator readProperties(final Properties properties) {
		Configurator configurator = Configurator.defaultConfig();
		readLevel(configurator, properties);
		readFormatPattern(configurator, properties);
		readLocale(configurator, properties);
		readMaxStackTraceElements(configurator, properties);
		readWriter(configurator, properties);
		readWritingThread(configurator, properties);
		return configurator;
	}

	/**
	 * Load default logging level and custom logging levels from properties.
	 * 
	 * @param configurator
	 *            Configurator to update
	 * @param properties
	 *            Properties with configuration
	 */
	static void readLevel(final Configurator configurator, final Properties properties) {
		String level = properties.getProperty(LEVEL_PROPERTY);
		if (level != null && level.length() > 0) {
			try {
				configurator.level(LoggingLevel.valueOf(level.toUpperCase(Locale.ENGLISH)));
			} catch (IllegalArgumentException ex) {
				InternalLogger.warn("\"{0}\" is an invalid logging level and will be ignored", level);
			}
		}

		Enumeration<Object> keys = properties.keys();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			if (key.startsWith(CUSTOM_LEVEL_PREFIX)) {
				String packageOrClass = key.substring(CUSTOM_LEVEL_PREFIX.length());
				String value = properties.getProperty(key);
				try {
					LoggingLevel loggingLevel = LoggingLevel.valueOf(value.toUpperCase(Locale.ENGLISH));
					configurator.level(packageOrClass, loggingLevel);
				} catch (IllegalArgumentException ex) {
					InternalLogger.warn("\"{0}\" is an invalid logging level and will be ignored", value);
					configurator.level(packageOrClass, null);
				}
			}
		}
	}

	/**
	 * Load format pattern from properties.
	 * 
	 * @param configurator
	 *            Configurator to update
	 * @param properties
	 *            Properties with configuration
	 */
	static void readFormatPattern(final Configurator configurator, final Properties properties) {
		String format = properties.getProperty(FORMAT_PROPERTY);
		if (format != null && format.length() > 0) {
			configurator.formatPattern(format);
		}
	}

	/**
	 * Load locale from properties.
	 * 
	 * @param configurator
	 *            Configurator to update
	 * @param properties
	 *            Properties with configuration
	 */
	static void readLocale(final Configurator configurator, final Properties properties) {
		String localeString = properties.getProperty(LOCALE_PROPERTY);
		if (localeString != null && localeString.length() > 0) {
			String[] localeArray = localeString.split("_", 3);
			if (localeArray.length == 1) {
				configurator.locale(new Locale(localeArray[0]));
			} else if (localeArray.length == 2) {
				configurator.locale(new Locale(localeArray[0], localeArray[1]));
			} else if (localeArray.length >= 3) {
				configurator.locale(new Locale(localeArray[0], localeArray[1], localeArray[2]));
			}
		}
	}

	/**
	 * Load number of max stack trace elements from properties.
	 * 
	 * @param configurator
	 *            Configurator to update
	 * @param properties
	 *            Properties with configuration
	 */
	static void readMaxStackTraceElements(final Configurator configurator, final Properties properties) {
		String stacktace = properties.getProperty(STACKTRACE_PROPERTY);
		if (stacktace != null && stacktace.length() > 0) {
			try {
				int limit = Integer.parseInt(stacktace);
				configurator.maxStackTraceElements(limit);
			} catch (NumberFormatException ex) {
				InternalLogger.warn("\"{0}\" is an invalid stack trace size and will be ignored", stacktace);
			}
		}
	}

	/**
	 * Load writer from properties.
	 * 
	 * @param configurator
	 *            Configurator to update
	 * @param properties
	 *            Properties with configuration
	 */
	static void readWriter(final Configurator configurator, final Properties properties) {
		String writer = properties.getProperty(WRITER_PROPERTY);
		if (writer != null && writer.length() > 0) {
			if (writer.equalsIgnoreCase("null")) {
				configurator.writer(null);
			} else {
				for (Class<?> implementation : findImplementations(LoggingWriter.class)) {
					org.pmw.tinylog.writers.PropertiesSupport propertiesSupport = implementation.getAnnotation(org.pmw.tinylog.writers.PropertiesSupport.class);
					if (propertiesSupport != null) {
						if (writer.equalsIgnoreCase(propertiesSupport.name())) {
							LoggingWriter loggingWriter = loadAndSetWriter(properties, propertiesSupport.properties(), implementation);
							if (loggingWriter == null) {
								InternalLogger.error("Failed to initialize {0} writer", writer);
							} else {
								configurator.writer(loggingWriter);
							}
							return;
						}
					}
				}
				InternalLogger.error("Cannot find a writer for the name \"{0}\"", writer);
			}
		}
	}

	/**
	 * Load writing thread data from properties.
	 * 
	 * @param configurator
	 *            Configurator to update
	 * @param properties
	 *            Properties with configuration
	 */
	static void readWritingThread(final Configurator configurator, final Properties properties) {
		String writingThread = properties.getProperty(WRITING_THREAD_PROPERTY);
		if ("true".equalsIgnoreCase(writingThread)) {
			String observedThread = properties.getProperty(WRITING_THREAD_OBSERVE_PROPERTY);
			boolean observedThreadDefined = observedThread != null;
			if (observedThreadDefined && observedThread.equalsIgnoreCase("null")) {
				observedThread = null;
			}
			String priorityString = properties.getProperty(WRITING_THREAD_PRIORITY_PROPERTY);
			Integer priority;
			if (priorityString == null) {
				priority = null;
			} else {
				try {
					priority = Integer.parseInt(priorityString.trim());
				} catch (NumberFormatException ex) {
					priority = null;
					InternalLogger.warn("\"{0}\" is an invalid thread priority and will be ignored", priorityString);
				}
			}
			if (priority != null && observedThreadDefined) {
				configurator.writingThread(observedThread, priority);
			} else if (priority != null) {
				configurator.writingThread(priority);
			} else if (observedThreadDefined) {
				configurator.writingThread(observedThread);
			} else {
				configurator.writingThread(true);
			}
		} else {
			configurator.writingThread(false);
		}
	}

	private static Collection<Class<?>> findImplementations(final Class<?> service) {
		try {
			Enumeration<URL> urls = PropertiesLoader.class.getClassLoader().getResources(SERVICES_PREFIX + service.getPackage().getName());
			if (urls == null || !urls.hasMoreElements()) {
				return Collections.emptyList();
			} else {
				Collection<Class<?>> services = new ArrayList<Class<?>>();
				while (urls.hasMoreElements()) {
					URL url = urls.nextElement();
					InputStream inputStream = null;
					BufferedReader reader = null;
					try {
						inputStream = url.openStream();
						reader = new BufferedReader(new InputStreamReader(inputStream, "utf-8"));
						for (String line = reader.readLine(); line != null; line = reader.readLine()) {
							line = line.trim();
							if (line != null) {
								try {
									Class<?> implementation = Class.forName(line);
									if (service.isAssignableFrom(implementation)) {
										services.add(implementation);
									}
								} catch (ClassNotFoundException ex) {
									InternalLogger.warn("Cannot find class \"{0}\"", line);
								}
							}
						}
					} finally {
						if (inputStream != null) {
							try {
								inputStream.close();
							} catch (IOException ex) {
								// Ignore
							}
						}
						if (reader != null) {
							try {
								reader.close();
							} catch (IOException ex) {
								// Ignore
							}
						}
					}
				}
				return services;
			}
		} catch (IOException ex) {
			InternalLogger.error(ex, "Failed to read services from \"{0}\"", SERVICES_PREFIX + service.getPackage().getName());
			return Collections.emptyList();
		}
	}

	private static LoggingWriter loadAndSetWriter(final Properties properties, final org.pmw.tinylog.writers.Property[] definition, final Class<?> writerClass) {
		Object[] parameters = loadParameters(properties, definition);

		if (parameters != null) {
			for (Constructor<?> constructor : writerClass.getConstructors()) {
				Class<?>[] parameterTypes = constructor.getParameterTypes();
				if (parameterTypes.length <= definition.length) {
					BitSet skiped = new BitSet(definition.length);
					boolean matches = true;
					int offset = 0;
					for (int i = 0; i < definition.length; ++i) {
						Class<?> parameterType = i - offset < parameterTypes.length ? parameterTypes[i - offset] : null;
						if (parameterType == null || !parameterType.equals(definition[i].type())) {
							if (definition[i].optional() && parameters[i] == null) {
								skiped.set(i);
								++offset;
							} else {
								matches = false;
								break;
							}
						} else if (parameters[i] == null && parameterType.isPrimitive()) {
							matches = false;
							break;
						}
					}
					if (matches) {
						try {
							if (parameters.length > parameterTypes.length) {
								List<Object> list = new ArrayList<Object>();
								for (int i = 0; i < parameters.length; ++i) {
									if (!skiped.get(i)) {
										list.add(parameters[i]);
									}
								}
								parameters = list.toArray();
							}
							return (LoggingWriter) constructor.newInstance(parameters);
						} catch (IllegalArgumentException ex) {
							InternalLogger.error(ex, "Failed to create an instance of \"{0}\"", writerClass.getName());
							return null;
						} catch (InstantiationException ex) {
							InternalLogger.error(ex, "Failed to create an instance of \"{0}\"", writerClass.getName());
							return null;
						} catch (IllegalAccessException ex) {
							InternalLogger.error(ex, "Failed to create an instance of \"{0}\"", writerClass.getName());
							return null;
						} catch (InvocationTargetException ex) {
							InternalLogger.error(ex.getTargetException(), "Failed to create an instance of \"{0}\"", writerClass.getName());
							return null;
						}
					}
				}
			}
		}

		return null;
	}

	private static Object[] loadParameters(final Properties properties, final org.pmw.tinylog.writers.Property[] definition) {
		Object[] parameters = new Object[definition.length];

		for (int i = 0; i < definition.length; ++i) {
			String name = definition[i].name();
			String value = properties.getProperty(WRITER_PROPERTY + "." + name);
			if (value == null) {
				if (definition[i].optional()) {
					parameters[i] = null;
				} else {
					InternalLogger.error("Missing required property \"{0}\"", WRITER_PROPERTY + "." + name);
					return null;
				}
			} else {
				Class<?> type = definition[i].type();
				if (boolean.class.equals(type)) {
					if ("true".equalsIgnoreCase(value)) {
						parameters[i] = Boolean.TRUE;
					} else if ("false".equalsIgnoreCase(value)) {
						parameters[i] = Boolean.FALSE;
					} else {
						InternalLogger.error("\"{1}\" for \"{0}\" is an invalid boolean", WRITER_PROPERTY + "." + name, value);
						return null;
					}
				} else if (int.class.equals(type)) {
					try {
						parameters[i] = Integer.parseInt(value);
					} catch (NumberFormatException ex) {
						InternalLogger.error("\"{1}\" for \"{0}\" is an invalid number", WRITER_PROPERTY + "." + name, value);
						return null;
					}
				} else if (String.class.equals(type)) {
					parameters[i] = value;
				} else if (String[].class.equals(type)) {
					parameters[i] = parseStrings(value);
				} else if (Labeler.class.equals(type)) {
					Object labeler = parseLabeler(value);
					if (labeler == null) {
						return null;
					} else {
						parameters[i] = labeler;
					}
				} else if (Policy.class.equals(type)) {
					Object policy = parsePolicy(value);
					if (policy == null) {
						return null;
					} else {
						parameters[i] = policy;
					}
				} else if (Policy[].class.equals(type)) {
					Policy[] policies = parsePolicies(value);
					if (policies == null) {
						return null;
					} else {
						parameters[i] = policies;
					}
				} else {
					InternalLogger.error("\"{1}\" for \"{0}\" is an unsupported type (String, int, boolean, Labeler, Policy and Policy[] are supported)",
							WRITER_PROPERTY + "." + name, type.getName());
					return null;
				}
			}
		}

		return parameters;
	}

	private static String[] parseStrings(final String value) {
		int size = 1;
		for (int i = 0; i < value.length(); ++i) {
			if (value.charAt(i) == ',') {
				++size;
			}
		}

		String[] values = new String[size];

		int start = 0;
		int counter = 0;
		for (int i = 0; i < value.length(); ++i) {
			if (value.charAt(i) == ',') {
				values[counter] = start >= i ? "" : value.substring(start, i).trim();
				start = i + 1;
				++counter;
			}
		}
		values[counter] = start >= value.length() ? "" : value.substring(start).trim();

		return values;
	}

	private static Labeler parseLabeler(final String string) {
		int separator = string.indexOf(':');
		String name = separator > 0 ? string.substring(0, separator).trim() : string.trim();
		String parameter = separator > 0 ? string.substring(separator + 1).trim() : null;

		for (Class<?> implementation : findImplementations(Labeler.class)) {
			org.pmw.tinylog.labelers.PropertiesSupport propertiesSupport = implementation.getAnnotation(org.pmw.tinylog.labelers.PropertiesSupport.class);
			if (propertiesSupport != null) {
				if (name.equalsIgnoreCase(propertiesSupport.name())) {
					Labeler labeler = (Labeler) createInstance(implementation, parameter);
					if (labeler == null) {
						InternalLogger.error("Failed to initialize {0} labeler", name);
					}
					return labeler;
				}
			}
		}

		InternalLogger.error("Cannot find a labeler for the name \"{0}\"", name);
		return null;
	}

	private static Policy[] parsePolicies(final String string) {
		List<Policy> policies = new ArrayList<Policy>();
		for (String part : string.split(Pattern.quote(","))) {
			Policy policy = parsePolicy(part.trim());
			if (policy == null) {
				return null;
			} else {
				policies.add(policy);
			}
		}
		return policies.toArray(new Policy[0]);
	}

	private static Policy parsePolicy(final String string) {
		int separator = string.indexOf(':');
		String name = separator > 0 ? string.substring(0, separator).trim() : string.trim();
		String parameter = separator > 0 ? string.substring(separator + 1).trim() : null;

		for (Class<?> implementation : findImplementations(Policy.class)) {
			org.pmw.tinylog.policies.PropertiesSupport propertiesSupport = implementation.getAnnotation(org.pmw.tinylog.policies.PropertiesSupport.class);
			if (propertiesSupport != null) {
				if (name.equalsIgnoreCase(propertiesSupport.name())) {
					Policy policy = (Policy) createInstance(implementation, parameter);
					if (policy == null) {
						InternalLogger.error("Failed to initialize {0} policy", name);
					}
					return policy;
				}
			}
		}

		InternalLogger.error("Cannot find a policy for the name \"{0}\"", name);
		return null;
	}

	private static Object createInstance(final Class<?> clazz, final String parameter) {
		try {
			if (parameter == null) {
				Constructor<?> constructor = clazz.getDeclaredConstructor();
				constructor.setAccessible(true);
				return constructor.newInstance();
			} else {
				Constructor<?> constructor = clazz.getDeclaredConstructor(String.class);
				constructor.setAccessible(true);
				return constructor.newInstance(parameter);
			}
		} catch (InstantiationException ex) {
			InternalLogger.error(ex, "Failed to create an instance of \"{0}\"", clazz.getName());
			return null;
		} catch (IllegalAccessException ex) {
			InternalLogger.error(ex, "Failed to create an instance of \"{0}\"", clazz.getName());
			return null;
		} catch (IllegalArgumentException ex) {
			InternalLogger.error(ex, "Failed to create an instance of \"{0}\"", clazz.getName());
			return null;
		} catch (InvocationTargetException ex) {
			InternalLogger.error(ex.getTargetException(), "Failed to create an instance of \"{0}\"", clazz.getName());
			return null;
		} catch (NoSuchMethodException ex) {
			InternalLogger.error(ex, "Failed to create an instance of \"{0}\"", clazz.getName());
			return null;
		}
	}
}
