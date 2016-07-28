################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
CPP_SRCS += \
../05_Boost_Echo_Client/echoClient/connectionHandler.cpp \
../05_Boost_Echo_Client/echoClient/echoClient.cpp 

OBJS += \
./05_Boost_Echo_Client/echoClient/connectionHandler.o \
./05_Boost_Echo_Client/echoClient/echoClient.o 

CPP_DEPS += \
./05_Boost_Echo_Client/echoClient/connectionHandler.d \
./05_Boost_Echo_Client/echoClient/echoClient.d 


# Each subdirectory must supply rules for building sources it contributes
05_Boost_Echo_Client/echoClient/%.o: ../05_Boost_Echo_Client/echoClient/%.cpp
	@echo 'Building file: $<'
	@echo 'Invoking: GCC C++ Compiler'
	g++ -O0 -g3 -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@)" -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


