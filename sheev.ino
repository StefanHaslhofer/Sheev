#include <ESP8266WiFi.h>

#define DOOR_PIN  D7  // The ESP8266 pin D7 connected to door sensor's pin

#define DEBUG_MSG false

const char* ssid     = "SSID";         // The SSID (name) of the Wi-Fi network you want to connect to
const char* password = "PASSWORD";

int door_state; // current state of door sensor
int prev_door_state;    // previous state of door sensor

void setup() {
  Serial.begin(9600);                     // Initialize the Serial to communicate with the Serial Monitor.
  pinMode(DOOR_PIN, INPUT_PULLUP); // Configure the ESP8266 pin to the input pull-up mode

  door_state = digitalRead(DOOR_PIN); // read state
}

void loop() {
  prev_door_state = door_state;              // save the last state
  door_state  = digitalRead(DOOR_PIN); // read new state

  if (prev_door_state == LOW && door_state == HIGH) { // state change: LOW -> HIGH
    #if DEBUG_MSG
      Serial.println("The door-opening has been detected");  
    #endif
    // TODO: turn on alarm, light or send notification ...
  }
  else
  if (prev_door_state == HIGH && door_state == LOW) { // state change: HIGH -> LOW
    #if DEBUG_MSG
      Serial.println("The door-closing has been detected");
    #endif
    // TODO: turn off alarm, light or send notification ...
  }
}
