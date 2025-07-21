#!/bin/bash

# Script to run the Multithreading Demo Java application

echo "Building the Multithreading Demo..."
mvn clean compile

if [ $? -eq 0 ]; then
    echo "Build successful. Starting the application..."
    mvn javafx:run
else
    echo "Build failed. Please check the errors above."
    exit 1
fi
