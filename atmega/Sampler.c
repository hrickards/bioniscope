#include <avr/io.h>
#include "Global.h"

// Digital samples
Byte DigitalSamples[NUM_SAMPLES];

void SamplerSetup(void) {
  // Ensure digital input pins are inputs
  DDRA = 0x00;
}

void SamplerSample(void) {
  int i;
  for (i=0; i<NUM_SAMPLES; i++) {
	DigitalSamples[i] = PINA;
  }
}
