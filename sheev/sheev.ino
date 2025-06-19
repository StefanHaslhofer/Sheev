#include <AFMotor.h>
#include "Wire.h"
#include <Servo.h>

#define ADDR_SLAVE 0x08 // 7-bit slave address=ID+0x08 ID=0
#define REGISTER_TOTAL_SIZE 48 // total length of all registers
#define TOF_ADDR_DIS 0x24 // distance variable address

#define STOP_DIST 200 // allowed min distance to obstacle (in mm)
#define GO_DIST 250 // distance threshold needed to return to forward movement

#define MOTOR_SPEED 100
#define STANDARD_TRN_DUR 750 // turn duration in ms at MOTOR_SPEED = 100

#define DEBUG_MSG false

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
int turnDuration;

void setup(){
  Serial.begin(115200);

  // DC motor setup
  m1.setSpeed(MOTOR_SPEED);
  m2.setSpeed(MOTOR_SPEED);
  m3.setSpeed(MOTOR_SPEED);
  m4.setSpeed(MOTOR_SPEED);
  
  Wire.begin();
  Wire.setClock(400000);

  move(FW); // start in forward movement

  // servo setup
  servo1.write(SERVO_FW_POS);
  servo1.attach(10);

  turnDuration = 100/MOTOR_SPEED*STANDARD_TRN_DUR;
}

void loop(){
  drive_vehicle();

  delay(20);
}

/**
 * state management of vehicle driving direction
 */
void drive_vehicle() {
  uint32_t dist; // distance to obstacle

  while((dist = read_distance()) > STOP_DIST) {
    Serial.print("Move forward: "); Serial.println(dist);
    if (currDriveDir != FW) {
      move(FW);
    }
  }

  if (currDriveDir == FW) {
    move(HL); // halt vehicle
    delay(500);
    rn = random(10); // set random turn direction if forward movement stops 
  }

  while((dist = read_distance()) < GO_DIST) {
    delay(250);
    if (rn > 4) {
        move(RT);
        Serial.print("Turn right: "); Serial.println(dist);
    } else {
        move(LT);
        Serial.print("Turn left: "); Serial.println(dist);
    }
    delay(turnDuration);
    move(HL);
  }
}

/**
 * return distance to obstacle
 */
uint32_t read_distance() {
  uint32_t dist = 0;
  uint32_t minDist = 0;
  int pos = 0;

  // perform a forward sweep to determine the minimum distance within a wide angle range
  for (pos = SERVO_START_POS; pos <= SERVO_RANGE; pos += 1) {
    // in steps of 1 degree
    servo1.write(pos);              // tell servo to go to position
    delay(3);                       // waits 15ms for the servo to reach the position
  }

  for (pos = SERVO_RANGE; pos >= SERVO_START_POS; pos -= 1) {
    servo1.write(pos);
    delay(5);
    dist = I2C_read_distance();

    if (dist < GO_DIST || dist < STOP_DIST && currDriveDir != FW) {
      return dist;
    }

    if(minDist == 0 || dist < minDist) {
      minDist = dist;
    } 
  }

  return minDist;
}

/*
 * change motor directions based on driving action
 */
void move(DriveDirection direction) {
    #if DEBUG_MSG
      Serial.println(direction);
    #endif

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