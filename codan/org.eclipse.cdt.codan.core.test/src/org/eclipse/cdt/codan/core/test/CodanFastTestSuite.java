/*******************************************************************************
 * Copyright (c) 2004, 2011 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     QNX Software Systems - initial API and implementation
 *     Markus Schorn (Wind River Systems)
 *******************************************************************************/
package org.eclipse.cdt.codan.core.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.cdt.codan.core.cfg.ControlFlowGraphTest;
import org.eclipse.cdt.codan.core.cxx.CxxAstUtilsTest;
import org.eclipse.cdt.codan.core.param.BasicProblemPreferenceTest;
import org.eclipse.cdt.codan.core.param.ListProblemPreferenceTest;
import org.eclipse.cdt.codan.core.param.MapProblemPreferenceTest;

public class CodanFastTestSuite extends TestSuite {
	public CodanFastTestSuite() {
	}

	public CodanFastTestSuite(Class<? extends TestCase> theClass, String name) {
		super(theClass, name);
	}

	public CodanFastTestSuite(Class<? extends TestCase> theClass) {
		super(theClass);
	}

	public CodanFastTestSuite(String name) {
		super(name);
	}

	public static Test suite() {
		final CodanFastTestSuite suite = new CodanFastTestSuite();
		suite.addTestSuite(BasicProblemPreferenceTest.class);
		suite.addTestSuite(ListProblemPreferenceTest.class);
		suite.addTestSuite(MapProblemPreferenceTest.class);
		suite.addTestSuite(CxxAstUtilsTest.class);
		suite.addTestSuite(ControlFlowGraphTest.class);
		return suite;
	}
}
