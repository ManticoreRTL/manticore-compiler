#!/bin/bash

for blockIn in 16 32 64; do
  for blockOut in 16 32 64; do
    echo "blockIn=${blockIn} blockOut=${blockOut}"
    make clean
    make -j BLOCKIN=${blockIn} BLOCKOUT=${blockOut}
    sudo perf stat -o stat_blockIn${blockIn}_blockOut${blockOut}.txt -ddd ./obj_dir/VMain 1000000
  done
done