#!/bin/bash

# Check if .env.local exists
if [ -f .env.dev ]; then
    # Load variables from .env.local
    export $(grep -v '^#' .env.dev | xargs)
    echo "Environment variables loaded from .env.dev"
else
    echo "No .env.dev file found. Please create one based on .env.dev.example"
    exit 1
fi