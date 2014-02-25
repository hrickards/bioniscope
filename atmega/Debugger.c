#include "Global.h"
#include "USBUSART.h"
#include "Sampler.h"

// Setup debugging
void DebuggerSetup(void) {
  // Initialise USB USART
  USBUSARTInit();
}

// Called repeatedly to do some form of debugging
void DebuggerRun(void) {
  int i;
  for (i=0; i<NUM_SAMPLES; i++) { USBUSARTTransmit(AnalogueSamplesA[i]); }
  USBUSARTTransmit(0x0A);
}
