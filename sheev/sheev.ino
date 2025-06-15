#include "TOF_Sense.h"
#include <AFMotor.h>

// setup motors
AF_DCMotor m1(1, MOTOR12_1KHZ);
AF_DCMotor m2(2, MOTOR12_1KHZ);
AF_DCMotor m3(3, MOTOR34_1KHZ);
AF_DCMotor m4(4, MOTOR34_1KHZ);

void setup(){
  Serial.begin(115200);

  m1.setSpeed(200);
  m2.setSpeed(200);
  m3.setSpeed(200);
  m4.setSpeed(200);
  
  Wire.begin();
  Wire.setClock(400000);
}

void loop(){
  Serial.print("tick");
  motor.run(FORWARD); // Motor goes forward
  delay(1000);
  Serial.print("tock");
  motor.run(BACKWARD); // Motor goes backwards
  delay(1000);
  Serial.print("tack");
  motor.run(RELEASE); // Motor stops
  delay(1000);

  TOF_Inquire_I2C_Decoding();
  delay(20);
}