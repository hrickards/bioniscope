#define FOSC    16000000          // Clock speed of the microcontroller
#define BAUD    19200             // Baud rate to communicate with
#define UBRR    (FOSC/16/BAUD-1)  // Calculated constant used to set speed

// Import libraries for interrupts and USART
#include <avr/io.h>
#include <avr/interrupt.h>

// Setup USART. Should be called at the start of the program.
static void USARTInit(void) {
  // Set baud rate
  UBRR0H = (Byte) (UBRR >> 8);
  UBRR0L = (Byte) UBRR;

  UCSR0B = (1<<RXEN0)|(1<<TXEN0); // Enable receiver and transmitter
  UCSR0C = (1<<USBS0)|(3<<UCSZ00); // Set frame format: 8 data, 2 stop bit

  // Enable an interrupt when a byte is received
  UCSR0B |= (1 << RXCIE0);
  sei();  // Enable global interrupts
}

// Write a byte
void USARTWriteByte(Byte data) {
  // Wait until the transmit buffer is empty (i.e. all previous bytes have
  // been sent)
  while (!(UCSR0A & (1<<UDRE0)));

  // Send the byte by putting it into the transmit buffer
  UDR0 = data;
}

// Read a byte
Byte USARTReadByte(void) {
  loop_until_bit_is_set(UCSR0A, RXC0); // Wait until data exists in the buffer
  return UDR0; // Return the received byte
}

// The interrupt called when a byte is received
ISR(USART_RX_vect) {
  // The byte that has been received
  Byte data = UDR0; // Do something with this: the byte that has been received
}
