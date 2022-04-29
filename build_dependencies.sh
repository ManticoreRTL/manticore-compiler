#!/bin/bash

sudo apt update

################################################################################
# Build Dependencies ###########################################################
################################################################################

# C++ tools
# SWIG
# Java JDK >= 8.0
# Maven >= 3.3
sudo apt -y install git wget pkg-config build-essential cmake autoconf libtool zlib1g-dev lsb-release swig default-jdk maven verilator ninja-build


################################################################################
# SDKMAN #######################################################################
################################################################################

curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install sbt


################################################################################
# Build or-tools ###############################################################
################################################################################
export JAVA_HOME=/usr/lib/jvm/default-java
git clone https://github.com/google/or-tools
pushd or-tools
mkdir -p build
pushd build
cmake .. -DBUILD_JAVA=ON -DSKIP_GPG=ON -G "Ninja"
cmake --build .
popd # build
popd # or-tools