#include <stdlib.h>
#include <boost/locale.hpp>
#include "connectionHandler.h"
#include "../encoder/utf8.h"
#include "../encoder/encoder.h"
#include <thread>
#include <boost/thread.hpp>

bool keep_going = true;

void serverListener (ConnectionHandler *connectionHandler) {
	while (1) {
		std::string answer;
		// Get back an answer: by using the expected number of bytes (len bytes + newline delimiter)
		// We could also use: connectionHandler.getline(answer) and then get the answer without the newline char at the end
		if (!connectionHandler->getLine(answer)) {
			std::cout << "1: Disconnected. Exiting...\n" << std::endl;
			break;
		}
		int len=answer.length();
		// A C string must end with a 0 char delimiter.  When we filled the answer buffer from the socket
		// we filled up to the \n char - we must make sure now that a 0 char is also present. So we truncate last character.
		answer.resize(len-1);
		std::cout << answer << std::endl;
		if (answer.compare("SYSMSG QUIT ACCEPTED")==0) {
			std::cout << "Exiting...\n" << std::endl;
		    std::cout << "Press ENTER to exit!" << std::endl;
			break;
		}
	}
}

void userListener (ConnectionHandler *connectionHandler) {
	while (true) {
        const short bufsize = 1024;
        char buf[bufsize];
        if (keep_going == false)
        	break;
        std::cin.getline(buf, bufsize); //get next line from user
    	std::string line(buf);
        if (!connectionHandler->sendLine(line)) { //send next line /0 is the delimiter
            std::cout << "2: Disconnected. Exiting...\n" << std::endl;
            break;
        }
    }
}

int main (int argc, char *argv[]) {
    if (argc < 3) {
        std::cerr << "Usage: " << argv[0] << " host port" << std::endl << std::endl;
        return -1;
    }
    std::string host = argv[1];
    short port = atoi(argv[2]);
    ConnectionHandler *connectionHandler = new ConnectionHandler(host, port); //new connection-handler for each thread
    if (!connectionHandler->connect()) {
        std::cerr << "Cannot connect to " << host << ":" << port << std::endl;
        delete connectionHandler;
        return 1;
    }

    boost::thread servListener(&serverListener, connectionHandler); //create server listener (getting output from server)
    boost::thread usListener(&userListener, connectionHandler); //create user listener (waiting for input)
    servListener.join(); //wait for server thread to close
    keep_going = false;
    usListener.join();
    delete connectionHandler;
    return 0;
}
