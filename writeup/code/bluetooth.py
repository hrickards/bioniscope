import serial # pySerial used for serial communication with BT receiver

# Number of bytes of data to send each time
ITERATIONS = 384

# Open a serial socket to the BT device
# If this line fails, check /dev/rfcomm0 is the right port
ser = serial.Serial('/dev/rfcomm0', 9600, timeout=1)

# Remove any initial noise by writing a newline and waiting for it to return
ser.write("\n")
ser.readline()

# Write results to data.txt
f = open("data.txt", "wb")

j = 0 # Number of times the test has been run

# Keep running the indented part until the program is stopped
while True:
    # To be filled with booleans that are true iff the data is echoed correctly
    data = []
    # The character to send and have echoed back. Will be changed by the code
    # below.
    char = 0x00

    # Do the indented part ITERATIONS number of times
    for i in range(ITERATIONS):
        # Increment char, wrapping back to 0x00 after 0xFF
        char = (char + 1) % 0xFF
        # Write the character
        ser.write(chr(char))
        # Append to data whether or not the character was echoed correctly
        data.append(ser.read() == chr(char))

    # The line of output to show to the user and write to data.txt
    # test number: reliability average
    output = "%d: %f" % (j, sum(data)*100.0/len(data))
    print(output)
    f.write(output + "\n")

    j += 1
    raw_input("...") # Wait for the user to press enter

ser.close()
f.close()
