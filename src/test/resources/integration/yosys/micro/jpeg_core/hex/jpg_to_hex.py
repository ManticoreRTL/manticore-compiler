import argparse
import math
import numpy as np

# Main program (if executed as script)
if __name__ == "__main__":
  parser = argparse.ArgumentParser(description="Parses a JPEG file and emits .hex files for jpeg_core's `inport_data_i` and `inport_strb_i` inputs.")
  parser.add_argument("jpg", type=str, help="Input JPEG file.")
  parser.add_argument("fout_data_hex", type=str, help="Output data hex file.")
  parser.add_argument("fout_strb_hex", type=str, help="Output write strobe hex file.")
  args = parser.parse_args()

  # Must read input array as uint8 instead of <u4.
  # If we read as <u4, then we cannot tell that the last entry
  # of the array is smaller than 4 bytes and we can't generate
  # the strb.
  jpg = np.fromfile(args.jpg, dtype=np.uint8)

  # The hardware's input data bus is 32 bits wide -> 4 bytes.
  data_width_bytes = 4
  # Note that the last chunk may be smaller than the previous chunks if the
  # image is not a multiple of 4 bytes long.
  num_chunks = math.ceil(jpg.nbytes / data_width_bytes)

  data_hex = list()
  strb_hex = list()
  for chunk in np.array_split(jpg, num_chunks):
    strb = (1 << chunk.nbytes) - 1
    # The last chunk may be narrower than 4 bytes. We therefore pad the
    # data to a length of 4 bytes before continuing.
    zeros = np.zeros(data_width_bytes - chunk.nbytes, dtype=chunk.dtype)
    # Data is interpreted as big-endian so the bytes are reversed.
    data = np.concatenate([zeros, chunk]).view(dtype=np.uint32)
    data_hex.append(f"{data[0]:08x}")
    strb_hex.append(f"{strb:01x}")

  with open(args.fout_data_hex, "w") as f:
    f.writelines("\n".join(data_hex))

  with open(args.fout_strb_hex, "w") as f:
    f.write("\n".join(strb_hex))

  print("Done.")
