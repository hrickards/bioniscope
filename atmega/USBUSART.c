/* ****************************************************************************
   USBUSART.c
***************************************************************************** */
#define BAUD    9600      // Baud rate
#define UBRR    (F_CPU/16/BAUD-1)

#include <avr/io.h>
#include "Global.h"

void USBUSARTInit(void)
{
  /* Set baud rate */
  UBRR0H = (Byte) (UBRR>>8);
  UBRR0L = (Byte) UBRR;
  /* Enable receiver and transmitter */
  UCSR0B = (1<<RXEN0)|(1<<TXEN0);
  /* Set frame format: 8data, 2stop bit */
  UCSR0C = (1<<USBS0)|(3<<UCSZ00);
}

void USBUSARTTransmit(Byte data)
{
  /* Wait for empty transmit buffer */
  while (!( UCSR0A & (1<<UDRE0)));

  /* Put data into buffer, sends the data */
  UDR0 = data;
}

