#!/bin/bash

sudo apt update

# C++ tools
# SWIG
# Java JDK >= 8.0
# Maven >= 3.3
sudo apt -y install git wget pkg-config build-essential cmake autoconf libtool zlib1g-dev lsb-release swig default-jdk maven

# export JAVA_HOME=/usr/lib/jvm/default-java

# or-tools
git clone https://github.com/google/or-tools
pushd or-tools
make third_party
make -j48 java
# make test_java
make package_java
popd

mkdir -p lib/
pushd lib
cp ../or-tools/ortools-java-9.3.10497.jar .
cp ../or-tools/ortools-java-9.3.10497-javadoc.jar .
cp ../or-tools/ortools-java-9.3.10497-sources.jar .
cp ../or-tools/ortools-linux-x86-64-9.3.10497.jar .
cp ../or-tools/ortools-linux-x86-64-9.3.10497-sources.jar .
popd
