package com.github.eclipse.yatb;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.eclipse.debug.internal.ui.views.console.ProcessConsole;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.part.IPageSite;

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
		IPageSite site = page.getSite();
		this.bars = site.getActionBars();

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
				if (console instanceof ProcessConsole) {
					RuntimeProcess runtimeProcess = (RuntimeProcess) ((ProcessConsole) console)
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
				// Dont' really know if Objects::nonNull is actually required...
				Arrays.stream(DebugPlugin.getDefault().getLaunchManager().getLaunches()).filter(Objects::nonNull)
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

			if (Platform.OS_WIN32.equals(Platform.getOS())) {
				windowsKill(proc, hard);
			} else {
				unixKill(proc, hard);
			}
		} catch (ReflectiveOperationException e) {
			Activator.log(e);
		}
	}

	private final void unixKill(Process p, boolean hard) {
		try {
			if (hard)
				Runtime.getRuntime().exec("kill -SIGKILL " + p.pid());
			else
				Runtime.getRuntime().exec("kill -SIGTERM " + p.pid());
		} catch (Exception e) {
			Activator.log(e);
		}
	}

	private final void windowsKill(Process p, boolean hard) {
		try {
			if (hard)
				Runtime.getRuntime().exec("taskkill /pid " + p.pid());
			else
				Runtime.getRuntime().exec("taskkill /f /pid " + p.pid());
		} catch (Exception e) {
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
