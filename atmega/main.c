/* ****************************************************************************
   main.c
***************************************************************************** */
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

  #ifdef _DEBUG_
    // Setup the debugging interface (USB)
    DebuggerSetup();
  #endif

  // Setup potentiometer control interface
  PotsSetup();

  // Setup sampler (analogue and digital
  SamplerSetup();

  // Enable interrupts
  // sei();

  // Loop forever
  while(1) {
    // Take samples
    SamplerSample();

    // Respond to commands
    CommandRun();

    #ifdef _DEBUG_
      // Send any debugging info over USB
      DebuggerRun();
    #endif
  }
}
