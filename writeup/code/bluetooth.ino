// IMPORTANT
// Delays need to be added into setup() and loop() if the Arduino is to be
// programmed over USB. If the code is left as it is, the Arduino will not
// be able to communicate with the IDE over USB, so a dedicated AVR programmer
// (or another Arduino) will be needed to upload new sketches.

// Run when the Arduino starts up initially
void setup() {
  // Begin serial communication with the HC06 at a baud rate of 9600
  Serial.begin(9600);

  // Use pin 13 (connected to an LED) to show visually when data is being
  // written
  pinMode(13, OUTPUT);
}

// Loops continuously
void loop() {
  // Turn LED off
  digitalWrite(13, LOW);

  // Wait til serial available
  while (!Serial.available());

  // Turn LED on
  digitalWrite(13, HIGH);

  // Echo back the received serial byte
  Serial.write(Serial.read());
}
