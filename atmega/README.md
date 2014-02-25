# Fuses
To get a 16MHz clock using an external crystal, use CKSEL=1111, SUT=111.

To enable communication with the ADC: JTAG must be disabled! So set JTAGEN=0.

See [here](http://www.engbedded.com/fusecalc) for a great fuse calculator. The values I ended up with were L:FF, H:D9, E:FF.
