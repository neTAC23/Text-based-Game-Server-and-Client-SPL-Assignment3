CFLAGS:=-c -Wall -Weffc++ -g -std=c++11
LDFLAGS:=-lboost_system -lboost_locale -lpthread -lboost_thread -lboost_filesystem

all: bin/encoder.o EchoClient
	g++ -o bin/echoExample bin/encoder.o bin/connectionHandler.o bin/echoClient.o $(LDFLAGS) 

EchoClient: bin/connectionHandler.o bin/echoClient.o
	
bin/connectionHandler.o: echoClient/connectionHandler.cpp
	g++ $(CFLAGS) -o bin/connectionHandler.o echoClient/connectionHandler.cpp

bin/echoClient.o: echoClient/echoClient.cpp
	g++ $(CFLAGS) -o bin/echoClient.o echoClient/echoClient.cpp
	
bin/encoder.o: encoder/encoder.cpp
	g++ $(CFLAGS) -o bin/encoder.o encoder/encoder.cpp
	
.PHONY: clean
clean:
	rm -f bin/*
