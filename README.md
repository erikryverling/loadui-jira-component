This is a [LoadUI custom component](http://www.loadui.org/Custom-Components) that creates a JIRA issue for all Web Page Runner failures after a completed test.

*This was the winning entry of the [LoadUI component competition](http://www.loadui.org/Competition/component-competition.html)*

Updates
-------
**2011-12-01** Fixed the problems with the missing connection point and also the missing *smack* depencency.

Usage
-----

1. The JIRA component must be connected to the leftmost output terminal (resultTerminal) of the Web Page Runner component. The status message will change to Ready when you are connected.
2. Next, specify the maximum number of captured failures you want in your JIRA issue by turning the knob.
3. Then click on the Settings button and fill out the settings. Observe! The password is sent unencrypted over XML-RPC to the specified JIRA server. Use SSL if possible.
4. Now we are ready to capture some failures. Keep an eye on the Failures captured counter to know how many failures has been captured.
5. When the running test has been completed, an issue with all captured failures is created including the time, URL and generator properties used when the failure was captured.

Troubleshooting
---------------
If you get the status Issue failed when trying to create the JIRA issue, please check you settings. You could also try to run the loadUItest.bat script to a more specified error message.

