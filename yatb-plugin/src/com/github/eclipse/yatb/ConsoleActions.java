package com.github.eclipse.yatb;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

import org.eclipse.core.runtime.Platform;
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
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.part.IPageBookViewPage;

public class ConsoleActions implements IConsolePageParticipant {

	private IPageBookViewPage _page;
	private Action _forceStopAction;
	private Action _forceStopAllAction;
	private Action _stopAction;
	private Action _stopAllAction;
	private IActionBars _bars;
	private IOConsole _console;

	private boolean _enabled = true;
	private ProcessHandle _handle;

	@Override
	public void init(IPageBookViewPage page, IConsole console) {
		_console = (IOConsole) console;
		_page = page;
		_bars = page.getSite().getActionBars();

		_forceStopAction = createButton("Force Stop Process", "/icons/terminate_hard.gif", true);
		_stopAction = createButton("Shutdown Process", "/icons/terminate_soft.gif", false);

		_forceStopAllAction = createAllButton("Force Stop All Processes", "/icons/terminate_all_hard.gif", true);
		_stopAllAction = createAllButton("Shutdown all Processes", "/icons/terminate_all_soft.gif", false);

		_bars.getMenuManager().add(new Separator());

		final IToolBarManager toolbarManager = _bars.getToolBarManager();

		toolbarManager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, _forceStopAction);
		toolbarManager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, _forceStopAllAction);
		toolbarManager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, _stopAction);
		toolbarManager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, _stopAllAction);

		_bars.updateActionBars();

		try {
			RuntimeProcess p = (RuntimeProcess) _console.getAttribute(IDebugUIConstants.ATTR_CONSOLE_PROCESS);
			handle(p).ifPresent(handle -> {
				_handle = handle;
				_handle.onExit().thenRun(() -> {
					_enabled = false;
					_forceStopAction.setEnabled(_enabled);
					_stopAction.setEnabled(_enabled);
				});
			});
		} catch (Exception e) {
			Activator.log(e);
			_enabled = false;
		}
	}

	private static final Optional<ProcessHandle> handle(IProcess p) {
		try {
			final Method m = p.getClass().getDeclaredMethod("getSystemProcess");
			m.setAccessible(true);
			final Process proc = (Process) m.invoke(p);
			if (proc == null)
				return Optional.empty();
			final long pid = proc.pid();
			// Could just call Process.toHandle()... but it's documented as possibly
			// throwing unsupported.
			return ProcessHandle.of(pid);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Action createButton(String name, String icon, boolean hard) {
		return new Action(name, ImageDescriptor.createFromFile(getClass(), icon)) {
			@Override
			public void run() {
				kill(_handle, hard);
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
			Arrays.stream(launch.getProcesses()).map(ConsoleActions::handle).flatMap(Optional::stream)
					.forEach(p -> kill(p, hard));
		}
	}

	private final void kill(ProcessHandle handle, boolean hard) {
		try {
			// Windows ProcessImpl doesn't implement destroyForcibly.
			if (hard && Platform.OS_WIN32.equals(Platform.getOS()))
				Runtime.getRuntime().exec("taskkill /f /pid " + _handle.pid());
			else {
				if (hard)
					handle.destroyForcibly();
				else
					handle.destroy();
			}

		} catch (IOException e) {
			Activator.log(e);
		}
	}

	@Override
	public void dispose() {
		_forceStopAction = null;
		_forceStopAllAction = null;
		_stopAction = null;
		_stopAllAction = null;
		_bars = null;
		_page = null;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
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
		if (_page == null)
			return;

		_forceStopAction.setEnabled(_enabled);
		_stopAction.setEnabled(_enabled);

		_forceStopAllAction.setEnabled(true);
		_stopAllAction.setEnabled(true);

		_bars.updateActionBars();
	}

}
