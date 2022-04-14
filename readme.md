Yet some other Terminate Buttons for Eclipse
====

This project is based off the work of Nick Tan, with modifications provided by Christoph142 and missedone.

## 2.x branch requires Java 11!

## The problem
Eclipse uses the standard Java call Process.destroy() which sends SIGTERM (or WM_CLOSE) but also closes stdin, stdout and stderr. Closing these streams can cause a developer to lose insight into
the shutdown process.

## The solution
YaTB adds 4 buttons. 2 buttons to terminate all applications (gracefully or forcefully), and 2 buttons to stop the currently active application (gracefully or forcefully). There are no follow up
signals so it is up to the developer to know when to force shutdown vs graceful. This is intentional. 

Note: On windows systems, Java does not support the ProcessHandle.destroyForcibly call, so we get around that by running:

```shell
taskkill /f /pid %pid%
```

## Installation

1. Download the dist folder in this repository
2. run 'mvn package' in the root directory
3. in Eclipse open Help -> Install new Software... -> Add... -> Local...
3. Select the site/target/site folder
4. Select the plugin and proceed like normal
