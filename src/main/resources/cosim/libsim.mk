include VManticoreFlatSimKernel.mk

default: libsim.so

libsim.so: $(VK_OBJS) $(VK_USER_OBJS) $(VK_GLOBAL_OBJS) ../harness.cpp
	$(CXX) $(CXXFLAGS) $(CPPFLAGS) -fPIC -shared -o $@ $^