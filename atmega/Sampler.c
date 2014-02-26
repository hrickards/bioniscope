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

// TODO Better scaling
void delay_us(int num) {
  int i = 0;
  for (i = 0; i < num; i++) {
    _delay_us(1);
  }
}

// TODO Faster for small time delays
void SamplerSample(void) {
  int i = 0;
  // Take samples until the signal rises from below the threshold to above the threshold
  // Take up to 10*NUM_SAMPLES samples

  // Rising edge trigger
  if (DigitalTriggerType == 0x00) {
    Byte previousSample = 0xFF;
    Byte currentSample = 0x00;
    while(!(currentSample > DigitalTriggerThreshold && previousSample < DigitalTriggerThreshold) && i<10*NUM_SAMPLES) {
      previousSample = currentSample;
      currentSample = PINA;
      _delay_us(1);
      i++;
    }
  // Falling edge trigger
  } else if (DigitalTriggerType == 0x01) {
    Byte previousSample = 0x00;
    Byte currentSample = 0xFF;
    while(!(currentSample < DigitalTriggerThreshold && previousSample > DigitalTriggerThreshold) && i<10*NUM_SAMPLES) {
      previousSample = currentSample;
      currentSample = PINA;
      _delay_us(1);
      i++;
    }
  // Disable trigger
  } else {
  }

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
  for (i=0; i<NUM_SAMPLES; i++) {
    AnalogueSamplesA[i] = ADCSample(0x00);
    AnalogueSamplesB[i] = ADCSample(0xFF);
    delay_us(AnalogueTimeDelay);
  }
}
