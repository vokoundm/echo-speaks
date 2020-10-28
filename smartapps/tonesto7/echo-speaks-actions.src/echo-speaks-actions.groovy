/**
 *  Echo Speaks Actions
 *
 *  Copyright 2018, 2019, 2020 Anthony Santilli
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

String appVersion()  { return "3.6.4.2" }
String appModified() { return "2020-10-28" }
String appAuthor()   { return "Anthony S." }
Boolean isBeta()     { return false }
Boolean isST()       { return (getPlatform() == "SmartThings") }

// TODO: Add Lock Code triggers
// TODO: Create reports options
 /*
    Custom Reports for multiple builtin in routine items.
    Reports for home status like temp, contact, alarm status
*/
definition(
    name: "Echo Speaks - Actions",
    namespace: "tonesto7",
    author: "Anthony Santilli",
    description: "DO NOT INSTALL FROM MARKETPLACE\n\nAllows you to create echo device actions based on device/location events in your SmartThings/Hubitat home.",
    category: "My Apps",
    parent: "tonesto7:Echo Speaks",
    iconUrl: "https://raw.githubusercontent.com/tonesto7/echo-speaks/master/resources/icons/es_actions.png",
    iconX2Url: "https://raw.githubusercontent.com/tonesto7/echo-speaks/master/resources/icons/es_actions.png",
    iconX3Url: "https://raw.githubusercontent.com/tonesto7/echo-speaks/master/resources/icons/es_actions.png",
    importUrl  : "https://raw.githubusercontent.com/tonesto7/echo-speaks/master/smartapps/tonesto7/echo-speaks-actions.src/echo-speaks-actions.groovy",
    pausable: true)

preferences {
    page(name: "startPage")
    page(name: "uhOhPage")
    page(name: "codeUpdatePage")
    page(name: "mainPage")
    page(name: "prefsPage")
    page(name: "triggersPage")
    page(name: "conditionsPage")
    page(name: "condTimePage")
    page(name: "actionsPage")
    page(name: "actionTiersPage")
    page(name: "actionTiersConfigPage")
    page(name: "actTrigTasksPage")
    page(name: "actTierStartTasksPage")
    page(name: "actTierStopTasksPage")
    page(name: "actNotifPage")
    page(name: "actNotifTimePage")
    page(name: "actionHistoryPage")
    page(name: "searchTuneInResultsPage")
    page(name: "uninstallPage")
    page(name: "namePage")
}

def startPage() {
    if(parent != null) {
        if(!state?.isInstalled && parent?.childInstallOk() != true) { return uhOhPage() }
        else { state?.isParent = false; return (minVersionFailed()) ? codeUpdatePage() : mainPage(); }
    } else { return uhOhPage(); }
}

def codeUpdatePage () {
    return dynamicPage(name: "codeUpdatePage", title: "Update is Required", install: false, uninstall: false) {
        section() { paragraph "Looks like your Action App needs an update\n\nPlease make sure all app and device code is updated to the most current version\n\nOnce updated your actions will resume normal operation.", required: true, state: null, image: getAppImg("exclude") }
    }
}

def uhOhPage () {
    return dynamicPage(name: "uhOhPage", title: "This install Method is Not Allowed", install: false, uninstall: true) {
        section() {
            paragraph "HOUSTON WE HAVE A PROBLEM!\n\nEcho Speaks - Actions can't be directly installed from the Marketplace.\n\nPlease use the Echo Speaks SmartApp to configure them.", required: true,
            state: null, image: getAppImg("exclude")
        }
        if(isST()) { remove("Remove this invalid Action", "WARNING!!!", "This is a BAD install of an Action SHOULD be removed") }
    }
}

def appInfoSect(sect=true)	{
    def instDt = state?.dateInstalled ? fmtTime(state?.dateInstalled, "MMM dd '@' h:mm a", true) : null
    section() { href "empty", title: pTS("${app?.name}", getAppImg("es_actions", true)), description: "${instDt ? "Installed: ${instDt}\n" : ""}Version: ${appVersion()}", image: getAppImg("es_actions") }
}

List cleanedTriggerList() {
    List newList = []
    settings?.triggerTypes?.each {
        newList?.push(it?.toString()?.split("::")[0] as String)
    }
    return newList?.unique()
}

String selTriggerTypes(type) {
    return settings?.triggerTypes?.findAll { it?.startsWith(type as String) }?.collect { it?.toString()?.split("::")[1] }?.join(", ")
}

private buildTriggerEnum() {
    List enumOpts = []
    Map buildItems = [:]
    buildItems["Date/Time"] = ["scheduled":"Scheduled Time"]?.sort{ it?.key }
    buildItems["Location"] = ["mode":"Modes", "routineExecuted":"Routines"]?.sort{ it?.key }
    if(!isST()) {
        buildItems?.Location?.remove("routineExecuted")
        //TODO: Once I can find a reliable method to list the scenes and subscribe to events on Hubitat I will re-activate
        // buildItems?.Location?.scene = "Scenes"
    }
    buildItems["Sensor Devices"] = ["contact":"Contacts | Doors | Windows", "battery":"Battery Level", "motion":"Motion", "illuminance": "Illuminance/Lux", "presence":"Presence", "temperature":"Temperature", "humidity":"Humidity", "water":"Water", "power":"Power", "acceleration":"Accelorometers"]?.sort{ it?.value }
    buildItems["Actionable Devices"] = ["lock":"Locks", "button":"Buttons", "switch":"Switches/Outlets", "level":"Dimmers/Level", "door":"Garage Door Openers", "valve":"Valves", "shade":"Window Shades", "thermostat":"Thermostat"]?.sort{ it?.value }
    if(!isST()) {
        buildItems["Actionable Devices"]?.remove("button")
        buildItems["Button Devices"] = ["pushed":"Button (Pushable)", "released":"Button (Releasable)", "held":"Button (Holdable)", "doubleTapped":"Button (Double Tapable)"]?.sort{ it?.value }
    }
    buildItems["Safety & Security"] = ["alarm": "${getAlarmSystemName()}", "smoke":"Fire/Smoke", "carbon":"Carbon Monoxide", "guard":"Alexa Guard"]?.sort{ it?.value }
    if(!parent?.guardAutoConfigured()) { buildItems["Safety & Security"]?.remove("guard") }
    if(isST()) {
        buildItems?.each { key, val-> addInputGrp(enumOpts, key, val) }
        // log.debug "enumOpts: $enumOpts"
        return enumOpts
    } else { return buildItems?.collectEntries { it?.value }?.sort { it?.key } }
}

private buildActTypeEnum() {
    List enumOpts = []
    Map buildItems = [:]
    buildItems["Speech"] = ["speak":"Speak", "announcement":"Announcement", "speak_tiered":"Speak (Tiered)", "announcement_tiered":"Announcement (Tiered)"]?.sort{ it?.key }
    buildItems["Built-in Sounds"] = ["sounds":"Play a Sound"]?.sort{ it?.key }
    buildItems["Built-in Responses"] = ["weather":"Weather Report", "builtin":"Birthday, Compliments, Facts, Jokes, News, Stories, Traffic, and more...", "calendar":"Read Calendar Events"]?.sort{ it?.key }
    buildItems["Media/Playback"] = ["music":"Play Music/Playlists", "playback":"Playback/Volume Control"]?.sort{ it?.key }
    buildItems["Alarms/Reminders"] = ["alarm":"Create Alarm", "reminder":"Create Reminder"]?.sort{ it?.key }
    buildItems["Devices Settings"] = ["wakeword":"Change Wake Word", "dnd":"Set Do Not Disturb", "bluetooth":"Bluetooth Control"]?.sort{ it?.key }
    buildItems["Custom"] = ["sequence":"Execute Sequence", "alexaroutine": "Execute Alexa Routine(s)"]?.sort{ it?.key }
    if(isST()) {
        buildItems?.each { key, val-> addInputGrp(enumOpts, key, val) }
        return enumOpts
    } else { return buildItems?.collectEntries { it?.value } }
}

def mainPage() {
    Boolean newInstall = (state?.isInstalled != true)
    return dynamicPage(name: "mainPage", nextPage: (!newInstall ? "" : "namePage"), uninstall: (newInstall == true), install: !newInstall) {
        appInfoSect()
        Boolean paused = isPaused()
        Boolean dup = (settings?.duplicateFlag == true || state?.dupPendingSetup == true)
        if(dup) {
            state?.dupOpenedByUser = true
            section() { paragraph pTS("This Action was just created from an existing action.\n\nPlease review the settings and save to activate...", getAppImg("pause_orange", true), false, "red"), required: true, state: null, image: getAppImg("pause_orange") }
        }
        if(paused) {
            section() {
                paragraph pTS("This Action is currently in a paused state...\nTo edit the please un-pause", getAppImg("pause_orange", true), false, "red"), required: true, state: null, image: getAppImg("pause_orange")
            }
        } else {
            if(settings?.cond_mode && !settings?.cond_mode_cmd) { settingUpdate("cond_mode_cmd", "are", "enum") }
            Boolean trigConf = triggersConfigured()
            Boolean condConf = conditionsConfigured()
            Boolean actConf = executionConfigured()
            section(sTS("Configuration: Part 1")) {
                if(isST()) {
                    input "actionType", "enum", title: inTS("Action Type", getAppImg("list", true)), description: "", groupedOptions: buildActTypeEnum(), multiple: false, required: true, submitOnChange: true, image: getAppImg("list")
                } else { input "actionType", "enum", title: inTS("Action Type", getAppImg("list", true)), description: "", options: buildActTypeEnum(), multiple: false, required: true, submitOnChange: true, image: getAppImg("list") }
            }
            section (sTS("Configuration: Part 2")) {
                if(settings?.actionType) {
                    href "triggersPage", title: inTS("Action Triggers", getAppImg("trigger", true)), description: getTriggersDesc(), state: (trigConf ? "complete" : ""), required: true, image: getAppImg("trigger")
                } else { paragraph pTS("These options will be shown once the action type is configured.", getAppImg("info", true)) }
            }
            section(sTS("Configuration: Part 3")) {
                if(settings?.actionType && trigConf) {
                    href "conditionsPage", title: inTS("Condition/Restrictions\n(Optional)", getAppImg("conditions", true)), description: getConditionsDesc(), state: (condConf ? "complete": ""), image: getAppImg("conditions")
                } else { paragraph pTS("These options will be shown once the triggers are configured.", getAppImg("info", true)) }
            }
            section(sTS("Configuration: Part 4")) {
                if(settings?.actionType && trigConf) {
                    href "actionsPage", title: inTS("Execution Config", getAppImg("es_actions", true)), description: getActionDesc(), state: (actConf ? "complete" : ""), required: true, image: getAppImg("es_actions")
                } else { paragraph pTS("These options will be shown once the triggers are configured.", getAppImg("info", true)) }
            }
            if(settings?.actionType && trigConf && actConf) {
                section(sTS("Notifications:")) {
                    def t0 = getAppNotifDesc()
                    href "actNotifPage", title: inTS("Send Notifications", getAppImg("notification2", true)), description: (t0 ? "${t0}\nTap to modify" : "Tap to configure"), state: (t0 ? "complete" : null), image: getAppImg("notification2")
                }
                // getTierStatusSection()
            }
        }

        section(sTS("Action History")) {
            href "actionHistoryPage", title: inTS("View Action History", getAppImg("tasks", true)), description: "", image: getAppImg("tasks")
        }

        section(sTS("Preferences")) {
            href "prefsPage", title: inTS("Debug/Preferences", getAppImg("settings", true)), description: "", image: getAppImg("settings")
            if(state?.isInstalled) {
                input "actionPause", "bool", title: inTS("Pause Action?", getAppImg("pause_orange", true)), defaultValue: false, submitOnChange: true, image: getAppImg("pause_orange")
                if(actionPause) { unsubscribe() }
                if(!paused) {
                    input "actTestRun", "bool", title: inTS("Test this action?", getAppImg("testing", true)), description: "", required: false, defaultValue: false, submitOnChange: true, image: getAppImg("testing")
                    if(actTestRun) { executeActTest() }
                }
            }
        }
        if(state?.isInstalled) {
            section(sTS("Name this Action:")) {
                input "appLbl", "text", title: inTS("Action Name", getAppImg("name_tag", true)), description: "", required:true, submitOnChange: true, image: getAppImg("name_tag")
            }
            section(sTS("Remove Action:")) {
                href "uninstallPage", title: inTS("Remove this Action", getAppImg("uninstall", true)), description: "Tap to Remove...", image: getAppImg("uninstall")
            }
            section(sTS("Feature Requests/Issue Reporting"), hideable: true, hidden: true) {
                def issueUrl = "https://github.com/tonesto7/echo-speaks/issues/new?assignees=tonesto7&labels=bug&template=bug_report.md&title=%28ACTIONS+BUG%29+&projects=echo-speaks%2F6"
                def featUrl = "https://github.com/tonesto7/echo-speaks/issues/new?assignees=tonesto7&labels=enhancement&template=feature_request.md&title=%5BActions+Feature+Request%5D&projects=echo-speaks%2F6"
                href url: featUrl, style: "external", required: false, title: inTS("New Feature Request", getAppImg("www", true)), description: "Tap to open browser", image: getAppImg("www")
                href url: issueUrl, style: "external", required: false, title: inTS("Report an Issue", getAppImg("www", true)), description: "Tap to open browser", image: getAppImg("www")
            }
        }
    }
}

def prefsPage() {
    return dynamicPage(name: "prefsPage", install: false, uninstall: false) {
        section(sTS("Logging:")) {
            input "logInfo", "bool", title: inTS("Show Info Logs?", getAppImg("debug", true)), required: false, defaultValue: true, submitOnChange: true, image: getAppImg("debug")
            input "logWarn", "bool", title: inTS("Show Warning Logs?", getAppImg("debug", true)), required: false, defaultValue: true, submitOnChange: true, image: getAppImg("debug")
            input "logError", "bool", title: inTS("Show Error Logs?", getAppImg("debug", true)), required: false, defaultValue: true, submitOnChange: true, image: getAppImg("debug")
            input "logDebug", "bool", title: inTS("Show Debug Logs?", getAppImg("debug", true)), description: "Auto disables after 6 hours", required: false, defaultValue: false, submitOnChange: true, image: getAppImg("debug")
            input "logTrace", "bool", title: inTS("Show Detailed Logs?", getAppImg("debug", true)), description: "Only enable when asked to.\n(Auto disables after 6 hours)", required: false, defaultValue: false, submitOnChange: true, image: getAppImg("debug")
        }
        if(advLogsActive()) { logsEnabled() }
        section(sTS("Other:")) {
            input "clrEvtHistory", "bool", title: inTS("Clear Device Event History?", getAppImg("reset", true)), description: "", required: false, defaultValue: false, submitOnChange: true, image: getAppImg("reset")
            if(clrEvtHistory) { clearEvtHistory() }
        }
    }
}

def namePage() {
    return dynamicPage(name: "namePage", install: true, uninstall: false) {
        section(sTS("Name this Automation:")) {
            input "appLbl", "text", title: inTS("Label this Action", getAppImg("name_tag", true)), description: "", required:true, submitOnChange: true, image: getAppImg("name_tag")
        }
    }
}

def actionHistoryPage() {
    return dynamicPage(name: "actionHistoryPage", install: false, uninstall: false) {
        section() {
            getActionHistory()
        }
        if(atomicState?.actionHistory?.size()) {
            section("") {
                input "clearActionHistory", "bool", title: inTS("Clear Action History?", getAppImg("reset", true)), description: "Clears Stored Action History.", defaultValue: false, submitOnChange: true, image: getAppImg("reset")
                if(settings?.clearActionHistory) { settingUpdate("clearActionHistory", "false", "bool"); atomicState?.actionHistory = []; }
            }
        }
    }
}

// TODO: Add flag to check for the old schedule settings and pause the action, and notifiy the user.
private scheduleConvert() {
    if(settings?.trig_scheduled_time || settings?.trig_scheduled_sunState && !settings?.trig_scheduled_type) {
        if(settings?.trig_scheduled_sunState) { settingUpdate("trig_scheduled_type", "${settings?.trig_scheduled_sunState}", "enum"); settingRemove("trig_scheduled_sunState"); }
        else if(settings?.trig_scheduled_time && settings?.trig_scheduled_recurrence) {
            if(settings?.trig_scheduled_recurrence == "Once") { settingUpdate("trig_scheduled_type", "One-Time", "enum") }
            if(settings?.trig_scheduled_recurrence in ["Daily", "Weekly", "Monthly"]) { settingUpdate("trig_scheduled_type", "Recurring", "enum") }
        }
    }
}

def triggersPage() {
    return dynamicPage(name: "triggersPage", nextPage: "mainPage", uninstall: false, install: false) {
        List stRoutines = getLocationRoutines() ?: []
        Boolean isTierAct = isTierAction()
        Boolean showSpeakEvtVars = false
        section (sTS("Select Capabilities")) {
            if(isST()) {
                input "triggerEvents", "enum", title: "Select Trigger Event(s)", groupedOptions: buildTriggerEnum(), multiple: true, required: true, submitOnChange: true, image: getAppImg("trigger")
            } else {
                input "triggerEvents", "enum", title: inTS("Select Trigger Event(s)", getAppImg("trigger", true)), options: buildTriggerEnum(), multiple: true, required: true, submitOnChange: true
            }
        }
        if (settings?.triggerEvents?.size()) {
            Integer trigItemCnt = 0
            Integer trigEvtCnt = settings?.triggerEvents?.size()
            if(!(settings?.triggerEvents in ["Scheduled", "Weather"])) { showSpeakEvtVars = true }
            if (valTrigEvt("scheduled")) {
                section(sTS("Time Based Events"), hideable: true) {
                    List schedTypes = ["One-Time", "Recurring", "Sunrise", "Sunset"]
                    input "trig_scheduled_type", "enum", title: inTS("Schedule Type?", getAppImg("checkbox", true)), options: schedTypes, multiple: false, required: true, submitOnChange: true, image: getAppImg("checkbox")
                    if(settings?.trig_scheduled_type) {
                        String schedType = settings?.trig_scheduled_type as String
                        switch(schedType as String) {
                            case "One-Time":
                            case "Recurring":
                                input "trig_scheduled_time", "time", title: inTS("Trigger Time?", getAppImg("clock", true)), required: false, submitOnChange: true, image: getAppImg("clock")
                                if(settings?.trig_scheduled_time && schedType == "Recurring") {
                                    List recurOpts = ["Daily", "Weekly", "Monthly"]
                                    input "trig_scheduled_recurrence", "enum", title: inTS("Recurrence?", getAppImg("day_calendar", true)), description: "", multiple: false, required: true, submitOnChange: true, options: recurOpts, defaultValue: "Once", image: getAppImg("day_calendar")
                                    // TODO: Build out the scheduling some more with quick items like below
                                    /*
                                        At 6 pm on the last day of every month: (0 0 18 L * ?)
                                        At 6 pm on the 3rd to last day of every month: (0 0 18 L-3 * ?)
                                        At 10:30 am on the last Thursday of every month: (0 30 10 ? * 5L)
                                        At 6 pm on the last Friday of every month during the years 2015, 2016 and 2017: (0 0 18 ? * 6L 2015-2017)
                                        At 10 am on the third Monday of every month: (0 0 10 ? * 2#3)
                                        At 12 am midnight on every day for five days starting on the 10th day of the month: (0 0 0 10/5 * ?)
                                    */
                                    if(settings?.trig_scheduled_recurrence) {
                                        switch(settings?.trig_scheduled_recurrence) {
                                            case "Daily":
                                                input "trig_scheduled_weekdays", "enum", title: inTS("Only of these Days of the Week?", getAppImg("day_calendar", true)), description: "(Optional)", multiple: true, required: false, submitOnChange: true, options: daysOfWeekMap(), image: getAppImg("day_calendar")
                                                break

                                            case "Weekly":
                                                input "trig_scheduled_weekdays", "enum", title: inTS("Days of the Week?", getAppImg("day_calendar", true)), description: "", multiple: true, required: true, submitOnChange: true, options: daysOfWeekMap(), image: getAppImg("day_calendar")
                                                input "trig_scheduled_weeks", "enum", title: inTS("Only these Weeks on the Month?", getAppImg("day_calendar", true)), description: "(Optional)", multiple: true, required: false, submitOnChange: true, options: weeksOfMonthMap(), image: getAppImg("day_calendar")
                                                input "trig_scheduled_months", "enum", title: inTS("Only on these Months?", getAppImg("day_calendar", true)), description: "(Optional)", multiple: true, required: false, submitOnChange: true, options: monthMap(), image: getAppImg("day_calendar")
                                                break

                                            case "Monthly":
                                                input "trig_scheduled_daynums", "enum", title: inTS("Days of the Month?", getAppImg("day_calendar", true)), description: (!settings?.trig_scheduled_weeks ? "(Optional)" : ""), multiple: true, required: (!settings?.trig_scheduled_weeks), submitOnChange: true, options: (1..31)?.collect { it as String }, image: getAppImg("day_calendar")
                                                if(!settings?.trig_scheduled_daynums) {
                                                    input "trig_scheduled_weeks", "enum", title: inTS("Weeks of the Month?", getAppImg("day_calendar", true)), description: (!settings?.trig_scheduled_daynums ? "(Optional)" : ""), multiple: true, required: (!settings?.trig_scheduled_daynums), submitOnChange: true, options: weeksOfMonthMap(), image: getAppImg("day_calendar")
                                                }
                                                input "trig_scheduled_months", "enum", title: inTS("Only on these Months?", getAppImg("day_calendar", true)), description: "(Optional)", multiple: true, required: false, submitOnChange: true, options: monthMap(), image: getAppImg("day_calendar")
                                                break
                                        }
                                    }
                                }
                                break
                            case "Sunrise":
                            case "Sunset":
                                input "trig_scheduled_sunState_offset", "number", range: "*..*", title: inTS("Offset ${schedType} this number of minutes (+/-)", getAppImg(schedType?.toLowerCase(), true)), required: true, image: getAppImg(schedType?.toLowerCase() + "")
                                break
                        }
                    }
                }
            }

            if (valTrigEvt("alarm")) {
                section (sTS("${getAlarmSystemName()} (${getAlarmSystemName(true)}) Events"), hideable: true) {
                    input "trig_alarm", "enum", title: inTS("${getAlarmSystemName()} Modes", getAppImg("alarm_home", true)), options: getAlarmTrigOpts(), multiple: true, required: true, submitOnChange: true, image: getAppImg("alarm_home")
                    if(settings?.trig_alarm) {
                        // input "trig_alarm_once", "bool", title: inTS("Only alert once a day?\n(per mode)", getAppImg("question", true)), required: false, defaultValue: false, submitOnChange: true, image: getAppImg("question")
                        // input "trig_alarm_wait", "number", title: inTS("Wait between each report (in seconds)\n(Optional)", getAppImg("delay_time", true)), required: false, defaultValue: null, submitOnChange: true, image: getAppImg("delay_time")
                        triggerVariableDesc("alarm", false, trigItemCnt++)
                    }
                }
            }

            if (valTrigEvt("guard")) {
                section (sTS("Alexa Guard Events"), hideable: true) {
                    input "trig_guard", "enum", title: inTS("Alexa Guard Modes", getAppImg("alarm_home", true)), options: ["ARMED_STAY", "ARMED_AWAY", "any"], multiple: true, required: true, submitOnChange: true, image: getAppImg("alarm_home")
                    if(settings?.trig_guard) {
                        // input "trig_guard_once", "bool", title: inTS("Only alert once a day?\n(per mode)", getAppImg("question", true)), required: false, defaultValue: false, submitOnChange: true, image: getAppImg("question")
                        // input "trig_guard_wait", "number", title: inTS("Wait between each report (in seconds)\n(Optional)", getAppImg("delay_time", true)), required: false, defaultValue: null, submitOnChange: true, image: getAppImg("delay_time")
                        triggerVariableDesc("guard", false, trigItemCnt++)
                    }
                }
            }

            if (valTrigEvt("mode")) {
                section (sTS("Mode Events"), hideable: true) {
                    input "trig_mode", "mode", title: inTS("Location Modes", getAppImg("mode", true)), multiple: true, required: true, submitOnChange: true, image: getAppImg("mode")
                    if(settings?.trig_mode) {
                        input "trig_mode_once", "bool", title: inTS("Only alert once a day?\n(per type: mode)", getAppImg("question", true)), required: false, defaultValue: false, submitOnChange: true, image: getAppImg("question")
                        input "trig_mode_wait", "number", title: inTS("Wait between each report (in seconds)\n(Optional)", getAppImg("delay_time", true)), required: false, defaultValue: null, submitOnChange: true, image: getAppImg("delay_time")
                        triggerVariableDesc("mode", false, trigItemCnt++)
                    }
                }
            }

            if(valTrigEvt("routineExecuted")) {
                section(sTS("Routine Events"), hideable: true) {
                    input "trig_routineExecuted", "enum", title: inTS("Routines", getAppImg("routine", true)), options: stRoutines, multiple: true, required: true, submitOnChange: true, image: getAppImg("routine")
                    if(settings?.trig_routineExecuted) {
                        input "trig_routineExecuted_once", "bool", title: inTS("Only alert once a day?\n(per type: routine)", getAppImg("question", true)), required: false, defaultValue: false, submitOnChange: true, image: getAppImg("question")
                        input "trig_routineExecuted_wait", "number", title: inTS("Wait between each report (in seconds)\n(Optional)", getAppImg("delay_time", true)), required: false, defaultValue: null, submitOnChange: true, image: getAppImg("delay_time")
                        triggerVariableDesc("routineExecuted", false, trigItemCnt++)
                    }
                }
            }

            if(valTrigEvt("scene")) {
                section(sTS("Scene Events"), hideable: true) {
                    input "trig_scene", "device.sceneActivator", title: inTS("Scene Devices", getAppImg("routine", true)), multiple: true, required: true, submitOnChange: true, image: getAppImg("routine")
                    if(settings?.trig_scene) {
                        input "trig_scene_once", "bool", title: inTS("Only alert once a day?\n(per type: scene)", getAppImg("question", true)), required: false, defaultValue: false, submitOnChange: true, image: getAppImg("question")
                        input "trig_scene_wait", "number", title: inTS("Wait between each report (in seconds)\n(Optional)", getAppImg("delay_time", true)), required: false, defaultValue: null, submitOnChange: true, image: getAppImg("delay_time")
                        triggerVariableDesc("scene", false, trigItemCnt++)
                    }
                }
            }

            if (valTrigEvt("switch")) {
                trigNonNumSect("switch", "switch", "Switches", "Switches", ["on", "off", "any"], "are turned", ["on", "off"], "switch", trigItemCnt++)
            }

            if (valTrigEvt("level")) {
                trigNumValSect("level", "switchLevel", "Dimmers/Levels", "Dimmers/Levels", "Level is", "speed_knob", trigItemCnt++)
            }

            if (valTrigEvt("battery")) {
                trigNumValSect("battery", "battery", "Battery Level", "Batteries", "Level is", "speed_knob", trigItemCnt++)
            }

            if (valTrigEvt("motion")) {
                trigNonNumSect("motion", "motionSensor", "Motion Sensors", "Motion Sensors", ["active", "inactive", "any"], "become", ["active", "inactive"], "motion", trigItemCnt++)
            }

            if (valTrigEvt("presence")) {
                trigNonNumSect("presence", "presenceSensor", "Presence Sensors", "Presence Sensors", ["present", "not present", "any"], "changes to", ["present", "not present"], "presence", trigItemCnt++)
            }

            if (valTrigEvt("contact")) {
                trigNonNumSect("contact", "contactSensor", "Contacts, Doors, Windows", "Contacts, Doors, Windows", ["open", "closed", "any"], "changes to", ["open", "closed"], "contact", trigItemCnt++)
            }

            if (valTrigEvt("acceleration")) {
                trigNonNumSect("acceleration", "accelerationSensor", "Accelorometers", "Accelorometers", ["active", "inactive", "any"], "changes to", ["active", "inactive"], "acceleration", trigItemCnt++)
            }

            if (valTrigEvt("door")) {
                trigNonNumSect("door", "garageDoorControl", "Garage Door Openers", "Garage Doors", ["open", "closed", "opening", "closing", "any"], "changes to", ["open", "closed"], "garage_door", trigItemCnt++)
            }

            if (valTrigEvt("lock")) {
                //TODO: Add lock code triggers
                trigNonNumSect("lock", "lock", "Locks", "Smart Locks", ["locked", "unlocked", "any"], "changes to", ["locked", "unlocked"], "lock", trigItemCnt++, (settings?.trig_lockCode))
                // section (sTS("Lock Code Events"), hideable: true) {
                //     input "trig_lockCode", "capability.lock", title: inTS("Monitor Lock Codes", getAppImg("lock", true)), multiple: true, required: false, submitOnChange: true, image: getAppImg("lock")
                //     if (settings?.trig_lockCode) {
                //         def lockCodes = settings?.trig_lockCode?.currentValue("lockCodes") ?: null
                //         Map codeOpts = lockCodes ? parseJson(lockCodes[0]?.toString()) : [:]
                //         log.debug "lockCodes: ${codeOpts}"
                //         input "trig_lockCode_items", "enum", title: inTS("Lock code items...", getAppImg("command", true)), options: codeOpts?.collectEntries { [(it?.key as String): "Code ${it?.key}: (${it?.value})"] }, multiple: true, required: true, submitOnChange: true, image: getAppImg("command")
                //         if(settings?."trig_lockCode_items") {
                //             input "trig_lockCode_once", "bool", title: inTS("Only alert once a day?\n(per device)", getAppImg("question", true)), required: false, defaultValue: false, submitOnChange: true, image: getAppImg("question")
                //             input "trig_lockCode_wait", "number", title: inTS("Wait between each report (in seconds)\n(Optional)", getAppImg("delay_time", true)), required: false, defaultValue: null, submitOnChange: true, image: getAppImg("delay_time")
                //             triggerVariableDesc("Lock Code", true, trigItemCnt)
                //         }
                //     }
                // }
            }

            if (valTrigEvt("button") && isST()) {
                section (sTS("Button Events"), hideable: true) {
                    input "trig_button", "capability.button", title: inTS("Buttons", getAppImg("button", true)), required: true, multiple: true, submitOnChange: true, image: getAppImg("button")
                    if (settings?.trig_button) {
                        input "trig_button_cmd", "enum", title: inTS("changes to?", getAppImg("command", true)), options: ["pushed", "held", "any"], required: true, submitOnChange: true, image: getAppImg("command")
                        if(settings?.trig_button_cmd) {
                            triggerVariableDesc("button", false, trigItemCnt++)
                        }
                    }
                }
            }
            if (valTrigEvt("pushed")) {
                section (sTS("Button Pushed Events"), hideable: true) {
                    input "trig_pushed", "capability.pushableButton", title: inTS("Pushable Buttons", getAppImg("button", true)), required: true, multiple: true, submitOnChange: true, image: getAppImg("button")
                    if (settings?.trig_pushed) {
                        input "trig_pushed_cmd", "enum", title: inTS("changes to?", getAppImg("command", true)), options: ["pushed"], required: true, multiple: false, defaulValue: "pushed", submitOnChange: true, image: getAppImg("command")
                        input "trig_pushed_nums", "enum", title: inTS("button numbers?", getAppImg("command", true)), options: 1..8, required: true, multiple: true,  submitOnChange: true, image: getAppImg("command")
                        if(settings?.trig_pushed_nums) {
                            triggerVariableDesc("pushed", false, trigItemCnt++)
                        }
                    }
                }
            }

            if (valTrigEvt("released")) {
                section (sTS("Button Released Events"), hideable: true) {
                    input "trig_released", "capability.releasableButton", title: inTS("Releasable Buttons", getAppImg("button", true)), required: true, multiple: true, submitOnChange: true, image: getAppImg("button")
                    if (settings?.trig_released) {
                        input "trig_released_cmd", "enum", title: inTS("changes to?", getAppImg("command", true)), options: ["released"], required: true, multiple: false, defaulValue: "released", submitOnChange: true, image: getAppImg("command")
                        input "trig_released_nums", "enum", title: inTS("button numbers?", getAppImg("command", true)), options: 1..8, required: true, multiple: true, submitOnChange: true, image: getAppImg("command")
                        if(settings?.trig_released_nums) {
                            triggerVariableDesc("released", false, trigItemCnt++)
                        }
                    }
                }
            }

            if (valTrigEvt("held")) {
                section (sTS("Button Held Events"), hideable: true) {
                    input "trig_held", "capability.holdableButton", title: inTS("Holdable Buttons", getAppImg("button", true)), required: true, multiple: true, submitOnChange: true, image: getAppImg("button")
                    if (settings?.trig_held) {
                        input "trig_held_cmd", "enum", title: inTS("changes to?", getAppImg("command", true)), options: ["held"], required: true, multiple: false, defaulValue: "held", submitOnChange: true, image: getAppImg("command")
                        input "trig_held_nums", "enum", title: inTS("button numbers?", getAppImg("command", true)), options: 1..8, required: true,  multiple: true, submitOnChange: true, image: getAppImg("command")
                        if(settings?.trig_held_nums) {
                            triggerVariableDesc("held", false, trigItemCnt++)
                        }
                    }
                }
            }

            if (valTrigEvt("doubleTapped")) {
                section (sTS("Button Double Tap Events"), hideable: true) {
                    input "trig_doubleTapped", "capability.doubleTapableButton", title: inTS("Double Tap Buttons", getAppImg("button", true)), required: true, multiple: true, submitOnChange: true, image: getAppImg("button")
                    if (settings?.trig_doubleTapped) {
                        input "trig_doubleTapped_cmd", "enum", title: inTS("changes to?", getAppImg("command", true)), options: ["doubleTapped"], required: true, multiple: false, defaulValue: "doubleTapped", submitOnChange: true, image: getAppImg("command")
                        input "trig_doubleTapped_nums", "enum", title: inTS("button numbers?", getAppImg("command", true)), options: 1..8, required: true, multiple: true, submitOnChange: true, image: getAppImg("command")
                        if(settings?.trig_doubleTapped_nums) {
                            triggerVariableDesc("doubleTapped", false, trigItemCnt++)
                        }
                    }
                }
            }

            if (valTrigEvt("temperature")) {
                trigNumValSect("temperature", "temperatureMeasurement", "Temperature Sensor", "Temperature Sensors", "Temperature", "temperature", trigItemCnt++)
            }

            if (valTrigEvt("humidity")) {
                trigNumValSect("humidity", "relativeHumidityMeasurement", "Humidity Sensors", "Relative Humidity Sensors", "Relative Humidity (%)", "humidity", trigItemCnt++)
            }

            if (valTrigEvt("water")) {
                trigNonNumSect("water", "waterSensor", "Water Sensors", "Water/Moisture Sensors", ["wet", "dry", "any"], "changes to", ["wet", "dry"], "water", trigItemCnt++)
            }

            if (valTrigEvt("power")) {
                trigNumValSect("power", "powerMeter", "Power Events", "Power Meters", "Power Level (W)", "power", trigItemCnt++)
            }

            if (valTrigEvt("carbon")) {
                section (sTS("Carbon Monoxide Events"), hideable: true) {
                    input "trig_carbonMonoxide", "capability.carbonMonoxideDetector", title: inTS("Carbon Monoxide Sensors", getAppImg("co", true)), required: !(settings?.trig_smoke), multiple: true, submitOnChange: true, image: getAppImg("co")
                    if (settings?.trig_carbonMonoxide) {
                        input "trig_carbonMonoxide_cmd", "enum", title: inTS("changes to?", getAppImg("command", true)), options: ["detected", "clear", "any"], required: true, submitOnChange: true, image: getAppImg("command")
                        if(settings?.trig_carbonMonoxide_cmd) {
                            if (settings?.trig_carbonMonoxide?.size() > 1 && settings?.trig_carbonMonoxide_cmd != "any") {
                                input "trig_carbonMonoxide_all", "bool", title: inTS("Require ALL Smoke Detectors to be (${settings?.trig_carbonMonoxide_cmd})?", getAppImg("checkbox", true)), required: false, defaultValue: false, submitOnChange: true, image: getAppImg("checkbox")
                            }
                            triggerVariableDesc("carbonMonoxide", false, trigItemCnt++)
                        }
                    }
                }
            }

            if (valTrigEvt("smoke")) {
                section (sTS("Smoke Events"), hideable: true) {
                    input "trig_smoke", "capability.smokeDetector", title: inTS("Smoke Detectors", getAppImg("smoke", true)), required: !(settings?.trig_carbonMonoxide), multiple: true, submitOnChange: true, image: getAppImg("smoke")
                    if (settings?.trig_smoke) {
                        input "trig_smoke_cmd", "enum", title: inTS("changes to?", getAppImg("command", true)), options: ["detected", "clear", "any"], required: true, submitOnChange: true, image: getAppImg("command")
                        if(settings?.trig_smoke_cmd) {
                            if (settings?.trig_smoke?.size() > 1 && settings?.trig_smoke_cmd != "any") {
                                input "trig_smoke_all", "bool", title: inTS("Require ALL Smoke Detectors to be (${settings?.trig_smoke_cmd})?", getAppImg("checkbox", true)), required: false, defaultValue: false, submitOnChange: true, image: getAppImg("checkbox")
                            }
                            triggerVariableDesc("smoke", false, trigItemCnt++)
                        }
                    }
                }
            }

            if (valTrigEvt("illuminance")) {
                trigNumValSect("illuminance", "illuminanceMeasurement", "Illuminance Events", "Illuminance Sensors", "Lux Level (%)", "illuminance", trigItemCnt++)
            }

            if (valTrigEvt("shade")) {
                trigNonNumSect("shade", "windowShades", "Window Shades", "Window Shades", ["open", "closed", "opening", "closing", "any"], "changes to", ["open", "closed"], "shade", trigItemCnt++)
            }

            if (valTrigEvt("valve")) {
                trigNonNumSect("valve", "valve", "Valves", "Valves", ["open", "closed", "any"], "changes to", ["open", "closed"], "valve", trigItemCnt++)
            }

            if (valTrigEvt("thermostat")) {
                section (sTS("Thermostat Events"), hideable: true) {
                    input "trig_thermostat", "capability.thermostat", title: inTS("Thermostat", getAppImg("thermostat", true)), multiple: true, required: true, submitOnChange: true, image: getAppImg("thermostat")
                    if (settings?.trig_thermostat) {
                        input "trig_thermostat_cmd", "enum", title: inTS("Thermostat Event is...", getAppImg("command", true)), options: ["ambient":"Ambient Change", "setpoint":"Setpoint Change", "mode":"Mode Change", "operatingstate":"Operating State Change"], required: true, multiple: false, submitOnChange: true, image: getAppImg("command")
                        if (settings?.trig_thermostat_cmd) {
                            if (settings?.trig_thermostat_cmd == "setpoint") {
                                input "trig_thermostat_setpoint_type", "enum", title: inTS("SetPoint type is...", getAppImg("command", true)), options: ["cooling", "heating", "any"], required: false, submitOnChange: true, image: getAppImg("command")
                                if(settings?.trig_thermostat_setpoint_type) {
                                    input "trig_thermostat_setpoint_cmd", "enum", title: inTS("Setpoint temp is...", getAppImg("command", true)), options: ["between", "below", "above", "equals"], required: true, multiple: false, submitOnChange: true, image: getAppImg("command")
                                    if (settings?.trig_thermostat_setpoint_cmd) {
                                        if (settings?.trig_thermostat_setpoint_cmd in ["between", "below"]) {
                                            input "trig_thermostat_setpoint_low", "number", title: inTS("a ${trig_thermostat_setpoint_cmd == "between" ? "Low " : ""}Setpoint temp of..."), required: true, submitOnChange: true
                                        }
                                        if (settings?.trig_thermostat_setpoint_cmd in ["between", "above"]) {
                                            input "trig_thermostat_setpoint_high", "number", title: inTS("${trig_thermostat_setpoint_cmd == "between" ? "and a high " : "a "}Setpoint temp of..."), required: true, submitOnChange: true
                                        }
                                        if (settings?.trig_thermostat_setpoint_cmd == "equals") {
                                            input "trig_thermostat_setpoint_equal", "number", title: inTS("a Setpoint temp of..."), required: true, submitOnChange: true
                                        }
                                    }
                                }
                            }
                            if(settings?.trig_thermostat_cmd == "ambient") {
                                input "trig_thermostat_ambient_cmd", "enum", title: inTS("Ambient Temp is...", getAppImg("command", true)), options: ["between", "below", "above", "equals"], required: true, multiple: false, submitOnChange: true, image: getAppImg("command")
                                if (settings?.trig_thermostat_ambient_cmd) {
                                    if (settings?.trig_thermostat_ambient_cmd in ["between", "below"]) {
                                        input "trig_thermostat_ambient_low", "number", title: inTS("a ${trig_thermostat_ambient_cmd == "between" ? "Low " : ""}Ambient Temp of..."), required: true, submitOnChange: true
                                    }
                                    if (settings?.trig_thermostat_ambient_cmd in ["between", "above"]) {
                                        input "trig_thermostat_ambient_high", "number", title: inTS("${trig_thermostat_ambient_cmd == "between" ? "and a high " : "a "}Ambient Temp of..."), required: true, submitOnChange: true
                                    }
                                    if (settings?.trig_thermostat_ambient_cmd == "equals") {
                                        input "trig_thermostat_ambient_equal", "number", title: inTS("a Ambient Temp of..."), required: true, submitOnChange: true
                                    }
                                }
                            }
                            if (settings?.trig_thermostat_cmd == "mode") {
                                input "trig_thermostat_mode_cmd", "enum", title: inTS("Hvac Mode changes to?", getAppImg("command", true)), options: ["auto", "cool", " heat", "emergency heat", "off", "every mode"], required: true, submitOnChange: true, image: getAppImg("command")
                            }
                            if (settings?.trig_thermostat_cmd == "operatingstate") {
                                input "trig_thermostat_state_cmd", "enum", title: inTS("Operating State changes to?", getAppImg("command", true)), options: ["cooling", "heating", "idle", "every state"], required: true, submitOnChange: true, image: getAppImg("command")
                            }
                            input "trig_thermostat_once", "bool", title: inTS("Only alert once a day?\n(per type: thermostat)", getAppImg("question", true)), required: false, defaultValue: false, submitOnChange: true, image: getAppImg("question")
                            input "trig_thermostat_wait", "number", title: inTS("Wait between each report (in seconds)\n(Optional)", getAppImg("delay_time", true)), required: false, defaultValue: null, submitOnChange: true, image: getAppImg("delay_time")
                            triggerVariableDesc("thermostat", false, trigItemCnt++)
                        }
                    }
                }
            }
            if(triggersConfigured()) {
                section("") {
                    paragraph pTS("You are all done with this step.\nPress Done/Save to go back", getAppImg("done", true)), state: "complete", image: getAppImg("done")
                }
            }
        }
        state?.showSpeakEvtVars = showSpeakEvtVars
    }
}

