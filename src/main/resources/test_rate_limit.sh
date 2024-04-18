#!/bin/bash
# Endpoint URL
URL="http://localhost:8080/users/all"

# Number of requests
REQUESTS=60

# Loop for the specified number of requests
for (( i=1; i<=REQUESTS; i++ ))
do
   echo "Request $i"
   curl -s -o /dev/null -w "%{http_code} - %{url_effective}\n" $URL
   sleep 0.1  # Delay to simulate second-by-second requests
done
