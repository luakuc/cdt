/*******************************************************************************
 * Copyright (c) 2010, 2016 Wind River Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Wind River Systems - Initial API and implementation
 *   James Blackburn (Broadcom Corp.)
 *   Andrew Gvozdev
 *   IBM Corporation
 *******************************************************************************/
package org.eclipse.cdt.managedbuilder.core;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.cdt.build.core.scannerconfig.CfgInfoContext;
import org.eclipse.cdt.build.core.scannerconfig.ICfgScannerConfigBuilderInfo2Set;
import org.eclipse.cdt.build.internal.core.scannerconfig2.CfgScannerConfigProfileManager;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.ErrorParserManager;
import org.eclipse.cdt.core.ICommandLauncher;
import org.eclipse.cdt.core.IConsoleParser;
import org.eclipse.cdt.core.IMarkerGenerator;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.language.settings.providers.ScannerDiscoveryLegacySupport;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.internal.core.BuildRunnerHelper;
import org.eclipse.cdt.make.core.scannerconfig.IScannerConfigBuilderInfo2;
import org.eclipse.cdt.make.core.scannerconfig.IScannerInfoConsoleParser;
import org.eclipse.cdt.make.internal.core.scannerconfig.ScannerInfoConsoleParserFactory;
import org.eclipse.cdt.managedbuilder.internal.core.ManagedMakeMessages;
import org.eclipse.cdt.managedbuilder.macros.BuildMacroException;
import org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider;
import org.eclipse.cdt.utils.CommandLineUtil;
import org.eclipse.cdt.utils.EFSExtensionManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

/**
 * @author dschaefer
 * @since 8.0
 */
public class ExternalBuildRunner extends AbstractBuildRunner {
	private static final int PROGRESS_MONITOR_SCALE = 100;
	private static final int TICKS_STREAM_PROGRESS_MONITOR = 1 * PROGRESS_MONITOR_SCALE;
	private static final int TICKS_DELETE_MARKERS = 1 * PROGRESS_MONITOR_SCALE;
	private static final int TICKS_EXECUTE_COMMAND = 1 * PROGRESS_MONITOR_SCALE;
	private static final int TICKS_REFRESH_PROJECT = 1 * PROGRESS_MONITOR_SCALE;

	@Override
	public boolean invokeBuild(int kind, IProject project, IConfiguration configuration,
			IBuilder builder, IConsole console, IMarkerGenerator markerGenerator,
			IncrementalProjectBuilder projectBuilder, IProgressMonitor monitor) throws CoreException {
		return invokeExternalBuild(kind, project, configuration, builder, console,
				markerGenerator, projectBuilder, monitor);
	}