def trigNonNumSect(String inType, String capType, String sectStr, String devTitle, cmdOpts, String cmdTitle, cmdAfterOpts, String image, Integer trigItemCnt, devReq=true) {
    section (sTS(sectStr), hideable: true) {
        input "trig_${inType}", "capability.${capType}", title: inTS(devTitle, getAppImg(image, true)), multiple: true, required: devReq, submitOnChange: true, image: getAppImg(image)
        if (settings?."trig_${inType}") {
            input "trig_${inType}_cmd", "enum", title: inTS("${cmdTitle}...", getAppImg("command", true)), options: cmdOpts, multiple: false, required: true, submitOnChange: true, image: getAppImg("command")
            if(settings?."trig_${inType}_cmd") {
                if (settings?."trig_${inType}"?.size() > 1 && settings?."trig_${inType}_cmd" != "any") {
                    input "trig_${inType}_all", "bool", title: inTS("Require ALL ${devTitle} to be (${settings?."trig_${inType}_cmd"})?", getAppImg("checkbox", true)), required: false, defaultValue: false, submitOnChange: true, image: getAppImg("checkbox")
                }
                if(!isTierAction() && settings?."trig_${inType}_cmd" in cmdAfterOpts) {
                    input "trig_${inType}_after", "number", title: inTS("Only after (${settings?."trig_${inType}_cmd"}) for (xx) seconds?", getAppImg("delay_time", true)), required: false, defaultValue: null, submitOnChange: true, image: getAppImg("delay_time")
                    if(settings?."trig_${inType}_after") {
                        input "trig_${inType}_after_repeat", "number", title: inTS("Repeat every (xx) seconds until it's not ${settings?."trig_${inType}_cmd"}?", getAppImg("delay_time", true)), required: false, defaultValue: null, submitOnChange: true, image: getAppImg("delay_time")
                        if(settings?."trig_${inType}_after_repeat") {
                            input "trig_${inType}_after_repeat_cnt", "number", title: inTS("Only repeat this many times? (Optional)", getAppImg("question", true)), required: false, defaultValue: null, submitOnChange: true, image: getAppImg("question")
                        }
                    }
                }
                if(!settings?."trig_${inType}_after") {
                    input "trig_${inType}_once", "bool", title: inTS("Only alert once a day?\n(per type: ${inType})", getAppImg("question", true)), required: false, defaultValue: false, submitOnChange: true, image: getAppImg("question")
                    input "trig_${inType}_wait", "number", title: inTS("Wait between each report (in seconds)\n(Optional)", getAppImg("delay_time", true)), required: false, defaultValue: null, submitOnChange: true, image: getAppImg("delay_time")
                }
                triggerVariableDesc(inType, true, trigItemCnt)
            }
        }
    }
}

def trigNumValSect(String inType, String capType, String sectStr, String devTitle, String cmdTitle, String image, Integer trigItemCnt, devReq=true) {
    section (sTS(sectStr), hideable: true) {
        input "trig_${inType}", "capability.${capType}", tite: inTS(devTitle, getAppImg(image, true)), multiple: true, submitOnChange: true, required: devReq, image: getAppImg(image)
        if(settings?."trig_${inType}") {
            input "trig_${inType}_cmd", "enum", title: inTS("${cmdTitle} is...", getAppImg("command", true)), options: ["between", "below", "above", "equals"], required: true, multiple: false, submitOnChange: true, image: getAppImg("command")
            if (settings?."trig_${inType}_cmd") {
                if (settings?."trig_${inType}_cmd" in ["between", "below"]) {
                    input "trig_${inType}_low", "number", title: inTS("a ${settings?."trig_${inType}_cmd" == "between" ? "Low " : ""}${cmdTitle} of...", getAppImg("low", true)), required: true, submitOnChange: true, image: getAppImg("low")
                }
                if (settings?."trig_${inType}_cmd" in ["between", "above"]) {
                    input "trig_${inType}_high", "number", title: inTS("${settings?."trig_${inType}_cmd" == "between" ? "and a high " : "a "}${cmdTitle} of...", getAppImg("high", true)), required: true, submitOnChange: true, image: getAppImg("high")
                }
                if (settings?."trig_${inType}_cmd" == "equals") {
                    input "trig_${inType}_equal", "number", title: inTS("a ${cmdTitle} of...", getAppImg("equal", true)), required: true, submitOnChange: true, image: getAppImg("equal")
                }
                if (settings?."trig_${inType}"?.size() > 1) {
                    input "trig_${inType}_all", "bool", title: inTS("Require ALL devices to be (${settings?."trig_${inType}_cmd"}) values?", getAppImg("checkbox", true)), required: false, defaultValue: false, submitOnChange: true, image: getAppImg("checkbox")
                    if(!settings?."trig_${inType}_all") {
                        input "trig_${inType}_avg", "bool", title: inTS("Use the average of all selected device values?", getAppImg("checkbox", true)), required: false, defaultValue: false, submitOnChange: true, image: getAppImg("checkbox")
                    }
                }
                input "trig_${inType}_once", "bool", title: inTS("Only alert once a day?\n(per type: ${inType})", getAppImg("question", true)), required: false, defaultValue: false, submitOnChange: true, image: getAppImg("question")
                input "trig_${inType}_wait", "number", title: inTS("Wait between each report", getAppImg("delay_time", true)), required: false, defaultValue: 120, submitOnChange: true, image: getAppImg("delay_time")
                triggerVariableDesc(inType, false, trigItemCnt)
            }
        }
    }
}

Boolean locationTriggers() {
    return (
        (valTrigEvt("mode") && settings?.trig_mode) || (valTrigEvt("alarm") && settings?.trig_alarm) ||
        (valTrigEvt("routineExecuted") && settings?.trig_routineExecuted) ||
        (valTrigEvt("scene") && settings?.trig_scene) || (valTrigEvt("guard") && settings?.trig_guard)
    )
}

Boolean deviceTriggers() {
    return (
        (settings?.trig_shade && settings?.trig_shade_cmd) || (settings?.trig_door && settings?.trig_door_cmd) || (settings?.trig_valve && settings?.trig_valve_cmd) ||
        (settings?.trig_switch && settings?.trig_switch_cmd) || (settings?.trig_level && settings?.trig_level_cmd) || (settings?.trig_lock && settings?.trig_lock_cmd) ||
        (settings?.trig_battery && settings?.trig_battery_cmd) || (thermostatTriggers()) ||
        (settings?.trig_button && settings?.trig_button_cmd) || (settings?.trig_pushed && settings?.trig_pushed_cmd && settings?.trig_pushed_nums) ||
        (settings?.trig_released && settings?.trig_released_cmd && settings?.trig_released_nums) || 
        (settings?.trig_held && settings?.trig_held_cmd && settings?.trig_held_nums) || 
        (settings?.trig_doubleTapped && settings?.trig_doubleTapped_cmd && settings?.trig_doubleTapped_nums)
    )
}

Boolean thermostatTriggers() {
    if (settings?.trig_thermostat && settings?.trig_thermostat_cmd) {
        switch(settings?.trig_thermostat_cmd) {
            case "setpoint":
                return (settings?.trig_thermostat_setpoint_cmd && (trig_thermostat_setpoint_low || trig_thermostat_setpoint_high || trig_thermostat_setpoint_equal))
            case "mode":
                return (settings?.trig_thermostat_mode_cmd)
            case "operatingstate":
                return (settings?.trig_thermostat_state_cmd)
            case "ambient":
                return (settings?.trig_thermostat_ambient_cmd && (trig_thermostat_ambient_low || trig_thermostat_ambient_high || trig_thermostat_ambient_equal))
        }
    }
    return false
}

Boolean sensorTriggers() {
    return (
        (settings?.trig_temperature && settings?.trig_temperature_cmd) || (settings?.trig_carbonMonoxide && settings?.trig_carbonMonoxide_cmd) || (settings?.trig_humidity && settings?.trig_humidity_cmd) ||
        (settings?.trig_water && settings?.trig_water_cmd) || (settings?.trig_smoke && settings?.trig_smoke_cmd) || (settings?.trig_presence && settings?.trig_presence_cmd) || (settings?.trig_motion && settings?.trig_motion_cmd) ||
        (settings?.trig_contact && settings?.trig_contact_cmd) || (settings?.trig_power && settings?.trig_power_cmd) || (settings?.trig_illuminance && settings?.trig_illuminance_low && settings?.trig_illuminance_high) ||
        (settings?.trig_acceleration && settings?.trig_acceleration_cmd)
    )
}

Boolean triggersConfigured() {
    Boolean sched = schedulesConfigured()
    Boolean loc = locationTriggers()
    Boolean dev = deviceTriggers()
    Boolean sen = sensorTriggers()
    // log.debug "sched: $sched | loc: $loc | dev: $dev | sen: $sen"
    return (sched || loc || dev || sen)
}

/******************************************************************************
    CONDITIONS SELECTION PAGE
******************************************************************************/

def conditionsPage() {
    return dynamicPage(name: "conditionsPage", title: "", nextPage: "mainPage", install: false, uninstall: false) {
        Boolean multiConds = multipleConditions()
        if(multiConds) {
            section() {
                input "cond_require_all", "bool", title: inTS("Require All Selected Conditions to Pass Before Activating Zone?", getAppImg("checkbox", true)), required: false, defaultValue: true, submitOnChange: true, image: getAppImg("checkbox")
                paragraph pTS("Notice:\n${(settings?.cond_require_all == true) ? "All selected conditions must pass before this zone will be marked active." : "Any condition will make this zone active."}", null, false, "#2784D9"), state: "complete"
            }
        }
        if(!multiConds && settings?.cond_require_all == true) { settingUpdate("cond_require_all", "false", "bool") }
        section(sTS("Time/Date")) {
            // input "test_time", "time", title: "Trigger Time?", required: false, submitOnChange: true, image: getAppImg("clock")
            href "condTimePage", title: inTS("Time Schedule", getAppImg("clock", true)), description: getTimeCondDesc(false), state: (timeCondConfigured() ? "complete" : null), image: getAppImg("clock")
            input "cond_days", "enum", title: inTS("Days of the week", getAppImg("day_calendar", true)), multiple: true, required: false, submitOnChange: true, options: weekDaysEnum(), image: getAppImg("day_calendar")
            input "cond_months", "enum", title: inTS("Months of the year", getAppImg("day_calendar", true)), multiple: true, required: false, submitOnChange: true, options: monthEnum(), image: getAppImg("day_calendar")
        }
        section (sTS("Mode Conditions")) {
            input "cond_mode", "mode", title: inTS("Location Modes...", getAppImg("mode", true)), multiple: true, required: false, submitOnChange: true, image: getAppImg("mode")
            if(settings?.cond_mode) {
                input "cond_mode_cmd", "enum", title: inTS("are...", getAppImg("command", true)), options: ["not":"Not in these modes", "are":"In these Modes"], required: true, multiple: false, submitOnChange: true, image: getAppImg("command")
            }
        }
        section (sTS("Alarm Conditions")) {
            input "cond_alarm", "enum", title: inTS("${getAlarmSystemName()} is...", getAppImg("alarm_home", true)), options: getAlarmTrigOpts(), multiple: true, required: false, submitOnChange: true, image: getAppImg("alarm_home")
        }

        condNonNumSect("switch", "switch", "Switches/Outlets Conditions", "Switches/Outlets", ["on","off"], "are", "switch")

        condNonNumSect("motion", "motionSensor", "Motion Conditions", "Motion Sensors", ["active", "inactive"], "are", "motion")

        condNonNumSect("presence", "presenceSensor", "Presence Conditions", "Presence Sensors", ["present", "not present"], "are", "presence")

        condNonNumSect("contact", "contactSensor", "Door, Window, Contact Sensors Conditions", "Contact Sensors", ["open","closed"], "are", "contact")

        condNonNumSect("acceleration", "accelerationSensor", "Accelorometer Conditions", "Accelorometer Sensors", ["active","inactive"], "are", "acceleration")

        condNonNumSect("lock", "lock", "Lock Conditions", "Smart Locks", ["locked", "unlocked"], "are", "lock")

        condNonNumSect("door", "garageDoorControl", "Garage Door Conditions", "Garage Doors", ["open", "closed"], "are", "garage_door")

        condNumValSect("temperature", "temperatureMeasurement", "Temperature Conditions", "Temperature Sensors", "Temperature", "temperature")

        condNumValSect("humidity", "relativeHumidityMeasurement", "Humidity Conditions", "Relative Humidity Sensors", "Relative Humidity (%)", "humidity")

        condNumValSect("illuminance", "illuminanceMeasurement", "Illuminance Conditions", "Illuminance Sensors", "Lux Level (%)", "illuminance")

        condNumValSect("level", "switchLevel", "Dimmers/Levels", "Dimmers/Levels", "Level (%)", "speed_knob")

        condNonNumSect("water", "waterSensor", "Water Sensors", "Water Sensors", ["wet", "dry"], "are", "water")

        condNumValSect("power", "powerMeter", "Power Events", "Power Meters", "Power Level (W)", "power")

        condNonNumSect("shade", "windowShades", "Window Shades", "Window Shades", ["open", "closed"], "are", "shade")

        condNonNumSect("valve", "valve", "Valves", "Valves", ["open", "closed"], "are", "valve")

        condNumValSect("battery", "battery", "Battery Level Conditions", "Batteries", "Level (%)", "battery")
    }
}

def condNonNumSect(String inType, String capType, String sectStr, String devTitle, cmdOpts, String cmdTitle, String image) {
    section (sTS(sectStr), hideWhenEmpty: true) {
        input "cond_${inType}", "capability.${capType}", title: inTS(devTitle, getAppImg(image, true)), multiple: true, submitOnChange: true, required:false, image: getAppImg(image), hideWhenEmpty: true
        if (settings?."cond_${inType}") {
            input "cond_${inType}_cmd", "enum", title: inTS("${cmdTitle}...", getAppImg("command", true)), options: cmdOpts, multiple: false, required: true, submitOnChange: true, image: getAppImg("command")
            if (settings?."cond_${inType}_cmd" && settings?."cond_${inType}"?.size() > 1) {
                input "cond_${inType}_all", "bool", title: inTS("ALL ${devTitle} must be (${settings?."cond_${inType}_cmd"})?", getAppImg("checkbox", true)), required: false, defaultValue: false, submitOnChange: true, image: getAppImg("checkbox")
            }
        }
    }
}

def condNumValSect(String inType, String capType, String sectStr, String devTitle, String cmdTitle, String image, hideable= false) {
    section (sTS(sectStr), hideWhenEmpty: true) {
        input "cond_${inType}", "capability.${capType}", title: inTS(devTitle, getAppImg(image, true)), multiple: true, submitOnChange: true, required: false, image: getAppImg(image), hideWhenEmpty: true
        if(settings?."cond_${inType}") {
            input "cond_${inType}_cmd", "enum", title: inTS("${cmdTitle} is...", getAppImg("command", true)), options: ["between", "below", "above", "equals"], required: true, multiple: false, submitOnChange: true, image: getAppImg("command")
            if (settings?."cond_${inType}_cmd") {
                if (settings?."cond_${inType}_cmd" in ["between", "below"]) {
                    input "cond_${inType}_low", "number", title: inTS("a ${settings?."cond_${inType}_cmd" == "between" ? "Low " : ""}${cmdTitle} of...", getAppImg("low", true)), required: true, submitOnChange: true, image: getAppImg("low")
                }
                if (settings?."cond_${inType}_cmd" in ["between", "above"]) {
                    input "cond_${inType}_high", "number", title: inTS("${settings?."cond_${inType}_cmd" == "between" ? "and a high " : "a "}${cmdTitle} of...", getAppImg("high", true)), required: true, submitOnChange: true, image: getAppImg("high")
                }
                if (settings?."cond_${inType}_cmd" == "equals") {
                    input "cond_${inType}_equal", "number", title: inTS("a ${cmdTitle} of...", getAppImg("equal", true)), required: true, submitOnChange: true, image: getAppImg("equal")
                }
                if (settings?."cond_${inType}"?.size() > 1) {
                    input "cond_${inType}_all", "bool", title: inTS("Require ALL devices to be (${settings?."cond_${inType}_cmd"}) values?", getAppImg("checkbox", true)), required: false, defaultValue: false, submitOnChange: true, image: getAppImg("checkbox")
                    if(!settings?."cond_${inType}_all") {
                        input "cond_${inType}_avg", "bool", title: inTS("Use the average of all selected device values?", getAppImg("checkbox", true)), required: false, defaultValue: false, submitOnChange: true, image: getAppImg("checkbox")
                    }
                }
            }
        }
    }
}

private Map devsSupportVolume(devs) {
    List noSupport = []
    List supported = []
    if(devs instanceof List && devs?.size()) {
        devs?.each { dev->
            if(dev?.hasAttribute("permissions") && dev?.currentPermissions?.toString()?.contains("volumeControl")) {
                supported?.push(dev?.label)
            } else { noSupport?.push(dev?.label) }
        }
    }
    return [s:supported, n:noSupport]
}

def actVariableDesc(actType, hideUserTxt=false) {
    Map txtItems = customTxtItems()
    if(!isTierAction()) {
        if(!txtItems?.size() && state?.showSpeakEvtVars && !settings?."act_${actType}_txt") {
            String str = "NOTICE:\nYou can choose to leave the response field empty and generic text will be generated for each event type, or return to Step 2 and define responses for each trigger."
            paragraph pTS(str, getAppImg("info", true), false, "#2784D9"), required: true, state: "complete", image: getAppImg("info")
        }
        if(!hideUserTxt) {
            if(txtItems?.size()) {
                String str = "NOTICE: (Custom Text Defined)"
                txtItems?.each { i->
                    i?.value?.each { i2-> str += "\n \u2022 ${i?.key?.toString()?.capitalize()} ${i2?.key?.toString()?.capitalize()}: (${i2?.value?.size()} Responses)" }
                }
                paragraph pTS(str, null, true, "#2784D9"), state: "complete"
                paragraph pTS("WARNING:\nEntering text below will override the text you defined for the trigger types under Step 2.", null, true, "red"), required: true, state: null
            }
        }
    }
}

def triggerVariableDesc(inType, showRepInputs=false, itemCnt=0) {
    if(settings?.actionType in ["speak", "announcement"]) {
        String str = "Description:\nYou have 3 response options:\n"
        str += " \u2022 1. Leave the text empty below and text will be generated for each ${inType} trigger event.\n\n"
        str += " \u2022 2. Wait till Step 4 and define a single response for all triggers selected here.\n\n"
        str += " \u2022 3. Use the response builder below and create custom responses for each individual trigger type. (Supports randomization when multiple responses are configured)"
        // str += "Custom Text is only used when Speech or Announcement action type is selected in Step 4."
        paragraph pTS(str, getAppImg("info", true), false, "#2784D9"), required: true, state: "complete", image: getAppImg("info")
        //Custom Text Options
        href url: parent?.getTextEditorPath(app?.id as String, "trig_${inType}_txt"), style: (isST() ? "external" : "external"), required: false, title: "Custom ${inType?.capitalize()} Responses\n(Optional)", state: (settings?."trig_${inType}_txt" ? "complete" : ''),
                description: settings?."trig_${inType}_txt" ?: "Open Response Designer...", image: getAppImg("text")
        if(showRepInputs) {
            if(settings?."trig_${inType}_after_repeat") {
                //Custom Repeat Text Options
                paragraph pTS("Description:\nAdd custom responses for the ${inType} events that are repeated.", getAppImg("info", true), false, "#2784D9"), state: "complete", image: getAppImg("info")
                href url: parent?.getTextEditorPath(app?.id as String, "trig_${inType}_after_repeat_txt"), style: (isST() ? "external" : "external"), title: inTS("Custom ${inType?.capitalize()} Repeat Responses\n(Optional)", getAppImg("text", true)),
                        description: settings?."trig_${inType}_after_repeat_txt" ?: "Open Response Designer...", state: (settings?."trig_${inType}_after_repeat_txt" ? "complete" : '') , submitOnChange: true, required: false, image: getAppImg("text")
            }
        }
    }
}

String actionTypeDesc() {
    Map descs = [
        speak: "Speak any message you choose on you're Echo Devices.",
        announcement: "Plays a brief tone and speaks the message you define. If you select multiple devices it will be a synchronized broadcast.",
        speak_tiered: "Allows you to create tiered responses.  Each tier can have a different delay before the next message is spoken/announced.",
        announcement_tiered: "Allows you to create tiered responses.  Each tier can have a different delay before the next message is spoken/announced. Plays a brief tone and speaks the message you define. If you select multiple devices it will be a synchronized broadcast.",
        sequence: "Sequences are a custom command where you can string different alexa actions which are sent to Amazon as a single command.  The command is then processed by amazon sequentially or in parallel.",
        weather: "Plays a very basic weather report.",
        playback: "Allows you to control the media playback state or volume level of your Echo devices.",
        builtin: "Builtin items are things like Sing a Song, Tell a Joke, Say Goodnight, etc.",
        sounds: "Plays a selected amazon sound item.",
        music: "Allows playback of various Songs/Radio using any connected music provider",
        calendar: "This will read out events in your calendar (Requires accounts to be configured in the alexa app. Must not have PIN.)",
        alarm: "This will allow you to create Alexa alarm clock notifications.",
        reminder: "This will allow you to create Alexa reminder notifications.",
        dnd: "This will allow you to enable/disable Do Not Disturb mode",
        alexaroutine: "This will allow you to run your configured Alexa Routines",
        wakeword: "This will allow you to change the Wake Word of your Echo devices.",
        bluetooth: "This will allow you to connect or disconnect bluetooth devices paired to your echo devices."
    ]
    return descs?.containsKey(settings?.actionType) ? descs[settings?.actionType] as String : "No Description Found..."
}

def actionTiersPage() {
    return dynamicPage(name: "actionTiersPage", title: "", install: false, uninstall: false) {
        section() {
            input "act_tier_cnt", "number", title: inTS("Number of Tiers", getAppImg("equal", true)), required: true, submitOnChange: true, image: getAppImg("equal")
        }
        if(settings?.act_tier_cnt) {
            Integer tierCnt = settings?.act_tier_cnt as Integer
            (1..tierCnt)?.each { ti->
                section(sTS("Tier Item (${ti}) Config:")) {
                    if(ti > 1) {
                        input "act_tier_item_${ti}_delay", "number", title: inTS("Delay after Tier ${ti-1}\n(seconds)", getAppImg("equal", true)), defaultValue: (ti == 1 ? 0 : null), required: true, submitOnChange: true, image: getAppImg("equal")
                    }
                    if(ti==1 || settings?."act_tier_item_${ti}_delay") {
                        href url: parent?.getTextEditorPath(app?.id as String, "act_tier_item_${ti}_txt"), style: (isST() ? "external" : "external"), required: true, title: inTS("Tier Item ${ti} Response", getAppImg("text", true)), state: (settings?."act_tier_item_${ti}_txt" ? "complete" : ""),
                                    description: settings?."act_tier_item_${ti}_txt" ?: "Open Response Designer...", image: getAppImg("text")
                    }
                    input "act_tier_item_${ti}_volume_change", "number", title: inTS("Tier Item Volume", getAppImg("speed_knob", true)), defaultValue: null, required: false, submitOnChange: true, image: getAppImg("speed_knob")
                    input "act_tier_item_${ti}_volume_restore", "number", title: inTS("Tier Item Volume Restore", getAppImg("speed_knob", true)), defaultValue: null, required: false, submitOnChange: true, image: getAppImg("speed_knob")
                }
            }
            if(isTierActConfigured()) {
                section("") {
                    paragraph pTS("You are all done configuring tier responses.\nPress Done/Save to go back", getAppImg("done", true)), state: "complete", image: getAppImg("done")
                }
            }
        }
    }
}

String getTierRespDesc() {
    Map tierMap = getTierMap() ?: [:]
    String str = ""
    str += tierMap?.size() ? "Tiered Responses: (${tierMap?.size()})" : ""
    tierMap?.each { k,v->
        if(settings?."act_tier_item_${k}_txt") {
            str += (settings?."act_tier_item_${k}_delay") ? "\n \u2022 Tier ${k} delay: (${settings?."act_tier_item_${k}_delay"} sec)" : ""
            str += (settings?."act_tier_item_${k}_volume_change") ? "\n \u2022 Tier ${k} volume: (${settings?."act_tier_item_${k}_volume_change"})" : ""
            str += (settings?."act_tier_item_${k}_volume_restore") ? "\n \u2022 Tier ${k} restore: (${settings?."act_tier_item_${k}_volume_restore"})" : ""
        }
    }
    return str != "" ? str : null
}

Boolean isTierActConfigured() {
    if(!isTierAction()) { return false }
    Integer cnt = settings?.act_tier_cnt as Integer
    List tierKeys = settings?.findAll { it?.key?.startsWith("act_tier_item_") && it?.key?.endsWith("_txt") }?.collect { it?.key as String }
    return (tierKeys?.size() == cnt)
}

Map getTierMap() {
    Map exec = [:]
    Integer cnt = settings?.act_tier_cnt as Integer
    if(isTierActConfigured() && cnt) {
        List tiers = (1..cnt)
        tiers?.each { t->
            exec[t as Integer] = [
                message: settings["act_tier_item_${t}_txt"],
                delay: settings["act_tier_item_${t+1}_delay"],
                volume: [
                    change: settings["act_tier_item_${t}_volume_change"],
                    restore: settings["act_tier_item_${t}_volume_restore"]
                ]
            ]
        }
    }
    return (exec?.size()) ? exec : null
}

private tierItemCleanup() {
    List rem = []
    Boolean isTierAct = isTierAction()
    List tierKeys = settings?.findAll { it?.key?.startsWith("act_tier_item_") }?.collect { it?.key as String }
    List tierIds = isTierAct && settings?.act_tier_cnt ? (1..settings?.act_tier_cnt) : []
    // if(!isTierAct() || !tierCnt) { return }
    tierKeys?.each { k->
        List id = k?.tokenize("_") ?: []
        if(!isTierAct || (id?.size() && id?.size() < 4) || !id[3]?.toString()?.isNumber() || !(id[3]?.toInteger() in tierIds)) { rem?.push(k as String) }
    }
    if(rem?.size()) { log.debug "tierItemCleanup | Removing: ${rem}"; rem?.each { settingRemove(it as String) }; }
}

def actTextOrTiersInput(type) {
    if(isTierAction()) {
        def tDesc = getTierRespDesc()
        href "actionTiersPage", title: inTS("Create Tiered Responses?", getAppImg("text", true), (tDesc ? "#2678D9" : "red")), description: (tDesc ? "${tDesc}\n\nTap to modify..." : "Tap to configure..."), required: true, state: (tDesc ? "complete" : null), image: getAppImg("text")
        input "act_tier_stop_on_clear", "bool", title: inTS("Stop responses when trigger is cleared?", getAppImg("checkbox", true)), required: false, defaultValue: false, submitOnChange: true, image: getAppImg("checkbox")
    } else {
        String textUrl = parent?.getTextEditorPath(app?.id as String, type)
        href url: textUrl, style: (isST() ? "embedded" : "external"), required: false, title: inTS("Default Action Response\n(Optional)", getAppImg("text", true)), state: (settings?."${type}" ? "complete" : ""),
                description: settings?."${type}" ?: "Open Response Designer...", image: getAppImg("text")
    }
}

// private updActExecMap() {
//     String actionType = settings?.actionType
//     Map actionExecMap = [configured: false]

//     if(settings?.act_EchoDevices || settings?.act_EchoZones) {
//         actionExecMap?.actionType = settings?.actionType
//         actionExecMap?.config = [:]
//         switch(actionType) {
//             case "speak":
//             case "speak_tiered":
//                 actionExecMap?.config[actionType] = [text: settings?.act_speak_txt, evtText: ((state?.showSpeakEvtVars && !settings?.act_speak_txt) || hasUserDefinedTxt()), tiers: getTierMap()]
//                 break
//             case "announcement":
//             case "announcement_tiered":
//                 actionExecMap?.config[actionType] = [text: settings?.act_speak_txt, evtText: ((state?.showSpeakEvtVars && !settings?.act_speak_txt) || hasUserDefinedTxt()), tiers: getTierMap()]
//                 if(settings?.act_EchoDevices?.size() > 1) {
//                     List devObj = []
//                     devices?.each { devObj?.push([deviceTypeId: it?.getEchoDeviceType() as String, deviceSerialNumber: it?.getEchoSerial() as String]) }
//                     // log.debug "devObj: $devObj"
//                     actionExecMap?.config[actionType]?.deviceObjs = devObj
//                 }
//                 break
//             case "sequence":
//                 actionExecMap?.config[actionType] = [text: settings?."act_${actionType}_txt"]
//                 break
//             case "weather":
//                 actionExecMap?.config?.weather = [cmd: "playWeather"]
//                 break
//             case "playback":
//             case "builtin":
//             case "calendar":
//                 actionExecMap?.config[actionType] = [cmd: settings?."act_${actionType}_cmd"]
//                 break
//             case "music":

//                 break
//         }
//     }
// }


