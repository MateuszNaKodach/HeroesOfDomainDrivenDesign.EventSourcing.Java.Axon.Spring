#!/bin/bash

# =============================================================================
# BuildDwelling Request Firing Script
# Fires BuildDwelling requests at configurable intervals with unique dwelling IDs
# =============================================================================

# -----------------------------------------------------------------------------
# Configuration Variables
# -----------------------------------------------------------------------------
SERVER_PORT=3774
GAME_ID="scenario-1"
PLAYER_ID="player-1"
CREATURE_ID="angel"
COST_GOLD=3000
COST_GEMS=1

# Interval between requests in milliseconds
INTERVAL_MS=100

# Number of requests to fire (0 = infinite)
REQUEST_COUNT=0

# -----------------------------------------------------------------------------
# Script Logic
# -----------------------------------------------------------------------------
BASE_URL="http://localhost:${SERVER_PORT}"

generate_uuid() {
    if command -v uuidgen &> /dev/null; then
        uuidgen | tr '[:upper:]' '[:lower:]'
    else
        cat /proc/sys/kernel/random/uuid 2>/dev/null || \
        python3 -c 'import uuid; print(uuid.uuid4())' 2>/dev/null || \
        od -x /dev/urandom | head -1 | awk '{print $2$3"-"$4"-"$5"-"$6"-"$7$8$9}'
    fi
}

fire_build_dwelling() {
    local dwelling_id="$1"
    local url="${BASE_URL}/games/${GAME_ID}/dwellings/${dwelling_id}"

    local response
    response=$(curl -s -w "\n%{http_code}" -X PUT "${url}" \
        -H "Content-Type: application/json" \
        -H "X-Player-Id: ${PLAYER_ID}" \
        -d "{
            \"creatureId\": \"${CREATURE_ID}\",
            \"costPerTroop\": {
                \"gold\": ${COST_GOLD},
                \"gems\": ${COST_GEMS}
            }
        }")

    local http_code
    http_code=$(echo "$response" | tail -n1)
    local body
    body=$(echo "$response" | sed '$d')

    echo "[$(date '+%H:%M:%S.%3N')] Dwelling: ${dwelling_id} | HTTP: ${http_code}"
}

sleep_ms() {
    local ms="$1"
    if command -v perl &> /dev/null; then
        perl -e "select(undef,undef,undef,$ms/1000)"
    else
        sleep "$(echo "scale=3; $ms/1000" | bc)"
    fi
}

echo "=============================================="
echo "BuildDwelling Request Firing Script"
echo "=============================================="
echo "Server:    ${BASE_URL}"
echo "Game ID:   ${GAME_ID}"
echo "Player ID: ${PLAYER_ID}"
echo "Creature:  ${CREATURE_ID}"
echo "Interval:  ${INTERVAL_MS}ms"
echo "Count:     ${REQUEST_COUNT:-infinite}"
echo "=============================================="
echo "Press Ctrl+C to stop"
echo ""

count=0
while true; do
    dwelling_id=$(generate_uuid)
    fire_build_dwelling "$dwelling_id"

    count=$((count + 1))

    if [[ $REQUEST_COUNT -gt 0 && $count -ge $REQUEST_COUNT ]]; then
        echo ""
        echo "Completed ${count} requests."
        break
    fi

    sleep_ms "$INTERVAL_MS"
done
