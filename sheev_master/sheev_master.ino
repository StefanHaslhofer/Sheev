#include <ESP8266WiFi.h>
#include <WiFiUdp.h>

#define DOOR_PIN D7  // pin connected to door sensor's pin

#define DEBUG_MSG true

const char* ssid     = "Des Kaisers Richtfunk";
const char* password = "GottbewahredenKaiser";

const unsigned int NET_PORT = 58266;
WiFiUDP Udp;
IPAddress broadcastIP;

int door_state; // current state of door sensor
int prev_door_state;    // previous state of door sensor

void setup() {
  Serial.begin(9600);
  delay(10);
  pinMode(DOOR_PIN, INPUT_PULLUP);
  door_state = digitalRead(DOOR_PIN); // read state

  Serial.println('\n');
  
  WiFi.begin(ssid, password);             // connect to the network
  Serial.print("Connecting to ");
  Serial.print(ssid); Serial.print(" ...");

  int i = 0;
  while (WiFi.status() != WL_CONNECTED) { // wait for the Wi-Fi to connect
    delay(1000);
    Serial.print("...");
  }
  Serial.println(" connection established!");  
  Serial.print("IP address:\t");
  Serial.println(WiFi.localIP());

  // calculate last address in current subnet (= broadcast address) by bitwise operation
  broadcastIP = ~(uint32_t)WiFi.subnetMask() | (uint32_t)WiFi.gatewayIP();
  Serial.print("Broadcast IP: ");
  Serial.println(broadcastIP);
}

void loop() {
  prev_door_state = door_state; // save the last state
  door_state  = digitalRead(DOOR_PIN); // read new state

  if (prev_door_state == LOW && door_state == HIGH) { // state change: LOW -> HIGH
    #if DEBUG_MSG
      Serial.println("The door-opening has been detected");  
    #endif
    broadcast("opened");
  }
  else
  if (prev_door_state == HIGH && door_state == LOW) { // state change: HIGH -> LOW
    #if DEBUG_MSG
      Serial.println("The door-closing has been detected");
    #endif
    broadcast("closed");
  }

  delay(1000);
}

void broadcast(char msg[]){
  Udp.beginPacket(broadcastIP, NET_PORT);
  Udp.write(msg);
  Udp.endPacket();
}
