#include <avr/io.h>
#include <util/delay.h>
#include "Global.h"
#include "USBUSART.h"

#define WR    0x01<<0 // PB0
#define CS    0x01<<1 // PB1
#define INT   0x01<<2 // PB2
#define ADDR  0x01<<3 // PB3
#define RD    0x01<<4 // PB4

void ADCSetup(void) {
  // Ensure ADC data outputs are input pins
  DDRC = 0x00;

  // Output high on CS, RD, WR and ADDR
  DDRB |= CS | RD | WR | ADDR;
  PORTB |= CS | RD | WR | ADDR;

  // INT input
  // DDRB &= ~INT;
}

// Operating in pipelined mode
// TODO Operate in a faster mode
Byte ADCSample(void) {
  // Pull CS, RD and WR low
  PORTB &= ~CS & ~RD & ~WR & ~ADDR;

  // Small delay to let the ADC gather the data
  _delay_us(5);

  // Take the sample
  Byte sample = PINC;

  // Pull CS, RD and WR back high
  PORTB |= CS | RD | WR | ADDR;

  return sample;
}
