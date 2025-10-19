#include <AFMotor.h>
#include "Wire.h"
#include <Servo.h>
#include <stdint.h>

#define ADDR_SLAVE 0x08 // 7-bit slave address=ID+0x08 ID=0
#define REGISTER_TOTAL_SIZE 48 // total length of all registers
#define TOF_ADDR_DIS 0x24 // distance variable address

#define STOP_DIST 200 // allowed min distance to obstacle (in mm)
#define GO_DIST 250 // distance threshold needed to return to forward movement

#define MOTOR_SPEED 125
#define TURN_DURATION 50

#define DEBUG_MSG true

#define SERVO_FW_POS 75
#define SERVO_START_POS 20
#define SERVO_RANGE 140

// setup motors
AF_DCMotor m1(1, MOTOR12_1KHZ);
AF_DCMotor m2(2, MOTOR12_1KHZ);
AF_DCMotor m3(3, MOTOR34_1KHZ);
AF_DCMotor m4(4, MOTOR34_1KHZ);
Servo servo1;

enum DriveDirection {
    FW, // forward
    BW, // backward
    RT, // right turn
    LT, // left turn
    HL // halt
};

DriveDirection currDriveDir = HL;
long rn;
int pos = 0;
uint32_t minDist = UINT32_MAX;

void setup(){
  Serial.begin(9600);

  // DC motor setup
  m1.setSpeed(MOTOR_SPEED);
  m2.setSpeed(MOTOR_SPEED);
  m3.setSpeed(MOTOR_SPEED);
  m4.setSpeed(MOTOR_SPEED);
  
  Wire.begin();
  Wire.setClock(400000);

  move(HL, 0); // start in forward movement

  // servo setup
  servo1.write(SERVO_FW_POS);
  servo1.attach(10);
}

void loop(){
  delay(2);
  char incomingByte;

  if (pos >= SERVO_RANGE) {
    minDist = UINT32_MAX;
    pos = SERVO_START_POS;
    servo1.write(pos);   // set servo to initial position
    delay(15);           // waits 15ms for the servo to reach the position
  }

  servo1.write(pos+=3);
  delay (5);

  while (Serial.available()) {  
    incomingByte = Serial.read();
  }

  drive_vehicle(incomingByte);
}

/**
 * state management of vehicle driving direction
 */
void drive_vehicle(char incomingByte) {
  uint32_t dist = I2C_read_distance(); // distance to obstacle

  if(dist > 0 && dist < minDist) {
    minDist = dist;
  }

  // TODO: Actively look for person when noone is insight -> currently out of scope 
  if (incomingByte == '3' && minDist > GO_DIST) {
    #if DEBUG_MSG
      Serial.print("Stop: "); Serial.println(dist);
    #endif
    move(HL, 0);
    return;
  }

  if(minDist <= STOP_DIST && currDriveDir == FW) {
    #if DEBUG_MSG
      Serial.print("Stop: "); Serial.println(dist);
    #endif
    move(HL, 0);
    delay(250);  
    rn = random(10); // set random turn direction if forward movement stops 
  }

  if(incomingByte == '0' || (rn <= 4 && minDist <= GO_DIST)) {
    #if DEBUG_MSG
      Serial.print("Turn left: "); Serial.println(dist);
    #endif
    move(LT, 160);
    delay(TURN_DURATION);

    if (minDist <= GO_DIST) {
      move(HL, 0);
    }

    return;
  }

  if(incomingByte == '1' && minDist > GO_DIST) {
    #if DEBUG_MSG
      Serial.print("Move forward: "); Serial.println(dist);
    #endif
    move(FW, 125);
    return;
  }

  if(incomingByte == '2' || (rn > 4 && minDist <= GO_DIST)) {
    #if DEBUG_MSG
      Serial.print("Turn right: "); Serial.println(dist);
    #endif
    move(RT, 160);
    delay(TURN_DURATION);

    if (minDist <= GO_DIST) {
      move(HL, 0);
    }

    return;
  }
}

/*
 * change motor directions based on driving action
 */
void move(DriveDirection direction, uint8_t speed) {
    m1.setSpeed(speed);
    m2.setSpeed(speed);
    m3.setSpeed(speed);
    m4.setSpeed(speed);

    currDriveDir = direction;

    switch (direction) {
        case FW:
            m1.run(FORWARD);
            m2.run(FORWARD);
            m3.run(FORWARD);
            m4.run(FORWARD);
            break;
        case BW:
            m1.run(BACKWARD);
            m2.run(BACKWARD);
            m3.run(BACKWARD);
            m4.run(BACKWARD);
            break;
        case RT:
            m1.run(FORWARD);
            m2.run(FORWARD);
            m3.run(BACKWARD);
            m4.run(BACKWARD);
            break;
        case LT:
            m1.run(BACKWARD);
            m2.run(BACKWARD);
            m3.run(FORWARD);
            m4.run(FORWARD);
            break;
        case HL:
            m1.run(RELEASE);
            m2.run(RELEASE);
            m3.run(RELEASE);
            m4.run(RELEASE);
            break;
    }
}

/**
 * read bytes via I2C to a buffer
 */ 
void read_bytes(uint8_t reg, uint8_t *pdata, uint8_t len){
  uint8_t num = 0;
  Wire.beginTransmission(ADDR_SLAVE); // set the slave address and start I2C communication
  Wire.write(reg);                    // register address
  Wire.endTransmission(); // stop I2C communication

  // specify the corresponding number of slaves to read
  Wire.requestFrom((uint8_t) ADDR_SLAVE, len);
  while(len--)
  {
    // read data
    pdata[num++] = Wire.read(); 
  }

  Wire.endTransmission(); // stop I2C communication
}

/**
 * read in distance variable from sensor
 */ 
uint32_t I2C_read_distance()
{
    // buffer for sensor data
    uint8_t read_buf[256];

    // UNO R3 cannot read all the data at once, so it reads the data twice
    read_bytes(0x00, read_buf, REGISTER_TOTAL_SIZE); // read first half of the sensor data
    read_bytes(REGISTER_TOTAL_SIZE/2, &read_buf[REGISTER_TOTAL_SIZE/2], REGISTER_TOTAL_SIZE/2); // read second half of the sensor data

    // distance output by the TOF module according to spec
    uint32_t dist = (unsigned long)(((unsigned long)read_buf[TOF_ADDR_DIS + 3] << 24) | ((unsigned long)read_buf[TOF_ADDR_DIS + 2] << 16) | 
                 ((unsigned long)read_buf[TOF_ADDR_DIS + 1] << 8) | (unsigned long)read_buf[TOF_ADDR_DIS]);

    return dist;
}