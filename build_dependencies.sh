#!/bin/sh

# sudo apt update

# # C++ tools
# # SWIG
# # Java JDK >= 8.0
# # Maven >= 3.3
# sudo apt -y install git wget pkg-config build-essential cmake autoconf libtool zlib1g-dev lsb-release swig default-jdk maven

# export JAVA_HOME=/usr/lib/jvm/default-java

# # or-tools
# git clone https://github.com/google/or-tools
# cd or-tools
# make third_party
# make -j48 java
# # make test_java
# make package_java
# cd ../
mkdir -p lib/
cd lib/
ln -s ../or-tools/ortools-java-9.3.10497.jar ortools-java-9.3.10497.jar
ln -s ../or-tools/ortools-java-9.3.10497-javadoc.jar ortools-java-9.3.10497-javadoc.jar
ln -s ../or-tools/ortools-java-9.3.10497-sources.jar ortools-java-9.3.10497-sources.jar
ln -s ../or-tools/ortools-linux-x86-64-9.3.10497.jar ortools-linux-x86-64-9.3.10497.jar
ln -s ../or-tools/ortools-linux-x86-64-9.3.10497-sources.jar ortools-linux-x86-64-9.3.10497-sources.jar
