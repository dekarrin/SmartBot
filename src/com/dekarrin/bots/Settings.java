package com.dekarrin.bots;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Holds persistence settings for the bot and handles file i/o for the run
 * control file.
 * 
 * @author Rebecca 'TF' Nelson
 */
class Settings {
	
	private enum ReadMode {
		CORE,
		MODULE_SETTINGS,
		MODULES,
		NONE,
		OPERATORS
	}
	
	private Map<String, String> currentModuleSettings;
	
	private List<String> enabledModules;
	
	private Map<String, Map<String, String>> moduleSettings;
	
	private Set<String> operators;
	
	private final String rcfile;
	
	private ReadMode section = ReadMode.NONE;
	
	private boolean successfulLoad;
	
	private final boolean writeOnChange;
	
	public Settings(final String rcfile, final boolean writeOnChange) {
		moduleSettings = new HashMap<String, Map<String, String>>();
		operators = new HashSet<String>();
		enabledModules = new ArrayList<String>();
		this.rcfile = rcfile;
		this.writeOnChange = writeOnChange;
	}
	
	/**
	 * Clears all existing settings.
	 */
	public void clear() {
		moduleSettings = new HashMap<String, Map<String, String>>();
		operators = new HashSet<String>();
		enabledModules = new ArrayList<String>();
	}
	
	/**
	 * Returns all the settings for a module. The view is not editable.
	 * 
	 * @return The settings.
	 */
	public Map<String, String> getAllModuleSettings(String name) {
		Map<String, String> sets = moduleSettings.get(name.toUpperCase());
		if (sets != null) {
			return Collections.unmodifiableMap(sets);
		} else {
			return null;
		}
	}
	
	/**
	 * Gets the list of enabled modules, in the order that they were enabled.
	 * The returned list is immutable; attempting to modify will result in an
	 * exception being thrown.
	 */
	public List<String> getEnabledModules() {
		return Collections.unmodifiableList(enabledModules);
	}
	
	/**
	 * Gets a setting for a module.
	 * 
	 * @param mod The name of the module.
	 * @param setting The setting to get.
	 * @return The value of the setting, or null if the mod or its setting does
	 * not exist.
	 */
	public String getModuleSetting(final String mod, final String setting) {
		if (!moduleSettings.containsKey(mod.toUpperCase())) {
			return null;
		} else {
			return moduleSettings.get(mod.toUpperCase()).get(
					setting.toUpperCase());
		}
	}
	
	/**
	 * Gets the set of operators for the bot. The returned set is immutable;
	 * attempting to modify will result in an exception being thrown.
	 */
	public Set<String> getOperators() {
		return Collections.unmodifiableSet(operators);
	}
	
