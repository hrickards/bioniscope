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
    data = data[:-1]
    X = np.array([(1.6+ time_delay)*i for i in range(len(data))])
    Y = np.array(data)
    plt.plot(X, Y)
    plt.ylim([0, 256])
    plt.show()

DELTA = 50
def error_check(array):
    if not ERROR_CHECK: return array
    for i in range(len(array)):
        if i==0 or i==len(array)-1: continue 
        if (array[i-1] > array[i] + DELTA and array[i+1] > array[i] + DELTA) or (array[i-1] < array[i] - DELTA and array[i+1] < array[i] - DELTA):
            array[i] = (array[i-1]+array[i+1])*0.5
    return array

def moving_average(array):
    if not MOVING_AVERAGE: return array
    period = 3
    new_array = array[:]
    for i in range(len(array)):
        if i<period-1: continue 
        vals = [array[i-j] for j in range(period)]
        new_array[i] = sum(vals)*1.0/len(vals)
    
    return new_array

ERROR_CHECK = False
MOVING_AVERAGE = False

while True:
    if bt:
        # print aformat(command(0x0F, [0x01]))
        print aformat(command(0x08, [0x00|0x80]))

        # time_delay = int(0)
        # command(0x06, [time_delay>>8, time_delay&0xFF])
        # print command(0x00, [])
        # print aformat([command(0x00, [])[10], command(0x01, [])[10]])
        # for d in set(command(0x01, [])):
            # print "{0:08b}".format(d)
        # plot_data(moving_average(error_check(command(0x01, []))), 0)
        # plot_data(data, time_delay)

        plot_data(command(0x00, []), 0)
    ##if bt: print "COM: " + aformat(command(0x08, [0xFF]))
    ##if bt: print "COM: " + aformat(command(0x08, [0x7F]))

    # Get deubgging info
    if usb:
        print aformat(debug())
        # for datum in debug():
            # print("{0:08b}".format(datum)),
