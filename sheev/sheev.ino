#include <AFMotor.h>
#include "Wire.h"

#define ADDR_SLAVE 0x08 // 7-bit slave address=ID+0x08 ID=0
#define REGISTER_TOTAL_SIZE 48 // total length of all registers
#define TOF_ADDR_DIS 0x24 // distance variable address

#define STOP_DIST 200 // allowed min distance to obstacle (in mm)

// setup motors
AF_DCMotor m1(1, MOTOR12_1KHZ);
AF_DCMotor m2(2, MOTOR12_1KHZ);
AF_DCMotor m3(3, MOTOR34_1KHZ);
AF_DCMotor m4(4, MOTOR34_1KHZ);

enum DriveDirection {
    FW, // forward
    BW, // backward
    RT, // right turn
    LT, // left turn
    HL // halt
};

DriveDirection currDriveDir = HL;
long rn;

void setup(){
  Serial.begin(115200);

  m1.setSpeed(100);
  m2.setSpeed(100);
  m3.setSpeed(100);
  m4.setSpeed(100);
  
  Wire.begin();
  Wire.setClock(400000);
}

void loop(){
  uint32_t dist = I2C_read_distance();

  // TODO better code for automaton
  if(dist > STOP_DIST) {
    Serial.println("FW");
    if (currDriveDir != FW) {
      move(FW);
    }
  } else {
    move(HL);
    delay(1000);
    rn = random(2);
    // randomly decide wether to turn right or left
    if (rn == 1) {
      move(RT);
    } else {
      move(LT);
    }
    delay(1000);
  }

  delay(20);
}

void move(DriveDirection direction) {
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

    Serial.print("TOF distance is:");       Serial.println(dist);
    return dist;
}