def actionsPage() {
    return dynamicPage(name: "actionsPage", title: "", nextPage: "mainPage", install: false, uninstall: false) {
        Boolean done = false
        Map actionExecMap = [configured: false]
        if(settings?.actionType) {
            actionExecMap?.actionType = settings?.actionType
            actionExecMap?.config = [:]
            List devices = parent?.getDevicesFromList(settings?.act_EchoDevices)
            String actTypeDesc = "[${settings?.actionType?.toString()?.tokenize("_")?.collect { it?.capitalize() }?.join(" ")}]\n\n${actionTypeDesc()}"
            Boolean isTierAct = isTierAction()
            switch(actionType) {
                case "speak":
                case "speak_tiered":
                    section(sTS("Action Description:")) { paragraph pTS(actTypeDesc, getAppImg("info", true), false, "#2784D9"), state: "complete", image: getAppImg("info"); }
                    echoDevicesInputByPerm("TTS")
                    if(settings?.act_EchoDevices || settings?.act_EchoZones) {
                        section(sTS("Action Type Config:"), hideable: true) {
                            actVariableDesc(actionType)
                            actTextOrTiersInput("act_speak_txt")
                        }
                        actionVolumeInputs(devices)
                        actionExecMap?.config[actionType as String] = [text: settings?.act_speak_txt, evtText: ((state?.showSpeakEvtVars && !settings?.act_speak_txt) || hasUserDefinedTxt()), tiers: getTierMap()]
                        if(state?.showSpeakEvtVars || settings?.act_speak_txt || (isTierAct && isTierActConfigured())) { done = true } else { done = false }
                    } else { done = false }
                    break

                case "announcement":
                case "announcement_tiered":
                    section(sTS("Action Description:")) { paragraph pTS(actTypeDesc, getAppImg("info", true), false, "#2784D9"), state: "complete", image: getAppImg("info"); }
                    echoDevicesInputByPerm("announce")
                    if(settings?.act_EchoDevices || settings?.act_EchoZones) {
                        section(sTS("Action Type Config:")) {
                            actVariableDesc(actionType)
                            actTextOrTiersInput("act_announcement_txt")
                        }
                        actionVolumeInputs(devices)
                        actionExecMap?.config[actionType as String] = [text: settings?.act_announcement_txt, evtText: ((state?.showSpeakEvtVars && !settings?.act_speak_txt) || hasUserDefinedTxt()), tiers: getTierMap()]
                        if(settings?.act_EchoDevices?.size() > 1) {
                            List devObj = []
                            devices?.each { devObj?.push([deviceTypeId: it?.getEchoDeviceType() as String, deviceSerialNumber: it?.getEchoSerial() as String]) }
                            // log.debug "devObj: $devObj"
                            actionExecMap?.config[actionType as String]?.deviceObjs = devObj
                        }
                        if(state?.showSpeakEvtVars || settings?.act_announcement_txt || (isTierAct && isTierActConfigured())) { done = true } else { done = false }
                    } else { done = false }
                    break

                case "sequence":
                    section(sTS("Action Description:")) { paragraph pTS(actTypeDesc, getAppImg("info", true), false, "#2784D9"), state: "complete", image: getAppImg("info"); }
                    echoDevicesInputByPerm("TTS")
                    if(settings?.act_EchoDevices || settings?.act_EchoZones) {
                        section(sTS("Sequence Options Legend:"), hideable: true, hidden: false) {
                            String str1 = "Sequence Options:"
                            seqItemsAvail()?.other?.sort()?.each { k, v->
                                str1 += "${bulletItem(str1, "${k}${v != null ? "::${v}" : ""}")}"
                            }
                            String str4 = "DoNotDisturb Options:"
                            seqItemsAvail()?.dnd?.sort()?.each { k, v->
                                str4 += "${bulletItem(str4, "${k}${v != null ? "::${v}" : ""}")}"
                            }
                            String str2 = "Music Options:"
                            seqItemsAvail()?.music?.sort()?.each { k, v->
                                str2 += "${bulletItem(str2, "${k}${v != null ? "::${v}" : ""}")}"
                            }
                            String str3 = "Canned TTS Options:"
                            seqItemsAvail()?.speech?.sort()?.each { k, v->
                                def newV = v
                                if(v instanceof List) { newV = ""; v?.sort()?.each { newV += "     ${dashItem(newV, "${it}", true)}"; } }
                                str3 += "${bulletItem(str3, "${k}${newV != null ? "::${newV}" : ""}")}"
                            }
                            paragraph str1, state: "complete"
                            // paragraph str4, state: "complete"
                            paragraph str2, state: "complete"
                            paragraph str3, state: "complete"
                            paragraph pTS("Enter the command in a format exactly like this:\nvolume::40,, speak::this is so silly,, wait::60,, weather,, cannedtts_random::goodbye,, traffic,, amazonmusic::green day,, volume::30\n\nEach command needs to be separated by a double comma `,,` and the separator between the command and value must be command::value.", null, false, "violet"), state: "complete"
                        }
                        section(sTS("Action Type Config:")) {
                            input "act_sequence_txt", "text", title: inTS("Enter sequence text", getAppImg("text", true)), submitOnChange: true, required: false, image: getAppImg("text")
                        }
                        actionExecMap?.config?.sequence = [text: settings?.act_sequence_txt]
                        if(settings?.act_sequence_txt) { done = true } else { done = false }
                    } else { done = false }
                    break

                case "weather":
                    section(sTS("Action Description:")) { paragraph pTS(actTypeDesc, getAppImg("info", true), false, "#2784D9"), state: "complete", image: getAppImg("info"); }
                    echoDevicesInputByPerm("TTS")
                    if(settings?.act_EchoDevices || settings?.act_EchoZones) {
                        actionVolumeInputs(devices)
                        done = true
                        actionExecMap?.config?.weather = [cmd: "playWeather"]
                    } else { done = false }
                    break

                case "playback":
                    section(sTS("Action Description:")) { paragraph pTS(actTypeDesc, getAppImg("info", true), false, "#2784D9"), state: "complete", image: getAppImg("info"); }
                    echoDevicesInputByPerm("mediaPlayer")
                    if(settings?.act_EchoDevices || settings?.act_EchoZones) {
                        Map playbackOpts = [
                            "pause":"Pause", "stop":"Stop", "play":"Play", "nextTrack":"Next Track", "previousTrack":"Previous Track",
                            "mute":"Mute", "volume":"Volume"
                        ]
                        section(sTS("Playback Config:")) {
                            input "act_playback_cmd", "enum", title: inTS("Select Playback Action", getAppImg("command", true)), description: "", options: playbackOpts, required: true, submitOnChange: true, image: getAppImg("command")
                        }
                        if(settings?.act_playback_cmd == "volume") { actionVolumeInputs(devices, true) }
                        actionExecMap?.config?.playback = [cmd: settings?.act_playback_cmd]
                        if(settings?.act_playback_cmd) { done = true } else { done = false }
                    } else { done = false }
                    break

                case "sounds":
                    section(sTS("Action Description:")) { paragraph pTS(actTypeDesc, getAppImg("info", true), false, "#2784D9"), state: "complete", image: getAppImg("info"); }
                    echoDevicesInputByPerm("TTS")
                    if(settings?.act_EchoDevices || settings?.act_EchoZones) {
                        section(sTS("BuiltIn Sounds Config:")) {
                            input "act_sounds_cmd", "enum", title: inTS("Select Sound Type", getAppImg("command", true)), description: "", options: parent?.getAvailableSounds()?.collect { it?.key as String }, required: true, submitOnChange: true, image: getAppImg("command")
                        }
                        actionVolumeInputs(devices)
                        actionExecMap?.config?.sounds = [cmd: "playSoundByName", name: settings?.act_sounds_cmd]
                        done = (settings?.act_sounds_cmd != null)
                    } else { done = false }
                    break

                case "builtin":
                    section(sTS("Action Description:")) { paragraph pTS(actTypeDesc, getAppImg("info", true), false, "#2784D9"), state: "complete", image: getAppImg("info"); }
                    echoDevicesInputByPerm("TTS")
                    if(settings?.act_EchoDevices || settings?.act_EchoZones) {
                        Map builtinOpts = [
                            "playSingASong": "Sing a Song", "playFlashBrief": "Flash Briefing (News)", "playGoodNews": "Good News Only", "playFunFact": "Fun Fact", "playTraffic": "Traffic", "playJoke": "Joke",
                            "playTellStory": "Tell Story", "sayGoodbye": "Say Goodbye", "sayGoodNight": "Say Goodnight", "sayBirthday": "Happy Birthday",
                            "sayCompliment": "Give Compliment", "sayGoodMorning": "Good Morning", "sayWelcomeHome": "Welcome Home"
                        ]
                        section(sTS("BuiltIn Speech Config:")) {
                            input "act_builtin_cmd", "enum", title: inTS("Select Builtin Speech Type", getAppImg("command", true)), description: "", options: builtinOpts, required: true, submitOnChange: true, image: getAppImg("command")
                        }
                        actionVolumeInputs(devices)
                        actionExecMap?.config?.builtin = [cmd: settings?.act_builtin_cmd]
                        if(settings?.act_builtin_cmd) { done = true } else { done = false }
                    } else { done = false }
                    break

                case "music":
                    section(sTS("Action Description:")) { paragraph pTS(actTypeDesc, getAppImg("info", true), false, "#2784D9"), state: "complete", image: getAppImg("info"); }
                    echoDevicesInputByPerm("mediaPlayer")
                    if(settings?.act_EchoDevices || settings?.act_EchoZones) {
                        List musicProvs = devices[0]?.hasAttribute("supportedMusic") ? devices[0]?.currentValue("supportedMusic")?.split(",")?.collect { "${it?.toString()?.trim()}"} : []
                        logDebug("Music Providers: ${musicProvs}")
                        if(musicProvs) {
                            section(sTS("Music Providers:")) {
                                input "act_music_provider", "enum", title: inTS("Select Music Provider", getAppImg("music", true)), description: "", options: musicProvs, multiple: false, required: true, submitOnChange: true, image: getAppImg("music")
                            }
                            if(settings?.act_music_provider) {
                                if(settings?.act_music_provider == "TuneIn") {
                                    section(sTS("TuneIn Search Results:")) {
                                        paragraph "Enter a search phrase to query TuneIn to help you find the right search term to use in searchTuneIn() command.", state: "complete"
                                        input "tuneinSearchQuery", "text", title: inTS("Enter search phrase for TuneIn", getAppImg("tunein", true)), defaultValue: null, required: false, submitOnChange: true, image: getAppImg("tunein")
                                        if(settings?.tuneinSearchQuery) {
                                            href "searchTuneInResultsPage", title: inTS("View search results!", getAppImg("search", true)), description: "Tap to proceed...", image: getAppImg("search")
                                        }
                                    }
                                }
                                section(sTS("Action Type Config:")) {
                                    input "act_music_txt", "text", title: inTS("Enter Music Search text", getAppImg("text", true)), submitOnChange: true, required: false, image: getAppImg("text")
                                }
                                actionVolumeInputs(devices)
                            }
                        }
                        actionExecMap?.config?.music = [cmd: "searchMusic", provider: settings?.act_music_provider, search: settings?.act_music_txt]
                        if(settings?.act_music_provider && settings?.act_music_txt) { done = true } else { done = false }
                    } else { done = false }
                    break

                case "calendar":
                    section(sTS("Action Description:")) { paragraph pTS(actTypeDesc, getAppImg("info", true), false, "#2784D9"), state: "complete", image: getAppImg("info"); }
                    echoDevicesInputByPerm("TTS")
                    if(settings?.act_EchoDevices || settings?.act_EchoZones) {
                        section(sTS("Action Type Config:")) {
                            input "act_calendar_cmd", "enum", title: inTS("Select Calendar Action", getAppImg("command", true)), description: "", options: ["playCalendarToday":"Today", "playCalendarTomorrow":"Tomorrow", "playCalendarNext":"Next Events"],
                                    required: true, submitOnChange: true, image: getAppImg("command")
                        }
                        actionVolumeInputs(devices)
                        actionExecMap?.config?.calendar = [cmd: settings?.act_calendar_cmd]
                        if(act_calendar_cmd) { done = true } else { done = false }
                    } else { done = false }
                    break

                case "alarm":
                    //TODO: Offer to remove alarm after event.
                    section(sTS("Action Description:")) { paragraph pTS(actTypeDesc, getAppImg("info", true), false, "#2784D9"), state: "complete", image: getAppImg("info"); }
                    echoDevicesInputByPerm("alarms")
                    if(settings?.act_EchoDevices) {
                        Map repeatOpts = ["everyday":"Everyday", "weekends":"Weekends", "weekdays":"Weekdays", "daysofweek":"Days of the Week", "everyxdays":"Every Nth Day"]
                        String rptType = null
                        def rptTypeOpts = null
                        section(sTS("Action Type Config:")) {
                            input "act_alarm_label", "text", title: inTS("Alarm Label", getAppImg("name_tag", true)), submitOnChange: true, required: true, image: getAppImg("name_tag")
                            input "act_alarm_date", "text", title: inTS("Alarm Date\n(yyyy-mm-dd)", getAppImg("day_calendar", true)), submitOnChange: true, required: true, image: getAppImg("day_calendar")
                            input "act_alarm_time", "time", title: inTS("Alarm Time", getAppImg("clock", true)), submitOnChange: true, required: true, image: getAppImg("clock")
                            // if(act_alarm_label && act_alarm_date && act_alarm_time) {
                            //     input "act_alarm_rt", "enum", title: inTS("Repeat (Optional)", getAppImg("command", true)), description: "", options: repeatOpts, required: true, submitOnChange: true, image: getAppImg("command")
                            //     if(settings?."act_alarm_rt") {
                            //         rptType = settings?.act_alarm_rt
                            //         if(settings?."act_alarm_rt" == "daysofweek") {
                            //             input "act_alarm_rt_wd", "enum", title: inTS("Weekday", getAppImg("checkbox", true)), description: "", options: weekDaysAbrvEnum(), multiple: true, required: true, submitOnChange: true, image: getAppImg("checkbox")
                            //             if(settings?.act_alarm_rt_wd) rptTypeOpts = settings?.act_alarm_rt_wd
                            //         }
                            //         if(settings?."act_alarm_rt" == "everyxdays") {
                            //             input "act_alarm_rt_ed", "number", title: inTS("Every X Days", getAppImg("checkbox", true)), description: "", range: "1..31", required: true, submitOnChange: true, image: getAppImg("checkbox")
                            //             if(settings?.act_alarm_rt_ed) rptTypeOpts = settings?.act_alarm_rt_ed
                            //         }
                            //     }
                            // }
                            // input "act_alarm_remove", "bool", title: "Remove Alarm when done", defaultValue: true, submitOnChange: true, required: false, image: getAppImg("question")
                        }
                        actionVolumeInputs(devices, false, true)
                        def newTime = settings?.act_alarm_time ? parseFmtDt("yyyy-MM-dd'T'HH:mm:ss.SSSZ", 'HH:mm', settings?.act_alarm_time) : null
                        actionExecMap?.config?.alarm = [cmd: "createAlarm", label: settings?.act_alarm_label, date: settings?.act_alarm_date, time: newTime, recur: [type: rptType, opts: rptTypeOpts], remove: settings?.act_alarm_remove]
                        if(act_alarm_label && act_alarm_date && act_alarm_time) { done = true } else { done = false }
                    } else { done = false }
                    break

                case "reminder":
                    //TODO: Offer to remove reminder after event.
                    section(sTS("Action Description:")) { paragraph pTS(actTypeDesc, getAppImg("info", true), false, "#2784D9"), state: "complete", image: getAppImg("info"); }
                    echoDevicesInputByPerm("reminders")
                    if(settings?.act_EchoDevices) {
                        Map repeatOpts = ["everyday":"Everyday", "weekends":"Weekends", "weekdays":"Weekdays", "daysofweek":"Days of the Week", "everyxdays":"Every Nth Day"]
                        String rptType = null
                        def rptTypeOpts = null
                        section(sTS("Action Type Config:")) {
                            input "act_reminder_label", "text", title: inTS("Reminder Label", getAppImg("name_tag", true)), submitOnChange: true, required: true, image: getAppImg("name_tag")
                            input "act_reminder_date", "text", title: inTS("Reminder Date\n(yyyy-mm-dd)", getAppImg("day_calendar", true)), submitOnChange: true, required: true, image: getAppImg("day_calendar")
                            input "act_reminder_time", "time", title: inTS("Reminder Time", getAppImg("clock", true)), submitOnChange: true, required: true, image: getAppImg("clock")
                            // if(act_reminder_label && act_reminder_date && act_reminder_time) {
                            //     input "act_reminder_rt", "enum", title: inTS("Repeat (Optional)", getAppImg("command", true)), description: "", options: repeatOpts, required: true, submitOnChange: true, image: getAppImg("command")
                            //     if(settings?."act_reminder_rt") {
                            //         rptType = settings?.act_reminder_rt
                            //         if(settings?."act_reminder_rt" == "daysofweek") {
                            //             input "act_reminder_rt_wd", "enum", title: inTS("Weekday", getAppImg("checkbox", true)), description: "", options: weekDaysAbrvEnum(), multiple: true, required: true, submitOnChange: true, image: getAppImg("checkbox")
                            //             if(settings?.act_reminder_rt_wd) rptTypeOpts = settings?.act_reminder_rt_wd
                            //         }
                            //         if(settings?."act_reminder_rt" && settings?."act_reminder_rt" == "everyxdays") {
                            //             input "act_reminder_rt_ed", "number", title: inTS("Every X Days (1-31)", getAppImg("checkbox", true)), description: "", range: "1..31", required: true, submitOnChange: true, image: getAppImg("checkbox")
                            //             if(settings?.act_reminder_rt_ed) rptTypeOpts = settings?.act_reminder_rt_ed
                            //         }
                            //     }
                            // }
                            // input "act_reminder_remove", "bool", title: "Remove Reminder when done", defaultValue: true, submitOnChange: true, required: false, image: getAppImg("question")
                        }
                        actionVolumeInputs(devices, false, true)
                        def newTime = settings?.act_reminder_time ? parseFmtDt("yyyy-MM-dd'T'HH:mm:ss.SSSZ", 'HH:mm', settings?.act_reminder_time) : null
                        actionExecMap?.config?.reminder = [cmd: "createReminderNew", label: settings?.act_reminder_label, date: settings?.act_reminder_date, time: newTime, recur: [type: rptType, opts: rptTypeOpts], remove: settings?.act_reminder_remove]
                        if(act_reminder_label && act_reminder_date && act_reminder_time) { done = true } else { done = false }
                    } else { done = false }
                    break

                case "dnd":
                    echoDevicesInputByPerm("doNotDisturb")
                    if(settings?.act_EchoDevices) {
                        Map dndOpts = ["doNotDisturbOn":"Enable", "doNotDisturbOff":"Disable"]
                        section(sTS("Action Description:")) { paragraph pTS(actTypeDesc, getAppImg("info", true), false, "#2784D9"), state: "complete", image: getAppImg("info"); }
                        section(sTS("Action Type Config:")) {
                            input "act_dnd_cmd", "enum", title: inTS("Select Do Not Disturb Action", getAppImg("command", true)), description: "", options: dndOpts, required: true, submitOnChange: true, image: getAppImg("command")
                        }
                        actionExecMap?.config?.dnd = [cmd: settings?.act_dnd_cmd]
                        if(settings?.act_dnd_cmd) { done = true } else { done = false }
                    } else { done = false }
                    break

                case "alexaroutine":
                    echoDevicesInputByPerm("wakeWord")
                    if(settings?.act_EchoDevices) {
                        section(sTS("Action Description:")) { paragraph pTS(actTypeDesc, getAppImg("info", true), false, "#2784D9"), state: "complete", image: getAppImg("info"); }
                        def routinesAvail = parent?.getAlexaRoutines(null, true) ?: [:]
                        logDebug("routinesAvail: $routinesAvail")
                        section(sTS("Action Type Config:")) {
                            input "act_alexaroutine_cmd", "enum", title: inTS("Select Alexa Routine", getAppImg("command", true)), description: "", options: routinesAvail, multiple: false, required: true, submitOnChange: true, image: getAppImg("command")
                        }
                        actionExecMap?.config?.alexaroutine = [cmd: "executeRoutineId", routineId: settings?.act_alexaroutine_cmd]
                        if(settings?.act_alexaroutine_cmd) { done = true } else { done = false }
                    } else { done = false }
                    break

                case "wakeword":
                    echoDevicesInputByPerm("wakeWord")
                    if(settings?.act_EchoDevices) {
                        Integer devsCnt = settings?.act_EchoDevices?.size() ?: 0
                        List devsObj = []
                        section(sTS("Action Description:")) { paragraph pTS(actTypeDesc, getAppImg("info", true), false, "#2784D9"), state: "complete", image: getAppImg("info"); }
                        if(devsCnt >= 1) {
                            List wakeWords = devices[0]?.hasAttribute("wakeWords") ? devices[0]?.currentValue("wakeWords")?.replaceAll('"', "")?.split(",") : []
                            // logDebug("WakeWords: ${wakeWords}")
                            devices?.each { cDev->
                                section(sTS("${cDev?.getLabel()}:")) {
                                    if(wakeWords?.size()) {
                                        paragraph "Current Wake Word: ${cDev?.hasAttribute("alexaWakeWord") ? cDev?.currentValue("alexaWakeWord") : "Unknown"}"
                                        input "act_wakeword_device_${cDev?.id}", "enum", title: inTS("New Wake Word", getAppImg("list", true)), description: "", options: wakeWords, required: true, submitOnChange: true, image: getAppImg("list")
                                        devsObj?.push([device: cDev?.id as String, wakeword: settings?."act_wakeword_device_${cDev?.id}", cmd: "setWakeWord"])
                                    } else { paragraph "Oops...\nNo Wake Words have been found!  Please Remove the device from selection.", state: null, required: true }
                                }
                            }
                        }
                        actionExecMap?.config?.wakeword = [ devices: devsObj]
                        def aCnt = settings?.findAll { it?.key?.startsWith("act_wakeword_device_") && it?.value }
                        // log.debug "aCnt: ${aCnt} | devsCnt: ${devsCnt}"
                        if(settings?.findAll { it?.key?.startsWith("act_wakeword_device_") && it?.value }?.size() == devsCnt) { done = true } else { done = false }
                    } else { done = false }
                    break

                case "bluetooth":
                    echoDevicesInputByPerm("bluetoothControl")
                    if(settings?.act_EchoDevices) {
                        Integer devsCnt = settings?.act_EchoDevices?.size() ?: 0
                        List devsObj = []
                        section(sTS("Action Description:")) { paragraph pTS(actTypeDesc, getAppImg("info", true), false, "#2784D9"), state: "complete", image: getAppImg("info"); }
                        if(devsCnt >= 1) {
                            devices?.each { cDev->
                                def btData = cDev?.hasAttribute("btDevicesPaired") ? cDev?.currentValue("btDevicesPaired") : null
                                List btDevs = (btData) ? parseJson(btData)?.names : []
                                // log.debug "btDevs: $btDevs"
                                section(sTS("${cDev?.getLabel()}:")) {
                                    if(btDevs?.size()) {
                                        input "act_bluetooth_device_${cDev?.id}", "enum", title: inTS("BT device to use", getAppImg("bluetooth", true)), description: "", options: btDevs, required: true, submitOnChange: true, image: getAppImg("bluetooth")
                                        input "act_bluetooth_action_${cDev?.id}", "enum", title: inTS("BT action to take", getAppImg("command", true)), description: "", options: ["connectBluetooth":"connect", "disconnectBluetooth":"disconnect"], required: true, submitOnChange: true, image: getAppImg("command")
                                        devsObj?.push([device: cDev?.id as String, btDevice: settings?."act_bluetooth_device_${cDev?.id}", cmd: settings?."act_bluetooth_action_${cDev?.id}"])
                                    } else { paragraph "Oops...\nNo Bluetooth devices are paired to this Echo Device!  Please Remove the device from selection.", state: null, required: true }
                                }
                            }
                        }
                        actionExecMap?.config?.bluetooth = [devices: devsObj]
                        if(settings?.findAll { it?.key?.startsWith("act_bluetooth_device_") && it?.value }?.size() == devsCnt &&
                            settings?.findAll { it?.key?.startsWith("act_bluetooth_action_") && it?.value }?.size() == devsCnt) { done = true } else { done = false }
                    } else { done = false }
                    break
                default:
                    paragraph pTS("Unknown Action Type Defined...", getAppImg("error", true), true, "red"), required: true, state: null, image: getAppImg("error")
                    break
            }
            if(done) {
                if(isST()) {
                    section(sTS("Delay Config:")) {
                        input "act_delay", "number", title: inTS("Delay Action in Seconds\n(Optional)", getAppImg("delay_time", true)), required: false, submitOnChange: true, image: getAppImg("delay_time")
                    }
                }
                if(isTierAct && settings?.act_tier_cnt > 1) {
                    section(sTS("Tier Action Start Tasks:")) {
                        href "actTrigTasksPage", title: inTS("Perform Tasks on Tier Start?", getAppImg("tasks", true)), description: actTaskDesc("act_tier_start_", true), params:[type: "act_tier_start_"], state: (actTaskDesc("act_tier_start_") ? "complete" : null), image: getAppImg("tasks")
                    }
                    section(sTS("Tier Action Stop Tasks:")) {
                        href "actTrigTasksPage", title: inTS("Perform Tasks on Tier Stop?", getAppImg("tasks", true)), description: actTaskDesc("act_tier_stop_", true), params:[type: "act_tier_stop_"], state: (actTaskDesc("act_tier_stop_") ? "complete" : null), image: getAppImg("tasks")
                    }
                } else {
                    section(sTS("Action Triggered Tasks:")) {
                        href "actTrigTasksPage", title: inTS("Perform tasks on Trigger?", getAppImg("tasks", true)), description: actTaskDesc("act_", true), params:[type: "act_"], state: (actTaskDesc("act_") ? "complete" : null), image: getAppImg("tasks")
                    }
                }
                actionSimulationSect()
                section("") {
                    paragraph pTS("You are all done with this step.\nPress Done/Save to go back", getAppImg("done", true)), state: "complete", image: getAppImg("done")
                }
                actionExecMap?.config?.volume = [change: settings?.act_volume_change, restore: settings?.act_volume_restore, alarm: settings?.act_alarm_volume]

                actionExecMap?.delay = settings?.act_delay
                actionExecMap?.configured = true
                updConfigStatusMap()
                tierItemCleanup()
                //TODO: Add Cleanup of non selected inputs
            } else { actionExecMap = [configured: false] }
        }
        atomicState?.actionExecMap = (done && actionExecMap?.configured == true) ? actionExecMap : [configured: false]
        logDebug("actionExecMap: ${atomicState?.actionExecMap}")
    }
}

def actTrigTasksPage(params) {
    def t = params?.type
    if(params?.type) {
        atomicState?.curPageParams = params
    } else { t = atomicState?.curPageParams?.type }
    return dynamicPage(name: "actTrigTasksPage", title: "", install: false, uninstall: false) {
        Map dMap = [:]
        section() {
            switch(t) {
                case "act_":
                    dMap = [def: "", delay: "tasks"]
                    paragraph pTS("These tasks will be performed when the action is triggered.\n(Delay is optional)", null, false, "#2678D9"), state: "complete"
                    break
                case "act_tier_start_":
                    dMap = [def: " with Tier start", delay: "Tier Start tasks"]
                    paragraph pTS("These tasks will be performed with when the first tier of action is triggered.\n(Delay is optional)", null, false, "#2678D9"), state: "complete"
                    break
                case "act_tier_stop_":
                    dMap = [def: " with Tier stop", delay: "Tier Stop tasks"]
                    paragraph pTS("These tasks will be performed with when the last tier of action is triggered.\n(Delay is optional)", null, false, "#2678D9"), state: "complete"
                    break
            }
        }
        String pt = t == "act_" ? "" : t
        section(sTS("Control Devices:")) {
            input "${t}switches_on", "capability.switch", title: inTS("Turn ON these Switches${dMap?.def}\n(Optional)", getAppImg("switch", true)), multiple: true, required: false, submitOnChange: true, image: getAppImg("switch")
            input "${t}switches_off", "capability.switch", title: inTS("Turn OFF these Switches${dMap?.def}\n(Optional)", getAppImg("switch", true)), multiple: true, required: false, submitOnChange: true, image: getAppImg("switch")
        }

        section(sTS("Control Lights:")) {
            input "${t}lights", "capability.switch", title: inTS("Turn ON these Lights${dMap?.def}\n(Optional)", getAppImg("light", true)), multiple: true, required: false, submitOnChange: true, image: getAppImg("light")
            if(settings?."${t}lights") {
                List lights = settings?."${t}lights"
                if(lights?.any { i-> (i?.hasCommand("setColor")) } && !lights?.every { i-> (i?.hasCommand("setColor")) }) {
                    paragraph pTS("Not all selected devices support color. So color options are hidden.", null, true, "red"), state: null, required: true
                } else {
                    input "${t}lights_color", "enum", title: inTS("To this color?\n(Optional)", getAppImg("command", true)), multiple: false, options: fillColorSettings()?.name, required: false, submitOnChange: true, image: getAppImg("color")
                    if(settings?."${t}lights_color") {
                        input "${t}lights_color_delay", "number", title: inTS("Restore original light state after (x) seconds?\n(Optional)", getAppImg("delay", true)), required: true, submitOnChange: true, image: getAppImg("delay")
                    }
                }
                if(lights?.any { i-> (i?.hasCommand("setLevel")) } && !lights?.every { i-> (i?.hasCommand("setLevel")) }) {
                    paragraph pTS("Not all selected devices support level. So level option is hidden.", null, true, "red"), state: null, required: true
                } else { input "${t}lights_level", "enum", title: inTS("At this level?\n(Optional)", getAppImg("speed_knob", true)), options: dimmerLevelEnum(), required: false, submitOnChange: true, image: getAppImg("speed_knob")}
            }
        }

        section(sTS("Control Locks:")) {
            input "${t}locks_lock", "capability.lock", title: inTS("Lock these Locks${dMap?.def}\n(Optional)", getAppImg("lock", true)), multiple: true, required: false, submitOnChange: true, image: getAppImg("lock")
            input "${t}locks_unlock", "capability.lock", title: inTS("Unlock these Locks${dMap?.def}\n(Optional)", getAppImg("lock", true)), multiple: true, required: false, submitOnChange: true, image: getAppImg("lock")
        }

        section(sTS("Control Doors:")) {
            input "${t}doors_close", "capability.garageDoorControl", title: inTS("Close these Garage Doors${dMap?.def}\n(Optional)", getAppImg("garage_door", true)), multiple: true, required: false, submitOnChange: true, image: getAppImg("garage_door")
            input "${t}doors_open", "capability.garageDoorControl", title: inTS("Open these Garage Doors${dMap?.def}\n(Optional)", getAppImg("garage_door", true)), multiple: true, required: false, submitOnChange: true, image: getAppImg("garage_door")
        }

        section(sTS("Control Siren:")) {
            input "${t}sirens", "capability.alarm", title: inTS("Activate these Sirens${dMap?.def}\n(Optional)", getAppImg("siren", true)), multiple: true, required: false, submitOnChange: true, image: getAppImg("siren")
            if(settings?."${t}sirens") {
                input "${t}siren_cmd", "enum", title: inTS("Alarm action to take${dMap?.def}\n(Optional)", getAppImg("command", true)), options: ["both": "Siren & Stobe", "strobe":"Strobe Only", "siren":"Siren Only"], multiple: false, required: true, submitOnChange: true, image: getAppImg("command")
                input "${t}siren_time", "number", title: inTS("Stop after (x) seconds...", getAppImg("delay", true)), required: true, submitOnChange: true, image: getAppImg("delay")
            }
        }
        section(sTS("Location Actions:")) {
            input "${t}mode_run", "enum", title: inTS("Set Location Mode${dMap?.def}\n(Optional)", getAppImg("mode", true)), options: getLocationModes(true), multiple: false, required: false, submitOnChange: true, image: getAppImg("mode")
            if(isST()) {
                def routines = location.helloHome?.getPhrases()?.collectEntries { [(it?.id): it?.label] }?.sort { it?.value }
                input "${t}routine_run", "enum", title: inTS("Execute a routine${dMap?.def}\n(Optional)", getAppImg("routine", true)), options: routines, multiple: false, required: false, submitOnChange: true, image: getAppImg("routine")
            }
        }
        section (sTS("Execute a webCoRE Piston:")) {
            input "enableWebCoRE", "bool", title: inTS("Enable webCoRE Integration", webCore_icon()), required: false, defaultValue: false, submitOnChange: true, image: (isST() ? webCore_icon() : "")
            if(settings?.enableWebCoRE) {
                if(!atomicState?.webCoRE) { webCoRE_init() }
                input "${t == "act_" ? "" : t}webCorePistons", "enum", title: inTS("Execute Piston${dMap?.def}", webCore_icon()), options: webCoRE_list('name'), multiple: false, required: false, submitOnChange: true, image: (isST() ? webCore_icon() : "")
            }
        }
        if(actTasksConfiguredByType(t)) {
            section("Delay before running Tasks: ") {
                input "${t}tasks_delay", "number", title: inTS("Delay running ${dMap?.delay} in Seconds\n(Optional)", getAppImg("delay_time", true)), required: false, submitOnChange: true, image: getAppImg("delay_time")
            }
        }
    }
}

Boolean actTasksConfiguredByType(pType) {
    String pt = pType == "act_" ? "" : pType
    return (
        settings?."${pType}mode_run" || settings?."${pType}routine_run" || settings?."${pType}switches_off" || settings?."${pType}switches_on" || settings?."${pt}webCorePistons" ||
        settings?."${pType}lights" || settings?."${pType}locks" || settings?."${pType}sirens" || settings?."${pType}doors")
}

private executeTaskCommands(data) {
    String p = data?.type ?: null
    String pt = p == "act_" ? "" : p
    if(settings?."${p}switches_off") { settings?."${p}switches_off"?.off() }
    if(settings?."${p}switches_on") { settings?."${p}switches_on"?.on() }
    if(settings?."${p}locks_lock") { settings?."${p}locks_lock"?.lock() }
    if(settings?."${p}locks_unlock") { settings?."${p}locks_unlock"?.unlock() }
    if(settings?."${p}doors_close") { settings?."${p}doors_close"?.close() }
    if(settings?."${p}doors_open") { settings?."${p}doors_open"?.open() }
    if(settings?."${p}sirens" && settings?."${p}sirens_cmd") { settings?."${p}sirens"?."${settings?."${p}sirens_cmd"}"(); if(settings?."${p}sirens_time") { runIn(settings?."${p}sirens_time", postTaskCommands); } }
    if(settings?.enableWebCoRE && settings?."${pt}webCorePistons") { webCoRE_execute(settings?."${pt}webCorePistons") }
    if(settings?."${p}mode_run") { setLocationMode(settings?."${p}mode_run" as String) }
    if(isST && settings?."${p}routine_run") { execRoutineById(settings?."${p}routine_run" as String) }
    if(settings?."${p}lights") {
        if(settings?."${data?.type}lights_color_delay") { captureLightState(settings?."${p}lights") }
        settings?."${p}lights"?.on()
        if(settings?."${p}lights_level") { settings?."${p}lights"?.setLevel(getColorName(settings?."${p}lights_level")) }
        if(settings?."${p}lights_color") { settings?."${p}lights"?.setColor(getColorName(settings?."${p}lights_color", settings?."${p}lights_level")) }
        if(settings?."${data?.type}lights_color_delay") { runIn(settings?."${data?.type}lights_color_delay" as Integer, [data:[type: p]]) }
    }
}

String actTaskDesc(t, isInpt=false) {
    String pt = t == "act_" ? "" : t
    String str = ""
    if(actTasksConfiguredByType(t)) {
        switch(t) {
            case "act_":
                str += "${isInpt ? "" : "\n\n"}Trigger Tasks:"
                break
            case "act_tier_start_":
                str += "${isInpt ? "" : "\n\n"}Tier Start Tasks:"
                break
            case "act_tier_stop_":
                str += "${isInpt ? "" : "\n\n"}Tier Stop Tasks:"
                break
        }
        str += settings?."${t}switches_on" ? "\n \u2022 Switches On: (${settings?."${t}switches_on"?.size()})" : ""
        str += settings?."${t}switches_off" ? "\n \u2022 Switches Off: (${settings?."${t}switches_off"?.size()})" : ""
        str += settings?."${t}lights" ? "\n \u2022 Lights: (${settings?."${t}lights"?.size()})" : ""
        str += settings?."${t}lights" && settings?."${t}lights_level" ? "\n    - Level: (${settings?."${t}lights_level"}%)" : ""
        str += settings?."${t}lights" && settings?."${t}lights_color" ? "\n    - Color: (${settings?."${t}lights_color"})" : ""
        str += settings?."${t}lights" && settings?."${t}lights_color" && settings?."${t}lights_color_delay" ? "\n    - Restore After: (${settings?."${t}lights_color_delay"} sec.)" : ""
        str += settings?."${t}locks_unlock" ? "\n \u2022 Locks Unlock: (${settings?."${t}locks_unlock"?.size()})" : ""
        str += settings?."${t}locks_lock" ? "\n \u2022 Locks Lock: (${settings?."${t}locks_lock"?.size()})" : ""
        str += settings?."${t}doors_open" ? "\n \u2022 Garages Open: (${settings?."${t}doors_open"?.size()})" : ""
        str += settings?."${t}doors_close" ? "\n \u2022 Garages Close: (${settings?."${t}doors_close"?.size()})" : ""
        str += settings?."${t}sirens" ? "\n \u2022 Sirens On: (${settings?."${t}sirens"?.size()})${settings?."${t}sirens_delay" ? "(${settings?."${t}sirens_delay"} sec)" : ""}" : ""

        str += settings?."${t}mode_run" ? "\n \u2022 Set Mode:\n \u2022 ${settings?."${t}mode_run"}" : ""
        str += settings?."${t}routine_run" ? "\n \u2022 Execute Routine:\n    - ${getRoutineById(settings?."${t}routine_run")?.label}" : ""
        str += (settings?.enableWebCoRE && settings?.webCorePistons) ? "\n \u2022 webCoRE Piston:\n    - ${settings?."${t == "act_" ? "" : t}webCorePistons"}" : ""
    }
    return str != "" ? (isInpt ? "${str}\n\nTap to modify" : "${str}") : (isInpt ? "On trigger control devices, set mode, execute routines or WebCore Pistons\n\nTap to configure" : null)
}

