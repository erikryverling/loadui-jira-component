//
// Copyright (C) 2011  Erik Rålenius (erik@ralenius.se)

// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

/**
 * Create JIRA issue for Web Page Runner failures.
 *
 * @name JIRA
 * @category output
 * @dependency org.codehaus.groovy:groovy-xmlrpc:0.7
 * @m2repo http://www.eviware.com/repository/maven2/
 */

import groovy.net.xmlrpc.XMLRPCServerProxy as Proxy
import com.eviware.loadui.util.layout.DelayedFormattedString
import com.eviware.loadui.api.events.ActionEvent
import com.eviware.loadui.api.events.PropertyEvent
import com.eviware.loadui.impl.component.ActivityStrategies

class JiraProxy extends Proxy {
    JiraProxy(url) { super(url) }
    Object invokeMethod(String methodname, args) {
        super.invokeMethod('jira1.'+methodname, args)
    }
}

createProperty('project', String)
createProperty('type', Integer, 1)
createProperty('priority', Long, 4)
createProperty('assignee', String)
createProperty('jiraServer', String)
createProperty('username', String)
password = createProperty('_password', String)
createProperty('maxCapturedFailures', Integer, 100)

status = 'Not ready'
issueDescription = ""
capturedFailures = 0
isConnected = false

capturedFailuresDisplay = new DelayedFormattedString('%d', 200, value {capturedFailures} )
statusDisplay = new DelayedFormattedString('%s', 200, value {status})

addEventListener( ActionEvent ) { event ->
    if (isConnected && event.key == 'START' ) {
        status = 'Capturing'
        setActivityStrategy(ActivityStrategies.BLINKING)
    }
    else if (isConnected && event.key == 'STOP') {
        status = 'Stopped'
        setActivityStrategy(ActivityStrategies.OFF)
    }
    else if (isConnected && event.key == 'COMPLETE') {
        if (capturedFailures > 0) {
            createIssue()
            issueDescription = ""
            setActivityStrategy(ActivityStrategies.OFF)
        } else {
            status = 'No captures'
        }
    }
   else if (event.key == 'RESET') {        
        capturedFailures = 0
        issuesDescription = ""
   }
}

output = { message ->
    if (capturedFailures < maxCapturedFailures.value) {
        if (isConnected && message['Status'] == false) {
            addFailureToIssue(message)
            capturedFailures++
        }
   }
}

onConnect = { outgoing, incoming ->
    println outgoing.label
    if (outgoing.terminalHolder.label == 'Web Page Runner' 
        && outgoing.label == 'Results') {
        isConnected = true
        status = 'Ready'
    }
}

onDisconnect = { outgoing, incoming ->
    isConnected = false
    status = 'Not ready'
}

onRelease = {
   capturedFailuresDisplay.release()
   statusDisplay.release()
}

// Layout
layout { 
    property(property: maxCapturedFailures, label: 'Max captured failures', min: 1, max: 100)
    separator(vertical: true)
    box(widget: 'display') {
        node(label: 'Failures captured', fString: capturedFailuresDisplay, constraints: 'wmin 75')
        node(label: 'Status', fString: statusDisplay, constraints: 'wmin 75')
   }
}

compactLayout {
    box(widget:'display') {
        node(label: 'Failures captured', fString: capturedFailuresDisplay, constraints: 'wmin 75')
        node(label: 'Status', fString: statusDisplay, constraints: 'wmin 75')
   }   
}

// Settings
settings(label: 'Issue properties') {
    property(property: project, label: 'Project (required)')
    property(property: assignee, label: 'Assignee')
    box {
        property(property: priority, label: 
            'Priority (1: Blocker, 2: Critical, 3: Major, 4: Minor, 5: Trivial)')
    }
}

settings(label: 'Authentication') {
    property(property: jiraServer, label: 'JIRA server')
    property(property: username, label: 'Username')
    property(property: password, widget: 'password', label: 'Pasword')
}

private def addFailureToIssue(message) {
    generatorsProperties = getGeneratorPropertiesAsMessage()
    formatedTimestamp = timestampToFormatedDate(message['TriggerTimestamp'])
    issueDescription += """Request to ${message['ID']} failed at $formatedTimestamp
$generatorsProperties
---

"""
}

private def timestampToFormatedDate(long timestamp) {
    return new Date(timestamp)
}

private def createIssue() {
    // JIRA won't accept null values
    if (assignee.value == null) {
        assignee.value = ""
    }
    
    Map issueData = 
    [
        summary: "loadUI: $capturedFailures failures captured while running ${canvas.label}",
        description: issueDescription,
        type: 1,
        assignee: assignee.value,
        project: project.value, 
        priority: priority.value
    ]
    
    String jiraURL = "${jiraServer.value}/rpc/xmlrpc"
    
    try {
        jira = new JiraProxy(jiraURL)
        loginToken = jira.login(username.value, password.value)
        issue = jira.createIssue(loginToken, issueData)
        status = 'Issue created'
    } 
    catch (exception) {
         log.error(exception.message, exception)
        status = 'Creation failed'     
    }
}

private def getGeneratorPropertiesAsMessage() {
    message = ""
    for (component in canvas.components) {
        if (component.category == 'generators') {
            message = message + "\n" + "Generator: " + component.label + "\n"
            for (property in component.properties) {
                if (isAllowed(property.key)) {
                    message = message + property.key.capitalize() + ": " + property.value + "\n"
                }
            }
        }
    }
    return message 
}

private def isAllowed(key) {
    return key[0] != '_' && key != 'ModelItem.description';
}
