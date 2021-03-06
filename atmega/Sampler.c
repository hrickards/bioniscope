/* ****************************************************************************
   Sampler.c
***************************************************************************** */
#include <avr/io.h>
#include <util/delay.h>
#include "Global.h"
#include "ADC.h"

// Sample registers
Byte DigitalSamples[NUM_SAMPLES];
Byte AnalogueSamplesA[NUM_SAMPLES];
Byte AnalogueSamplesB[NUM_SAMPLES];

void SamplerSetup(void) {
  // Ensure digital input pins are inputs
  DDRA = 0x00;

  // Setup ADC
  ADCSetup();
}

void delay_us(int num) {
  int i = 0;
  for (i = 0; i < num; i++) {
    _delay_us(1);
  }
}

void SamplerSample(void) {
  int i = 0;
  if (DigitalTimeDelay<2) {
    // Take digital samples just as inputs to PORTA
    for (i=0; i<NUM_SAMPLES; i++) {
      DigitalSamples[i] = PINA;
    }
  } else {
    // Take digital samples just as inputs to PORTA
    for (i=0; i<NUM_SAMPLES; i++) {
      DigitalSamples[i] = PINA;
      delay_us(DigitalTimeDelay);
    }
  }

  // Take analogue samples using the functions in ADC.c
  // The four different cases based upon the value of AnalogueSampleChannels
  
  // Channel A enabled based on bit 0
  Byte aEnabled = AnalogueSampleChannels & 0x01;
  // Channel B enabled based on bit 1
  Byte bEnabled = AnalogueSampleChannels & 0x02;

  // A enabled and B enabled
  if (aEnabled && bEnabled) {
    for (i=0; i<NUM_SAMPLES; i++) {
      AnalogueSamplesA[i] = ADCSample(0x00);
      AnalogueSamplesB[i] = ADCSample(0xFF);
      delay_us(AnalogueTimeDelay);
    }
  // A enabled and B not enabled
  } else if (aEnabled) {
    for (i=0; i<NUM_SAMPLES; i++) {
      AnalogueSamplesA[i] = ADCSample(0x00);
      delay_us(AnalogueTimeDelay);
    }
  // B enabled and A not enabled
  } else if (bEnabled) {
    for (i=0; i<NUM_SAMPLES; i++) {
      AnalogueSamplesB[i] = ADCSample(0xFF);
      delay_us(AnalogueTimeDelay);
    }
  // Error: neither channel enabled
  } else {
    for (i=0; i<NUM_SAMPLES; i++) {
      AnalogueSamplesA[i] = 0x00;
      AnalogueSamplesB[i] = 0xFF;
    }
  }
}