private flashLights(data) {
    // log.debug "data: ${data}"
    if(data && data?.type && settings?."${data?.type}lights") {
        def devs = settings?."${data?.type}lights"
        // log.debug "devs: $devs"
        if(data?.cycle <= data?.cycles ) {
            log.debug "state: ${data?.state} | color1Map: ${data?.color1Map} | color2Map: ${data?.color2Map}"
            if(data?.state == "off" || (data?.color1Map && data?.color2Map && data?.state == data?.color2Map)) {
                if(data?.color1Map) {
                    data?.state = data?.color1Map
                    dev?.setColor(data?.color1Map)
                } else {
                    data?.state = "on"
                }
                devs?.on()
                runIn(1, "flashLights", [data: data])
            } else {
                if(data?.color2Map) {
                    data?.state = data?.color2Map
                    devs?.setColor(data?.color2Map)
                } else {
                    data?.state = "off"; devs?.off();
                }
                data?.cycle = data?.cycle + 1
                runIn(1, "flashLights", [data: data])
            }
        } else {
            log.debug "restoring state"
            restoreLightState(settings?."${p}lights")
        }
    }
}

private restoreLights(data) {
    if(data?.type && settings?."${data?.type}lights") { restoreLightState(settings?."${data?.type}lights") }
}

Boolean isActDevContConfigured() {
    return isTierAction() ? (settings?.act_tier_start_switches_off || settings?.act_tier_start_switches_on || settings?.act_tier_stop_switches_off || settings?.act_tier_stop_switches_on) : (settings?.act_switches_off || settings?.act_switches_on)
}

def actionSimulationSect() {
    section(sTS("Simulate Action")) {
        paragraph pTS("Toggle this to execute the action and see the results.\nWhen global text is not defined, this will generate a random event based on your trigger selections.${act_EchoZones ? "\nTesting with zones requires you to save the app and come back in to test." : ""}", getAppImg("info", true), false, "#2784D9"), image: getAppImg("info")
        input "actTestRun", "bool", title: inTS("Test this action?", getAppImg("testing", true)), description: "", required: false, defaultValue: false, submitOnChange: true, image: getAppImg("testing")
        if(actTestRun) { executeActTest() }
    }
}

Boolean customMsgRequired() { return ((settings?.actionType in ["speak", "announcement"]) != true) }
Boolean customMsgConfigured() { return (settings?.notif_use_custom && settings?.notif_custom_message) }

def actNotifPage() {
    return dynamicPage(name: "actNotifPage", title: "Action Notifications", install: false, uninstall: false) {
        section (sTS("Message Customization:")) {
            Boolean custMsgReq = customMsgRequired()
            // if(custMsgReq && !settings?.notif_use_custom) { settingUpdate("notif_use_custom", "true", "bool") }
            paragraph pTS("When using speak and announcements you can leave this off and a notification will be sent with speech text.  For other action types a custom message is required", null, false, "gray")
            input "notif_use_custom", "bool", title: inTS("Send a custom notification...", getAppImg("question", true)), required: false, defaultValue: false, submitOnChange: true, image: getAppImg("question")
            if(settings?.notif_use_custom || custMsgReq) {
                input "notif_custom_message", "text", title: inTS("Enter custom message...", getAppImg("text", true)), required: custMsgReq, submitOnChange: true, image: getAppImg("text")
            }
        }

        section (sTS("Push Messages:")) {
            input "notif_send_push", "bool", title: inTS("Send Push Notifications...", getAppImg("question", true)), required: false, defaultValue: false, submitOnChange: true, image: getAppImg("question")
        }
        section (sTS("Text Messages:"), hideWhenEmpty: true) {
            paragraph pTS("To send to multiple numbers separate the number by a comma\n\nE.g. 8045551122,8046663344", getAppImg("info", true), false, "gray")
            paragraph pTS("SMS Support will soon be removed from Hubitat and SmartThings (UK)", getAppImg("info", true), false, "gray")
            input "notif_sms_numbers", "text", title: inTS("Send SMS Text to...", getAppImg("sms_phone", true)), required: false, submitOnChange: true, image: getAppImg("sms_phone")
        }
        section (sTS("Notification Devices:")) {
            input "notif_devs", "capability.notification", title: inTS("Send to Notification devices?", getAppImg("notification", true)), required: false, multiple: true, submitOnChange: true, image: getAppImg("notification")
        }
        section (sTS("Alexa Mobile Notification:")) {
            paragraph pTS("This will send a push notification the Alexa Mobile app.", null, false, "gray")
            input "notif_alexa_mobile", "bool", title: inTS("Send message to Alexa App?", getAppImg("notification", true)), required: false, defaultValue: false, submitOnChange: true, image: getAppImg("notification")
        }
        if(isST()) {
            section(sTS("Pushover Support:")) {
                input "notif_pushover", "bool", title: inTS("Use Pushover Integration", getAppImg("pushover_icon", true)), required: false, submitOnChange: true, image: getAppImg("pushover")
                if(settings?.notif_pushover == true) {
                    def poDevices = parent?.getPushoverDevices()
                    if(!poDevices) {
                        parent?.pushover_init()
                        paragraph pTS("If this is the first time enabling Pushover than leave this page and come back if the devices list is empty", null, false, "#2784D9"), state: "complete"
                    } else {
                        input "notif_pushover_devices", "enum", title: inTS("Select Pushover Devices", getAppImg("select_icon", true)), description: "Tap to select", groupedOptions: poDevices, multiple: true, required: false, submitOnChange: true, image: getAppImg("select_icon")
                        if(settings?.notif_pushover_devices) {
                            def t0 = [(-2):"Lowest", (-1):"Low", 0:"Normal", 1:"High", 2:"Emergency"]
                            input "notif_pushover_priority", "enum", title: inTS("Notification Priority (Optional)", getAppImg("priority", true)), description: "Tap to select", defaultValue: 0, required: false, multiple: false, submitOnChange: true, options: t0, image: getAppImg("priority")
                            input "notif_pushover_sound", "enum", title: inTS("Notification Sound (Optional)", getAppImg("sound", true)), description: "Tap to select", defaultValue: "pushover", required: false, multiple: false, submitOnChange: true, options: parent?.getPushoverSounds(), image: getAppImg("sound")
                        }
                    }
                }
            }
        }
        if(isActNotifConfigured()) {
            section(sTS("Notification Restrictions:")) {
                def nsd = getNotifSchedDesc()
                href "actNotifTimePage", title: inTS("Quiet Restrictions", getAppImg("restriction", true)), description: (nsd ? "${nsd}\nTap to modify..." : "Tap to configure"), state: (nsd ? "complete" : null), image: getAppImg("restriction")
            }
            if(!state?.notif_message_tested) {
                def actDevices = settings?.notif_alexa_mobile ? parent?.getDevicesFromList(settings?.act_EchoDevices) : []
                def aMsgDev = actDevices?.size() && settings?.notif_alexa_mobile ? actDevices[0] : null
                if(sendNotifMsg("Info", "Action Notification Test Successful. Notifications Enabled for ${app?.getLabel()}", aMsgDev, true)) { state?.notif_message_tested = true }
            }
        } else { state?.notif_message_tested = false }
    }
}

def actNotifTimePage() {
    return dynamicPage(name:"actNotifTimePage", title: "", install: false, uninstall: false) {
        def pre = "notif"
        Boolean timeReq = (settings["${pre}_time_start"] || settings["${pre}_time_stop"])
        section(sTS("Start Time:")) {
            input "${pre}_time_start_type", "enum", title: inTS("Starting at...", getAppImg("start_time", true)), options: ["time":"Time of Day", "sunrise":"Sunrise", "sunset":"Sunset"], required: false , submitOnChange: true, image: getAppImg("start_time")
            if(settings?."${pre}_time_start_type" == "time") {
                input "${pre}_time_start", "time", title: inTS("Start time", getAppImg("start_time", true)), required: timeReq, submitOnChange: true, image: getAppImg("start_time")
            } else if(settings?."${pre}_time_start_type" in ["sunrise", "sunrise"]) {
                input "${pre}_time_start_offset", "number", range: "*..*", title: inTS("Offset in minutes (+/-)", getAppImg("start_time", true)), required: false, submitOnChange: true, image: getAppImg("threshold")
            }
        }
        section(sTS("Stop Time:")) {
            input "${pre}_time_stop_type", "enum", title: inTS("Stopping at...", getAppImg("start_time", true)), options: ["time":"Time of Day", "sunrise":"Sunrise", "sunset":"Sunset"], required: false , submitOnChange: true, image: getAppImg("stop_time")
            if(settings?."${pre}_time_stop_type" == "time") {
                input "${pre}_time_stop", "time", title: inTS("Stop time", getAppImg("start_time", true)), required: timeReq, submitOnChange: true, image: getAppImg("stop_time")
            } else if(settings?."${pre}_time_stop_type" in ["sunrise", "sunrise"]) {
                input "${pre}_time_stop_offset", "number", range: "*..*", title: inTS("Offset in minutes (+/-)", getAppImg("start_time", true)), required: false, submitOnChange: true, image: getAppImg("threshold")
            }
        }
        section(sTS("Quiet Days:")) {
            input "${pre}_days", "enum", title: inTS("Only on these week days", getAppImg("day_calendar", true)), multiple: true, required: false, image: getAppImg("day_calendar"), options: weekDaysEnum()
        }
        section(sTS("Quiet Modes:")) {
            input "${pre}_modes", "mode", title: inTS("When these Modes are Active", getAppImg("mode", true)), multiple: true, submitOnChange: true, required: false, image: getAppImg("mode")
        }
    }
}

def ssmlInfoSection() {
    String ssmlTestUrl = "https://topvoiceapps.com/ssml"
    String ssmlDocsUrl = "https://developer.amazon.com/docs/custom-skills/speech-synthesis-markup-language-ssml-reference.html"
    String ssmlSoundsUrl = "https://developer.amazon.com/docs/custom-skills/ask-soundlibrary.html"
    String ssmlSpeechConsUrl = "https://developer.amazon.com/docs/custom-skills/speechcon-reference-interjections-english-us.html"
    section(sTS("SSML Documentation:"), hideable: true, hidden: true) {
        paragraph title: "What is SSML?", pTS("SSML allows for changes in tone, speed, voice, emphasis. As well as using MP3, and access to the Sound Library", null, false, "#2784D9"), state: "complete", image: getAppImg("info")
        href url: ssmlDocsUrl, style: "external", required: false, title: inTS("Amazon SSML Docs", getAppImg("www", true)), description: "Tap to open browser", image: getAppImg("www")
        href url: ssmlSoundsUrl, style: "external", required: false, title: inTS("Amazon Sound Library", getAppImg("www", true)), description: "Tap to open browser", image: getAppImg("www")
        href url: ssmlSpeechConsUrl, style: "external", required: false, title: inTS("Amazon SpeechCons", getAppImg("www", true)), description: "Tap to open browser", image: getAppImg("www")
        href url: ssmlTestUrl, style: "external", required: false, title: inTS("SSML Designer and Tester", getAppImg("www", true)), description: "Tap to open browser", image: getAppImg("www")
    }
}

def cleanupDevSettings(prefix) {
    List cDevs = settings?.act_EchoDevices
    List sets = settings?.findAll { it?.key?.startsWith(prefix) }?.collect { it?.key as String }
    log.debug "cDevs: $cDevs | sets: $sets"
    List rem = []
    if(sets?.size()) {
        if(cDevs?.size()) {
            cDevs?.each {
                if(!sets?.contains("${prefix}${it}")) {
                    rem?.push("${prefix}${it}")
                }
            }
        } else { rem = rem + sets }
    }
    log.debug "rem: $rem"
    // rem?.each { sI-> if(settings?.containsKey(sI as String)) { settingRemove(sI as String) } }
}

Map customTxtItems() {
    Map items = [:]
    settings?.triggerEvents?.each { tr->
        if(settings?."trig_${tr}_txt") { if(!items[tr]) { items[tr] = [:]; }; items[tr]?.event = settings?."trig_${tr}_txt"?.toString()?.tokenize(";"); }
        if(settings?."trig_${tr}_after_repeat_txt") { if(!items[tr]) { items[tr] = [:]; };  items[tr]?.repeat = settings?."trig_${tr}_after_repeat_txt"?.toString()?.tokenize(";"); }
    }
    return items
}

Boolean hasRepeatTriggers() {
    Map items = [:]
    settings?.triggerEvents?.each { tr->
        if(settings?."trig_${tr}_after_repeat_txt") { if(!items[tr]) { items[tr] = [:]; };  items[tr]?.repeat = settings?."trig_${tr}_after_repeat_txt"?.toString()?.tokenize(";"); }
    }
    return (items?.size())
}

Boolean hasUserDefinedTxt() {
    List items = []
    settings?.triggerEvents?.each {
        if(settings?."trig_${it}_txt") { return true }
        if(settings?."trig_${it}_after_repeat_txt") { return true }
    }
    return false
}

Boolean executionConfigured() {
    Boolean opts = (state?.actionExecMap && state?.actionExecMap?.configured == true)
    Boolean devs = (settings?.act_EchoDevices || settings?.act_EchoZones)
    return (opts || devs)
}

private getLastEchoSpokenTo() {
    return parent?.getChildDevicesByCap("TTS")?.find { (it?.currentWasLastSpokenToDevice?.toString() == "true") } ?: null
}

private echoDevicesInputByPerm(type) {
    List echoDevs = parent?.getChildDevicesByCap(type as String)
    Boolean capOk = (type in ["TTS", "announce"])
    Boolean zonesOk = (settings?.actionType in ["speak", "speak_tiered", "announcement", "announcement_tiered", "sequence", "weather", "calendar", "music", "sounds", "builtin"])
    Map echoZones = (capOk && zonesOk) ? parent?.getZones() : [:]
    section(sTS("Alexa Devices${echoZones?.size() ? " & Zones" : ""}:")) {
        if(echoZones?.size()) {
            if(!settings?.act_EchoZones) { paragraph pTS("Zones are used to direct the speech output based on the conditions set in the zones themselves (Motion, presence, etc).\nWhen both Zones and Echo devices are selected zone will take priority over the echo devices.", null, false) }
            input "act_EchoZones", "enum", title: inTS("Zone(s) to Use", getAppImg("es_groups", true)), description: "Select the Zones", options: echoZones?.collectEntries { [(it?.key): it?.value?.name as String] }, multiple: true, required: (!settings?.act_EchoDevices), submitOnChange: true, image: getAppImg("es_groups")
        }
        if(settings?.act_EchoZones?.size() && echoDevs?.size() && !settings?.act_EchoDevices?.size()) {
            paragraph pTS("There may be times when none of your zones are active at the time of action execution.\nYou have the option to select devices to use when no zones are available.", null, false, "#2678D9")
        }
        if(echoDevs?.size()) {
            Boolean devsOpt = (settings?.act_EchoZones?.size())
            def eDevsMap = echoDevs?.collectEntries { [(it?.getId()): [label: it?.getLabel(), lsd: (it?.currentWasLastSpokenToDevice?.toString() == "true")]] }?.sort { a,b -> b?.value?.lsd <=> a?.value?.lsd ?: a?.value?.label <=> b?.value?.label }
            input "act_EchoDevices", "enum", title: inTS("Echo Speaks Devices${devsOpt ? "\n(Optional)" : ""}", getAppImg("echo_gen1", true)), description: (devsOpt ? "These devices are used when all zones are inactive." : "Select your devices"), options: eDevsMap?.collectEntries { [(it?.key): "${it?.value?.label}${(it?.value?.lsd == true) ? "\n(Last Spoken To)" : ""}"] }, multiple: true, required: (!settings?.act_EchoZones), submitOnChange: true, image: getAppImg("echo_gen1")
        } else { paragraph pTS("No devices were found with support for ($type)", null, true, "red") }

    }
}

private actionVolumeInputs(devices, showVolOnly=false, showAlrmVol=false) {
    if(showAlrmVol) {
        section(sTS("Volume Options:")) {
            input "act_alarm_volume", "number", title: inTS("Alarm Volume\n(Optional)", getAppImg("speed_knob", true)), range: "0..100", required: false, submitOnChange: true, image: getAppImg("speed_knob")
        }
    } else {
        if((devices || settings?.act_EchoZones) && settings?.actionType in ["speak", "announcement", "weather", "sounds", "builtin", "music", "calendar", "playback"]) {
            Map volMap = devsSupportVolume(devices)
            section(sTS("Volume Options:")) {
                if(volMap?.n?.size() > 0 && volMap?.n?.size() < devices?.size()) { paragraph "Some of the selected devices do not support volume control" }
                else if(devices?.size() == volMap?.n?.size()) { paragraph "Some of the selected devices do not support volume control"; return; }
                input "act_volume_change", "number", title: inTS("Volume Level\n(Optional)", getAppImg("speed_knob", true)), description: "(0% - 100%)", range: "0..100", required: false, submitOnChange: true, image: getAppImg("speed_knob")
                if(!showVolOnly) { input "act_volume_restore", "number", title: inTS("Restore Volume\n(Optional)", getAppImg("speed_knob", true)), description: "(0% - 100%)", range: "0..100", required: false, submitOnChange: true, image: getAppImg("speed_knob") }
            }
        }
    }
}

def condTimePage() {
    return dynamicPage(name:"condTimePage", title: "", install: false, uninstall: false) {
        Boolean timeReq = (settings["cond_time_start"] || settings["cond_time_stop"])
        section(sTS("Start Time:")) {
            input "cond_time_start_type", "enum", title: inTS("Starting at...", getAppImg("start_time", true)), options: ["time":"Time of Day", "sunrise":"Sunrise", "sunset":"Sunset"], required: false , submitOnChange: true, image: getAppImg("start_time")
            if(cond_time_start_type  == "time") {
                input "cond_time_start", "time", title: inTS("Start time", getAppImg("start_time", true)), required: timeReq, submitOnChange: true, image: getAppImg("start_time")
            } else if(cond_time_start_type in ["sunrise", "sunrise"]) {
                input "cond_time_start_offset", "number", range: "*..*", title: inTS("Offset in minutes (+/-)", getAppImg("start_time", true)), required: false, submitOnChange: true, image: getAppImg("threshold")
            }
        }
        section(sTS("Stop Time:")) {
            input "cond_time_stop_type", "enum", title: inTS("Stopping at...", getAppImg("start_time", true)), options: ["time":"Time of Day", "sunrise":"Sunrise", "sunset":"Sunset"], required: false , submitOnChange: true, image: getAppImg("stop_time")
            if(cond_time_stop_type == "time") {
                input "cond_time_stop", "time", title: inTS("Stop time", getAppImg("start_time", true)), required: timeReq, submitOnChange: true, image: getAppImg("stop_time")
            } else if(cond_time_stop_type in ["sunrise", "sunrise"]) {
                input "cond_time_stop_offset", "number", range: "*..*", title: inTS("Offset in minutes (+/-)", getAppImg("start_time", true)), required: false, submitOnChange: true, image: getAppImg("threshold")
            }
        }
    }
}

def uninstallPage() {
    return dynamicPage(name: "uninstallPage", title: "Uninstall", install: false , uninstall: true) {
        section("") { paragraph "This will delete this Echo Speaks Action." }
        if(isST()) { remove("Remove ${app?.label} Action", "WARNING!!!", "Last Chance to Stop!\nThis action is not reversible\n\nThis Action will be removed") }
    }
}

Boolean wordInString(String findStr, String fullStr) {
    List parts = fullStr?.split(" ")?.collect { it?.toString()?.toLowerCase() }
    return (findStr in parts)
}

def installed() {
    logInfo("Installed Event Received...")
    state?.dateInstalled = getDtNow()
    initialize()
}

def updated() {
    logInfo("Updated Event Received...")
    if(state?.dupOpenedByUser == true) { state?.dupPendingSetup = false }
    initialize()
}

def initialize() {
    // logInfo("Initialize Event Received...")
    unsubscribe()
    unschedule()
    if(settings?.duplicateFlag == true && state?.dupPendingSetup == false) {
        settingUpdate("duplicateFlag", "false", "bool")
        state?.remove("dupOpenedByUser")
    } else if(settings?.duplicateFlag == true && state?.dupPendingSetup != false) {
        String newLbl = app?.getLabel() + app?.getLabel()?.toString()?.contains("(Dup)") ? "" : " (Dup)"
        app?.updateLabel(newLbl)
        state?.dupPendingSetup = true
        def dupState = parent?.getDupActionStateData()
        if(dupState?.size()) {
            dupState?.each {k,v-> state[k] = v }
            parent?.clearDuplicationItems()
        }
        logInfo("Duplicated Action has been created... Please open action and configure to complete setup...")
        return
    }
    state?.isInstalled = true
    updAppLabel()
    runIn(3, "actionCleanup")
    runIn(7, "subscribeToEvts")
    runEvery1Hour("healthCheck")
    updateZoneSubscriptions() // Subscribes to Echo Speaks Zone Activation Events...
    updConfigStatusMap()
    resumeTierJobs()
}

def updateZoneSubscriptions() {
    if(settings?.act_EchoZones) {
        subscribe(location, "es3ZoneState", zoneStateHandler); subscribe(location, "es3ZoneRemoved", zoneRemovedHandler);
        sendLocationEvent(name: "es3ZoneRefresh", value: "sendStatus", data: [sendStatus: true], isStateChange: true)
    }
}

String getActionName() { return settings?.appLbl as String }

private updAppLabel() {
    String newLbl = "${settings?.appLbl} (A${isPaused() ? " \u275A\u275A" : ""})"?.replaceAll(/(Dup)/, "").replaceAll("\\s"," ")
    if(settings?.appLbl && app?.getLabel() != newLbl) { app?.updateLabel(newLbl) }
}

private updConfigStatusMap() {
    Map sMap = atomicState?.configStatusMap ?: [:]
    sMap?.triggers = triggersConfigured()
    sMap?.conditions = conditionsConfigured()
    sMap?.actions = executionConfigured()
    sMap?.tiers = isTierActConfigured()
    atomicState?.configStatusMap = sMap
}

private getConfStatusItem(item) {
    return (state?.configStatusMap?.containsKey(item) && state?.configStatusMap[item] == true)
}

private actionCleanup() {
    // State Cleanup
    List items = ["afterEvtMap", "afterEvtChkSchedMap", "actTierState", "tierSchedActive"]
    items?.each { si-> if(state?.containsKey(si as String)) { state?.remove(si)} }
    //Cleans up unused action setting items
    List setItems = []
    List setIgn = ["act_delay", "act_volume_change", "act_volume_restore", "act_tier_cnt", "act_switches_off", "act_switches_on", "act_routine_run", "act_mode_run"]
    if(settings?.act_EchoZones) { setIgn?.push("act_EchoZones") }
    else if(settings?.act_EchoDevices) { setIgn?.push("act_EchoDevices") }

    if(settings?.actionType) {
        def isTierAct = isTierAction()
        if(!isTierAct) {
            ["act_lights", "act_locks", "act_doors", "act_sirens"]?.each { settings?.each { sI -> if(sI?.key?.startsWith(it)) { isTierAct ? setItems?.push(sI?.key as String) : setIgn?.push(sI?.key as String) } } }
        }
        ["act_tier_start_", "act_tier_stop_"]?.each { settings?.each { sI -> if(sI?.key?.startsWith(it)) { isTierAct ? setIgn?.push(sI?.key as String) : setItems?.push(sI?.key as String) } } }
        settings?.each { si->
            if(!(si?.key in setIgn) && si?.key?.startsWith("act_") && !si?.key?.startsWith("act_${settings?.actionType}") && (!isTierAct && si?.key?.startsWith("act_tier_item_"))) { setItems?.push(si?.key as String) }
        }
    }

    // Cleanup Unused Trigger Types...
    if(settings?.triggerEvents) {
        List trigKeys = settings?.findAll { it?.key?.startsWith("trig_") && !(it?.key?.tokenize("_")[1] in settings?.triggerEvents) }?.keySet()?.collect { it?.tokenize("_")[1] as String }?.unique()
        // log.debug "trigKeys: $trigKeys"
        if(trigKeys?.size()) {
            trigKeys?.each { tk-> setItems?.push("trig_${tk}"); ["wait", "all", "cmd", "once", "after", "txt", "nums"]?.each { ei-> setItems?.push("trig_${tk}_${ei}"); }; }
        }
    }

    // Cleanup Unused Schedule Trigger Items
    setItems = setItems + ["trig_scheduled_sunState", "trig_scheduled_sunState_offset",]
    if(!settings?.trig_scheduled_type) {
        setItems = setItems + ["trig_scheduled_daynums", "trig_scheduled_months", "trig_scheduled_type", "trig_scheduled_recurrence", "trig_scheduled_time", "trig_scheduled_weekdays", "trig_scheduled_weeks"]
    } else {
        switch(settings?.trig_scheduled_type) {
            case "One-Time":
            case "Sunrise":
            case "Sunset":
                setItems = setItems + ["trig_scheduled_daynums", "trig_scheduled_months", "trig_scheduled_recurrence", "trig_scheduled_weekdays", "trig_scheduled_weeks"]
                if(settings?.trig_scheduled_type in ["Sunset", "Sunrise"]) { setItems?.push("trig_scheduled_time") }
                break
            case "Recurring":
                switch(settings?.trig_scheduled_recurrence) {
                    case "Daily":
                        setItems = setItems + ["trig_scheduled_daynums", "trig_scheduled_months", "trig_scheduled_weeks"]
                        break
                    case "Weekly":
                        setItems?.push("trig_scheduled_daynums")
                        break
                    case "Monthly":
                        setItems?.push("trig_scheduled_weekdays")
                        if(settings?.trig_scheduled_daynums && settings?.trig_scheduled_weeks) { setItems?.push("trig_scheduled_weeks") }
                        break
                }
                break
        }
    }

    settings?.each { si-> if(si?.key?.startsWith("broadcast") || si?.key?.startsWith("musicTest") || si?.key?.startsWith("announce") || si?.key?.startsWith("sequence") || si?.key?.startsWith("speechTest")) { setItems?.push(si?.key as String) } }
    if(atomicState?.webCoRE && !settings?.enableWebCoRE) { setItems?.push("webCorePistons"); state?.remove("webCoRE"); }
    // Performs the Setting Removal
    setItems = setItems + ["tuneinSearchQuery", "usePush", "smsNumbers", "pushoverSound", "pushoverDevices", "pushoverEnabled", "pushoverPriority", "alexaMobileMsg", "appDebug"]
    // log.debug "setItems: $setItems"
    setItems?.unique()?.each { sI-> if(settings?.containsKey(sI as String)) { settingRemove(sI as String) } }
}

Boolean isPaused() { return (settings?.actionPause == true) }
public triggerInitialize() { runIn(3, "initialize") }
private valTrigEvt(key) { return (key in settings?.triggerEvents) }

public updatePauseState(Boolean pause) {
    if(settings?.actionPause != pause) {
        logDebug("Received Request to Update Pause State to (${pause})")
        settingUpdate("actionPause", "${pause}", "bool")
        runIn(4, "updated")
    }
}

private healthCheck() {
    // logTrace("healthCheck", true)
    if(advLogsActive()) { logsDisable() }
}

String cronBuilder() {
    /****
        Cron Expression Format: (<second> <minute> <hour> <day-of-month> <month> <day-of-week> <?year>)

        * (all) – it is used to specify that event should happen for every time unit. For example, “*” in the <minute> field – means “for every minute”
        ? (any) – it is utilized in the <day-of-month> and <day-of -week> fields to denote the arbitrary value – neglect the field value. For example, if we want to fire a script at “5th of every month” irrespective of what the day of the week falls on that date, then we specify a “?” in the <day-of-week> field
        – (range) – it is used to determine the value range. For example, “10-11” in <hour> field means “10th and 11th hours”
        , (values) – it is used to specify multiple values. For example, “MON, WED, FRI” in <day-of-week> field means on the days “Monday, Wednesday, and Friday”
        / (increments) – it is used to specify the incremental values. For example, a “5/15” in the <minute> field, means at “5, 20, 35 and 50 minutes of an hour”
        L (last) – it has different meanings when used in various fields. For example, if it's applied in the <day-of-month> field, then it means last day of the month, i.e. “31st for January” and so on as per the calendar month. It can be used with an offset value, like “L-3“, which denotes the “third to last day of the calendar month”. In the <day-of-week>, it specifies the “last day of a week”. It can also be used with another value in <day-of-week>, like “6L“, which denotes the “last Friday”
        W (weekday) – it is used to specify the weekday (Monday to Friday) nearest to a given day of the month. For example, if we specify “10W” in the <day-of-month> field, then it means the “weekday near to 10th of that month”. So if “10th” is a Saturday, then the job will be triggered on “9th”, and if “10th” is a Sunday, then it will trigger on “11th”. If you specify “1W” in the <day-of-month> and if “1st” is Saturday, then the job will be triggered on “3rd” which is Monday, and it will not jump back to the previous month
        # – it is used to specify the “N-th” occurrence of a weekday of the month, for example, “3rd Friday of the month” can be indicated as “6#3“

        At 12:00 pm (noon) every day during the year 2017:  (0 0 12 * * ? 2017)

        Every 5 minutes starting at 1 pm and ending on 1:55 pm and then starting at 6 pm and ending at 6:55 pm, every day: (0 0/5 13,18 * * ?)

        Every minute starting at 1 pm and ending on 1:05 pm, every day: (0 0-5 13 * * ?)

        At 1:15 pm and 1:45 pm every Tuesday in the month of June: (0 15,45 13 ? 6 Tue)

        At 9:30 am every Monday, Tuesday, Wednesday, Thursday, and Friday: (0 30 9 ? * MON-FRI)

        At 9:30 am on 15th day of every month: (0 30 9 15 * ?)

        At 6 pm on the last day of every month: (0 0 18 L * ?)

        At 6 pm on the 3rd to last day of every month: (0 0 18 L-3 * ?)

        At 10:30 am on the last Thursday of every month: (0 30 10 ? * 5L)

        At 6 pm on the last Friday of every month during the years 2015, 2016 and 2017: (0 0 18 ? * 6L 2015-2017)

        At 10 am on the third Monday of every month: (0 0 10 ? * 2#3)

        At 12 am midnight on every day for five days starting on the 10th day of the month: (0 0 0 10/5 * ?)
    ****/
    String cron = null
    def time = settings?.trig_scheduled_time ?: null
    List dayNums = settings?.trig_scheduled_daynums ? settings?.trig_scheduled_daynums?.collect { it as Integer }?.sort() : null
    List weekNums = settings?.trig_scheduled_weeks ? settings?.trig_scheduled_weeks?.collect { it as Integer }?.sort() : null
    List monthNums = settings?.trig_scheduled_months ? settings?.trig_scheduled_months?.collect { it as Integer }?.sort() : null
    if(time) {
        def hour = fmtTime(time, "HH") ?: "0"
        def minute = fmtTime(time, "mm") ?: "0"
        def second = "0" //fmtTime(time, "mm") ?: "0"
        def daysOfWeek = settings?.trig_scheduled_weekdays ? settings?.trig_scheduled_weekdays?.join(",") : null
        def daysOfMonth = dayNums?.size() ? (dayNums?.size() > 1 ? "${dayNums?.first()}-${dayNums?.last()}" : dayNums[0]) : null
        def weeks = (weekNums && !dayNums) ? weekNums?.join(",") : null
        def months = monthNums ? monthNums?.sort()?.join(",") : null
        // log.debug "hour: ${hour} | m: ${minute} | s: ${second} | daysOfWeek: ${daysOfWeek} | daysOfMonth: ${daysOfMonth} | weeks: ${weeks} | months: ${months}"
        if(hour || minute || second) {
            List cItems = []
            cItems?.push(second ?: "*")
            cItems?.push(minute ?: "0")
            cItems?.push(hour ?: "0")
            cItems?.push(daysOfMonth ?: (!daysOfWeek ? "*" : "?"))
            cItems?.push(months ?: "*")
            cItems?.push(daysOfWeek ? daysOfWeek?.toString()?.replaceAll("\"", "") : "?")
            if(year) { cItems?.push(" ${year}") }
            cron = cItems?.join(" ")
        }
    }
    logInfo("cronBuilder | Cron: ${cron}")
    return cron
}

Boolean schedulesConfigured() {
    if(settings?.trig_scheduled_type in ["Sunrise", "Sunset"]) { return true }
    else if(settings?.trig_scheduled_type == "One-Time" && settings?.trig_scheduled_time) { return true }
    else if(settings?.trig_scheduled_type == "Recurring" && settings?.trig_scheduled_time && settings?.trig_scheduled_recurrence) {
        if(settings?.trig_scheduled_recurrence == "Daily") { return true }
        else if (settings?.trig_scheduled_recurrence == "Weekly" && settings?.trig_scheduled_weekdays) { return true }
        else if (settings?.trig_scheduled_recurrence == "Monthly" && (settings?.trig_scheduled_daynums || settings?.trig_scheduled_weeks)) { return true }
    }
    return false
}

private subscribeToEvts() {
    if(minVersionFailed ()) { logError("CODE UPDATE required to RESUME operation.  No events will be monitored.", true); return; }
    if(isPaused()) { logWarn("Action is PAUSED... No Events will be subscribed to or scheduled....", true); return; }
    settings?.triggerEvents?.each { te->
        if(te == "scheduled" || settings?."trig_${te}") {
            switch (te as String) {
                case "scheduled":
                    // Scheduled Trigger Events
                    if (schedulesConfigured()) {
                        if(settings?.trig_scheduled_type == "Sunset") { subscribe(location, "sunsetTime", scheduleTrigEvt) }
                        else if(settings?.trig_scheduled_type == "Sunrise") { subscribe(location, "sunriseTime", scheduleTrigEvt) }
                        else if(settings?.trig_scheduled_type in ["One-Time", "Recurring"] && settings?.trig_scheduled_time) { schedule(cronBuilder() as String, "scheduleTrigEvt") }
                    }
                    break
                case "guard":
                    // Alexa Guard Status Events
                    state?.handleGuardEvents = true
                    break
                case "alarm":
                    // Location Alarm Events
                    subscribe(location, (!isST() ? "hsmStatus" : "alarmSystemStatus"), alarmEvtHandler)
                    if(!isST() && settings?.trig_alarm == "Alerts") { subscribe(location, "hsmAlert", alarmEvtHandler) } // Only on Hubitat
                    break
                case "mode":
                    // Location Mode Events
                    if(settings?.cond_mode && !settings?.cond_mode_cmd) { settingUpdate("cond_mode_cmd", "are", "enum") }
                    subscribe(location, "mode", modeEvtHandler)
                    break
                case "routineExecuted":
                    // Routine Execution Events
                    subscribe(location, "routineExecuted", routineEvtHandler)
                    break
                case "thermostat":
                    // Thermostat Events
                    subscribe(settings?."trig_${te}", attributeConvert(te as String), thermostatEvtHandler)
                    break
                default:
                    // Handles Remaining Device Events
                    subscribe(settings?."trig_${te}", attributeConvert(te as String), getDevEvtHandlerName(te as String))
                    break
            }
        }
    }
}

String attributeConvert(String attr) {
    Map atts = ["door":"garageDoorControl", "shade":"windowShade"]
    return (atts?.containsKey(attr as String)) ? atts[attr] as String : attr as String
}

private getDevEvtHandlerName(String type) {
    // if(isTierAction()) { return "deviceTierEvtHandler" }
    return (type && settings?."trig_${type}_after") ? "devAfterEvtHandler" : "deviceEvtHandler"
}

def zoneStateHandler(evt) {
    String id = evt?.value?.toString()
    Map data = evt?.jsonData;
    // log.debug "zoneStateHandler: ${id} | data: ${data}"
    if(settings?.act_EchoZones && id && data && (id?.toString() in settings?.act_EchoZones)) {
        Map zoneMap = atomicState?.zoneStatusMap ?: [:]
        zoneMap[id as String] = [name: data?.name, active: data?.active]
        atomicState?.zoneStatusMap = zoneMap
    }
}

def zoneRemovedHandler(evt) {
    String id = evt?.value?.toString()
    Map data = evt?.jsonData;
    log.trace "zone removed: ${id} | Data: $data"
    if(data && id) {
        Map zoneMap = atomicState?.zoneStatusMap ?: [:]
        if(zoneMap?.containsKey(id as String)) { zoneMap?.remove(id as String) }
        atomicState?.zoneStatusMap = zoneMap
    }
}

public Map getZones() {
    return atomicState?.zoneStatusMap ?: [:]
}

