#include "Global.h"
#include "USBUSART.h"

// Setup debugging
void DebuggerSetup(void) {
  // Initialise USB USART
  USBUSARTInit();
}

// Called repeatedly to do some form of debugging
void DebuggerRun(void) {
  // Return 0x00 then a newline
  USBUSARTTransmit(0x00);
  USBUSARTTransmit(0x0A);
}
