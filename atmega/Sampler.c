#include <avr/io.h>
#include <util/delay.h>
#include "Global.h"
#include "ADC.h"

// Sample registers
Byte DigitalSamples[NUM_SAMPLES];
Byte AnalogueSamples[NUM_SAMPLES];

void SamplerSetup(void) {
  // Ensure digital input pins are inputs
  DDRA = 0x00;

  // Setup ADC
  ADCSetup();
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
  if (TimeDelay<2) {
    // Take digital samples just as inputs to PORTA
    for (i=0; i<NUM_SAMPLES; i++) {
      DigitalSamples[i] = PINA;
    }
  } else {
    // Take digital samples just as inputs to PORTA
    for (i=0; i<NUM_SAMPLES; i++) {
      DigitalSamples[i] = PINA;
      delay_us(TimeDelay);
    }
  }

  // Take analogue samples using the functions in ADC.c
  for (i=0; i<NUM_SAMPLES; i++) {
    AnalogueSamples[i] = ADCSample();
  }
}
