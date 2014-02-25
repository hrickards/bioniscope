#include "Global.h"
#include "SPI.h"
#include "Pots.h"

void PotsSetup(void) {
  // Setup SPI
  SPISetup();

  // Set initial values to their highest
  PotsSet(0x7F);
  PotsSet(0xFF);
}

// Set pot resistance to value of byte (0 to 127 inc.)
// The MSB gives the address, and the 7 LSB give the level
void PotsSet(Byte control) {
  Byte address = (control & 0x80) ? 0x01 : 0x00; // Get MSB
  Byte level = control & 0x7f; // 7 LSB

  // Send 9 zeroes followed by 7 data bits
  SPIWriteCommand(0x00, level, address);
}
