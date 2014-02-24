#include <util/delay.h>
#include <avr/interrupt.h>

#include "USBUSART.h"
#include "Command.h"
#include "Global.h"
#include "Debugger.h"
#include "Pots.h"
#include "Sampler.h"

int main(void) {
  // Setup the command interface (Bluetooth)
  CommandSetup();
  // Setup the debugging interface (USB)
  // DebuggerSetup();

  // Setup potentiometer control interface
  PotsSetup();

  // Setup sampler (analogue and digital
  SamplerSetup();

  // Enable interrupts
  sei();

  // Loop forever
  while(1) {
    // Take samples
    SamplerSample();

    // Respond to commands
    CommandRun();

    // Send any debugging info over USB
    // DebuggerRun();

    _delay_ms(10);
  }
}
