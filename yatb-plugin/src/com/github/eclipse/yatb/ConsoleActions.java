package com.github.eclipse.yatb;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.part.IPageBookViewPage;

public class ConsoleActions implements IConsolePageParticipant {

	private IPageBookViewPage page;
	private Action terminateHardAction;
	private Action terminateAllHardAction;
	private Action terminateSoftAction;
	private Action terminateAllSoftAction;
	private IActionBars bars;
	private IConsole console;

	@Override
	public void init(final IPageBookViewPage page, final IConsole console) {
		this.console = console;
		this.page = page;
		this.bars = page.getSite().getActionBars();

		terminateHardAction = createButton("Kill Process", "/icons/terminate_hard.gif", true);
		terminateSoftAction = createButton("Request Shutdown from Process", "/icons/terminate_soft.gif", false);

		terminateAllHardAction = createAllButton("Kill All Processes", "/icons/terminate_all_hard.gif", true);
		terminateAllSoftAction = createAllButton("Request Shutdown from all Processes", "/icons/terminate_all_soft.gif",
				false);

		bars.getMenuManager().add(new Separator());

		IToolBarManager toolbarManager = bars.getToolBarManager();

		toolbarManager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, terminateHardAction);
		toolbarManager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, terminateAllHardAction);
		toolbarManager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, terminateSoftAction);
		toolbarManager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, terminateAllSoftAction);

		bars.updateActionBars();
	}

	private Action createButton(String name, String icon, boolean hard) {
		return new Action(name, ImageDescriptor.createFromFile(getClass(), icon)) {
			@Override
			public void run() {
				if (console instanceof TextConsole) {
					RuntimeProcess runtimeProcess = (RuntimeProcess) ((TextConsole) console)
							.getAttribute(IDebugUIConstants.ATTR_CONSOLE_PROCESS);
					stopProcess(runtimeProcess.getLaunch(), hard);
				}
			}
		};
	}

	private Action createAllButton(String name, String icon, boolean hard) {
		return new Action(name, ImageDescriptor.createFromFile(getClass(), icon)) {
			@Override
			public void run() {
				Arrays.stream(DebugPlugin.getDefault().getLaunchManager().getLaunches())
						.forEach(l -> stopProcess(l, hard));
			}
		};
	}

	private void stopProcess(ILaunch launch, boolean hard) {
		if (!launch.isTerminated()) {
			Arrays.stream(launch.getProcesses()).forEach(p -> kill(p, hard));
		}
	}

	private final void kill(IProcess p, boolean hard) {
		try {
			Method m = p.getClass().getDeclaredMethod("getSystemProcess");
			m.setAccessible(true);
			Process proc = (Process) m.invoke(p);

			if (hard)
				proc.destroyForcibly();
			else
				proc.destroy();
		} catch (ReflectiveOperationException e) {
			Activator.log(e);
		}
	}

	@Override
	public void dispose() {
		terminateHardAction = null;
		terminateAllHardAction = null;
		terminateSoftAction = null;
		terminateAllSoftAction = null;
		bars = null;
		page = null;
	}

	@Override
	public Object getAdapter(Class adapter) {
		return null;
	}

	@Override
	public void activated() {
		updateVis();
	}

	@Override
	public void deactivated() {
		updateVis();
	}

	private void updateVis() {
		if (page == null)
			return;
		terminateHardAction.setEnabled(true);
		terminateAllHardAction.setEnabled(true);
		terminateSoftAction.setEnabled(true);
		terminateAllSoftAction.setEnabled(true);
		bars.updateActionBars();
	}

}
