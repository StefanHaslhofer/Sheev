#include <AFMotor.h>
#include "Wire.h"

#define ADDR_SLAVE 0x08 // 7-bit slave address=ID+0x08 ID=0
#define REGISTER_TOTAL_SIZE 48 // total length of all registers
#define TOF_ADDR_DIS 0x24 // distance variable address

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
  m1.run(FORWARD);

  uint32_t dist = I2C_read_distance();
  delay(2000);
}

/**
 * read bytes via I2C to a buffer
 */ 
void read_bytes(uint8_t Cmd, uint8_t *pdata, uint8_t len){
  uint8_t num = 0;
  Wire.beginTransmission(ADDR_SLAVE); // set the slave address and start I2C communication
  Wire.write(Cmd);                    // write register
  Wire.endTransmission(); // stop I2C communication

  // specify the corresponding number of slaves to read
  Wire.requestFrom((uint8_t) ADDR_SLAVE, len);
  while(len--)
  {
    // read data
    pdata[num++] = Wire.read(); 
  }

  Serial.println(pdata[0]);
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