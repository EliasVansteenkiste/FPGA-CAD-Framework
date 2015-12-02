#!/bin/bash

find src -type f -name "*.java" -print | xargs javac -d bin