	/**
	 * Reads the settings in the RC File. The current settings are cleared and
	 * replaced with those read from the file.
	 * 
	 * @throws FileNotFoundException If the RC file does not exist.
	 * @throws IOException
	 */
	public void read() throws FileNotFoundException, IOException {
		clear();
		section = ReadMode.NONE;
		String line = null;
		successfulLoad = true;
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(rcfile));
			while ((line = br.readLine()) != null) {
				line = line.replaceAll("#.*", "");
				if (!line.trim().equals("")) {
					processLine(line);
				}
			}
		} catch (final FileNotFoundException e) {
			successfulLoad = false;
			throw e;
		} catch (final IOException e) {
			successfulLoad = false;
			throw e;
		} finally {
			if (br != null) {
				br.close();
			}
		}
	}
	
	/**
	 * Sets whether a module is enabled. If it is, it is added to the end of the
	 * enabled list. If it is not, it is removed from the list.
	 * 
	 * @param module The name of the module to add.
	 * @param enable Whether it is enabled.
	 */
	public void setModuleEnabled(final String module, final boolean enabled) {
		if (enabled) {
			enabledModules.add(module);
		} else {
			enabledModules.remove(module);
		}
		if (writeOnChange) {
			try {
				write();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Sets a value for a module setting.
	 * 
	 * @param mod The name of the module.
	 * @param setting The setting to set.
	 * @param value What to set the value to.
	 */
	public void setModuleSetting(final String mod, final String setting,
			final String value) {
		if (!moduleSettings.containsKey(mod.toUpperCase())) {
			moduleSettings
					.put(mod.toUpperCase(), new HashMap<String, String>());
		}
		moduleSettings.get(mod.toUpperCase()).put(setting.toUpperCase(), value);
		if (writeOnChange) {
			try {
				write();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Sets whether a nick has operator status. If it does, it is added to the
	 * set of operators; if it does not, it is removed from the set.
	 * 
	 * @param nick The nickname of the user whose status is being set.
	 * @param isOp Whether the nick has op.
	 */
	public void setNickOp(final String nick, final boolean isOp) {
		if (isOp) {
			operators.add(nick);
		} else {
			operators.remove(nick);
		}
		if (writeOnChange) {
			try {
				write();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Checks whether the last load was successful.
	 */
	public boolean successfullyLoaded() {
		return successfulLoad;
	}
	
	/**
	 * Writes the current settings to the given RC file.
	 * 
	 * @throws IOException
	 */
	public void write() throws IOException {
		final BufferedWriter bw = new BufferedWriter(new FileWriter(rcfile));
		writeCore(bw);
		writeModules(bw);
		writeOperators(bw);
		writeSettings(bw);
		bw.close();
	}
	
	private void changeSection(final String header) {
		if (header.equalsIgnoreCase("OPERATORS")) {
			section = ReadMode.OPERATORS;
		} else if (header.equalsIgnoreCase("MODULES")) {
			section = ReadMode.MODULES;
		} else if (header.toUpperCase().equalsIgnoreCase("CORE")) {
			section = ReadMode.CORE;
			currentModuleSettings = new HashMap<String, String>();
			moduleSettings
					.put(SmartBot.CORE_MODULE_NAME, currentModuleSettings);
		} else if (header.toUpperCase().startsWith("MODULESETTINGS")) {
			section = ReadMode.MODULE_SETTINGS;
			final String modName = header.substring("MODULESETTINGS".length())
					.toUpperCase();
			currentModuleSettings = new HashMap<String, String>();
			moduleSettings.put(modName, currentModuleSettings);
		} else {
			section = ReadMode.NONE;
		}
	}
	
	private String compose(final String setting, String value) {
		if (value == null) {
			value = "";
		}
		return setting + "=" + value;
	}
	
	private String[] parse(final String settingLine) {
		final String[] parts = settingLine.split("=", 2);
		parts[0] = parts[0].toUpperCase();
		return parts;
	}
	
	private void processLine(String line) {
		if (line.matches("\\[[^]]+]")) {
			changeSection(line.substring(1, line.length() - 1));
		} else {
			switch (section) {
				case MODULES:
					line = line.trim();
					if (line.matches("[\\$A-Za-z_][0-9A-Za-z_]*")) {
						enabledModules.add(line);
					}
					break;
				case OPERATORS:
					line = line.trim();
					operators.add(line);
					break;
				case MODULE_SETTINGS:
				case CORE:
					if (line.matches("[\\$A-Za-z_-][0-9A-Za-z_-]*=.*")) {
						final String[] setting = parse(line.replaceAll("^\\s+",
								""));
						currentModuleSettings.put(setting[0], setting[1]);
					}
					break;
				case NONE:
					break;
			}
		}
	}
	
	private void writeCore(final BufferedWriter bw) throws IOException {
		bw.write("[Core]");
		bw.newLine();
		writeModuleSettings(bw, SmartBot.CORE_MODULE_NAME);
		bw.newLine();
	}
	
	private void writeModules(final BufferedWriter bw) throws IOException {
		bw.write("[Modules]");
		bw.newLine();
		for (final String module : enabledModules) {
			bw.write(module);
			bw.newLine();
		}
		bw.newLine();
	}
	
	private void writeModuleSettings(final BufferedWriter bw,
			final String moduleName) throws IOException {
		final Map<String, String> settings = moduleSettings.get(moduleName);
		if (settings != null) {
			for (final String setting : settings.keySet()) {
				bw.write(compose(setting.toLowerCase(), settings.get(setting)));
				bw.newLine();
			}
		}
	}
	
	private void writeOperators(final BufferedWriter bw) throws IOException {
		bw.write("[Operators]");
		bw.newLine();
		for (final String op : operators) {
			bw.write(op);
			bw.newLine();
		}
		bw.newLine();
	}
	
	private void writeSettings(final BufferedWriter bw) throws IOException {
		for (final String mod : moduleSettings.keySet()) {
			if (!mod.equals(SmartBot.CORE_MODULE_NAME)) {
				bw.write("[ModuleSettings" + mod + "]");
				bw.newLine();
				writeModuleSettings(bw, mod);
				bw.newLine();
			}
		}
	}
}
