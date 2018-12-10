/**
 *  Lights, camera, Action !
 *
 *  Copyright 2018 Mark Hooper
 *
 */
definition(
    name: "Lights, camera, Action !",
    namespace: "h00pz",
    author: "Mark Hooper",
    description: "do the things",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("When there's movement...") {
		input "motion1", "capability.motionSensor", title: "Where?"
	}
	section("While I'm out...") {
		input "presence1", "capability.presenceSensor", title: "Who?", multiple: true
	}
    section("Turn on the lights and text me at...") {
        input "switch1", "capability.switch", required: true
		input("recipients", "contact", title: "Send notifications to") {
            input "phone1", "phone", title: "Phone number?", multiple: true
        }
    }
}

def installed() {
	subscribe(motion1, "motion.active", motionActiveHandler)
}

def updated() {
	unsubscribe()
	subscribe(motion1, "motion.active", motionActiveHandler)
}

def motionActiveHandler(evt) {
	log.trace "$evt.value: $evt, $settings"

	if (presence1.latestValue("presence") == "not present") {
		// Don't send a continuous stream of text messages
		def deltaSeconds = 10
		def timeAgo = new Date(now() - (1000 * deltaSeconds))
		def recentEvents = motion1.eventsSince(timeAgo)
		log.debug "Found ${recentEvents?.size() ?: 0} events in the last $deltaSeconds seconds"
		def alreadySentSms = recentEvents.count { it.value && it.value == "active" } > 1

		if (alreadySentSms) {
			log.debug "SMS already sent within the last $deltaSeconds seconds"
		} else {
            if (location.contactBookEnabled) {
                log.debug "$motion1 has moved while you were out, sending notifications to: ${recipients?.size()}"
                sendNotificationToContacts("${motion1.label} ${motion1.name} moved while you were out", recipients)
                switch1.on()
            }
            else {
                log.debug "$motion1 has moved while you were out, sending text"
                sendSms(phone1, "${motion1.label} ${motion1.name} moved while you were out")
                switch1.on()
            }
		}
	} else {
		log.debug "Motion detected, but presence sensor indicates you are present"
	}
}
