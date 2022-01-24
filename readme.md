Yet some other Terminate Buttons for Eclipse
====

This project is based off the work of Nick Tan, with modifications provided by Christoph142 and missedone.

This plugin adds four buttons to the Eclipse console. Two for each kind of shutdown mentioned below.
One of each set is for the active console, the other one to rule them all.

2.x branch requires Java 11!

## Why

### Soft shutdown
Eclipse stops processes using the built in Java call Process.destroy(). This will send SIGTERM on Unix systems, or WM_CLOSE on windows. It also closes stdin, stdout, and stderr
so the developer no longer has insight into any logging from the application if it fails to stop.
These buttons call the new ProcessHandle.destroy() api which only sends SIGTERM/WM_CLOSE, allowing the ide to continue showing any output.

### Hard shutdown
If a process should fail to stop in a timely manner, the hard shutdown buttons will force the application to shutdown using SIGKILL on Unix systems, or by calling "taskkill /f /pid %pid%" on 
windows. 

## Installation

1. Download the dist folder in this repository
2. run 'mvn package' in the root directory
3. in Eclipse open Help -> Install new Software... -> Add... -> Local...
3. Select the site/target folder
4. Select the plugin and proceed like normal
