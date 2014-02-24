#include <avr/io.h>
#include <util/delay.h>
#include "Global.h"

// Digital samples
Byte DigitalSamples[NUM_SAMPLES];

void SamplerSetup(void) {
  // Ensure digital input pins are inputs
  DDRA = 0x00;
}

// TODO Better scaling
void delay_us(int num) {
  int i = 0;
  for (i = 0; i < num; i++) {
    _delay_us(1);
  }
}

// TODO Faster for small time delays
void SamplerSample(void) {
  int i;
  for (i=0; i<NUM_SAMPLES; i++) {
    DigitalSamples[i] = PINA;
    delay_us(TimeDelay);
  }
}