	protected boolean invokeExternalBuild(int kind, IProject project, IConfiguration configuration,
			IBuilder builder, IConsole console, IMarkerGenerator markerGenerator,
			IncrementalProjectBuilder projectBuilder, IProgressMonitor monitor) throws CoreException {

		boolean isClean = false;

		BuildRunnerHelper buildRunnerHelper = new BuildRunnerHelper(project);
		try {
			if (monitor == null) {
				monitor = new NullProgressMonitor();
			}
			monitor.beginTask(ManagedMakeMessages.getResourceString("MakeBuilder.Invoking_Make_Builder") + project.getName(), //$NON-NLS-1$
					TICKS_STREAM_PROGRESS_MONITOR + TICKS_DELETE_MARKERS + TICKS_EXECUTE_COMMAND + TICKS_REFRESH_PROJECT);

			IPath buildCommand = builder.getBuildCommand();
			if (buildCommand != null) {
				String cfgName = configuration.getName();
				String toolchainName = configuration.getToolChain().getName();
				boolean isSupported = configuration.isSupported();

				ICommandLauncher launcher = builder.getCommandLauncher();

				String[] targets = getTargets(kind, builder);
				if (targets.length != 0 && targets[targets.length - 1].equals(builder.getCleanBuildTarget()))
					isClean = true;
				boolean isOnlyClean = isClean && (targets.length == 1);

				String[] args = getCommandArguments(builder, targets);

				URI workingDirectoryURI = ManagedBuildManager.getBuildLocationURI(configuration, builder);

				Map<String, String> envMap = getEnvironment(builder);
				String[] envp = BuildRunnerHelper.envMapToEnvp(envMap);

				String[] errorParsers = builder.getErrorParsers();
				ErrorParserManager epm = new ErrorParserManager(project, workingDirectoryURI, markerGenerator, errorParsers);

				List<IConsoleParser> parsers = new ArrayList<IConsoleParser>();
				if (!isOnlyClean) {
					ICConfigurationDescription cfgDescription = ManagedBuildManager.getDescriptionForConfiguration(configuration);
					ManagedBuildManager.collectLanguageSettingsConsoleParsers(cfgDescription, epm, parsers);
					if (ScannerDiscoveryLegacySupport.isLegacyScannerDiscoveryOn(cfgDescription)) {
						collectScannerInfoConsoleParsers(project, configuration, workingDirectoryURI, markerGenerator, parsers);
					}
				}

				buildRunnerHelper.setLaunchParameters(launcher, buildCommand, args, workingDirectoryURI, envp);
				buildRunnerHelper.prepareStreams(epm, parsers, console, new SubProgressMonitor(monitor, TICKS_STREAM_PROGRESS_MONITOR));

				buildRunnerHelper.removeOldMarkers(project, new SubProgressMonitor(monitor, TICKS_DELETE_MARKERS, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));

				buildRunnerHelper.greeting(kind, cfgName, toolchainName, isSupported);
				int state = buildRunnerHelper.build(new SubProgressMonitor(monitor, TICKS_EXECUTE_COMMAND, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));
				buildRunnerHelper.close();
				buildRunnerHelper.goodbye();

				if (state != ICommandLauncher.ILLEGAL_COMMAND) {
					buildRunnerHelper.refreshProject(cfgName, new SubProgressMonitor(monitor, TICKS_REFRESH_PROJECT, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));
				}
			} else {
				String msg = ManagedMakeMessages.getFormattedString("ManagedMakeBuilder.message.undefined.build.command", builder.getId()); //$NON-NLS-1$
				throw new CoreException(new Status(IStatus.ERROR, ManagedBuilderCorePlugin.PLUGIN_ID, msg, new Exception()));
			}
		} catch (Exception e) {
			String msg = ManagedMakeMessages.getFormattedString("ManagedMakeBuilder.message.error.build", //$NON-NLS-1$
					new String[] { project.getName(), configuration.getName() });
			throw new CoreException(new Status(IStatus.ERROR, ManagedBuilderCorePlugin.PLUGIN_ID, msg, e));
		} finally {
			try {
				buildRunnerHelper.close();
			} catch (IOException e) {
				ManagedBuilderCorePlugin.log(e);
			}
			monitor.done();
		}
		return isClean;
	}

	private String[] getCommandArguments(IBuilder builder, String[] targets) {
		String[] builderArgs = CommandLineUtil.argumentsToArray(builder.getBuildArguments());
		String[] args = new String[targets.length + builderArgs.length];
		System.arraycopy(builderArgs, 0, args, 0, builderArgs.length);
		System.arraycopy(targets, 0, args, builderArgs.length, targets.length);
		return args;
	}

	protected String[] getTargets(int kind, IBuilder builder) {
		String targets = ""; //$NON-NLS-1$
		switch (kind) {
		case IncrementalProjectBuilder.AUTO_BUILD :
			targets = builder.getAutoBuildTarget();
			break;
		case IncrementalProjectBuilder.INCREMENTAL_BUILD : // now treated as the same!
		case IncrementalProjectBuilder.FULL_BUILD :
			targets = builder.getIncrementalBuildTarget();
			break;
		case IncrementalProjectBuilder.CLEAN_BUILD :
			targets = builder.getCleanBuildTarget();
			break;
		}

		String targetsArray[] = CommandLineUtil.argumentsToArray(targets);


		return targetsArray;
	}

