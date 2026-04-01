#!/bin/bash

# 1. Run the comprehensive deployment script
# This script handles logo downloads, web build, Capacitor sync, and Android deployment.
# We've optimized it to avoid re-downloading Gradle every time.

bash ./scripts/deploy.sh
