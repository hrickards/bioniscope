/* ****************************************************************************
   ADC.c
***************************************************************************** */
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
  
  PORTC = 0x00;

  // INT input
  // DDRB &= ~INT;
}

// Operating in pipelined mode
Byte ADCSample(char address) {
  // Set address of the ADC
  if (address == 0xFF) {
    PORTB |= ADDR;
  } else {
    PORTB &= ~ADDR;
  }

  // Pull CS, RD and WR low
  PORTB &= ~CS & ~RD & ~WR;

  // Small delay to let the ADC gather the data
  // 0.25 to 10uS
  // Minus 2 clock cycles for the IO lines
  // Gives a minimum of approx 0.1uS
  _delay_us(0.1);

  // Pull CS, RD and WR back high
  PORTB |= CS | RD | WR;

  // Tintl + Tid = max 545 ns
  // _delay_us(1);

  // Take the sample
  return PINC;
}
