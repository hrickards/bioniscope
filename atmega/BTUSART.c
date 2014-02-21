#define BAUD    9600      // Baud rate
#define UBRR    (F_CPU/16/BAUD-1)

#include <avr/io.h>
#include "Global.h"

void BTUSARTInit(void) {
  // Set baud rate
  UBRR1H = (Byte) (UBRR>>8);
  UBRR1L = (Byte) UBRR;
  // Enable receiver and transmitter
  UCSR1B = (1<<RXEN1)|(1<<TXEN1);
  // Set frame format: 8data, 2stop bit
  UCSR1C = (1<<USBS1)|(3<<UCSZ10);
}

void BTUSARTTransmit(Byte data) {
  // Wait for empty transmit buffer
  while (!( UCSR1A & (1<<UDRE1)));

  // Put data into buffer, sends the data
  UDR1 = data;
}

Byte BTUSARTRead(void) {
  // Wait until data exists
  loop_until_bit_is_set(UCSR1A, RXC1);
  return UDR1;
}
