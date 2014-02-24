import serial, os.path
import matplotlib.pyplot as plt
import numpy as np
from multiprocessing import Process

num_samples = 1024

bt = True
if bt: ser = serial.Serial('/dev/rfcomm0', 9600, timeout=1)

usb = False
if usb: ser_dbg = serial.Serial('/dev/ttyUSB0', 9600, timeout=1)

# Send a command read response
def command(com, data):
    for x in ([com] + data): ser.write(chr(x))
    return [ord(x) for x in ser.readline()]

# Receive any debugging info uC is sending
def debug():
    return [ord(x) for x in ser_dbg.readline()]

# Format array of numbers for output as hex string
def aformat(nums):
    return " ".join(map(hex_full, nums))

# Format a hex num to 2sf
def hex_full(num):
    return "0x{:02X}".format(num)

# Plotting
def plot_data(data, time_delay):
    X = np.array([(1.6+ time_delay)*i for i in range(len(data))])
    Y = np.array(data)
    plt.plot(X, Y)
    plt.ylim([-0.05,1.05])
    plt.show()

while True:
    if bt:
        time_delay = int(100)
        command(0x06, [time_delay>>8, time_delay&0xFF])

        command(0x0D, [])
        data = command(0x02, [])
        print "COM: " + aformat(data)
        data = [x>>7 for x in data[:-1]]
        print data
        plot_data(data, time_delay)
    ##if bt: print "COM: " + aformat(command(0x08, [0xFF]))
    ##if bt: print "COM: " + aformat(command(0x08, [0x7F]))

    # Get deubgging info
    if usb: print "DBG: " + aformat(debug())
