/*******************************************************************************
 * Copyright (c) 2015, 2017 Ericsson and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.cdt.debug.gdbjtag.core.dsf.gdb.service.extensions;

import org.eclipse.cdt.debug.gdbjtag.core.dsf.gdb.service.GDBJtagControl_7_12;
import org.eclipse.cdt.dsf.debug.service.command.ICommandControlService;
import org.eclipse.cdt.dsf.gdb.service.GdbDebugServicesFactory;
import org.eclipse.cdt.dsf.mi.service.command.CommandFactory;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.debug.core.ILaunchConfiguration;

/**
 * Top-level class in the version hierarchy of the JTag implementations of {@link ICommandControlService}.
 * <br> 
 * Extenders should subclass this class for their special needs, which will allow
 * them to always extend the most recent version of the service.
 * For example, if GDB<Service>_7_9 is added, this GDB<Service>_HEAD class
 * will be changed to extend it instead of the previous version, therefore
 * automatically allowing extenders to be extending the new class.
 *
 * NOTE: Older versions of GDB that were already using an extending class,
 *       will automatically start using the new service version, which may
 *       not be desirable.  Extenders should update how they extend
 *       GdbDebugServicesFactory to properly choose the version of the
 *       service that should be used for older GDBs.
 *       
 *       On the contrary, not using GDB<Service>_HEAD requires the
 *       extender to update how they extend GdbDebugServicesFactory
 *       whenever a new GDB<Service> version is added.
 *       
 *       Extenders that prefer to focus on the latest GDB version are
 *       encouraged to extend GDB<Service>_HEAD.
 * 
 * @since 8.5
 */
public class GDBJtagControl_HEAD extends GDBJtagControl_7_12 {
	public GDBJtagControl_HEAD(DsfSession session, ILaunchConfiguration config, CommandFactory factory) {
		super(session, config, factory);
		
		validateGdbVersion(session);
	}
	
	protected String getMinGDBVersionSupported() { return GdbDebugServicesFactory.GDB_7_12_VERSION; }
	
	protected void validateGdbVersion(DsfSession session) {
		GdbDebugServicesFactory.validateGdbVersion(session, getMinGDBVersionSupported(), this);
	}
}
