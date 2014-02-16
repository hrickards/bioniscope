import serial, random 

ITERATIONS = 384

ser = serial.Serial('/dev/rfcomm0', 9600, timeout=1)

# Get rid of initial noise
ser.write("\n")
ser.readline()

f = open("data.txt", 'wb')

j = 0
while True:
    data = []
    char = 0x00
    for i in range(ITERATIONS):
        char = (char + 1) % 0xff
        ser.write(chr(char))
        data.append(ser.read() == chr(char))

    output = "%d: %f" % (j, sum(data)*100.0/len(data))
    print(output)
    f.write(output + "\n")
    j += 1
    raw_input("...")

ser.close()
f.close()
