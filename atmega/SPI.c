#include "Global.h"
#include <avr/io.h>
#include <util/delay.h>

// Bit-bangs SPI
// Master -> slave communication only at this point as that's all that's
// needed for the digital pots
// Controls two slaves

#define SCLK _BV(PD5)
#define DATA _BV(PD4)
#define SS1 _BV(PD6)
#define SS2 _BV(PD7)

// Setup inputs/outputs for SPI
void SPISetup(void) {
  // Set DATA, SCLK and SS output
  DDRD |= DATA | SCLK | SS1 | SS2;

  // Output SS high (active low) and SCLK low (SPI mode 11)
  PORTD |= SS1 | SS2;
  PORTD &= ~SCLK;
}

// Write a single byte of data. Doesn't control SS.
static void SPIWriteByte(Byte data) {
  unsigned char i;
  for (i = 0; i < 8; i++) {
    // Output DATA based on MSB of data
    if (data & 0x80) {
	  PORTD |= DATA;
    } else {
	  PORTD &= DATA;
    }

    // Cycle clock
    PORTD |= SCLK;
    _delay_us(20);
    PORTD &= ~SCLK;

    // Bit-shift data so we look at the next MSB next
    data <<= 1;
  }
}

// Write an SPI command (consisting of control byte then data byte)
void SPIWriteCommand(Byte control, Byte data, Byte address) {
  // Pull SS low
  if (address) { PORTD &= ~SS2; }
  else { PORTD &= ~SS1; }

  SPIWriteByte(control);
  SPIWriteByte(data);

  // Pull SS back high
  if (address) { PORTD |= SS2; }
  else { PORTD |= SS1; }
}
