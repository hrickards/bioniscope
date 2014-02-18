// Divide a clock by 2^divisor with a 50% duty-cycle output (1:1 mark:space)
module clock_divider(clk_in, clk_out);
  // This is intended to be changed by the code calling this module
  parameter divisor = 2;

  // Input to be divided
  input clk_in;

  // Create a counter out of flip flops, and toggle clk_bit whenever the
  // counter's most significant bit toggles. This produces a 50% duty-cycle
  // output that we use to toggle clk_out.
  reg [divisor-1:0] counter = 0;
  always @(posedge clk_in) counter <= counter + 1;
  output reg clk_out = counter[divisor-1];
endmodule