public Map getActiveZones() {
    Map zones = atomicState?.zoneStatusMap ?: [:]
    return zones?.size() ? zones?.findAll { it?.value?.active == true } : [:]
}

public List getActiveZoneNames() {
    Map zones = atomicState?.zoneStatusMap ?: [:]
    return zones?.size() ? zones?.findAll { it?.value?.active == true }?.collect { it?.value?.name as String } : []
}

/***********************************************************************************************************
    EVENT HANDLER FUNCTIONS
************************************************************************************************************/

def scheduleTest() {
    scheduleTrigEvt(null)
}

def scheduleTrigEvt(evt=null) {
    Map dateMap = getDateMap()
    // log.debug "dateMap: $dateMap"
    Map sTrigMap = atomicState?.schedTrigMap ?: [:]
    String recur = settings?.trig_scheduled_recurrence ?: null
    def days = settings?.trig_scheduled_weekdays ?: null
    def daynums = settings?.trig_scheduled_daynums ?: null
    def weeks = settings?.trig_scheduled_weeks ?: null
    def months = settings?.trig_scheduled_months ?: null
    Boolean wdOk = (days && recur in ["Daily", "Weekly"]) ? (dateMap?.dayNameShort in days && sTrigMap?.lastRun?.dayName != dateMap?.dayNameShort) : true
    Boolean mdOk = (recur && daynums) ? (dateMap?.day in daynums && sTrigMap?.lastRun?.day != dateMap?.day) : true
    Boolean wOk = (recur && weeks && recur in ["Weekly"]) ? (dateMap?.week in weeks && sTrigMap?.lastRun?.week != dateMap?.week) : true
    Boolean mOk = (recur && months && recur in ["Weekly", "Monthly"]) ? (dateMap?.month in months && sTrigMap?.lastRun?.month != dateMap?.month) : true
    // Boolean yOk = (recur && recur in ["Yearly"]) ? (sTrigMap?.lastRun?.y != dateMap?.y) : true
    if(wdOk && mdOk && wOk && mOk) {
        sTrigMap?.lastRun = dateMap
        atomicState?.schedTrigMap = sTrigMap
        if(evt) {
            executeAction(evt, false, "scheduleTrigEvt", false, false)
        } else {
            def dt = dateTimeFmt(new Date(), "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            executeAction([name: "Schedule", displayName: "Scheduled Trigger", value: time2Str(dt?.toString()), date: dt, deviceId: null], false, "scheduleTrigEvt", false, false)
        }
    } else {
        log.debug("scheduleTrigEvt | dayOfWeekOk: $wdOk | dayOfMonthOk: $mdOk | weekOk: $wOk | monthOk: $mOk")
    }
}

def alarmEvtHandler(evt) {
    def evtDelay = now() - evt?.date?.getTime()
    logTrace( "${evt?.name} Event | Device: ${evt?.displayName} | Value: (${strCapitalize(evt?.value)}) with a delay of ${evtDelay}ms")
    // Boolean dco = (settings?."trig_alarm_once" == true)
    // Integer dcw = settings?."trig_alarm_wait" ?: null
    // Boolean evtWaitOk = ((dco || dcw) ? evtWaitRestrictionOk([date: evt?.date, deviceId: "alarm", value: evt?.value, name: evt?.name, displayName: evt?.displayName], dco, dcw) : true)
    // if(!evtWaitOk) { return }
    switch(evt?.name) {
        case "hsmStatus":
        case "alarmSystemStatus":
        case "hsmAlert":
            if(getConfStatusItem("tiers")) {
                processTierTrigEvt(evt, true)
            } else { executeAction(evt, false, "alarmEvtHandler(${evt?.name})", false, false) }
            break
    }
}

public guardEventHandler(guardState) {
    if(!state?.alexaGuardState || state?.alexaGuardState != guardState) {
        state?.alexaGuardState = guardState
        def evt = [name: "guard", displayName: "Alexa Guard", value: state?.alexaGuardState, date: new Date(), deviceId: null]
        logTrace( "${evt?.name} Event | Device: ${evt?.displayName} | Value: (${strCapitalize(evt?.value)})")
        if(state?.handleGuardEvents) {
            // Boolean dco = (settings?."trig_guard_once" == true)
            // Integer dcw = settings?."trig_guard_wait" ?: null
            // Boolean evtWaitOk = ((dco || dcw) ? evtWaitRestrictionOk(evt, dco, dcw) : true)
            // if(!evtWaitOk) { return }
            executeAction(evt, false, "guardEventHandler", false, false)
        }
    }
}

def routineEvtHandler(evt) {
    logTrace( "${evt?.name?.toUpperCase()} Event | Routine: ${evt?.displayName} | with a delay of ${now() - evt?.date?.getTime()}ms")
    if(evt?.displayName in settings?.trig_routineExecuted) {
        Boolean dco = (settings?."trig_routineExecuted_once" == true)
        Integer dcw = settings?."trig_routineExecuted_wait" ?: null
        Boolean evtWaitOk = ((dco || dcw) ? evtWaitRestrictionOk([date: evt?.date, deviceId: "routineExecuted", value: evt?.displayName, name: evt?.name, displayName: evt?.displayName], dco, dcw) : true)
        if(!evtWaitOk) { return }
        if(getConfStatusItem("tiers")) {
            processTierTrigEvt(evt, true)
        } else { executeAction(evt, false, "routineEvtHandler", false, false) }
    }
}

def sceneEvtHandler(evt) {
    logTrace( "${evt?.name?.toUpperCase()} Event | Value: (${strCapitalize(evt?.value)}) with a delay of ${now() - evt?.date?.getTime()}ms")
    Boolean dco = (settings?."trig_scene_once" == true)
    Integer dcw = settings?."trig_scene_wait" ?: null
    Boolean evtWaitOk = ((dco || dcw) ? evtWaitRestrictionOk([date: evt?.date, deviceId: "scene", value: evt?.value, name: evt?.name, displayName: evt?.displayName], dco, dcw) : true)
    if(!evtWaitOk) { return }
    if(getConfStatusItem("tiers")) {
        processTierTrigEvt(evt, true)
    } else { executeAction(evt, false, "sceneEvtHandler", false, false) }
}

def modeEvtHandler(evt) {
    logTrace("${evt?.name?.toUpperCase()} Event | Mode: (${strCapitalize(evt?.value)}) with a delay of ${now() - evt?.date?.getTime()}ms")
    if(evt?.value in settings?.trig_mode) {
        Boolean dco = (settings?."trig_mode_once" == true)
        Integer dcw = settings?."trig_mode_wait" ?: null
        Boolean evtWaitOk = ((dco || dcw) ? evtWaitRestrictionOk([date: evt?.date, deviceId: "mode", value: evt?.value, name: evt?.name, displayName: evt?.displayName], dco, dcw) : true)
        if(!evtWaitOk) { return }
        if(getConfStatusItem("tiers")) {
            processTierTrigEvt(evt, true)
        } else { executeAction(evt, false, "modeEvtHandler", false, false) }
    }
}

Integer getLastAfterEvtCheck() { return !state?.lastAfterEvtCheck ? 10000000 : GetTimeDiffSeconds(state?.lastAfterEvtCheck, "getLastAfterEvtCheck").toInteger() }

def afterEvtCheckWatcher() {
    Map aEvtMap = atomicState?.afterEvtMap ?: [:]
    Map aSchedMap = atomicState?.afterEvtChkSchedMap ?: null
    if((aEvtMap?.size() == 0 && aSchedMap && aSchedMap?.id) || (aEvtMap?.size() && getLastAfterEvtCheck() > 240)) {
        runIn(2, "afterEvtCheckHandler")
    }
}

def devAfterEvtHandler(evt) {
    def evtDelay = now() - evt?.date?.getTime()
    Boolean ok = true
    Map aEvtMap = atomicState?.afterEvtMap ?: [:]
    Boolean aftWatSched = state?.afterEvtCheckWatcherSched ?: false
    def evtDt = parseDate(evt?.date?.toString())
    String dc = settings?."trig_${evt?.name}_cmd" ?: null
    Integer dcaf = settings?."trig_${evt?.name}_after" ?: null
    Integer dcafr = settings?."trig_${evt?.name}_after_repeat" ?: null
    Integer dcafrc = settings?."trig_${evt?.name}_after_repeat_cnt" ?: null
    String eid = "${evt?.deviceId}_${evt?.name}"
    Boolean schedChk = (dc && dcaf && evt?.value == dc)
    logTrace( "Device Event | ${evt?.name?.toUpperCase()} | Name: ${evt?.displayName} | Value: (${strCapitalize(evt?.value)}) with a delay of ${evtDelay}ms | SchedCheck: (${schedChk})")
    if(aEvtMap?.containsKey(eid)) {
        if(dcaf && !schedChk) {
            aEvtMap?.remove(eid)
            log.warn "Removing ${evt?.displayName} from AfterEvtCheckMap | Reason: (${evt?.name?.toUpperCase()}) no longer has the state of (${dc}) | Remaining Items: (${aEvtMap?.size()})"
        }
    }
    ok = schedChk
    if(ok) { aEvtMap["${evt?.deviceId}_${evt?.name}"] =
        [ dt: evt?.date?.toString(), deviceId: evt?.deviceId as String, displayName: evt?.displayName, name: evt?.name, value: evt?.value, triggerState: dc, wait: dcaf ?: null, isRepeat: false, repeatWait: dcafr ?: null, repeatCnt: 0, repeatCntMax: dcafrc ]
    }
    atomicState?.afterEvtMap = aEvtMap
    if(ok) {
        runIn(2, "afterEvtCheckHandler")
        if(!aftWatSched) {
            state?.afterEvtCheckWatcherSched = true
            runEvery5Minutes("afterEvtCheckWatcher")
        }
    }
}

def afterEvtCheckHandler() {
    Map aEvtMap = atomicState?.afterEvtMap ?: [:]
    if(aEvtMap?.size()) {
        // Collects all of the evt items and stores there wait values as a list
        Integer timeLeft = null
        Integer lowWait = aEvtMap?.findAll { it?.value?.wait != null }?.collect { it?.value?.wait }?.min()
        Integer lowLeft = aEvtMap?.findAll { it?.value?.wait == lowWait }?.collect { it?.value?.timeLeft} ?.min()
        def nextItem = aEvtMap?.find { it?.value?.wait == lowWait && it?.value?.timeLeft == lowLeft }
        def nextVal = nextItem?.value ?: null
        def nextId = (nextVal?.deviceId && nextVal?.name) ? "${nextVal?.deviceId}_${nextVal?.name}" : null
        if(nextVal) {
            def prevDt = nextVal?.isRepeat && nextVal?.repeatDt ? parseDate(nextVal?.repeatDt?.toString()) : parseDate(nextVal?.dt?.toString())
            def fullDt = parseDate(nextVal?.dt?.toString())
            def devs = settings?."trig_${nextVal?.name}" ?: null
            // log.debug "nextVal: $nextVal"
            Integer repeatCnt = (nextVal?.repeatCnt >= 0) ? nextVal?.repeatCnt + 1 : 1
            Integer repeatCntMax = nextVal?.repeatCntMax ?: null
            Boolean isRepeat = nextVal?.isRepeat ?: false
            Boolean hasRepeat = (settings?."trig_${nextVal?.name}_after_repeat" != null)
            if(prevDt) {
                def timeNow = new Date()?.getTime()
                Integer evtElap = (int) ((long)(timeNow - prevDt?.getTime())/1000)
                Integer fullElap = (int) ((long)(timeNow - fullDt?.getTime())/1000)
                Integer reqDur = (nextVal?.isRepeat && nextVal?.repeatWait) ? nextVal?.repeatWait : nextVal?.wait ?: null
                timeLeft = (reqDur - evtElap)
                aEvtMap[nextItem?.key]?.timeLeft = timeLeft
                aEvtMap[nextItem?.key]?.repeatCnt = repeatCnt
                // log.warn "After Debug | TimeLeft: ${timeLeft}(<=4 ${(timeLeft <= 4)}) | LastCheck: ${evtElap} | EvtDuration: ${fullElap} | RequiredDur: ${reqDur} | AfterWait: ${nextVal?.wait} | RepeatWait: ${nextVal?.repeatWait} | isRepeat: ${nextVal?.isRepeat} | RepeatCnt: ${repeatCnt} | RepeatCntMax: ${repeatCntMax}"
                if(timeLeft <= 4 && nextVal?.deviceId && nextVal?.name) {
                    timeLeft = reqDur
                    // log.debug "reqDur: $reqDur | evtElap: ${evtElap} | timeLeft: $timeLeft"
                    Boolean skipEvt = (nextVal?.triggerState && nextVal?.deviceId && nextVal?.name && devs) ? !devCapValEqual(devs, nextVal?.deviceId as String, nextVal?.name, nextVal?.triggerState) : true
                    Boolean skipEvtCnt = (repeatCntMax && (repeatCnt > repeatCntMax))
                    aEvtMap[nextItem?.key]?.timeLeft = timeLeft
                    if(!skipEvt && !skipEvtCnt) {
                        if(hasRepeat) {
                            // log.warn "Last Repeat ${nextVal?.displayName?.toString()?.capitalize()} (${nextVal?.name}) Event | TimeLeft: ${timeLeft} | LastCheck: ${evtElap} | EvtDuration: ${fullElap} | Required: ${reqDur}"
                            aEvtMap[nextItem?.key]?.repeatDt = formatDt(new Date())
                            aEvtMap[nextItem?.key]?.isRepeat = true
                            deviceEvtHandler([date: parseDate(nextVal?.repeatDt?.toString()), deviceId: nextVal?.deviceId as String, displayName: nextVal?.displayName, name: nextVal?.name, value: nextVal?.value, totalDur: fullElap], true, isRepeat)
                        } else {
                            aEvtMap?.remove(nextId)
                            log.warn "Wait Threshold (${reqDur} sec) Reached for ${nextVal?.displayName} (${nextVal?.name?.toString()?.capitalize()}) | TriggerState: (${nextVal?.triggerState}) | EvtDuration: ${fullElap}"
                            deviceEvtHandler([date: parseDate(nextVal?.dt?.toString()), deviceId: nextVal?.deviceId as String, displayName: nextVal?.displayName, name: nextVal?.name, value: nextVal?.value], true)
                        }
                    } else {
                        aEvtMap?.remove(nextId)
                        if(!skipEvt && skipEvtCnt) {
                            logInfo("${nextVal?.displayName} | (${nextVal?.name?.toString()?.capitalize()}) has repeated ${repeatCntMax} times | Skipping Action Repeat...")
                        } else {
                            logInfo("${nextVal?.displayName} | (${nextVal?.name?.toString()?.capitalize()}) state is already ${nextVal?.triggerState} | Skipping Action...")
                        }
                    }
                }
            }
        }
        // log.debug "nextId: $nextId | timeLeft: ${timeLeft}"
        runIn(2, "scheduleAfterCheck", [data: [val: timeLeft, id: nextId, repeat: isRepeat]])
        atomicState?.afterEvtMap = aEvtMap
        // logTrace( "afterEvtCheckHandler Remaining Items: (${aEvtMap?.size()})")
    } else { clearAfterCheckSchedule() }
    state?.lastAfterEvtCheck = getDtNow()
}

def deviceEvtHandler(evt, aftEvt=false, aftRepEvt=false) {
    def evtDelay = now() - evt?.date?.getTime()
    Boolean evtOk = false
    Boolean evtAd = false
    List d = settings?."trig_${evt?.name}"
    String dc = settings?."trig_${evt?.name}_cmd"
    Boolean dca = (settings?."trig_${evt?.name}_all" == true)
    Boolean dcavg = (!dca && settings?."trig_${evt?.name}_avg" == true)
    Boolean dco = (!settings?."trig_${evt?.name}_after" && settings?."trig_${evt?.name}_once" == true)
    Integer dcw = (!settings?."trig_${evt?.name}_after" && settings?."trig_${evt?.name}_wait") ? settings?."trig_${evt?.name}_wait" : null
    logTrace("Device Event | ${evt?.name?.toUpperCase()} | Name: ${evt?.displayName} | Value: (${strCapitalize(evt?.value)}) with a delay of ${evtDelay}ms${aftEvt ? " | (AfterEvt)" : ""}")
    Boolean devEvtWaitOk = ((dco || dcw) ? evtWaitRestrictionOk(evt, dco, dcw) : true)
    switch(evt?.name) {
        case "switch":
        case "lock":
        case "door":
        case "smoke":
        case "carbonMonoxide":
        case "windowShade":
        case "presence":
        case "contact":
        case "acceleration":
        case "motion":
        case "water":
        case "valve":
        case "button":
            if(d?.size() && dc) {
                if(dc == "any") { evtOk = true; }
                else {
                    if(dca && (allDevCapValsEqual(d, dc, evt?.value))) { evtOk = true; evtAd = true; }
                    else if(evt?.value == dc) { evtOk=true }
                }
            }
            break
        case "pushed":
        case "released":
        case "held":
        case "doubleTapped":
            def dcn = settings?."trig_${evt?.name}_nums"
            if(d?.size() && dc && dcn && dcn?.size() > 0) {
                if(dcn?.contains(evt?.value)) { evtOk = true; }
            }
            break;
        case "humidity":
        case "temperature":
        case "power":
        case "illuminance":
        case "level":
        case "battery":
            Double dcl = settings?."trig_${evt?.name}_low"
            Double dch = settings?."trig_${evt?.name}_high"
            Double dce = settings?."trig_${evt?.name}_equal"
            Map valChk = deviceEvtProcNumValue(evt, d, dc, dcl, dch, dce, dca, dcavg)
            evtOk = valChk?.evtOk
            evtAd = valChk?.evtAd
            break
    }
    Boolean execOk = (evtOk && devEvtWaitOk)
    if(getConfStatusItem("tiers")) {
        processTierTrigEvt(evt, execOk)
    } else if (execOk) { executeAction(evt, false, "deviceEvtHandler(${evt?.name})", evtAd, aftRepEvt) }
}

private processTierTrigEvt(evt, Boolean evtOk) {
    // log.debug "processTierTrigEvt | Name: ${evt?.name} | Value: ${evt?.value} | EvtOk: ${evtOk}"
    if (evtOk) {
        if(atomicState?.actTierState?.size()) { return }
        tierEvtHandler(evt)
    } else if(!evtOk && settings?.act_tier_stop_on_clear == true) {
        def tierConf = atomicState?.actTierState?.evt
        if(tierConf?.size() && tierConf?.name == evt?.name && tierConf?.deviceId == evt?.deviceId) {
            log.debug("Tier Trigger no longer valid... Clearing TierState and Schedule...")
            unschedule("tierEvtHandler")
            atomicState?.actTierState = [:]
            atomicState?.tierSchedActive = false
            updTsVal("lastTierRespStopDt")
        }
    }
}

Boolean isTierAction() {
    return (settings?.actionType in ["speak_tiered", "announcement_tiered"])
}

def getTierStatusSection() {
    String str = ""
    if(isTierAction()) {
        Map tS = atomicState?.actTierState ?: null
        str += "Tier Size: ${getTierMap()?.size()}\n"
        str += "Schedule Active: ${atomicState?.tierSchedActive == true}\n"
        str += tS?.cycle ? "Tier Cycle: ${tS?.cycle}\n" : ""
        str += tS?.schedDelay ? "Next Delay: ${tS?.schedDelay}\n" : ""
        str += tS?.lastMsg ? "Is Last Cycle: ${tS?.lastMsg == true}\n" : ""
        str += getTsVal("lastTierRespStartDt") ? "Last Tier Start: ${getTsVal("lastTierRespStartDt")}\n" : ""
        str += getTsVal("lastTierRespStopDt") ? "Last Tier Stop: ${getTsVal("lastTierRespStopDt")}\n" : ""
        section("Tier Response Status: ") {
            paragraph pTS(str, null, false, "#2678D9"), state: "complete"
        }
    }
}

private resumeTierJobs() {
    if(atomicState?.actTierState?.size() && atomicState?.tierSchedActive) {
        tierSchedHandler();
    }
}

private tierEvtHandler(evt=null) {
    Map tierMap = getTierMap() ?: [:]
    Map tierState = atomicState?.actTierState ?: [:]
    Boolean schedNext = false
    log.debug "tierState: ${tierState}"
    // log.debug "tierMap: ${tierMap}"
    if(tierMap && tierMap?.size()) {
        Map newEvt = tierState?.evt ?: [name: evt?.name, displayName: evt?.displayName, value: evt?.value, unit: evt?.unit, deviceId: evt?.deviceId, date: evt?.date]
        Integer curPass = (tierState?.cycle && tierState?.cycle?.toString()?.isNumber()) ? tierState?.cycle?.toInteger()+1 : 1
        if(curPass == 1) { updTsVal("lastTierRespStartDt"); remTsVal("lastTierRespStopDt"); }
        if(curPass <= tierMap?.size()) {
            schedNext = true
            tierState?.cycle = curPass
            tierState?.schedDelay = tierMap[curPass]?.delay ?: null
            tierState?.message = tierMap[curPass]?.message ?: null
            tierState?.volume = [:]
            if(tierMap[curPass]?.volume?.change) tierState?.volume?.change = tierMap[curPass]?.volume?.change ?: null
            if(tierMap[curPass]?.volume?.restore) tierState?.volume?.restore = tierMap[curPass]?.volume?.restore ?: null
            tierState?.evt = newEvt
            tierState?.lastMsg = (curPass+1 > tierMap?.size())
            log.trace("tierSize: (${tierMap?.size()}) | cycle: ${tierState?.cycle} | curPass: (${curPass}) | nextPass: ${curPass+1} | schedDelay: (${tierState?.schedDelay}) | Message: (${tierState?.message}) | LastMsg: (${tierState?.lastMsg})")
            atomicState?.actTierState = tierState
            tierSchedHandler([sched: schedNext, tierState: tierState])
        } else {
            logDebug("Tier Cycle has completed... Clearing TierState...")
            atomicState?.actTierState = [:]
            atomicState?.tierSchedActive = false
            updTsVal("lastTierRespStopDt")
        }
    }
}

private tierSchedHandler(data) {
    if(data && data?.tierState?.size() && data?.tierState?.message) {
        // log.debug "tierSchedHandler(${data})"
        Map evt = data?.tierState?.evt
        evt?.date = dateTimeFmt(new Date(), "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        executeAction(evt, false, "tierSchedHandler", false, false, [msg: data?.tierState?.message as String, volume: data?.tierState?.volume, isFirst: (data?.tierState?.cycle == 1), isLast: (data?.tierState?.lastMsg == true)])
        if(data?.sched) {
            if(data?.tierState?.schedDelay && data?.tierState?.lastMsg == false) {
                logDebug("Scheduling Next Tier Message for (${data?.tierState?.schedDelay} seconds)");
                runIn(data?.tierState?.schedDelay - 2, "tierEvtHandler"); //Subtracted 2 seconds from delay to offset processing delay
            } else {
                logDebug("Scheduling cleanup for (5 seconds) as this was the last message");
                runIn(5, "tierEvtHandler");
            }
            atomicState?.tierSchedActive = true
        }
    }
}

Map deviceEvtProcNumValue(evt, List devs = null, String cmd = null, Double dcl = null, Double dch = null, Double dce = null, Boolean dca = false, Boolean dcavg=false) {
    Boolean evtOk = false
    Boolean evtAd = false
    log.debug "deviceEvtProcNumValue | cmd: ${cmd} | low: ${dcl} | high: ${dch} | equal: ${dce} | all: ${dca}"
    if(devs?.size() && cmd && evt?.value?.toString()?.isNumber()) {
        Double evtValue = (dcavg ? getDevValueAvg(devs, evt?.name) : evt?.value) as Double
        log.debug "evtValue: ${evtValue}"
        switch(cmd) {
            case "equals":
                if(!dca && dce && dce?.toDouble() == evtValue) {
                    evtOk=true
                } else if(dca && dce && allDevCapNumValsEqual(devs, evt?.name, dce)) { evtOk=true; evtAd=true; }
                break
            case "between":
                if(!dca && dcl && dch && (evtValue in (dcl..dch))) {
                    evtOk=true
                } else if(dca && dcl && dch && allDevCapNumValsBetween(devs, evt?.name, dcl, dch)) { evtOk=true; evtAd=true; }
                break
            case "above":
                if(!dca && dch && (evtValue > dch)) {
                    evtOk=true
                } else if(dca && dch && allDevCapNumValsAbove(devs, evt?.name, dch)) { evtOk=true; evtAd=true; }
                break
            case "below":
                if(dcl && (evtValue < dcl)) {
                    evtOk=true
                } else if(dca && dcl && allDevCapNumValsBelow(devs, evt?.name, dcl)) { evtOk=true; evtAd=true; }
                break
        }
    }
    return [evtOk: evtOk, evtAd: evtAd]
}

def thermostatEvtHandler(evt) {
    def evtDelay = now() - evt?.date?.getTime()
    Boolean evtOk = false
    Boolean evtAd = false
    List d = settings?."trig_${evt?.name}"
    String dc = settings?."trig_${evt?.name}_cmd"
    Boolean dca = (settings?."trig_${evt?.name}_all" == true)
    Boolean dco = (!settings?."trig_${evt?.name}_after" && settings?."trig_${evt?.name}_once" == true)
    Integer dcw = (!settings?."trig_${evt?.name}_after" && settings?."trig_${evt?.name}_wait") ? settings?."trig_${evt?.name}_wait" : null
    logTrace( "Thermostat Event | ${evt?.name?.toUpperCase()} | Name: ${evt?.displayName} | Value: (${strCapitalize(evt?.value)}) with a delay of ${evtDelay}ms")
    Boolean devEvtWaitOk = ((dco || dcw) ? evtWaitRestrictionOk(evt, dco, dcw) : true)

    if(d?.size() && dc) {
        switch(dc) {
            case "setpoint":
                switch(evt?.name) {
                    case "coolSetpoint":
                    case "heatSetpoint":
                        if(dsc == "any" || (dsc == "cooling" && evt?.name == "coolSetpoint") || (dsc == "heating" && evt?.name == "heatSetpoint")) {
                            String dsc = settings?.trig_thermostat_setpoint_cmd ?: null
                            Double dcl = settings?.trig_thermostat_setpoint_low
                            Double dch = settings?.trig_thermostat_setpoint_high
                            Double dce = settings?.trig_thermostat_setpoint_equal
                            Map valChk = deviceEvtProcNumValue(evt, d, dsc, dcl, dch, dce, null)
                            evtOk = valChk?.evtOk
                            evtAd = valChk?.evtAd
                        }
                        break
                }
                break

            case "ambient":
                if(evt?.name == "temperature") {
                    String dac = settings?.trig_thermostat_ambient_cmd ?: null
                    Double dcl = settings?.trig_thermostat_ambient_low
                    Double dch = settings?.trig_thermostat_ambient_high
                    Double dce = settings?.trig_thermostat_ambient_equal
                    Map valChk = deviceEvtProcNumValue(evt, d, dac, dcl, dch, dce, null)
                    evtOk = valChk?.evtOk
                    evtAd = valChk?.evtAd
                }
                break

            case "mode":
            case "operatingstate":
            case "fanmode":
                if(evt?.name == "thermostatMode") {
                    String dmc = settings?.trig_thermostat_fanmode_cmd ?: null
                    if(dmc == "any" || evt?.value == dmc) { evtOk=true }
                }

                if(evt?.name == "thermostatOperatingState") {
                    String doc = settings?.trig_thermostat_state_cmd ?: null
                    if(doc == "any" || evt?.value == doc) { evtOk=true }
                }

                if(evt?.name == "thermostatFanMode") {
                    String dfc = settings?.trig_thermostat_mode_cmd ?: null
                    if(dfc == "any" || evt?.value == dfc) { evtOk=true }
                }
                break
        }
    }
    Boolean execOk = (evtOk && devEvtWaitOk)
    if(getConfStatusItem("tiers")) {
        processTierTrigEvt(evt, execOk)
    } else if (execOk) { executeAction(evt, false, "thermostatEvtHandler(${evt?.name})", false, evtAd) }
}

String evtValueCleanup(val) {
    // log.debug "val(in): ${val}"
    val = (val?.toString()?.isNumber() && val?.toString()?.endsWith(".0")) ? val?.toDouble()?.round(0) : val
    // log.debug "val(out): ${val}"
    return val
}

private clearEvtHistory() {
    settingUpdate("clrEvtHistory", "false", "bool")
    atomicState?.valEvtHistory = null
}

Boolean evtWaitRestrictionOk(evt, Boolean once, Integer wait) {
    Boolean ok = true
    Map evtHistMap = atomicState?.valEvtHistory ?: [:]
    def evtDt = parseDate(evt?.date?.toString())
    // log.debug "prevDt: ${evtHistMap["${evt?.name}"]?.date ? parseDate(evtHistMap["${evt?.name}"]?.dt as String) : null} | evtDt: ${evtDt}"
    if(evtHistMap?.containsKey("${evt?.name}") && evtHistMap["${evt?.name}"]?.dt) {
        // log.debug "prevDt: ${evtHistMap["${evt?.deviceId}_${evt?.name}"]?.dt as String}"
        def prevDt = parseDate(evtHistMap["${evt?.name}"]?.dt?.toString())
        if(prevDt && evtDt) {
            def dur = (int) ((long)(evtDt?.getTime() - prevDt?.getTime())/1000)
            def waitOk = ( (wait && dur) && (wait < dur));
            def dayOk = !once || (once && !isDateToday(prevDt))
            // log.debug("Last ${evt?.name?.toString()?.capitalize()} Event for Device Occurred: (${dur} sec ago) | Desired Wait: (${wait} sec) - Status: (${waitOk ? "${okSym()}" : "${notOkSym()}"}) | OnceDaily: (${once}) - Status: (${dayOk ? "${okSym()}" : "${notOkSym()}"})")
            ok = (waitOk && dayOk)
        }
    }
    if(ok) { evtHistMap["${evt?.name}"] = [dt: evt?.date?.toString(), value: evt?.value, name: evt?.name as String] }
    // log.debug "evtWaitRestrictionOk: $ok"
    atomicState?.valEvtHistory = evtHistMap
    return ok
}

String getAttrPostfix(attr) {
    switch(attr) {
        case "humidity":
        case "level":
        case "battery":
            return " percent"
        case "temperature":
            return " degrees"
        case "illuminance":
            return " lux"
        case "power":
            return " watts"
        default:
            return ""
    }
}

def scheduleAfterCheck(data) {
    Integer val = data?.val ? (data?.val < 2 ? 2 : data?.val-4) : 60
    String id = data?.id?.toString() ?: null
    Boolean rep = (data?.repeat == true)
    Map aSchedMap = atomicState?.afterEvtChkSchedMap ?: null
    if(aSchedMap && aSchedMap?.id?.toString() && id && aSchedMap?.id?.toString() == id) {
        // log.debug "Active Schedule Id (${aSchedMap?.id}) is the same as the requested schedule ${id}."
    }
    runIn(val, "afterEvtCheckHandler")
    atomicState?.afterEvtChkSchedMap = [id: id, dur: val, dt: getDtNow()]
    logDebug("Schedule After Event Check${rep ? " (Repeat)" : ""} for (${val} seconds) | Id: ${id}")
}

private clearAfterCheckSchedule() {
    unschedule("afterEvtCheckHandler")
    logDebug("Clearing After Event Check Schedule...")
    atomicState?.afterEvtChkSchedMap = null
    if(state?.afterEvtCheckWatcherSched) {
        state?.afterEvtCheckWatcherSched = false
        unschedule("afterEvtCheckWatcher")
    }
}

/***********************************************************************************************************
   CONDITIONS HANDLER
************************************************************************************************************/
Boolean reqAllCond() { return ( !multipleConditions() || (multipleConditions() && settings?.cond_require_all == true))}

Boolean timeCondOk() {
    def startTime = null
    def stopTime = null
    def now = new Date()
    def sun = getSunriseAndSunset() // current based on geofence, previously was: def sun = getSunriseAndSunset(zipCode: zipCode)
    if(settings?.cond_time_start_type && settings?.cond_time_stop_type) {
        if(settings?.cond_time_start_type == "sunset") { startTime = sun?.sunset }
        else if(settings?.cond_time_start_type == "sunrise") { startTime = sun?.sunrise }
        else if(settings?.cond_time_start_type == "time" && settings?.cond_time_start) { startTime = settings?.cond_time_start }

        if(settings?.cond_time_stop_type == "sunset") { stopTime = sun?.sunset }
        else if(settings?.cond_time_stop_type == "sunrise") { stopTime = sun?.sunrise }
        else if(settings?.cond_time_stop_type == "time" && settings?.cond_time_stop) { stopTime = settings?.cond_time_stop }
    } else { return null }
    if(startTime && stopTime) {
        if(!isST()) {
            startTime = toDateTime(startTime)
            stopTime = toDateTime(stopTime)
        }
        Boolean isBtwn = timeOfDayIsBetween(startTime, stopTime, now, location?.timeZone)
        log.debug "TimeCheck | CurTime: (${now}) is between ($startTime and $stopTime) | ${isBtwn}"
        return isBtwn
    } else { return null }
}

Boolean dateCondOk() {
    if(settings?.cond_days == null && settings?.cond_months == null) return null
    Boolean dOk = settings?.cond_days ? (isDayOfWeek(settings?.cond_days)) : true
    Boolean mOk = settings?.cond_months ? (isMonthOfYear(settings?.cond_months)) : true
    logDebug("dateConditions | monthOk: $mOk | daysOk: $dOk")
    return reqAllCond() ? (mOk && dOk) : (mOk || dOk)
}

Boolean locationCondOk() {
    if(settings?.cond_mode == null && settings?.cond_mode_cmd == null && settings?.cond_alarm == null) return null
    Boolean mOk = (settings?.cond_mode && settings?.cond_mode_cmd) ? (isInMode(settings?.cond_mode, (settings?.cond_mode_cmd == "not"))) : true
    Boolean aOk = settings?.cond_alarm ? isInAlarmMode(settings?.cond_alarm) : true
    logDebug("locationConditions | modeOk: $mOk | alarmOk: $aOk")
    return reqAllCond() ? (mOk && aOk) : (mOk || aOk)
}

Boolean checkDeviceCondOk(type) {
    List devs = settings?."cond_${type}" ?: null
    String cmdVal = settings?."cond_${type}_cmd" ?: null
    Boolean all = (settings?."cond_${type}_all" == true)
    if( !(type && devs && cmdVal) ) { return true }
    return all ? allDevCapValsEqual(devs, type, cmdVal) : anyDevCapValsEqual(devs, type, cmdVal)
}

Boolean checkDeviceNumCondOk(type) {
    List devs = settings?."cond_${type}" ?: null
    String cmd = settings?."cond_${type}_cmd" ?: null
    Double dcl = settings?."cond_${type}_low" ?: null
    Double dch = settings?."cond_${type}_high" ?: null
    Double dce = settings?."cond_${type}_equal" ?: null
    Boolean dca = (settings?."cond_${type}_all" == true) ?: false
    if( !(type && devs && cmd) ) { return true }

    switch(cmd) {
        case "equals":
            if(dce) {
                if(dca) { return allDevCapNumValsEqual(devs, type, dce) }
                else { return anyDevCapNumValEqual(devs, type, dce) }
            }
            return true
            break
        case "between":
            if(dcl && dch) {
                if(dca) { return allDevCapNumValsBetween(devs, type, dcl, dch) }
                else { return anyDevCapNumValBetween(devs, type, dcl, dch) }
            }
            return true
            break
        case "above":
            if(dch) {
                if(dca) { return allDevCapNumValsAbove(devs, type, dch) }
                else { return anyDevCapNumValAbove(devs, type, dch) }
            }
            return true
            break
        case "below":
            if(dcl) {
                if(dca) { return allDevCapNumValsBelow(devs, type, dcl) }
                else { return anyDevCapNumValBelow(devs, type, dcl) }
            }
            return true
            break
    }
}

Boolean deviceCondOk() {
    List skipped = []
    List passed = []
    List failed = []
    ["switch", "motion", "presence", "contact", "acceleration", "lock", "door", "shade", "valve"]?.each { i->
        if(!settings?."cond_${i}") { skipped?.push(i); return; }
        checkDeviceCondOk(i) ? passed?.push(i) : failed?.push(i);
    }
    ["temperature", "humidity", "illuminance", "level", "power", "battery"]?.each { i->
        if(!settings?."cond_${i}") { skipped?.push(i); return; }
        checkDeviceNumCondOk(i) ? passed?.push(i) : failed?.push(i);
    }
    logDebug("DeviceCondOk | Found: (${(passed?.size() + failed?.size())}) | Skipped: $skipped | Passed: $passed | Failed: $failed")
    Integer cndSize = (passed?.size() + failed?.size())
    if(cndSize == 0) return null
    return reqAllCond() ? (cndSize == passed?.size()) : (cndSize > 0 && passed?.size() >= 1)
}

def conditionStatus() {
    Boolean reqAll = reqAllCond()
    List failed = []
    List passed = []
    List skipped = []
    ["time", "date", "location", "device"]?.each { i->
        def s = "${i}CondOk"()
        if(s == null) { skipped?.push(i); return; }
        s ? passed?.push(i) : failed?.push(i);
    }
    Integer cndSize = (passed?.size() + failed?.size())
    logDebug("ConditionsStatus | RequireAll: ${reqAll} | Found: (${cndSize}) | Skipped: $skipped | Passed: $passed | Failed: $failed")
    Boolean ok = reqAll ? (cndSize == passed?.size()) : (cndSize > 0 && passed?.size() >= 1)
    if(cndSize == 0) ok = true;
    return [ok: ok, passed: passed, blocks: failed]
}

Boolean devCondConfigured(type) {
    return (settings?."cond_${type}" && settings?."cond_${type}_cmd")
}

Boolean devNumCondConfigured(type) {
    return (settings?."cond_${type}_cmd" && (settings?."cond_${type}_low" || settings?."cond_${type}_low" || settings?."trig_${type}_equal"))
}

Boolean timeCondConfigured() {
    Boolean startTime = (settings?.cond_time_start_type in ["sunrise", "sunset"] || (settings?.cond_time_start_type == "time" && settings?.cond_time_start))
    Boolean stopTime = (settings?.cond_time_stop_type in ["sunrise", "sunset"] || (settings?.cond_time_stop_type == "time" && settings?.cond_time_stop))
    return (startTime && stopTime)
}

Boolean dateCondConfigured() {
    Boolean days = (settings?.cond_days)
    Boolean months = (settings?.cond_months)
    return (days || months)
}

Boolean locationModeConfigured() {
    if(settings?.cond_mode && !settings?.cond_mode_cmd) { settingUpdate("cond_mode_cmd", "are", "enum") }
    return (settings?.cond_mode && settings?.cond_mode_cmd)
}

Boolean locationAlarmConfigured() {
    return (settings?.cond_alarm)
}

Boolean deviceCondConfigured() {
    List devConds = ["switch", "motion", "presence", "contact", "acceleration", "lock", "door", "shade", "valve", "temperature", "humidity", "illuminance", "level", "power", "battery"]
    List items = []
    devConds?.each { dc-> if(devCondConfigured(dc)) { items?.push(dc) } }
    return (items?.size() > 0)
}

Integer deviceCondCount() {
    List devConds = ["switch", "motion", "presence", "contact", "acceleration", "lock", "door", "shade", "valve", "temperature", "humidity", "illuminance", "level", "power", "battery"]
    List items = []
    devConds?.each { dc-> if(devCondConfigured(dc)) { items?.push(dc) } }
    return items?.size() ?: 0
}

Boolean conditionsConfigured() {
    return (timeCondConfigured() || dateCondConfigured() || locationModeConfigured() || locationAlarmConfigured() || deviceCondConfigured())
}

Boolean multipleConditions() {
    Integer cnt = 0
    if(timeCondConfigured()) cnt++
    if(dateCondConfigured()) cnt++
    if(locationModeConfigured()) cnt++
    if(locationAlarmConfigured()) cnt++
    cnt = cnt + deviceCondCount()
    return (cnt>1)
}

/***********************************************************************************************************
    ACTION EXECUTION
************************************************************************************************************/

private executeActTest() {
    settingUpdate("actTestRun", "false", "bool")
    Map evt = [name: "contact", displayName: "some test device", value: "open", date: new Date()]
    if(getConfStatusItem("tiers")) {
        processTierTrigEvt(null, true)
    } else {
        if(settings?.actionType in ["speak", "announcement"]) {
            evt = getRandomTrigEvt()
        }
        executeAction(evt, true, "executeActTest", false, false)
    }
}

Map getRandomTrigEvt() {
    List noDevTrigs = ["mode", "routine", "schedule", "scene", "hsmStatus", "alarmSystemStatus", "alarm"]
    Boolean useDev = (!(trig in noDevOpts))
    Map evt = [:]
    String trig = getRandomItem(settings?.triggerEvents?.collect { it as String })
    List trigItems = settings?."trig_${trig}" ?: null
    def randItem = trigItems?.size() ? getRandomItem(trigItems) : null
    def trigItem = randItem ? (randItem instanceof String ? [displayName: null, id: null] : (trigItems?.size() ? trigItems?.find { it?.id?.toString() == randItem?.id?.toString() } : [displayName: null, id: null])) : null
    // logDebug "trig: ${trig} | trigItem: ${trigItem} | ${trigItem?.displayName} | ${trigItem?.id} | Evt: ${evt}"
    Map attVal = [
        "switch": getRandomItem(["on", "off"]),
        door: getRandomItem(["open", "closed", "opening", "closing"]),
        contact: getRandomItem(["open", "closed"]),
        acceleration: getRandomItem(["active", "inactive"]),
        lock: getRandomItem(["locked", "unlocked"]),
        water: getRandomItem(["wet", "dry"]),
        presence: getRandomItem(["present", "not present"]),
        motion: getRandomItem(["active", "inactive"]),
        valve: getRandomItem(["open", "closed"]),
        shade: getRandomItem(["open", "closed"]),
        button: getRandomItem(["pushed", "held"]),
        pushed: getRandomItem(["pushed"]),
        released: getRandomItem(["released"]),
        held: getRandomItem(["held"]),
        doubleTapped: getRandomItem(["doubleTapped"]),
        smoke: getRandomItem(["detected", "clear"]),
        carbonMonoxide: getRandomItem(["detected", "clear"]),
        temperature: getRandomItem(30..80),
        illuminance: getRandomItem(1..100),
        humidity: getRandomItem(1..100),
        battery: getRandomItem(1..100),
        power: getRandomItem(100..3000),
        thermostat: getRandomItem(["cooling is "]),
        mode: getRandomItem(location?.modes),
        alarm: getRandomItem(getAlarmTrigOpts()?.collect {it?.value as String}),
        guard: getRandomItem(["ARMED_AWAY", "ARMED_STAY"]),
        routineExecuted: isST() ? getRandomItem(getLocationRoutines()) : null
    ]
    if(attVal?.containsKey(trig)) { evt = [name: trig, displayName: trigItem?.displayName ?: "", value: attVal[trig], date: new Date(), deviceId: trigItem?.id?.toString() ?: null] }
    // log.debug "getRandomTrigEvt | trig: ${trig} | Evt: ${evt}"
    return evt
}

String convEvtType(type) {
    Map typeConv = [
        "routineExecuted": "Routine",
        "alarmSystemStatus": "Alarm system",
        "hsmStatus": "Alarm system"
    ]
    return (type && typeConv?.containsKey(type)) ? typeConv[type] : type
}

String decodeVariables(evt, str) {
    if(evt && str) {
        // log.debug "str: ${str} | vars: ${(str =~ /%[a-z]+%/)}"
        if(str?.contains("%type%") && str?.contains("%name%")) {
            str = (str?.contains("%type%") && evt?.name) ? str?.replaceAll("%type%", !evt?.displayName?.toLowerCase()?.contains(evt?.name) ? convEvtType(evt?.name) : "") : str
            str = (str?.contains("%name%")) ? str?.replaceAll("%name%", evt?.displayName) : str
        } else {
            str = (str?.contains("%type%") && evt?.name) ? str?.replaceAll("%type%", convEvtType(evt?.name)) : str
            str = (str?.contains("%name%")) ? str?.replaceAll("%name%", evt?.displayName) : str
        }
        str = (str?.contains("%unit%") && evt?.name) ? str?.replaceAll("%unit%", getAttrPostfix(evt?.name)) : str
        str = (str?.contains("%value%") && evt?.value) ? str?.replaceAll("%value%", evt?.value?.toString()?.isNumber() ? evtValueCleanup(evt?.value) : evt?.value) : str
        str = (str?.contains("%duration%") && evt?.totalDur) ? str?.replaceAll("%duration%", "${evt?.totalDur} second${evt?.totalDur > 1 ? "s" : ""} ago") : str
        str = (str?.contains("%duration_min%") && evt?.totalDur) ? str?.replaceAll("%duration_min%", "${durationToMinutes(evt?.totalDur)} minute${durationToMinutes(evt?.totalDur) > 1 ? "s" : ""} ago") : str
        str = (str?.contains("%durationmin%") && evt?.totalDur) ? str?.replaceAll("%durationmin%", "${durationToMinutes(evt?.totalDur)} minute${durationToMinutes(evt?.totalDur) > 1 ? "s" : ""} ago") : str
        str = (str?.contains("%durationval%") && evt?.totalDur) ? str?.replaceAll("%durationval%", "${evt?.totalDur} second${evt?.totalDur > 1 ? "s" : ""}") : str
        str = (str?.contains("%durationvalmin%") && evt?.totalDur) ? str?.replaceAll("%durationvalmin%", "${durationToMinutes(evt?.totalDur)} minute${durationToMinutes(evt?.totalDur) > 1 ? "s" : ""}") : str
    }
    str = (str?.contains("%date%")) ? str?.replaceAll("%date%", convToDate(evt?.date ?: new Date())) : str
    str = (str?.contains("%time%")) ? str?.replaceAll("%time%", convToTime(evt?.date ?: new Date())) : str
    str = (str?.contains("%datetime%")) ? str?.replaceAll("%datetime%", convToDateTime(evt?.date ?: new Date())) : str
    return str
}

def durationToMinutes(dur) {
    if(dur && dur>=60) return (dur/60)?.toInteger()
    return dur?.toInteger()
}

def durationToHours(dur) {
    if(dur && dur>= (60*60)) return (dur / 60 / 60)?.toInteger()
    return dur?.toInteger()
}

String getResponseItem(evt, tierMsg=null, evtAd=false, isRepeat=false, testMode=false) {
    // log.debug "getResponseItem | EvtName: ${evt?.name} | EvtDisplayName: ${evt?.displayName} | EvtValue: ${evt?.value} | AllDevsResp: ${evtAd} | Repeat: ${isRepeat} | TestMode: ${testMode}"
    String glbText = settings?."act_${settings?.actionType}_txt" ?: null
    if(glbText) {
        List eTxtItems = glbText ? glbText?.toString()?.tokenize(";") : []
        return decodeVariables(evt, getRandomItem(eTxtItems))
    } else if(tierMsg) {
        List eTxtItems = tierMsg ? tierMsg?.toString()?.tokenize(";") : []
        return decodeVariables(evt, getRandomItem(eTxtItems))
    } else {
        List devs = settings?."trig_${evt?.name}" ?: []
        String dc = settings?."trig_${evt?.name}_cmd"
        Boolean dca = (settings?."trig_${evt?.name}_all" == true)
        String dct = settings?."trig_${evt?.name}_txt" ?: null
        String dcart = settings?."trig_${evt?.name}_after_repeat" && settings?."trig_${evt?.name}_after_repeat_txt" ? settings?."trig_${evt?.name}_after_repeat_txt" : null
        List eTxtItems = dct ? dct?.toString()?.tokenize(";") : []
        List rTxtItems = dcart ? dcart?.toString()?.tokenize(";") : []
        List testItems = eTxtItems + rTxtItems
        if(testMode && testItems?.size()) {
            return  decodeVariables(evt, getRandomItem(testItems))
        } else if(!testMode && isRepeat && rTxtItems?.size()) {
            return  decodeVariables(evt, getRandomItem(rTxtItems))
        } else if(!testMode && eTxtItems?.size()) {
            return  decodeVariables(evt, getRandomItem(eTxtItems))
        } else {
            String postfix = getAttrPostfix(evt?.name) ?: ""
            switch(evt?.name) {
                case "thermostatMode":
                case "thermostatFanMode":
                case "thermostatOperatingState":
                case "coolSetpoint":
                case "heatSetpoint":
                    return "${evt?.displayName}${!evt?.displayName?.toLowerCase()?.contains(evt?.name) ? " ${evt?.name}" : ""} ${evt?.name} is ${evt?.value} ${postfix}"
                case "mode":
                    return  "The location mode is now set to ${evt?.value}"
                case "routineExecuted":
                    return  "The ${evt?.displayName} routine was just executed!."
                case "scene":
                    return  "The ${evt?.value} scene was just executed!."
                case "alarm":
                case "hsmStatus":
                case "alarmSystemStatus":
                    return "The ${getAlarmSystemName()} is now set to ${evt?.value}"
                case "guard":
                    return "Alexa Guard is now set to ${evt?.value}"
                case "hsmAlert":
                    return "A ${getAlarmSystemName()} ${evt?.displayName} alert with ${evt?.value} has occurred."
                case "sunriseTime":
                case "sunsetTime":
                    return "The ${getAlarmSystemName()} is now set to ${evt?.value}"
                case "schedule":
                    return "Your scheduled event has occurred at ${evt?.value}"
                case "smoke":
                case "carbonMonoxide":
                    return "${evt?.name} is ${evt?.value} on ${evt?.displayName}!"
                case "pushed":
                case "held":
                case "released":
                case "doubleTapped":
                    return "Button ${getRandomItem(1..8)} was ${evt?.name == "doubleTapped" ? "double tapped" : evt?.name} on ${evt?.displayName}"
                default:
                    if(evtAd && devs?.size()>1) {
                        return "All ${devs?.size()}${!evt?.displayName?.toLowerCase()?.contains(evt?.name) ? " ${evt?.name}" : ""} devices are ${evt?.value}"
                    } else {
                        return "${evt?.displayName}${!evt?.displayName?.toLowerCase()?.contains(evt?.name) ? " ${evt?.name}" : ""} is ${evt?.value} ${postfix}"
                    }
                    break
            }
        }
    }
    return "Invalid Text Received... Please verify Action configuration..."
}

public getActionHistory(asObj=false) {
    List eHist = atomicState?.actionHistory ?: []
    Boolean isST = isST()
    List output = []
    if(eHist?.size()) {
        eHist?.each { h->
            String str = ""
            str += "Trigger: [${h?.evtName}]"
            str += "\nDevice: [${h?.evtDevice}]"
            str += "\nCondition Status: ${h?.active ? "Passed" : "Failed"}"
            str += "\nConditions Passed: ${h?.passed}"
            str += "\nConditions Blocks: ${h?.blocks}"
            str += h?.test ? "\nTest Mode: true" : ""
            str += h?.isTier ? "\nTest Cmd: true" : ""
            str += h?.isRepeat ? "\nRepeat: true" : ""
            str += "\nSource: ${h?.src}"
            str += "\nDateTime: ${h?.dt}"
            output?.push(str)
        }
    } else { output?.push("No History Items Found...") }
    if(!asObj) {
        output?.each { i-> paragraph pTS(i) }
    } else {
        return output
    }
}

private addToActHistory(evt, data, Integer max=10) {
    Boolean ssOk = (stateSizePerc() <= 70)
    List eData = atomicState?.actionHistory ?: []
    eData?.push([
        dt: getDtNow(),
        active: (data?.status?.ok == true),
        evtName: evt?.name,
        evtDevice: evt?.displayName,
        blocks: data?.status?.blocks,
        passed: data?.status?.passed,
        test: data?.test,
        isTierCmd: data?.isTier,
        isRepeat: data?.isRepeat
    ])
    if(!ssOk || eData?.size() > max) {
        // if(!ssOk) log.warn "stateOk: ${ssOk}"
        // if(eData?.size() > max) log.warn "Action History (${eData?.size()}) has more than ${max} items... | Need to drop: (${(eData?.size()-max)})"
        eData = eData?.drop( (eData?.size()-max)+1 )
    }
    // log.debug "actionHistory Size: ${eData?.size()}"
    atomicState?.actionHistory = eData
}

private executeAction(evt = null, testMode=false, src=null, allDevsResp=false, isRptAct=false, tierData=null) {
    def startTime = now()
    logTrace( "executeAction${src ? "($src)" : ""}${testMode ? " | [TestMode]" : ""}${allDevsResp ? " | [AllDevsResp]" : ""}${isRptAct ? " | [RepeatEvt]" : ""}")
    if(isPaused()) { logWarn("Action is PAUSED... Skipping Action Execution...", true); return; }
    Map condStatus = conditionStatus()
    // log.debug "condStatus: ${condStatus}"
    Boolean actOk = getConfStatusItem("actions")
    addToActHistory(evt, [status: condStatus, test: testMode, src: src, isRepeat: isRptAct, isTier: (tierData != null)] )
    Boolean isST = isST()
    Map actMap = state?.actionExecMap ?: null
    List actDevices = settings?.act_EchoDevices ? parent?.getDevicesFromList(settings?.act_EchoDevices) : []
    Map activeZones = settings?.act_EchoZones ? getActiveZones() : [:]
    // log.debug "activeZones: $activeZones"
    String actMsgTxt = null
    String actType = settings?.actionType
    Boolean firstTierMsg = (tierData && tierData?.isFirst == true)
    Boolean lastTierMsg = (tierData && tierData?.isLast == true)
    if(actOk && actType) {
        def alexaMsgDev = actDevices?.size() && settings?.notif_alexa_mobile ? actDevices[0] : null
        if(condStatus?.ok != true) { logWarn("executeAction | Skipping execution because ${condStatus?.blocks} conditions have not been met", true); return; }
        if(!actMap || !actMap?.size()) { logError("executeAction Error | The ActionExecutionMap is not found or is empty", true); return; }
        if(settings?.act_EchoZones && !activeZones?.size() && !actDevices?.size()) { logWarn("executeAction | No Active Zones Available and No Alternate Echo Devices Selected.", true); return; }
        if(!actDevices?.size() && !settings?.act_EchoZones) { logError("executeAction Error | Echo Device List not found or is empty", true); return; }
        if(!actMap?.actionType) { logError("executeAction Error | The ActionType is missing or is empty", true); return; }
        Map actConf = actMap?.config
        Integer actDelay = actMap?.delay ?: 0
        Integer actDelayMs = actMap?.delay ? (actMap?.delay*1000) : 0
        Integer changeVol = actMap?.config?.volume?.change as Integer ?: null
        Integer restoreVol = actMap?.config?.volume?.restore as Integer ?: null
        Integer alarmVol = actMap?.config?.volume?.alarm ?: null
        switch(actType) {
            case "speak":
            case "speak_tiered":
            case "announcement":
            case "announcement_tiered":
                if(actConf[actType]) {
                    if(tierData?.volume && tierData?.volume?.change || tierData?.volume?.restore) {
                        if(tierData?.volume?.change) changeVol = tierData?.volume?.change
                        if(tierData?.volume?.restore) restoreVol = tierData?.volume?.restore
                    }
                    String txt = getResponseItem(evt, tierData?.msg, allDevsResp, isRptAct, testMode) ?: null
                    // log.debug "txt: $txt"
                    if(!txt) { txt = "Invalid Text Received... Please verify Action configuration..." }
                    actMsgTxt = txt
                    if(activeZones?.size()) {
                        sendLocationEvent(name: "es3ZoneCmd", value: actType?.replaceAll("_tiered", ""), data:[ zones: activeZones?.collect { it?.key as String }, cmd: actType?.replaceAll("_tiered", ""), title: getActionName(), message: txt, changeVol: changeVol, restoreVol: restoreVol, delay: actDelayMs], isStateChange: true, display: false, displayed: false)
                        logDebug("Sending Speech Text: (${txt}) to Zones (${activeZones?.collect { it?.value?.name }})${changeVol ? " | Volume: ${changeVol}" : ""}${restoreVol ? " | Restore Volume: ${restoreVol}" : ""}${actDelay ? " | Delay: (${actDelay})" : ""}")
                    } else {
                        if(actType in ["speak", "speak_tiered"]) {
                            //Speak Command Logic
                            if(actDevices?.size()) {
                                if(changeVol || restoreVol) {
                                    if(isST && actDelayMs) {
                                        actDevices?.each { dev-> dev?.setVolumeSpeakAndRestore(changeVol, txt, restoreVol, [delay: actDelayMs]) }
                                    } else { actDevices?.each { dev-> dev?.setVolumeSpeakAndRestore(changeVol, txt, restoreVol) } }
                                } else {
                                    if(isST && actDelayMs) {
                                        actDevices?.each { dev-> dev?.speak(txt, [delay: actDelayMs]) }
                                    } else { actDevices?.each { dev-> dev?.speak(txt) } }
                                }
                                logDebug("Sending Speech Text: (${txt}) to ${actDevices}${changeVol ? " | Volume: ${changeVol}" : ""}${restoreVol ? " | Restore Volume: ${restoreVol}" : ""}")
                            }
                        } else if (actType in ["announcement", "announcement_tiered"]) {
                            //Announcement Command Logic
                            if(actDevices?.size() > 1 && actConf[actType]?.deviceObjs && actConf[actType]?.deviceObjs?.size()) {
                                //NOTE: Only sends command to first device in the list | We send the list of devices to announce one and then Amazon does all the processing
                                def devJson = new groovy.json.JsonOutput().toJson(actConf[actType]?.deviceObjs)
                                if(isST && actDelayMs) {
                                    actDevices[0]?.sendAnnouncementToDevices(txt, (getActionName() ?: "Echo Speaks Action"), actConf[actType]?.deviceObjs, changeVol, restoreVol, [delay: actDelayMs])
                                } else { actDevices[0]?.sendAnnouncementToDevices(txt, (getActionName() ?: "Echo Speaks Action"), actConf[actType]?.deviceObjs, changeVol, restoreVol) }
                            } else {
                                actDevices?.each { dev->
                                    if(isST && actDelayMs) {
                                        dev?.playAnnouncement(txt, (getActionName() ?: "Echo Speaks Action"), changeVol, restoreVol, [delay: actDelayMs])
                                    } else { dev?.playAnnouncement(txt, (getActionName() ?: "Echo Speaks Action"), changeVol, restoreVol) }
                                }
                            }
                            logDebug("Sending Announcement Command: (${txt}) to ${actDevices}${actDelay ? " | Delay: (${actDelay})" : ""}${changeVol ? " | Volume: ${changeVol}" : ""}${restoreVol ? " | Restore Volume: ${restoreVol}" : ""}")
                        }
                    }
                }
                break

            case "sequence":
                if(actConf[actType] && actConf[actType]?.text) {
                    if(activeZones?.size()) {
                        sendLocationEvent(name: "es3ZoneCmd", value: actType, data:[ zones: activeZones?.collect { it?.key as String }, cmd: actType, message: actConf[actType]?.text, delay: actDelayMs], isStateChange: true, display: false, displayed: false)
                        logDebug("Sending Sequence Command: (${txt}) to Zones (${activeZones?.collect { it?.value?.name }})${actDelay ? " | Delay: (${actDelay})" : ""}")
                    } else if(actDevices?.size()) {
                        actDevices?.each { dev->
                            if(isST && actDelayMs) {
                                dev?.executeSequenceCommand(actConf[actType]?.text as String, [delay: actDelayMs])
                            } else { dev?.executeSequenceCommand(actConf[actType]?.text as String) }
                        }
                        logDebug("Sending Sequence Command to Zones: (${actConf[actType]?.text}) to ${actDevices}${actDelay ? " | Delay: (${actDelay})" : ""}")
                    }
                }
                break

            case "playback":
            case "dnd":
                if(actConf[actType] && actConf[actType]?.cmd) {
                    if(actType == "playback" && activeZones?.size()) {
                        sendLocationEvent(name: "es3ZoneCmd", value: actType, data:[zones: activeZones?.collect { it?.key as String }, cmd: actConf[actType]?.cmd, changeVol: changeVol, restoreVol: restoreVol, delay: actDelayMs], isStateChange: true, display: false, displayed: false)
                        logDebug("Sending Sequence Command: (${txt}) to Zones (${activeZones?.collect { it?.value?.name }})${actDelay ? " | Delay: (${actDelay})" : ""}")
                    } else if(actDevices?.size()) {
                        actDevices?.each { dev->
                            if(isST && actDelayMs) {
                                if(actConf[actType]?.cmd != "volume") { dev?."${actConf[actType]?.cmd}"([delay: actDelayMs]) }
                                else if(actConf[actType]?.cmd == "volume") { dev?.setVolume(changeVol, [delay: actDelayMs]) }
                                if(changeVol) { dev?.volume(changeVol, [delay: actDelayMs]) }
                            } else {
                                if(actConf[actType]?.cmd != "volume") { dev?."${actConf[actType]?.cmd}"() }
                                else if(actConf[actType]?.cmd == "volume") { dev?.setVolume(changeVol) }
                                if(changeVol) { dev?.volume(changeVol) }
                            }
                        }
                        logDebug("Sending ${actType?.toString()?.capitalize()} Command: (${actConf[actType]?.cmd}) to ${actDevices}${actDelay ? " | Delay: (${actDelay})" : ""}${changeVol ? " | Volume: ${changeVol}" : ""}")
                    }
                }
                break

            case "builtin":
            case "calendar":
            case "weather":
                if(actConf[actType] && actConf[actType]?.cmd) {
                    if(activeZones?.size()) {
                        sendLocationEvent(name: "es3ZoneCmd", value: actType, data:[ zones: activeZones?.collect { it?.key as String }, cmd: actConf[actType]?.cmd, message: actConf[actType]?.text, changeVol: changeVol, restoreVol: restoreVol, delay: actDelayMs], isStateChange: true, display: false, displayed: false)
                        logDebug("Sending ${actType?.toString()?.capitalize()} Command: (${actConf[actType]?.cmd}) to Zones (${activeZones?.collect { it?.value?.name }})${actDelay ? " | Delay: (${actDelay})" : ""}${changeVol ? " | Volume: ${changeVol}" : ""}${restoreVol ? " | Restore Volume: ${restoreVol}" : ""}")
                    } else if(actDevices?.size()) {
                        actDevices?.each { dev->
                            if(isST && actDelayMs) {
                                dev?."${actConf[actType]?.cmd}"(changeVol, restoreVol, [delay: actDelayMs])
                            } else { dev?."${actConf[actType]?.cmd}"(changeVol, restoreVol) }
                        }
                        logDebug("Sending ${actType?.toString()?.capitalize()} Command: (${actConf[actType]?.cmd}) to ${actDevices}${actDelay ? " | Delay: (${actDelay})" : ""}${changeVol ? " | Volume: ${changeVol}" : ""}${restoreVol ? " | Restore Volume: ${restoreVol}" : ""}")
                    }
                }
                break

            case "sounds":
                if(actConf[actType] && actConf[actType]?.cmd && actConf[actType]?.name) {
                    if(activeZones?.size()) {
                        sendLocationEvent(name: "es3ZoneCmd", value: actType, data:[ zones: activeZones?.collect { it?.key as String }, cmd: actConf[actType]?.cmd, message: actConf[actType]?.name, changeVol: changeVol, restoreVol: restoreVol, delay: actDelayMs], isStateChange: true, display: false, displayed: false)
                        logDebug("Sending ${actType?.toString()?.capitalize()} Command: (${actConf[actType]?.cmd} | Name: ${actConf[actType]?.name}) to Zones (${activeZones?.collect { it?.value?.name }})${actDelay ? " | Delay: (${actDelay})" : ""}${changeVol ? " | Volume: ${changeVol}" : ""}${restoreVol ? " | Restore Volume: ${restoreVol}" : ""}")
                    } else if(actDevices?.size()) {
                        actDevices?.each { dev->
                            if(isST && actDelayMs) {
                                dev?."${actConf[actType]?.cmd}"(actConf[actType]?.name ,onchangeVol, restoreVol, [delay: actDelayMs])
                            } else { dev?."${actConf[actType]?.cmd}"(actConf[actType]?.name, changeVol, restoreVol) }
                        }
                        logDebug("Sending ${actType?.toString()?.capitalize()} Command: (${actConf[actType]?.cmd} | Name: ${actConf[actType]?.name}) to ${actDevices}${actDelay ? " | Delay: (${actDelay})" : ""}${changeVol ? " | Volume: ${changeVol}" : ""}${restoreVol ? " | Restore Volume: ${restoreVol}" : ""}")
                    }
                }
                break

            case "alarm":
            case "reminder":
                if(actConf[actType] && actConf[actType]?.cmd && actConf[actType]?.label && actConf[actType]?.date && actConf[actType]?.time) {
                    actDevices?.each { dev->
                        if(isST && actDelayMs) {
                            dev?."${actConf[actType]?.cmd}"(actConf[actType]?.label, actConf[actType]?.date, actConf[actType]?.time, [delay: actDelayMs])
                        } else {
                            dev?."${actConf[actType]?.cmd}"(actConf[actType]?.label, actConf[actType]?.date, actConf[actType]?.time)
                        }
                        // if(isST && actDelayMs) {
                        //     dev?."${actConf[actType]?.cmd}"(actConf[actType]?.label, actConf[actType]?.date, actConf[actType]?.time, actConf[actType]?.recur?.type, actConf[actType]?.recur?.opt, [delay: actDelayMs])
                        // } else {
                        //     dev?."${actConf[actType]?.cmd}"(actConf[actType]?.label, actConf[actType]?.date, actConf[actType]?.time, actConf[actType]?.recur?.type, actConf[actType]?.recur?.opt)
                        // }
                    }
                    logDebug("Sending ${actType?.toString()?.capitalize()} Command: (${actConf[actType]?.cmd}) to ${actDevices} | Label: ${actConf[actType]?.label} | Date: ${actConf[actType]?.date} | Time: ${actConf[actType]?.time}")
                }
                break

            case "music":
                if(actConf[actType] && actConf[actType]?.cmd && actConf[actType]?.provider && actConf[actType]?.search) {
                    log.debug "musicProvider: ${actConf[actType]?.provider} | ${convMusicProvider(actConf[actType]?.provider)}"
                    if(activeZones?.size()) {
                        sendLocationEvent(name: "es3ZoneCmd", value: actType, data:[ zones: activeZones?.collect { it?.key as String }, cmd: actType, search: actConf[actType]?.search, provider: convMusicProvider(actConf[actType]?.provider), changeVol: changeVol, restoreVol: restoreVol, delay: actDelayMs], isStateChange: true, display: true, displayed: true)
                        logDebug("Sending ${actType?.toString()?.capitalize()} Command: (${txt}) to Zones (${activeZones?.collect { it?.value?.name }} | Provider: ${actConf[actType]?.provider} | Search: ${actConf[actType]?.search} | Command: (${actConf[actType]?.cmd}) to ${actDevices}${actDelay ? " | Delay: (${actDelay})" : ""}${changeVol ? " | Volume: ${changeVol}" : ""}${restoreVol ? " | Restore Volume: ${restoreVol}" : ""}")
                    } else if(actDevices?.size()) {
                        actDevices?.each { dev->
                            if(isST && actDelayMs) {
                                dev?."${actConf[actType]?.cmd}"(actConf[actType]?.search, convMusicProvider(actConf[actType]?.provider), changeVol, restoreVol, [delay: actDelayMs])
                            } else { dev?."${actConf[actType]?.cmd}"(actConf[actType]?.search, convMusicProvider(actConf[actType]?.provider), changeVol, restoreVol) }
                        }
                        logDebug("Sending ${actType?.toString()?.capitalize()} | Provider: ${actConf[actType]?.provider} | Search: ${actConf[actType]?.search} | Command: (${actConf[actType]?.cmd}) to ${actDevices}${actDelay ? " | Delay: (${actDelay})" : ""}${changeVol ? " | Volume: ${changeVol}" : ""}${restoreVol ? " | Restore Volume: ${restoreVol}" : ""}")
                    }
                }
                break

            case "alexaroutine":
                if(actConf[actType] && actConf[actType]?.cmd && actConf[actType]?.routineId) {
                    actDevices[0]?."${actConf[actType]?.cmd}"(actConf[actType]?.routineId as String)
                    logDebug("Sending ${actType?.toString()?.capitalize()} Command: (${actConf[actType]?.cmd}) | RoutineId: ${actConf[actType]?.routineId} to ${actDevices}${actDelay ? " | Delay: (${actDelay})" : ""}")
                }
                break

            case "wakeword":
                if(actConf[actType] && actConf[actType]?.devices && actConf[actType]?.devices?.size()) {
                    actConf[actType]?.devices?.each { d->
                        def aDev = actDevices?.find { it?.id?.toString() == d?.device?.toString() }
                        if(aDev) {
                            if(isST && actDelayMs) {
                                aDev?."${d?.cmd}"(d?.wakeword, [delay: actDelayMs])
                            } else { aDev?."${d?.cmd}"(d?.wakeword) }
                            logDebug("Sending WakeWord: (${d?.wakeword}) | Command: (${d?.cmd}) to ${aDev}${actDelay ? " | Delay: (${actDelay})" : ""}")
                        }
                    }
                }
                break

            case "bluetooth":
                if(actConf[actType] && actConf[actType]?.devices && actConf[actType]?.devices?.size()) {
                    actConf[actType]?.devices?.each { d->
                        def aDev = actDevices?.find { it?.id?.toString() == d?.device?.toString() }
                        if(aDev) {
                            if(d?.cmd == "disconnectBluetooth") {
                                if(isST && actDelayMs) {
                                    aDev?."${d?.cmd}"([delay: actDelayMs])
                                } else { aDev?."${d?.cmd}"() }
                            } else {
                                if(isST && actDelayMs) {
                                    aDev?."${d?.cmd}"(d?.btDevice, [delay: actDelayMs])
                                } else { aDev?."${d?.cmd}"(d?.btDevice) }
                            }
                            logDebug("Sending ${d?.cmd} | Bluetooth Device: ${d?.btDevice} to ${aDev}${actDelay ? " | Delay: (${actDelay})" : ""}")
                        }
                    }
                }
                break
        }
        if(isActNotifConfigured()) {
            Boolean ok2SendNotif = true
            if(customMsgConfigured()) { actMsgTxt = settings?.notif_custom_message; }
            if(customMsgRequired() && !customMsgConfigured()) { ok2SendNotif = false }
            if(ok2SendNotif && actMsgTxt) {
                if(sendNotifMsg(getActionName() as String, actMsgTxt as String, alexaMsgDev, false)) { logDebug("Sent Action Notification...") }
            }
        }
        if(tierData?.size() && settings?.act_tier_cnt > 1) {
            log.debug "firstTierMsg: ${firstTierMsg} | lastTierMsg: ${lastTierMsg}"
            if(firstTierMsg) {
                if(settings?.act_tier_start_delay) {
                    runIn(settings?.act_tier_start_delay, "executeTaskCommands", [data:[type: "act_tier_start_"]])
                } else { executeTaskCommands([type:"act_tier_start_"]) }
            }
            if(lastTierMsg) {
                if(settings?.act_tier_stop_delay) {
                    runIn(settings?.act_tier_stop_delay, "executeTaskCommands", [data:[type: "act_tier_stop_"]])
                } else { executeTaskCommands([type:"act_tier_stop_"]) }
            }
        } else {
            if(settings?.act_tasks_delay) {
                runIn(settings?.act_tasks_delay, "executeTaskCommands", [data:[type: "act_"]])
            } else { executeTaskCommands([type: "act_"]) }
        }
    }
    logDebug("ExecuteAction Finished | ProcessTime: (${now()-startTime}ms)")
}


private postTaskCommands() {
    if(settings?."${pType}sirens" && settings?."${pType}sirens_cmd") { settings?."${pType}sirens"?.off() }
}

Map getInputData(inName) {
    String desc = null
    String title = null
    String template = null
    String vDesc = "<li>Example: %name% %type% is now %value% would translate into The garage door contact is now open</li>"
    vDesc += "<li>To make beep sounds use: <b>wop, wop, wop</b> (equals 3 beeps)</li>"
    switch(inName) {
        case "act_speak_txt":
            title = "Global Action Speech Event"
            desc = "<li>Add custom responses to use when this action is executed.</li>"
            template = '%name% %type% is now %value% %unit%'
            break
        case "act_announcement_txt":
            title = "Global Announcement Speech Event"
            desc = "<li>Add custom responses to use when this action is executed.</li>"
            template = '%name% %type% is now %value% %unit%'
            break
        default:
            if(inName?.startsWith("trig_")) {
                def i = inName?.tokenize("_")
                if(i?.contains("repeat")) {
                    title = "(${i[1]?.toString()?.capitalize()}) Trigger Repeat Events"
                    desc = "<li>Add custom responses for ${i[1]?.toString()?.capitalize()} events which have to be repeated.</li>${vDesc}"
                    template = '%name% %type% is now %value% %unit%'
                } else {
                    title = "(${i[1]?.toString()?.capitalize()}) Trigger Events"
                    desc = "<li>Add custom responses for ${i[1]?.toString()?.capitalize()} trigger events.</li>${vDesc}"
                    template = '%name% %type% is now %value% %unit%'
                }
            }
            else if(inName?.startsWith("act_tier_item_") && inName?.endsWith("_txt")) {
                def i = inName?.tokenize("_")
                title = "Tier Response (${i[3]})"
                desc = "<li>Add custom responses to use when this action is executed.</li>"
                template = "Custom tier ${i[3]} message here."
            }
            break
    }
    Map o = [
        val: settings?."${inName}" ? settings?."${inName}"?.toString()?.replaceAll("\'", "") : null,
        desc: """<ul class="pl-3" style="list-style-type: bullet;">${desc}</ul>""",
        title: title,
        template: template,
        version: appVersion()
    ]
    return o
}

def updateTxtEntry(obj) {
    // log.debug "updateTxtEntry | Obj: $obj"
    if(obj?.name && obj?.type) {
        settingUpdate("${obj?.name}", obj?.val ?: "", obj?.type as String)
        return true
    }
    return false
}

public getSettingInputVal(inName) {
    // log.debug "getSettingInputVal: ${inName}"
    return settings?."${inName}" ?: null
}


/***********************************************************************************************************
   HELPER UTILITES
************************************************************************************************************/

Boolean advLogsActive() { return (settings?.logDebug || settings?.logTrace) }
public logsEnabled() { if(advLogsActive() && getTsVal("logsEnabled")) { updTsVal("logsEnabled") } }
public logsDisable() { Integer dtSec = getLastTsValSecs("logsEnabled", null); if(dtSec && (dtSec > 3600*6) && advLogsActive()) { settingUpdate("logDebug", "false", "bool"); settingUpdate("logTrace", "false", "bool"); remTsVal("logsEnabled"); } }

private updTsVal(key, dt=null) {
    def data = atomicState?.tsDtMap ?: [:]
    if(key) { data[key] = dt ?: getDtNow() }
    atomicState?.tsDtMap = data
}

private remTsVal(key) {
    def data = atomicState?.tsDtMap ?: [:]
    if(key) {
        if(key instanceof List) {
            key?.each { k-> if(data?.containsKey(k)) { data?.remove(k) } }
        } else { if(data?.containsKey(key)) { data?.remove(key) } }
        atomicState?.tsDtMap = data
    }
}

def getTsVal(val) {
    def tsMap = atomicState?.tsDtMap
    if(val && tsMap && tsMap[val]) { return tsMap[val] }
    return null
}

private updAppFlag(key, val) {
    def data = atomicState?.appFlagsMap ?: [:]
    if(key) { data[key] = val }
    atomicState?.appFlagsMap = data
}

private remAppFlag(key) {
    def data = atomicState?.appFlagsMap ?: [:]
    if(key) {
        if(key instanceof List) {
            key?.each { k-> if(data?.containsKey(k)) { data?.remove(k) } }
        } else { if(data?.containsKey(key)) { data?.remove(key) } }
        atomicState?.appFlagsMap = data
    }
}

Boolean getAppFlag(val) {
    def aMap = atomicState?.appFlagsMap
    if(val && aMap && aMap[val]) { return aMap[val] }
    return false
}

private stateMapMigration() {
    //Timestamp State Migrations
    Map tsItems = [:]
    tsItems?.each { k, v-> if(state?.containsKey(k)) { updTsVal(v as String, state[k as String]); state?.remove(k as String); } }

    //App Flag Migrations
    Map flagItems = [:]
    flagItems?.each { k, v-> if(state?.containsKey(k)) { updAppFlag(v as String, state[k as String]); state?.remove(k as String); } }
    updAppFlag("stateMapConverted", true)
}

Integer getLastTsValSecs(val, nullVal=1000000) {
    def tsMap = atomicState?.tsDtMap
    return (val && tsMap && tsMap[val]) ? GetTimeDiffSeconds(tsMap[val]).toInteger() : nullVal
}

void settingUpdate(name, value, type=null) {
    if(name && type) { app?.updateSetting("$name", [type: "$type", value: value]) }
    else if (name && type == null) { app?.updateSetting(name.toString(), value) }
}

void settingRemove(String name) {
    logTrace("settingRemove($name)...")
    if(name && settings?.containsKey(name as String)) { isST() ? app?.deleteSetting(name as String) : app?.removeSetting(name as String) }
}

List weekDaysEnum() { return ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"] }
List weekDaysAbrvEnum() { return ["MO":"Monday", "TU":"Tuesday", "WE":"Wednesday", "TH":"Thursday", "FR":"Friday", "SA":"Saturday", "SU":"Sunday"] }
List monthEnum() { return ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"] }
Map daysOfWeekMap() { return ["MON":"Monday", "TUE":"Tuesday", "WED":"Wednesday", "THU":"Thursday", "FRI":"Friday", "SAT":"Saturday", "SUN":"Sunday"] }
Map weeksOfMonthMap() { return ["1":"1st Week", "2":"2nd Week", "3":"3rd Week", "4":"4th Week", "5":"5th Week"] }
Map monthMap() { return ["1":"January", "2":"February", "3":"March", "4":"April", "5":"May", "6":"June", "7":"July", "8":"August", "9":"September", "10":"October", "11":"November", "12":"December"] }

Map getAlarmTrigOpts() {
    return isST() ? ["away":"Armed Away","stay":"Armed Home","off":"Disarmed"] : ["armedAway":"Armed Away","armedHome":"Armed Home","disarm":"Disarmed", "alerts":"Alerts"]
}

def getShmIncidents() {
    def incidentThreshold = now() - 604800000
    return location.activeIncidents.collect{[date: it?.date?.time, title: it?.getTitle(), message: it?.getMessage(), args: it?.getMessageArgs(), sourceType: it?.getSourceType()]}.findAll{ it?.date >= incidentThreshold } ?: null
}

public Map getActionMetrics() {
    Map out = [:]
    out?.version = appVersion()
    out?.type = settings?.actionType ?: null
    out?.delay = settings?.actDelay ?: 0
    out?.triggers = settings?.triggerEvents ?: []
    out?.echoZones = settings?.act_EchoZones ?: []
    out?.echoDevices = settings?.act_EchoDevices ?: []
    out?.usingCustomText = (customTxtItems()?.size())
    Integer tDevCnt = 0
    settings?.triggerEvents?.each { if(settings?."trig_${it}" && settings?."trig_${it}"?.size()) { tDevCnt = tDevCnt+settings?."trig_${it}"?.size() } }
    out?.triggerDeviceCnt = tDevCnt
    return out
}

Boolean pushStatus() { return (settings?.notif_sms_numbers?.toString()?.length()>=10 || settings?.notif_send_push || settings?.notif_pushover) ? ((settings?.notif_send_push || (settings?.notif_pushover && settings?.notif_pushover_devices)) ? "Push Enabled" : "Enabled") : null }
Integer getLastNotifMsgSec() { return !state?.lastNotifMsgDt ? 100000 : GetTimeDiffSeconds(state?.lastNotifMsgDt, "getLastMsgSec").toInteger() }
Integer getLastChildInitRefreshSec() { return !state?.lastChildInitRefreshDt ? 3600 : GetTimeDiffSeconds(state?.lastChildInitRefreshDt, "getLastChildInitRefreshSec").toInteger() }
Boolean getOk2Notify() {
    Boolean smsOk = (settings?.notif_sms_numbers?.toString()?.length()>=10)
    Boolean pushOk = settings?.notif_send_push
    Boolean alexaMsg = (settings?.notif_alexa_mobile)
    Boolean pushOver = (settings?.notif_pushover && settings?.notif_pushover_devices)
    Boolean notifDevsOk = (settings?.notif_devs?.size())
    Boolean daysOk = settings?.notif_days ? (isDayOfWeek(settings?.notif_days)) : true
    Boolean timeOk = notifTimeOk()
    Boolean modesOk = settings?.notif_mode ? (isInMode(settings?.notif_mode)) : true
    logDebug("getOk2Notify() | smsOk: $smsOk | pushOk: $pushOk | pushOver: $pushOver || alexaMsg: $alexaMsg || daysOk: $daysOk | timeOk: $timeOk | modesOk: $modesOk")
    if(!(smsOk || pushOk || alexaMsg || notifDevsOk || pushOver)) { return false }
    if(!(daysOk && modesOk && timeOk)) { return false }
    return true
}

Boolean notifTimeOk() {
    def startTime = null
    def stopTime = null
    def now = new Date()
    def sun = getSunriseAndSunset() // current based on geofence, previously was: def sun = getSunriseAndSunset(zipCode: zipCode)
    if(settings?.notif_time_start_type && settings?.notif_time_stop_type) {
        if(settings?.notif_time_start_type == "sunset") { startTime = sun?.sunset }
        else if(settings?.notif_time_start_type == "sunrise") { startTime = sun?.sunrise }
        else if(settings?.notif_time_start_type == "time" && settings?.notif_time_start) { startTime = settings?.notif_time_start }

        if(settings?.notif_time_stop_type == "sunset") { stopTime = sun?.sunset }
        else if(settings?.notif_time_stop_type == "sunrise") { stopTime = sun?.sunrise }
        else if(settings?.notif_time_stop_type == "time" && settings?.notif_time_stop) { stopTime = settings?.notif_time_stop }
    } else { return true }
    if(startTime && stopTime) {
        if(!isST()) {
            startTime = toDateTime(startTime)
            stopTime = toDateTime(stopTime)
        }
        return timeOfDayIsBetween(startTime, stopTime, new Date(), location?.timeZone)
    } else { return true }
}

// Sends the notifications based on app settings
public sendNotifMsg(String msgTitle, String msg, alexaDev=null, Boolean showEvt=true) {
    logTrace("sendNotifMsg() | msgTitle: ${msgTitle}, msg: ${msg}, $alexaDev, showEvt: ${showEvt}")
    List sentSrc = ["Push"]
    Boolean sent = false
    try {
        String newMsg = "${msgTitle}: ${msg}"
        String flatMsg = newMsg.toString().replaceAll("\n", " ")
        if(!getOk2Notify()) {
            logInfo( "sendNotifMsg: Message Skipped During Quiet Time ($flatMsg)")
            if(showEvt) { sendNotificationEvent(newMsg) }
        } else {
            if(settings?.notif_send_push) {
                sendSrc?.push("Push Message")
                if(showEvt) {
                    sendPush(newMsg)	// sends push and notification feed
                } else { sendPushMessage(newMsg) } // sends push
                sent = true
            }
            if(settings?.notif_pushover && settings?.notif_pushover_devices) {
                sentSrc?.push("Pushover Message")
                Map msgObj = [title: msgTitle, message: msg, priority: (settings?.notif_pushover_priority?:0)]
                if(settings?.notif_pushover_sound) { msgObj?.sound = settings?.notif_pushover_sound }
                buildPushMessage(settings?.notif_pushover_devices, msgObj, true)
                sent = true
            }
            String smsPhones = settings?.notif_sms_numbers?.toString() ?: null
            if(smsPhones) {
                List phones = smsPhones?.toString()?.tokenize(",")
                for (phone in phones) {
                    String t0 = newMsg.take(140)
                    if(showEvt) {
                        sendSms(phone?.trim(), t0)	// send SMS and notification feed
                    } else { sendSmsMessage(phone?.trim(), t0) } // send SMS
                }
                sentSrc?.push("SMS Message to [${phones}]")
                sent = true
            }
            if(settings?.notif_devs) {
                sentSrc?.push("Notification Devices")
                settings?.notif_devs?.each { it?.deviceNotification(msg as String) }
                sent = true
            }
            if(settings?.notif_alexa_mobile && alexaDev) {
                alexaDev?.sendAlexaAppNotification(msg)
                sentSrc?.push("Alexa Mobile App")
                sent = true
            }
            if(sent) {
                state?.lastNotificationMsg = flatMsg
                state?.lastNotifMsgDt = getDtNow()
                logDebug("sendNotifMsg: Sent ${sendSrc} (${flatMsg})")
            }
        }
    } catch (ex) {
        logError("sendNotifMsg $sentstr Exception: ${ex}")
    }
    return sent
}

Boolean isActNotifConfigured() {
    if(customMsgRequired() && (!settings?.notif_use_custom || settings?.notif_custom_message)) { return false }
    return (settings?.notif_sms_numbers?.toString()?.length()>=10 || settings?.notif_send_push || settings?.notif_devs || settings?.notif_alexa_mobile || (isST() && settings?.notif_pushover && settings?.notif_pushover_devices))
}

//PushOver-Manager Input Generation Functions
private getPushoverSounds(){return (Map) state?.pushoverManager?.sounds?:[:]}
private getPushoverDevices(){List opts=[];Map pmd=state?.pushoverManager?:[:];pmd?.apps?.each{k,v->if(v&&v?.devices&&v?.appId){Map dm=[:];v?.devices?.sort{}?.each{i->dm["${i}_${v?.appId}"]=i};addInputGrp(opts,v?.appName,dm);}};return opts;}
private inputOptGrp(List groups,String title){def group=[values:[],order:groups?.size()];group?.title=title?:"";groups<<group;return groups;}
private addInputValues(List groups,String key,String value){def lg=groups[-1];lg["values"]<<[key:key,value:value,order:lg["values"]?.size()];return groups;}
private listToMap(List original){original.inject([:]){r,v->r[v]=v;return r;}}
private addInputGrp(List groups,String title,values){if(values instanceof List){values=listToMap(values)};values.inject(inputOptGrp(groups,title)){r,k,v->return addInputValues(r,k,v)};return groups;}
private addInputGrp(values){addInputGrp([],null,values)}
//PushOver-Manager Location Event Subscription Events, Polling, and Handlers
public pushover_init(){subscribe(location,"pushoverManager",pushover_handler);pushover_poll()}
public pushover_cleanup(){state?.remove("pushoverManager");unsubscribe("pushoverManager");}
public pushover_poll(){sendLocationEvent(name:"pushoverManagerCmd",value:"poll",data:[empty:true],isStateChange:true,descriptionText:"Sending Poll Event to Pushover-Manager")}
public pushover_msg(List devs,Map data){if(devs&&data){sendLocationEvent(name:"pushoverManagerMsg",value:"sendMsg",data:data,isStateChange:true,descriptionText:"Sending Message to Pushover Devices: ${devs}");}}
public pushover_handler(evt){Map pmd=state?.pushoverManager?:[:];switch(evt?.value){case"refresh":def ed = evt?.jsonData;String id = ed?.appId;Map pA = pmd?.apps?.size() ? pmd?.apps : [:];if(id){pA[id]=pA?."${id}"instanceof Map?pA[id]:[:];pA[id]?.devices=ed?.devices?:[];pA[id]?.appName=ed?.appName;pA[id]?.appId=id;pmd?.apps = pA;};pmd?.sounds=ed?.sounds;break;case "reset":pmd=[:];break;};state?.pushoverManager=pmd;}
//Builds Map Message object to send to Pushover Manager
private buildPushMessage(List devices,Map msgData,timeStamp=false){if(!devices||!msgData){return};Map data=[:];data?.appId=app?.getId();data.devices=devices;data?.msgData=msgData;if(timeStamp){data?.msgData?.timeStamp=new Date().getTime()};pushover_msg(devices,data);}
Integer versionStr2Int(str) { return str ? str.toString()?.replaceAll("\\.", "")?.toInteger() : null }
Boolean minVersionFailed() {
    try {
        Integer minDevVer = parent?.minVersions()["actionApp"] ?: null
        if(minDevVer != null && versionStr2Int(devVersion()) < minDevVer) { return true }
        else { return false }
    } catch (e) { 
        return false
    }
}


/************************************************************************************************************
        webCoRE Integration
************************************************************************************************************/
private webCoRE_handle(){return'webCoRE'}
private webCoRE_init(pistonExecutedCbk){atomicState.webCoRE=(atomicState?.webCoRE instanceof Map?atomicState?.webCoRE:[:])+(pistonExecutedCbk?[cbk:pistonExecutedCbk]:[:]);subscribe(location,"${webCoRE_handle()}.pistonList",webCoRE_handler);if(pistonExecutedCbk)subscribe(location,"${webCoRE_handle()}.pistonExecuted",webCoRE_handler);webCoRE_poll();}
private webCoRE_poll(){sendLocationEvent([name: webCoRE_handle(),value:'poll',isStateChange:true,displayed:false])}
public  webCoRE_execute(pistonIdOrName,Map data=[:]){def i=(atomicState?.webCoRE?.pistons?:[]).find{(it.name==pistonIdOrName)||(it.id==pistonIdOrName)}?.id;if(i){sendLocationEvent([name:i,value:app.label,isStateChange:true,displayed:false,data:data])}}
public  webCoRE_list(mode){def p=atomicState?.webCoRE?.pistons;if(p)p.collect{mode=='id'?it.id:(mode=='name'?it.name:[id:it.id,name:it.name])}}
public  webCoRE_handler(evt){switch(evt.value){case 'pistonList':List p=atomicState?.webCoRE?.pistons?:[];Map d=evt.jsonData?:[:];if(d.id&&d.pistons&&(d.pistons instanceof List)){p.removeAll{it.iid==d.id};p+=d.pistons.collect{[iid:d.id]+it}.sort{it.name};atomicState?.webCoRE = [updated:now(),pistons:p];};break;case 'pistonExecuted':def cbk=atomicState?.webCoRE?.cbk;if(cbk&&evt.jsonData)"$cbk"(evt.jsonData);break;}}
public  webCore_icon(){return "https://cdn.rawgit.com/ady624/webCoRE/master/resources/icons/app-CoRE.png"}
/******************************************
|   Restriction validators
*******************************************/

Double getDevValueAvg(devs, attr) {
    List vals = devs?.findAll { it?."current${attr?.capitalize()}" != null && it?."current${attr?.capitalize()}" != 0 }?.collect { it?."current${attr?.capitalize()}" as Double }
    return vals?.size() ? (vals?.sum()/vals?.size())?.round(1) as Double : null
}

String getCurrentMode() {
    return location?.mode
}

List getLocationModes(Boolean sorted=false) {
    List modes = location?.modes*.name
    // log.debug "modes: ${modes}"
    return (sorted) ? modes?.sort() : modes
}

List getLocationRoutines() {
    return (isST()) ? location.helloHome?.getPhrases()*.label?.sort() : []
}

def getRoutineById(rId) {
    return location?.helloHome?.getPhrases()?.find{it?.id == rId}
}

def execRoutineById(rId) {
    if(rId) {
        def nId = getRoutineById(rId)
        if(nId && nId?.label) { location.helloHome?.execute(nId?.label) }
    }
}

def getModeById(mId) {
    return location?.getModes()?.find{it?.id == mId}
}

Boolean isInMode(modes, not=false) {
    return (modes) ? (not ? (!(getCurrentMode() in modes)) : (getCurrentMode() in modes)) : false
}

Boolean isInAlarmMode(modes) {
    return (modes) ? (parent?.getAlarmSystemStatus() in modes) : false
}

Boolean areAllDevsSame(List devs, String attr, val) {
    if(devs && attr && val) { return (devs?.findAll { it?.currentValue(attr) == val as String }?.size() == devs?.size()) }
    return false
}

Boolean allDevCapValsEqual(List devs, String cap, val) {
    if(devs) { return (devs?.findAll { it?."current${cap?.capitalize()}" == val }?.size() == devs?.size()) }
    return false
}

Boolean anyDevCapValsEqual(List devs, String cap, val) {
    return (devs && cap && val) ? (devs?.findAll { it?."current${cap?.capitalize()}" == val }?.size() >= 1) : false
}

Boolean anyDevCapNumValAbove(List devs, String cap, val) {
    return (devs && cap && val) ? (devs?.findAll { it?."current${cap?.capitalize()}"?.toDouble() > val?.toDouble() }?.size() >= 1) : false
}
Boolean anyDevCapNumValBelow(List devs, String cap, val) {
    return (devs && cap && val) ? (devs?.findAll { it?."current${cap?.capitalize()}"?.toDouble() < val?.toDouble() }?.size() >= 1) : false
}
Boolean anyDevCapNumValBetween(List devs, String cap, low, high) {
    return (devs && cap && low && high) ? (devs?.findAll { ( (it?."current${cap?.capitalize()}"?.toDouble() > low?.toDouble()) && (it?."current${cap?.capitalize()}"?.toDouble() < high?.toDouble()) ) }?.size() >= 1) : false
}
Boolean anyDevCapNumValEqual(List devs, String cap, val) {
    return (devs && cap && val) ? (devs?.findAll { it?."current${cap?.capitalize()}"?.toDouble() == val?.toDouble() }?.size() >= 1) : false
}

Boolean allDevCapNumValsAbove(List devs, String cap, val) {
    return (devs && cap && val) ? (devs?.findAll { it?."current${cap?.capitalize()}"?.toDouble() > val?.toDouble() }?.size() == devs?.size()) : false
}
Boolean allDevCapNumValsBelow(List devs, String cap, val) {
    return (devs && cap && val) ? (devs?.findAll { it?."current${cap?.capitalize()}"?.toDouble() < val?.toDouble() }?.size() == devs?.size()) : false
}
Boolean allDevCapNumValsBetween(List devs, String cap, low, high) {
    return (devs && cap && low && high) ? (devs?.findAll { ( (it?."current${cap?.capitalize()}"?.toDouble() > low?.toDouble()) && (it?."current${cap?.capitalize()}"?.toDouble() < high?.toDouble()) ) }?.size() == devs?.size()) : false
}
Boolean allDevCapNumValsEqual(List devs, String cap, val) {
    return (devs && cap && val) ? (devs?.findAll { it?."current${cap?.capitalize()}"?.toDouble() == val?.toDouble() }?.size() == devs?.size()) : false
}

Boolean devCapValEqual(List devs, String devId, String cap, val) {
    if(devs) { return (devs?.find { it?."current${cap?.capitalize()}" == val }) }
    return false
}

String getAlarmSystemName(abbr=false) {
    return isST() ? (abbr ? "SHM" : "Smart Home Monitor") : (abbr ? "HSM" : "Hubitat Safety Monitor")
}
/******************************************
|    Time and Date Conversion Functions
*******************************************/
def formatDt(dt, tzChg=true) {
    def tf = new java.text.SimpleDateFormat("E MMM dd HH:mm:ss z yyyy")
    if(tzChg) { if(location.timeZone) { tf.setTimeZone(location?.timeZone) } }
    return tf?.format(dt)
}

def dateTimeFmt(dt, fmt) {
    if(!(dt instanceof Date)) { try { dt = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", dt?.toString()) } catch(e) { dt = Date.parse("E MMM dd HH:mm:ss z yyyy", dt?.toString()) } }
    def tf = new java.text.SimpleDateFormat(fmt)
    if(location?.timeZone) { tf.setTimeZone(location?.timeZone) }
    return tf?.format(dt)
}

def convToTime(dt) {
    def newDt = dateTimeFmt(dt, "h:mm a")
    if(newDt?.toString()?.contains(":00 ")) { newDt?.toString()?.replaceAll(":00 ", " ") }
    return newDt
}

def convToDate(dt) {
    def newDt = dateTimeFmt(dt, "EEE, MMM d")
    return newDt
}

def convToDateTime(dt) {
    def t = dateTimeFmt(dt, "h:mm a")
    def d = dateTimeFmt(dt, "EEE, MMM d")
    return "$d, $t"
}

def okSym() {
    return "\u2713"
}
def notOkSym() {
    return "\u2715"
}
Date parseDate(dt) { return Date.parse("E MMM dd HH:mm:ss z yyyy", dt?.toString()) }
Boolean isDateToday(Date dt) { return (dt && dt?.clearTime().compareTo(new Date()?.clearTime()) >= 0) }
String strCapitalize(str) { return str ? str?.toString().capitalize() : null }
String pluralizeStr(obj, para=true) { return (obj?.size() > 1) ? "${para ? "(s)": "s"}" : "" }

def parseDt(pFormat, dt, tzFmt=true) {
    def result
    def newDt = Date.parse("$pFormat", dt)
    result = formatDt(newDt, tzFmt)
    //log.debug "parseDt Result: $result"
    return result
}

def parseFmtDt(parseFmt, newFmt, dt) {
    def newDt = Date.parse(parseFmt, dt?.toString())
    def tf = new java.text.SimpleDateFormat(newFmt)
    if(location?.timeZone) { tf.setTimeZone(location?.timeZone) }
    return tf?.format(newDt)
}

def getDtNow() {
    def now = new Date()
    return formatDt(now)
}

def epochToTime(tm) {
    def tf = new java.text.SimpleDateFormat("h:mm a")
    if(location?.timeZone) { tf?.setTimeZone(location?.timeZone) }
    return tf.format(tm)
}

def time2Str(time, fmt="h:mm a") {
    if(time) {
        def t = timeToday(time as String, location?.timeZone)
        def f = new java.text.SimpleDateFormat(fmt as String)
        if(location?.timeZone) { tf?.setTimeZone(location?.timeZone) }
        return f?.format(t)
    }
}

def fmtTime(t, fmt="h:mm a", altFmt=false) {
    if(!t) return null
    def dt = new Date().parse(altFmt ? "E MMM dd HH:mm:ss z yyyy" : "yyyy-MM-dd'T'HH:mm:ss.SSSZ", t?.toString())
    def tf = new java.text.SimpleDateFormat(fmt as String)
    if(location?.timeZone) { tf?.setTimeZone(location?.timeZone) }
    return tf?.format(dt)
}

def GetTimeDiffSeconds(lastDate, sender=null) {
    try {
        if(lastDate?.contains("dtNow")) { return 10000 }
        def now = new Date()
        def lastDt = Date.parse("E MMM dd HH:mm:ss z yyyy", lastDate)
        def start = Date.parse("E MMM dd HH:mm:ss z yyyy", formatDt(lastDt)).getTime()
        def stop = Date.parse("E MMM dd HH:mm:ss z yyyy", formatDt(now)).getTime()
        def diff = (int) (long) (stop - start) / 1000
        return diff
    }
    catch (ex) {
        logError("GetTimeDiffSeconds Exception: (${sender ? "$sender | " : ""}lastDate: $lastDate): ${ex?.message}")
        return 10000
    }
}

def getDateByFmt(String fmt, dt=null) {
    def df = new java.text.SimpleDateFormat(fmt)
    df?.setTimeZone(location?.timeZone)
    return df?.format(dt ?: new Date())
}

Map getDateMap() {
    Map m = [:]
    m?.dayOfYear =    getDateByFmt("DD")
    m?.dayNameShort = getDateByFmt("EEE")?.toString()?.toUpperCase()
    m?.dayName =      getDateByFmt("EEEE")
    m?.day =          getDateByFmt("d")
    m?.week =         getDateByFmt("W")
    m?.weekOfYear =   getDateByFmt("w")
    m?.monthName =    getDateByFmt("MMMMM")
    m?.month =        getDateByFmt("MM")
    m?.year =         getDateByFmt("yyyy")
    m?.hour =         getDateByFmt("hh")
    m?.minute =       getDateByFmt("mm")
    m?.second =       getDateByFmt("ss")
    return m
}

Boolean isDayOfWeek(opts) {
    def df = new java.text.SimpleDateFormat("EEEE")
    df?.setTimeZone(location?.timeZone)
    def day = df?.format(new Date())
    return opts?.contains(day)
}

Boolean isMonthOfYear(opts) {
    def dtMap = getDateMap()
    return ( opts?.contains(dtMap?.monthName) )
}

Boolean isTimeBetween(startTime, stopTime, curTime= new Date()) {
    if(!startTime && !stopTime) { return true }
    if(!isST()) { startTime = toDateTime(startTime); stopTime = toDateTime(stopTime); }
    return timeOfDayIsBetween(startTime, stopTime, curTime, location?.timeZone)
}

/******************************************
|   App Input Description Functions
*******************************************/

Map dimmerLevelEnum() { return [100:"Set Level to 100%", 90:"Set Level to 90%", 80:"Set Level to 80%", 70:"Set Level to 70%", 60:"Set Level to 60%", 50:"Set Level to 50%", 40:"Set Level to 40%", 30:"Set Level to 30%", 20:"Set Level to 20%", 10:"Set Level to 10%"] }

String unitStr(type) {
    switch(type) {
        case "temp":
            return "\u00b0${getTemperatureScale() ?: "F"}"
        case "humidity":
            return "%"
        default:
            return ""
    }
}

String getAppNotifDesc(hide=false) {
    String str = ""
    if(isActNotifConfigured()) {
        str += hide ? "" : "Send To:\n"
        str += settings?.notif_sms_numbers ? " \u2022 (${settings?.notif_sms_numbers?.tokenize(",")?.size()} SMS Numbers)\n" : ""
        str += settings?.notif_send_push ? " \u2022 (Push Message)\n" : ""
        str += (settings?.notif_pushover && settings?.notif_pushover_devices?.size()) ? " \u2022 Pushover Device${pluralizeStr(settings?.notif_pushover_devices)} (${settings?.notif_pushover_devices?.size()})\n" : ""
        str += settings?.notif_alexa_mobile ? " \u2022 Alexa Mobile App\n" : ""
        str += getNotifSchedDesc(true) ? " \u2022 Restrictions: (${getOk2Notify() ? "${okSym()}" : "${notOkSym()}"})\n" : ""
    }
    return str != "" ? str : null
}

List getQuietDays() {
    List allDays = weekDaysEnum()
    List curDays = settings?.quietDays ?: []
    return allDays?.findAll { (!curDays?.contains(it as String)) }
}

String getNotifSchedDesc(min=false) {
    def sun = getSunriseAndSunset()
    def startInput = settings?.notif_time_start_type
    def startTime = settings?.notif_time_start
    def stopInput = settings?.notif_time_stop_type
    def stopTime = settings?.notif_time_stop
    def dayInput = settings?.notif_days
    def modeInput = settings?.notif_modes
    def str = ""
    def startLbl = ( (startInput == "Sunrise" || startInput == "Sunset") ? ( (startInput == "Sunset") ? epochToTime(sun?.sunset?.time) : epochToTime(sun?.sunrise?.time) ) : (startTime ? time2Str(startTime) : "") )
    def stopLbl = ( (stopInput == "Sunrise" || stopInput == "Sunset") ? ( (stopInput == "Sunset") ? epochToTime(sun?.sunset?.time) : epochToTime(sun?.sunrise?.time) ) : (stopTime ? time2Str(stopTime) : "") )
    str += (startLbl && stopLbl) ? " • Time: ${startLbl} - ${stopLbl}" : ""
    def qDays = getQuietDays()
    str += dayInput && qDays ? "${(startLbl || stopLbl) ? "\n" : ""} \u2022 Day${pluralizeStr(dayInput, false)}:${min ? " (${qDays?.size()} selected)" : "\n    - ${qDays?.join("\n    - ")}"}" : ""
    str += modeInput ? "${(startLbl || stopLbl || qDays) ? "\n" : ""} \u2022 Mode${pluralizeStr(modeInput, false)}:${min ? " (${modeInput?.size()} selected)" : "\n    - ${modeInput?.join("\n    - ")}"}" : ""
    return (str != "") ? "${str}" : null
}

String getTriggersDesc(hideDesc=false) {
    Boolean confd = triggersConfigured()
    List setItem = settings?.triggerEvents
    String sPre = "trig_"
    if(confd && setItem?.size()) {
        if(!hideDesc) {
            String str = "Triggers:\n"
            setItem?.each { evt->
                switch(evt as String) {
                    case "scheduled":
                        str += " \u2022 ${evt?.capitalize()}${settings?."${sPre}${evt}_type" ? " (${settings?."${sPre}${evt}_type"})" : ""}\n"
                        str += settings?."${sPre}${evt}_recurrence"     ? "    \u25E6 Recurrence: (${settings?."${sPre}${evt}_recurrence"})\n"      : ""
                        str += settings?."${sPre}${evt}_time"     ? "    \u25E6 Time: (${fmtTime(settings?."${sPre}${evt}_time")})\n"      : ""
                        str += settings?."${sPre}${evt}_weekdays"     ? "    \u25E6 Week Days: (${settings?."${sPre}${evt}_weekdays"?.join(",")})\n"      : ""
                        str += settings?."${sPre}${evt}_daynums"     ? "    \u25E6 Days of Month: (${settings?."${sPre}${evt}_daynums"?.size()})\n"      : ""
                        str += settings?."${sPre}${evt}_weeks"    ? "    \u25E6 Weeks of Month: (${settings?."${sPre}${evt}_weeks"?.join(",")})\n" : ""
                        str += settings?."${sPre}${evt}_months"   ? "    \u25E6 Months: (${settings?."${sPre}${evt}_months"?.join(",")})\n"  : ""
                        break
                    case "alarm":
                        str += " \u2022 ${evt?.capitalize()} (${getAlarmSystemName(true)})${settings?."${sPre}${evt}" ? " (${settings?."${sPre}${evt}"?.size()} Selected)" : ""}\n"
                        str += settings?."${sPre}${evt}_once" ? "    \u25E6 Once a Day: (${settings?."${sPre}${evt}_once"})\n" : ""
                        break
                    case "routineExecuted":
                    case "mode":
                    case "scene":
                        str += " \u2022 ${evt == "routineExecuted" ? "Routines" : evt?.capitalize()}${settings?."${sPre}${evt}" ? " (${settings?."${sPre}${evt}"?.size()} Selected)" : ""}\n"
                        str += settings?."${sPre}${evt}_once" ? "    \u25E6 Once a Day: (${settings?."${sPre}${evt}_once"})\n" : ""
                        break
                    default:
                        str += " \u2022 ${evt?.capitalize()}${settings?."${sPre}${evt}" ? " (${settings?."${sPre}${evt}"?.size()} Selected)" : ""}\n"
                        def subStr = ""
                        if(settings?."${sPre}${evt}_cmd" in ["above", "below", "equal", "between"]) {
                            if (settings?."${sPre}${evt}_cmd" == "between") {
                                str += settings?."${sPre}${evt}_cmd"  ? "    \u25E6 ${settings?."${sPre}${evt}_cmd"}: (${settings?."${sPre}${evt}_low"} - ${settings?."${sPre}${evt}_high"})\n" : ""
                            } else {
                                str += (settings?."${sPre}${evt}_cmd" == "above" && settings?."${sPre}${evt}_high")     ? "    \u25E6 Above: (${settings?."${sPre}${evt}_high"})\n" : ""
                                str += (settings?."${sPre}${evt}_cmd" == "below" && settings?."${sPre}${evt}_low")      ? "    \u25E6 Below: (${settings?."${sPre}${evt}_low"})\n" : ""
                                str += (settings?."${sPre}${evt}_cmd" == "equal" && settings?."${sPre}${evt}_equal")    ? "    \u25E6 Equals: (${settings?."${sPre}${evt}_equal"})\n" : ""
                            }
                        } else {
                            str += settings?."${sPre}${evt}_cmd"  ? "    \u25E6 Trigger State: (${settings?."${sPre}${evt}_cmd"})\n" : ""
                        }
                        str += settings?."${sPre}${evt}_nums"              ? "    \u25E6 Button Numbers: ${settings?."${sPre}${evt}_nums"}\n" : ""
                        str += settings?."${sPre}${evt}_after"              ? "    \u25E6 Only After: (${settings?."${sPre}${evt}_after"} sec)\n" : ""
                        str += settings?."${sPre}${evt}_after_repeat"       ? "    \u25E6 Repeat Every: (${settings?."${sPre}${evt}_after_repeat"} sec)\n" : ""
                        str += settings?."${sPre}${evt}_after_repeat_cnt"   ? "    \u25E6 Repeat Count: (${settings?."${sPre}${evt}_after_repeat_cnt"})\n" : ""
                        str += (settings?."${sPre}${evt}_all" == true)      ? "    \u25E6 Require All: (${settings?."${sPre}${evt}_all"})\n" : ""
                        str += settings?."${sPre}${evt}_once"               ? "    \u25E6 Once a Day: (${settings?."${sPre}${evt}_once"})\n" : ""
                        str += settings?."${sPre}${evt}_wait"               ? "    \u25E6 Wait: (${settings?."${sPre}${evt}_wait"})\n" : ""
                        str += (settings?."${sPre}${evt}_txt" || settings?."${sPre}${evt}_after_repeat_txt") ? "    \u25E6 Custom Responses:\n" : ""
                        str += settings?."${sPre}${evt}_txt"                ? "       \u02C3 Events: (${settings?."${sPre}${evt}_txt"?.toString()?.tokenize(";")?.size()} Items)\n" : ""
                        str += settings?."${sPre}${evt}_after_repeat_txt"   ? "       \u02C3 Repeats: (${settings?."${sPre}${evt}_after_repeat_txt"?.toString()?.tokenize(";")?.size()} Items)\n" : ""
                        break
                }
            }
            str += "\ntap to modify..."
            return str
        } else { return "tap to modify..." }
    } else {
        return "tap to configure..."
    }
}

String getConditionsDesc() {
    Boolean confd = conditionsConfigured()
    def time = null
    String sPre = "cond_"
    if(confd) {
        String str = "Conditions: (${(conditionStatus()?.ok == true) ? "${okSym()}" : "${notOkSym()}"})\n"
        str += reqAllCond() ?  " \u2022 All Conditions Required\n" : " \u2022 Any Condition Allowed\n"
        if(timeCondConfigured()) {
            str += " • Time Between: (${(timeCondOk() == true) ? "${okSym()}" : "${notOkSym()}"})\n"
            str += "    - ${getTimeCondDesc(false)}\n"
        }
        if(dateCondConfigured()) {
            str += " • Date:\n"
            str += settings?.cond_days      ? "    - Days: (${(isDayOfWeek(settings?.cond_days)) ? "${okSym()}" : "${notOkSym()}"})\n" : ""
            str += settings?.cond_months    ? "    - Months: (${(isMonthOfYear(settings?.cond_months)) ? "${okSym()}" : "${notOkSym()}"})\n"  : ""
        }
        if(settings?.cond_alarm || (settings?.cond_mode && settings?.cond_mode_cmd)) {
            str += " • Location: (${locationCondOk() ? "${okSym()}" : "${notOkSym()}"})\n"
            str += settings?.cond_alarm ? "    - Alarm Modes: (${(isInAlarmMode(settings?.cond_alarm)) ? "${okSym()}" : "${notOkSym()}"})\n" : ""
            str += settings?.cond_mode ? "    - Modes(${settings?.cond_mode_cmd == "not" ? "not in" : "in"}): (${(isInMode(settings?.cond_mode, (settings?.cond_mode_cmd == "not"))) ? "${okSym()}" : "${notOkSym()}"})\n" : ""
        }
        if(deviceCondConfigured()) {
            ["switch", "motion", "presence", "contact", "acceleration", "lock", "battery", "temperature", "illuminance", "shade", "door", "level", "valve", "water", "power"]?.each { evt->
                if(devCondConfigured(evt)) {
                    def condOk = false
                    if(evt in ["switch", "motion", "presence", "contact", "acceleration", "lock", "shade", "door", "valve", "water"]) { condOk = checkDeviceCondOk(evt) }
                    else if(evt in ["battery", "temperature", "illuminance", "level", "power"]) { condOk = checkDeviceNumCondOk(evt) }

                    str += settings?."${sPre}${evt}"     ? " • ${evt?.capitalize()} (${settings?."${sPre}${evt}"?.size()}) (${condOk ? "${okSym()}" : "${notOkSym()}"})\n" : ""
                    def cmd = settings?."${sPre}${evt}_cmd" ?: null
                    if(cmd in ["between", "below", "above", "equals"]) {
                        def cmdLow = settings?."${sPre}${evt}_low" ?: null
                        def cmdHigh = settings?."${sPre}${evt}_high" ?: null
                        def cmdEq = settings?."${sPre}${evt}_equal" ?: null
                        str += (cmd == "equals" && cmdEq) ? "    - Value: ( =${cmdEq}${attUnit(evt)})${settings?."cond_${inType}_avg" ? "(Avg)" : ""}\n" : ""
                        str += (cmd == "between" && cmdLow && cmdHigh) ? "    - Value: (${cmdLow-cmdHigh}${attUnit(evt)})${settings?."cond_${inType}_avg" ? "(Avg)" : ""}\n" : ""
                        str += (cmd == "above" && cmdHigh) ? "    - Value: ( >${cmdHigh}${attUnit(evt)})${settings?."cond_${inType}_avg" ? "(Avg)" : ""}\n" : ""
                        str += (cmd == "below" && cmdLow) ? "    - Value: ( <${cmdLow}${attUnit(evt)})${settings?."cond_${inType}_avg" ? "(Avg)" : ""}\n" : ""
                    } else {
                        str += cmd ? "    - Value: (${cmd})${settings?."cond_${inType}_avg" ? "(Avg)" : ""}\n" : ""
                    }
                    str += (settings?."${sPre}${evt}_all" == true) ? "    - Require All: (${settings?."${sPre}${evt}_all"})\n" : ""
                }
            }
        }
        str += "\ntap to modify..."
        return str
    } else {
        return "tap to configure..."
    }
}

String attUnit(attr) {
    switch(attr) {
        case "humidity":
        case "level":
        case "battery":
            return " percent"
        case "temperature":
            return " degrees"
        case "illuminance":
            return " lux"
        case "power":
            return " watts"
        default:
            return ""
    }
}

def getZoneStatus() {
    def echoZones = settings?.act_EchoZones ?: []
    def res = [:]
    if(echoZones?.size()) {
        def allZones = parent?.getZones()
        echoZones?.each { k -> if(allZones?.containsKey(k)) { res[k] = allZones[k] } }
        return res
    }
}

String getActionDesc() {
    Boolean confd = executionConfigured()
    def time = null
    String sPre = "act_"
    if(settings?.actionType && confd) {
        Boolean isTierAct = isTierAction()
        String str = ""
        def eDevs = parent?.getDevicesFromList(settings?.act_EchoDevices)
        def zones = getZoneStatus()
        def tierDesc = isTierAct ? getTierRespDesc() : null
        def tierStart = isTierAct ? actTaskDesc("act_tier_start_") : null
        def tierStop = isTierAct ? actTaskDesc("act_tier_stop_") : null
        str += zones?.size() ? "Echo Zones:\n${zones?.collect { " \u2022 ${it?.value?.name} (${it?.value?.active == true ? "Active" : "Inactive"})" }?.join("\n")}\n${eDevs?.size() ? "\n": ""}" : ""
        str += eDevs?.size() ? "Alexa Devices:${zones?.size() ? " (Zone Backups)" : ""}\n${eDevs?.collect { " \u2022 ${it?.displayName?.toString()?.replace("Echo - ", "")}" }?.join("\n")}\n" : ""
        str += tierDesc ? "\n${tierDesc}${tierStart || tierStop ? "" : "\n"}" : ""
        str += tierStart ? "${tierStart}\n" : ""
        str += tierStop ? "${tierStop}\n" : ""
        str += settings?.act_volume_change ? "New Volume: (${settings?.act_volume_change})\n" : ""
        str += settings?.act_volume_restore ? "Restore Volume: (${settings?.act_volume_restore})\n" : ""
        str += settings?.act_delay ? "Delay: (${settings?.act_delay})\n" : ""
        str += settings?.actionType in ["speak", "announcement", "speak_tiered", "announcement_tiered"] && settings?."act_${settings?.actionType}_txt" ? "Using Default Response: (True)\n" : ""
        def trigTasks = !isTierAct ? actTaskDesc("act_") : null
        str += trigTasks ? "${trigTasks}" : ""
        // str += settings?.act_switches_on ? "Switches On: (${settings?.act_switches_on?.size()})\n" : ""
        // str += settings?.act_switches_off ? "Switches Off: (${settings?.act_switches_off?.size()})\n" : ""
        // str += settings?.act_mode_run ? "Set Mode:\n \u2022 ${settings?.act_mode_run})\n" : ""
        // str += settings?.act_routine_run ? "Execute Routine:\n \u2022 ${settings?.act_routine_run})\n" : ""
        // str += (settings?.enableWebCoRE && settings?.webCorePistons) ? "webCoRE Piston:\n \u2022 ${settings?.webCorePistons}\n" : ""
        str += "\nTap to modify..."
        return str
    } else {
        return "tap to configure..."
    }
    return confd ? "Actions:\n • ${settings?.actionType}\n\ntap to modify..." : "tap to configure..."
}

String getTimeCondDesc(addPre=true) {
    def sun = getSunriseAndSunset()
    def sunsetTime = epochToTime(sun?.sunset?.time)
    def sunriseTime = epochToTime(sun?.sunrise?.time)
    def startType = settings?.cond_time_start_type
    def startTime = settings?.cond_time_start ? fmtTime(settings?.cond_time_start) : null
    def stopType = settings?.cond_time_stop_type
    def stopTime = settings?.cond_time_stop ? fmtTime(settings?.cond_time_stop) : null
    String startLbl = (
        (startType in ["Sunset", "Sunrise"]) ?
        ((startType == "Sunset") ? sunsetTime : sunriseTime) :
        startTime
    )
    def stopLbl = (
        (stopType in ["Sunrise", "Sunset"]) ?
        ((stopType == "Sunset") ? sunsetTime : sunriseTime) :
        stopTime
    )
    return ((startLbl && startLbl != "") && (stopLbl && stopLbl != "")) ? "${addPre ? "Time Condition:\n" : ""}(${startLbl} - ${stopLbl})" : "tap to configure..."
}

String getInputToStringDesc(inpt, addSpace = null) {
    Integer cnt = 0
    String str = ""
    if(inpt) {
        inpt.sort().each { item ->
            cnt = cnt+1
            str += item ? (((cnt < 1) || (inpt?.size() > 1)) ? "\n      ${item}" : "${addSpace ? "      " : ""}${item}") : ""
        }
    }
    //log.debug "str: $str"
    return (str != "") ? "${str}" : null
}

String randomString(Integer len) {
    def pool = ["a".."z",0..9].flatten()
    Random rand = new Random(new Date().getTime())
    def randChars = (0..len).collect { pool[rand.nextInt(pool.size())] }
    // logDebug("randomString: ${randChars?.join()}")
    return randChars.join()
}

def getRandomItem(items) {
    def list = new ArrayList<String>();
    items?.each { list?.add(it) }
    return list?.get(new Random().nextInt(list?.size()));
}

private getPlatform() {
    def p = "SmartThings"
    if(state?.hubPlatform == null) {
        try { [dummy: "dummyVal"]?.encodeAsJson(); } catch (e) { p = "Hubitat" }
        // p = (location?.hubs[0]?.id?.toString()?.length() > 5) ? "SmartThings" : "Hubitat"
        state?.hubPlatform = p
        // logDebug("hubPlatform: (${state?.hubPlatform})")
    }
    return state?.hubPlatform
}

Boolean showChgLogOk() { return (state?.isInstalled && state?.shownChgLog != true) }
String getAppImg(String imgName, frc=false) { return (frc || isST()) ? "https://raw.githubusercontent.com/tonesto7/echo-speaks/${isBeta() ? "beta" : "master"}/resources/icons/${imgName}.png" : "" }
String getPublicImg(String imgName) { return isST() ? "https://raw.githubusercontent.com/tonesto7/SmartThings-tonesto7-public/master/resources/icons/${imgName}.png" : "" }
String sTS(String t, String i = null) { return isST() ? t : """<h3>${i ? """<img src="${i}" width="42"> """ : ""} ${t?.replaceAll("\\n", "<br>")}</h3>""" }
String pTS(String t, String i = null, bold=true, color=null) { return isST() ? t : "${color ? """<div style="color: $color;">""" : ""}${bold ? "<b>" : ""}${i ? """<img src="${i}" width="42"> """ : ""}${t?.replaceAll("\\n", "<br>")}${bold ? "</b>" : ""}${color ? "</div>" : ""}" }
String inTS(String t, String i = null, color=null) { return isST() ? t : """${color ? """<div style="color: $color;">""" : ""}${i ? """<img src="${i}" width="42"> """ : ""} <u>${t?.replaceAll("\\n", " ")}</u>${color ? "</div>" : ""}""" }

String bulletItem(String inStr, String strVal) { return "${inStr == "" ? "" : "\n"} \u2022 ${strVal}" }
String dashItem(String inStr, String strVal, newLine=false) { return "${(inStr == "" && !newLine) ? "" : "\n"} - ${strVal}" }

Integer stateSize() {
    def j = new groovy.json.JsonOutput().toJson(state)
    return j?.toString().length()
}
Integer stateSizePerc() { return (int) ((stateSize() / 100000)*100).toDouble().round(0) }
String debugStatus() { return !settings?.appDebug ? "Off" : "On" }
String deviceDebugStatus() { return !settings?.childDebug ? "Off" : "On" }
Boolean isAppDebug() { return (settings?.appDebug == true) }
Boolean isChildDebug() { return (settings?.childDebug == true) }

String getAppDebugDesc() {
    def str = ""
    str += isAppDebug() ? "App Debug: (${debugStatus()})" : ""
    str += isChildDebug() && str != "" ? "\n" : ""
    str += isChildDebug() ? "Device Debug: (${deviceDebugStatus()})" : ""
    return (str != "") ? "${str}" : null
}

private addToLogHistory(String logKey, msg, Integer max=10) {
    Boolean ssOk = (stateSizePerc() <= 70)
    List eData = atomicState[logKey as String] ?: []
    if(eData?.find { it?.message == msg }) { return; }
    eData.push([dt: getDtNow(), message: msg])
    if(!ssOk || eData?.size() > max) { eData = eData?.drop( (eData?.size()-max) ) }
    atomicState[logKey as String] = eData
}
private logDebug(msg) { if(settings?.logDebug == true) { log.debug "Action (v${appVersion()}) | ${msg}" } }
private logInfo(msg) { if(settings?.logInfo != false) { log.info " Action (v${appVersion()}) | ${msg}" } }
private logTrace(msg) { if(settings?.logTrace == true) { log.trace "Action (v${appVersion()}) | ${msg}" } }
private logWarn(msg, noHist=false) { if(settings?.logWarn != false) { log.warn " Action (v${appVersion()}) | ${msg}"; }; if(!noHist) { addToLogHistory("warnHistory", msg, 15); } }
private logError(msg, noHist=false) { if(settings?.logError != false) { log.error "Action (v${appVersion()}) | ${msg}"; }; if(!noHist) { addToLogHistory("errorHistory", msg, 15); } }

Map getLogHistory() {
    return [ warnings: atomicState?.warnHistory ?: [], errors: atomicState?.errorHistory ?: [] ]
}
void clearLogHistory() {
    atomicState?.warnHistory = []
    atomicState?.errorHistory = []
}

String convMusicProvider(String prov) {
    switch (prov) {
        case "Amazon Music":
            return "AMAZON_MUSIC"
        case "Apple Music":
            return "APPLE_MUSIC"
        case "TuneIn":
            return "TUNEIN"
        case "Pandora":
            return "PANDORA"
        case "SiriusXM":
            return "SIRIUSXM"
        case "Spotify":
            return "SPOTIFY"
        case "Tidal":
            return "TIDAL"
        case "iHeartRadio":
            return "I_HEART_RADIO"
    }
}

/************************************************
            SEQUENCE TEST LOGIC
*************************************************/

Map seqItemsAvail() {
    return [
        other: [
            "weather":null, "traffic":null, "flashbriefing":null, "goodmorning":null, "goodnight":null, "cleanup":null,
            "singasong":null, "tellstory":null, "funfact":null, "joke":null, "playsearch":null, "calendartoday":null,
            "calendartomorrow":null, "calendarnext":null, "stop":null, "stopalldevices":null,
            "sound": "name",
            "wait": "value (seconds)", "volume": "value (0-100)", "speak": "message", "announcement": "message",
            "announcementall": "message", "pushnotification": "message", "email": null
        ],
        // dnd: [
        //     "dnd_duration": "2H30M", "dnd_time": "00:30", "dnd_all_duration": "2H30M", "dnd_all_time": "00:30",
        //     "dnd_duration":"2H30M", "dnd_time":"00:30"
        // ],
        speech: [
            "cannedtts_random": ["goodbye", "confirmations", "goodmorning", "compliments", "birthday", "goodnight", "iamhome"]
        ],
        music: [
            "amazonmusic": "search term", "applemusic": "search term", "iheartradio": "search term", "pandora": "search term",
            "spotify": "search term", "tunein": "search term", "cloudplayer": "search term"
        ]
    ]
}

def searchTuneInResultsPage() {
    return dynamicPage(name: "searchTuneInResultsPage", uninstall: false, install: false) {
        def results = executeTuneInSearch()
        Boolean onST = isST()
        section(sTS("Search Results: (Query: ${settings?.tuneinSearchQuery})")) {
            if(results?.browseList && results?.browseList?.size()) {
                results?.browseList?.eachWithIndex { item, i->
                    if(i < 25) {
                        if(item?.browseList != null && item?.browseList?.size()) {
                            item?.browseList?.eachWithIndex { item2, i2->
                                String str = ""
                                str += "ContentType: (${item2?.contentType})"
                                str += "\nId: (${item2?.id})"
                                str += "\nDescription: ${item2?.description}"
                                if(onST) {
                                    paragraph title: pTS(item2?.name?.take(75), (onST ? null : item2?.image)), str, required: true, state: (!item2?.name?.contains("Not Supported") ? "complete" : null), image: item2?.image ?: ""
                                } else { href "searchTuneInResultsPage", title: pTS(item2?.name?.take(75), (onST ? null : item2?.image)), description: str, required: true, state: (!item2?.name?.contains("Not Supported") ? "complete" : null), image: onST && item2?.image ? item2?.image : null }
                            }
                        } else {
                            String str = ""
                            str += "ContentType: (${item?.contentType})"
                            str += "\nId: (${item?.id})"
                            str += "\nDescription: ${item?.description}"
                            if(onST) {
                                paragraph title: pTS(item?.name?.take(75), (onST ? null : item?.image)), str, required: true, state: (!item?.name?.contains("Not Supported") ? "complete" : null), image: item?.image ?: ""
                            } else { href "searchTuneInResultsPage", title: pTS(item?.name?.take(75), (onST ? null : item?.image)), description: str, required: true, state: (!item?.name?.contains("Not Supported") ? "complete" : null), image: onST && item?.image ? item?.image : null }
                        }
                    }
                }
            } else { paragraph "No Results found..." }
        }
    }
}

private getColorName(desiredColor, level=null) {
    for (color in fillColorSettings()) {
        if (color?.name?.toLowerCase() == desiredColor?.toLowerCase()) {
            int hue = Math.round(color?.h / 3.6)
            level = level ?: color?.l
            return [hue: hue, saturation: color?.s, level: level]
        }
    }
}

private captureLightState(devs) {
    Map sMap = [:]
    if(devs) {
        devs?.each { dev->
            sMap[dev?.id] = [:]
            if(dev?.hasAttribute("switch")) { sMap[dev?.id]?.switch = dev?.currentSwitch }
            if(dev?.hasAttribute("level")) { sMap[dev?.id]?.level = dev?.currentLevel }
            if(dev?.hasAttribute("hue")) { sMap[dev?.id]?.hue = dev?.currentHue }
            if(dev?.hasAttribute("saturation")) { sMap[dev?.id]?.saturation = dev?.currentSaturation }
            if(dev?.hasAttribute("colorTemperature")) { sMap[dev?.id]?.colorTemperature = dev?.currentColorTemperature }
            if(dev?.hasAttribute("color")) { sMap[dev?.id]?.color = dev?.currentColor }
        }
    }
    atomicState?.light_restore_map = sMap
    log.debug "sMap: $sMap"
}

private restoreLightState(devs) {
    Map sMap = [:]
    if(sDevs && sMap?.size()) {
        sDevs?.each { dev->
            if(sMap?.containsKey(dev?.id)) {
                if(sMap?.level) {
                    if(sMap?.saturation && sMap?.hue) { dev?.setColor([h: sMap?.hue, s: sMap?.saturation, l: sMap?.level]) }
                    else { dev?.setLevel(sMap?.level) }
                }
                if(sMap?.colorTemperature) { dev?.setColorTemperature(sMap?.colorTemperature) }
                if(sMap?.switch) { dev?."${sMap?.switch}"() }
            }
        }
    }
    state?.remove("light_restore_map")
}


def fillColorSettings() {
    return [
        [name: "Soft White", rgb: "#B6DA7C", h: 83, s: 44, l: 67],              [name: "Warm White", rgb: "#DAF17E",	h: 51, s: 20, l: 100],      [name: "Very Warm White", rgb: "#DAF17E", h: 51, s: 60, l: 51],
        [name: "Daylight White", rgb: "#CEF4FD", h: 191, s: 9, l: 90],          [name: "Daylight", rgb: "#CEF4FD", h: 191, s: 9, l: 90],            [name: "Cool White", rgb: "#F3F6F7", h: 187, s: 19, l: 96],
        [name: "White", rgb: "#FFFFFF", h: 0, s: 0, l: 100],                    [name: "Alice Blue", rgb: "#F0F8FF", h: 208, s: 100, l: 97],        [name: "Antique White", rgb: "#FAEBD7", h: 34, s: 78, l: 91],
        [name: "Aqua", rgb: "#00FFFF", h: 180, s: 100, l: 50],                  [name: "Aquamarine", rgb: "#7FFFD4", h: 160, s: 100, l: 75],        [name: "Azure", rgb: "#F0FFFF", h: 180, s: 100, l: 97],
        [name: "Beige", rgb: "#F5F5DC", h: 60, s: 56, l: 91],                   [name: "Bisque", rgb: "#FFE4C4", h: 33, s: 100, l: 88],             [name: "Blanched Almond", rgb: "#FFEBCD", h: 36, s: 100, l: 90],
        [name: "Blue", rgb: "#0000FF", h: 240, s: 100, l: 50],                  [name: "Blue Violet", rgb: "#8A2BE2", h: 271, s: 76, l: 53],        [name: "Brown", rgb: "#A52A2A", h: 0, s: 59, l: 41],
        [name: "Burly Wood", rgb: "#DEB887", h: 34, s: 57, l: 70],              [name: "Cadet Blue", rgb: "#5F9EA0", h: 182, s: 25, l: 50],         [name: "Chartreuse", rgb: "#7FFF00", h: 90, s: 100, l: 50],
        [name: "Chocolate", rgb: "#D2691E", h: 25, s: 75, l: 47],               [name: "Coral", rgb: "#FF7F50", h: 16, s: 100, l: 66],              [name: "Corn Flower Blue", rgb: "#6495ED", h: 219, s: 79, l: 66],
        [name: "Corn Silk", rgb: "#FFF8DC", h: 48, s: 100, l: 93],              [name: "Crimson", rgb: "#DC143C", h: 348, s: 83, l: 58],            [name: "Cyan", rgb: "#00FFFF", h: 180, s: 100, l: 50],
        [name: "Dark Blue", rgb: "#00008B", h: 240, s: 100, l: 27],             [name: "Dark Cyan", rgb: "#008B8B", h: 180, s: 100, l: 27],         [name: "Dark Golden Rod", rgb: "#B8860B", h: 43, s: 89, l: 38],
        [name: "Dark Gray", rgb: "#A9A9A9", h: 0, s: 0, l: 66],                 [name: "Dark Green", rgb: "#006400", h: 120, s: 100, l: 20],        [name: "Dark Khaki", rgb: "#BDB76B", h: 56, s: 38, l: 58],
        [name: "Dark Magenta", rgb: "#8B008B", h: 300, s: 100, l: 27],          [name: "Dark Olive Green", 	rgb: "#556B2F", h: 82, s: 39, l: 30],   [name: "Dark Orange", rgb: "#FF8C00", h: 33, s: 100, l: 50],
        [name: "Dark Orchid", rgb: "#9932CC", h: 280, s: 61, l: 50],            [name: "Dark Red", rgb: "#8B0000", h: 0, s: 100, l: 27],            [name: "Dark Salmon", rgb: "#E9967A", h: 15, s: 72, l: 70],
        [name: "Dark Sea Green", rgb: "#8FBC8F", h: 120, s: 25, l: 65],         [name: "Dark Slate Blue", rgb: "#483D8B", h: 248, s: 39, l: 39],    [name: "Dark Slate Gray", rgb: "#2F4F4F", h: 180, s: 25, l: 25],
        [name: "Dark Turquoise", rgb: "#00CED1", h: 181, s: 100, l: 41],        [name: "Dark Violet", rgb: "#9400D3", h: 282, s: 100, l: 41],       [name: "Deep Pink", rgb: "#FF1493", h: 328, s: 100, l: 54],
        [name: "Deep Sky Blue", rgb: "#00BFFF", h: 195, s: 100, l: 50],         [name: "Dim Gray", rgb: "#696969", h: 0, s: 0, l: 41],              [name: "Dodger Blue", rgb: "#1E90FF", h: 210, s: 100, l: 56],
        [name: "Fire Brick", rgb: "#B22222", h: 0, s: 68, l: 42],               [name: "Floral White", rgb: "#FFFAF0", h: 40, s: 100, l: 97],       [name: "Forest Green", rgb: "#228B22", h: 120, s: 61, l: 34],
        [name: "Fuchsia", rgb: "#FF00FF", h: 300, s: 100, l: 50],               [name: "Gainsboro", rgb: "#DCDCDC", h: 0, s: 0, l: 86],             [name: "Ghost White", rgb: "#F8F8FF", h: 240, s: 100, l: 99],
        [name: "Gold", rgb: "#FFD700", h: 51, s: 100, l: 50],                   [name: "Golden Rod", rgb: "#DAA520", h: 43, s: 74, l: 49],          [name: "Gray", rgb: "#808080", h: 0, s: 0, l: 50],
        [name: "Green", rgb: "#008000", h: 120, s: 100, l: 25],                 [name: "Green Yellow", rgb: "#ADFF2F", h: 84, s: 100, l: 59],       [name: "Honeydew", rgb: "#F0FFF0", h: 120, s: 100, l: 97],
        [name: "Hot Pink", rgb: "#FF69B4", h: 330, s: 100, l: 71],              [name: "Indian Red", rgb: "#CD5C5C", h: 0, s: 53, l: 58],           [name: "Indigo", rgb: "#4B0082", h: 275, s: 100, l: 25],
        [name: "Ivory", rgb: "#FFFFF0", h: 60, s: 100, l: 97],                  [name: "Khaki", rgb: "#F0E68C", h: 54, s: 77, l: 75],               [name: "Lavender", rgb: "#E6E6FA", h: 240, s: 67, l: 94],
        [name: "Lavender Blush", rgb: "#FFF0F5", h: 340, s: 100, l: 97],        [name: "Lawn Green", rgb: "#7CFC00", h: 90, s: 100, l: 49],         [name: "Lemon Chiffon", rgb: "#FFFACD", h: 54, s: 100, l: 90],
        [name: "Light Blue", rgb: "#ADD8E6", h: 195, s: 53, l: 79],             [name: "Light Coral", rgb: "#F08080", h: 0, s: 79, l: 72],          [name: "Light Cyan", rgb: "#E0FFFF", h: 180, s: 100, l: 94],
        [name: "Light Golden Rod Yellow", rgb: "#FAFAD2", h: 60, s: 80, l: 90], [name: "Light Gray", rgb: "#D3D3D3", h: 0, s: 0, l: 83],            [name: "Light Green", rgb: "#90EE90", h: 120, s: 73, l: 75],
        [name: "Light Pink", rgb: "#FFB6C1", h: 351, s: 100, l: 86],            [name: "Light Salmon", rgb: "#FFA07A", h: 17, s: 100, l: 74],       [name: "Light Sea Green", 	rgb: "#20B2AA", h: 177, s: 70, l: 41],
        [name: "Light Sky Blue", 	rgb: "#87CEFA", h: 203, s: 92, l: 75],      [name: "Light Slate Gray", 	rgb: "#778899", h: 210, s: 14, l: 53],  [name: "Light Steel Blue", 	rgb: "#B0C4DE", h: 214, s: 41, l: 78],
        [name: "Light Yellow", rgb: "#FFFFE0", h: 60, s: 100, l: 94],           [name: "Lime", rgb: "#00FF00", h: 120, s: 100, l: 50],              [name: "Lime Green", rgb: "#32CD32", h: 120, s: 61, l: 50],
        [name: "Linen", rgb: "#FAF0E6", h: 30, s: 67, l: 94],                   [name: "Maroon", rgb: "#800000", h: 0, s: 100, l: 25],              [name: "Medium Aquamarine", rgb: "#66CDAA", h: 160, s: 51, l: 60],
        [name: "Medium Blue", rgb: "#0000CD", h: 240, s: 100, l: 40],           [name: "Medium Orchid", rgb: "#BA55D3", h: 288, s: 59, l: 58],      [name: "Medium Purple", rgb: "#9370DB", h: 260, s: 60, l: 65],
        [name: "Medium Sea Green", 	rgb: "#3CB371", h: 147, s: 50, l: 47],      [name: "Medium Slate Blue", rgb: "#7B68EE", h: 249, s: 80, l: 67],  [name: "Medium Spring Green", rgb: "#00FA9A", h: 157, s: 100, l: 49],
        [name: "Medium Turquoise", 	rgb: "#48D1CC", h: 178, s: 60, l: 55],      [name: "Medium Violet Red", rgb: "#C71585", h: 322, s: 81, l: 43],  [name: "Midnight Blue", rgb: "#191970", h: 240, s: 64, l: 27],
        [name: "Mint Cream", rgb: "#F5FFFA", h: 150, s: 100, l: 98],            [name: "Misty Rose", rgb: "#FFE4E1", h: 6, s: 100, l: 94],          [name: "Moccasin", rgb: "#FFE4B5", h: 38, s: 100, l: 85],
        [name: "Navajo White", rgb: "#FFDEAD", h: 36, s: 100, l: 84],           [name: "Navy", rgb: "#000080", h: 240, s: 100, l: 25],              [name: "Old Lace", rgb: "#FDF5E6", h: 39, s: 85, l: 95],
        [name: "Olive", rgb: "#808000", h: 60, s: 100, l: 25],                  [name: "Olive Drab", rgb: "#6B8E23", h: 80, s: 60, l: 35],          [name: "Orange", rgb: "#FFA500", h: 39, s: 100, l: 50],
        [name: "Orange Red", rgb: "#FF4500", h: 16, s: 100, l: 50],             [name: "Orchid", rgb: "#DA70D6", h: 302, s: 59, l: 65],             [name: "Pale Golden Rod", rgb: "#EEE8AA", h: 55, s: 67, l: 80],
        [name: "Pale Green", rgb: "#98FB98", h: 120, s: 93, l: 79],             [name: "Pale Turquoise", rgb: "#AFEEEE", h: 180, s: 65, l: 81],     [name: "Pale Violet Red", rgb: "#DB7093", h: 340, s: 60, l: 65],
        [name: "Papaya Whip", rgb: "#FFEFD5", h: 37, s: 100, l: 92],            [name: "Peach Puff", rgb: "#FFDAB9", h: 28, s: 100, l: 86],         [name: "Peru", rgb: "#CD853F", h: 30, s: 59, l: 53],
        [name: "Pink", rgb: "#FFC0CB", h: 350, s: 100, l: 88],                  [name: "Plum", rgb: "#DDA0DD", h: 300, s: 47, l: 75],               [name: "Powder Blue", rgb: "#B0E0E6", h: 187, s: 52, l: 80],
        [name: "Purple", rgb: "#800080", h: 300, s: 100, l: 25],                [name: "Red", rgb: "#FF0000", h: 0, s: 100, l: 50],                 [name: "Rosy Brown", rgb: "#BC8F8F", h: 0, s: 25, l: 65],
        [name: "Royal Blue", rgb: "#4169E1", h: 225, s: 73, l: 57],             [name: "Saddle Brown", rgb: "#8B4513", h: 25, s: 76, l: 31],        [name: "Salmon", rgb: "#FA8072", h: 6, s: 93, l: 71],
        [name: "Sandy Brown", rgb: "#F4A460", h: 28, s: 87, l: 67],             [name: "Sea Green", rgb: "#2E8B57", h: 146, s: 50, l: 36],          [name: "Sea Shell", rgb: "#FFF5EE", h: 25, s: 100, l: 97],
        [name: "Sienna", rgb: "#A0522D", h: 19, s: 56, l: 40],                  [name: "Silver", rgb: "#C0C0C0", h: 0, s: 0, l: 75],                [name: "Sky Blue", rgb: "#87CEEB", h: 197, s: 71, l: 73],
        [name: "Slate Blue", rgb: "#6A5ACD", h: 248, s: 53, l: 58],             [name: "Slate Gray", rgb: "#708090", h: 210, s: 13, l: 50],         [name: "Snow", rgb: "#FFFAFA", h: 0, s: 100, l: 99],
        [name: "Spring Green", rgb: "#00FF7F", h: 150, s: 100, l: 50],          [name: "Steel Blue", rgb: "#4682B4", h: 207, s: 44, l: 49],         [name: "Tan", rgb: "#D2B48C", h: 34, s: 44, l: 69],
        [name: "Teal", rgb: "#008080", h: 180, s: 100, l: 25],                  [name: "Thistle", rgb: "#D8BFD8", h: 300, s: 24, l: 80],            [name: "Tomato", rgb: "#FF6347", h: 9, s: 100, l: 64],
        [name: "Turquoise", rgb: "#40E0D0", h: 174, s: 72, l: 56],              [name: "Violet", rgb: "#EE82EE", h: 300, s: 76, l: 72],             [name: "Wheat", rgb: "#F5DEB3", h: 39, s: 77, l: 83],
        [name: "White Smoke", rgb: "#F5F5F5", h: 0, s: 0, l: 96],               [name: "Yellow", rgb: "#FFFF00", h: 60, s: 100, l: 50],             [name: "Yellow Green", rgb: "#9ACD32", h: 80, s: 61, l: 50]
    ]
}

//*******************************************************************
//    CLONE CHILD LOGIC
//*******************************************************************
public getDuplSettingData() {
    Map typeObj = [
        stat: [
            bool: ["notif_pushover", "notif_alexa_mobile", "logInfo", "logWarn", "logError", "logDebug", "logTrace", "enableWebCoRE"],
            enum: ["triggerEvents", "act_EchoDevices", "act_EchoZones", "zone_EchoDevices", "actionType", "cond_alarm", "cond_months", "cond_days", "notif_pushover_devices", "notif_pushover_priority", "notif_pushover_sound", "trig_alarm", "trig_guard", "webCorePistons"],
            mode: ["cond_mode", "trig_mode"],
            number: [],
            text: ["appLbl"]
        ],
        ends: [
            bool: ["_all", "_avg", "_once", "_send_push", "_use_custom", "_stop_on_clear", "_db"],
            enum: ["_cmd", "_type", "_time_start_type", "cond_time_stop_type", "_routineExecuted", "_scheduled_sunState", "_scheduled_recurrence", "_scheduled_days", "_scheduled_weeks", "_scheduled_months", "_scheduled_daynums", "_scheduled_type", "_routine_run", "_mode_run", "_webCorePistons", "_rt", "_rt_wd", "_nums"],
            number: ["_wait", "_low", "_high", "_equal", "_delay", "_cnt", "_volume", "_scheduled_sunState_offset", "_after", "_after_repeat", "_rt_ed", "_volume_change", "_volume_restore"],
            text: ["_txt", "_sms_numbers"],
            time: ["_time_start", "_time_stop", "_scheduled_time"]
        ],
        caps: [
            _acceleration: "accelerationSensor",
            _battery: "battery",
            _contact: "contactSensor",
            _door: "garageDoorControl",
            _temperature: "temperatureMeasurement",
            _illuminance: "illuminanceMeasurement",
            _humidity: "relativeHumidityMeasurement",
            _motion: "motionSensor",
            _level: "switchLevel",
            _button: "button",
            _pushed: "pushableButton",
            _held: "holdableButton",
            _released: "releasableButton",
            _doubleTapped: "doubleTapableButton",
            _presence: "presenceSensor",
            _switch: "switch",
            _power: "powerMeter",
            _shade: "windowShades",
            _water: "waterSensor",
            _valve: "valve",
            _thermostat: "thermostat",
            _carbonMonoxide: "carbonMonoxideDetector",
            _smoke: "smokeDetector",
            _lock: "lock",
            _lock_code: "lock",
            _switches_off: "switch",
            _switches_on: "switch",
            _lights: "level",
            _color: "colorControl"
        ],
        dev: [
            _scene: "sceneActivator"
        ]
    ]
    Map setObjs = [:]
    typeObj?.stat?.each { sk,sv->
        sv?.each { svi-> if(settings?.containsKey(svi)) { setObjs[svi] = [type: sk as String, value: settings[svi] ] } }
    }
    typeObj?.ends?.each { ek,ev->
        ev?.each { evi-> settings?.findAll { it?.key?.endsWith(evi) }?.each { fk, fv-> setObjs[fk] = [type: ek as String, value: fv] } }
    }
    typeObj?.caps?.each { ck,cv->
        settings?.findAll { it?.key?.endsWith(ck) }?.each { fk, fv-> setObjs[fk] = [type: "capability.${cv}" as String, value: fv?.collect { it?.id as String }] }
    }
    typeObj?.dev?.each { dk,dv->
        settings?.findAll { it?.key?.endsWith(dk) }?.each { fk, fv-> setObjs[fk] = [type: "device.${dv}" as String, value: fv] }
    }
    Map data = [:]
    data?.label = app?.getLabel()?.toString()?.replace(" (A \u275A\u275A)", "")
    data?.settings = setObjs
    return data
}

public getDuplStateData() {
    List stskip = ["isInstalled", "isParent", "lastNotifMsgDt", "lastNotificationMsg", "setupComplete", "valEvtHistory", "warnHistory", "errorHistory"]
    return state?.findAll { !(it?.key in stskip) }
}
