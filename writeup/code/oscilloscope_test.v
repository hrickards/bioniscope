// Output a square wave signal of various frequencies to test a CRO
module oscilloscope_test(clk, signal);
  // Accurate 50MHz clock input signal
  input clk;

  // Output square wave
  output wire signal;

  // Module that divides a frequency to produce a 50% duty-cycle output
  // using flip-flops
  clock_divider d1 (
    .clk_in(clk),
    .clk_out(signal)
  );
  // clk is divided by 2^divisor
  // So increase by 1 to halve the output frequency
  defparam d1.divisor = 21;
endmodule