	protected Map<String, String> getEnvironment(IBuilder builder) throws CoreException {
		Map<String, String> envMap = new HashMap<String, String>();
		if (builder.appendEnvironment()) {
			ICConfigurationDescription cfgDes = ManagedBuildManager.getDescriptionForConfiguration(builder.getParent().getParent());
			IEnvironmentVariableManager mngr = CCorePlugin.getDefault().getBuildEnvironmentManager();
			IEnvironmentVariable[] vars = mngr.getVariables(cfgDes, true);
			for (IEnvironmentVariable var : vars) {
				envMap.put(var.getName(), var.getValue());
			}
		}

		// Add variables from build info
		Map<String, String> builderEnv = builder.getExpandedEnvironment();
		if (builderEnv != null)
			envMap.putAll(builderEnv);

		return envMap;
	}

	@Deprecated
	protected static String[] getEnvStrings(Map<String, String> env) {
		// Convert into env strings
		List<String> strings= new ArrayList<String>(env.size());
		for (Entry<String, String> entry : env.entrySet()) {
			StringBuilder buffer= new StringBuilder(entry.getKey());
			buffer.append('=').append(entry.getValue());
			strings.add(buffer.toString());
		}

		return strings.toArray(new String[strings.size()]);
	}

	private static void collectScannerInfoConsoleParsers(IProject project, IConfiguration cfg, URI workingDirectoryURI,
			IMarkerGenerator markerGenerator, List<IConsoleParser> parsers) {
		ICfgScannerConfigBuilderInfo2Set container = CfgScannerConfigProfileManager.getCfgScannerConfigBuildInfo(cfg);
		Map<CfgInfoContext, IScannerConfigBuilderInfo2> map = container.getInfoMap();

		String pathFromURI = EFSExtensionManager.getDefault().getPathFromURI(workingDirectoryURI);
		if(pathFromURI == null) {
			// fallback to CWD
			pathFromURI = System.getProperty("user.dir"); //$NON-NLS-1$
		}
		IPath workingDirectory = new Path(pathFromURI);

		int oldSize = parsers.size();

		if(container.isPerRcTypeDiscovery()){
			for (IResourceInfo rcInfo : cfg.getResourceInfos()) {
				ITool tools[];
				if(rcInfo instanceof IFileInfo){
					tools = ((IFileInfo)rcInfo).getToolsToInvoke();
				} else {
					tools = ((IFolderInfo)rcInfo).getFilteredTools();
				}
				for (ITool tool : tools) {
					IInputType[] types = tool.getInputTypes();

					if(types.length != 0){
						for (IInputType type : types) {
							CfgInfoContext context = new CfgInfoContext(rcInfo, tool, type);
							IScannerInfoConsoleParser parser = getScannerInfoConsoleParser(project, map, context, workingDirectory, markerGenerator);
							if (parser != null) {
								parsers.add(parser);
							}
						}
					} else {
						CfgInfoContext context = new CfgInfoContext(rcInfo, tool, null);
						IScannerInfoConsoleParser parser = getScannerInfoConsoleParser(project, map, context, workingDirectory, markerGenerator);
						if (parser != null) {
							parsers.add(parser);
						}
					}
				}
			}
		}

		if(parsers.size() == oldSize){
			CfgInfoContext context = new CfgInfoContext(cfg);
			IScannerInfoConsoleParser parser = getScannerInfoConsoleParser(project, map, context, workingDirectory, markerGenerator);
			if (parser != null) {
				parsers.add(parser);
			}
		}
	}

	private static IScannerInfoConsoleParser getScannerInfoConsoleParser(IProject project, Map<CfgInfoContext, IScannerConfigBuilderInfo2> map,
			CfgInfoContext context, IPath workingDirectory, IMarkerGenerator markerGenerator) {
		return ScannerInfoConsoleParserFactory.getScannerInfoConsoleParser(project, context.toInfoContext(), workingDirectory, map.get(context), markerGenerator, null);
	}
}
