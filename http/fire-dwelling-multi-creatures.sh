#!/bin/bash

# =============================================================================
# Multi-Creature Dwelling Script
# Builds a single dwelling, then rapidly increases and recruits many different
# creatures on it — stress-testing the dwelling aggregate with varied creature types
# =============================================================================

# -----------------------------------------------------------------------------
# Configuration Variables
# -----------------------------------------------------------------------------
SERVER_PORT=3773
GAME_ID="scenario-1"
PLAYER_ID="player-1"
ARMY_ID="army-1"

# The single dwelling to operate on
DWELLING_ID="dwelling-multi-creature"

# Initial dwelling setup
INITIAL_CREATURE_ID="angel"
INITIAL_COST_GOLD=3000
INITIAL_COST_GEMS=1

# Creatures to cycle through — each will be increased and recruited
CREATURES=(
    "angel"
    "archangel"
    "dragon"
    "griffin"
    "swordsman"
    "pikeman"
    "cavalier"
    "monk"
    "champion"
    "crusader"
    "hydra"
    "behemoth"
    "phoenix"
    "thunderbird"
    "naga"
    "genie"
    "titan"
    "devil"
    "bone-dragon"
    "vampire-lord"
)

# How many creatures to increase per round
INCREASE_AMOUNT=10

# How many creatures to recruit per round
RECRUIT_QUANTITY=5

# Cost per troop for recruitment (expected cost = quantity * cost per troop)
COST_PER_TROOP_GOLD=500

# Interval between requests in milliseconds
INTERVAL_MS=5

# Number of full cycles through all creatures (0 = infinite)
CYCLE_COUNT=0

# -----------------------------------------------------------------------------
# Script Logic
# -----------------------------------------------------------------------------
BASE_URL="http://localhost:${SERVER_PORT}"

sleep_ms() {
    local ms="$1"
    if command -v perl &> /dev/null; then
        perl -e "select(undef,undef,undef,$ms/1000)"
    else
        sleep "$(echo "scale=3; $ms/1000" | bc)"
    fi
}

fire_build_dwelling() {
    local url="${BASE_URL}/games/${GAME_ID}/dwellings/${DWELLING_ID}"

    local response
    response=$(curl -s -w "\n%{http_code}" -X PUT "${url}" \
        -H "Content-Type: application/json" \
        -H "X-Player-Id: ${PLAYER_ID}" \
        -d "{
            \"creatureId\": \"${INITIAL_CREATURE_ID}\",
            \"costPerTroop\": {
                \"gold\": ${INITIAL_COST_GOLD},
                \"gems\": ${INITIAL_COST_GEMS}
            }
        }")

    local http_code
    http_code=$(echo "$response" | tail -n1)
    local body
    body=$(echo "$response" | sed '$d')

    echo "[$(date '+%H:%M:%S.%3N')] BUILD DWELLING | Dwelling: ${DWELLING_ID} | Creature: ${INITIAL_CREATURE_ID} | HTTP: ${http_code}"
    if [[ "$http_code" != "200" && "$http_code" != "202" ]]; then
        echo "  Body: ${body}"
    fi
}

fire_increase_available() {
    local creature_id="$1"
    local url="${BASE_URL}/games/${GAME_ID}/dwellings/${DWELLING_ID}/available-creatures-increases"

    local response
    response=$(curl -s -w "\n%{http_code}" -X PUT "${url}" \
        -H "Content-Type: application/json" \
        -H "X-Player-Id: ${PLAYER_ID}" \
        -d "{
            \"creatureId\": \"${creature_id}\",
            \"increaseBy\": ${INCREASE_AMOUNT}
        }")

    local http_code
    http_code=$(echo "$response" | tail -n1)
    local body
    body=$(echo "$response" | sed '$d')

    echo "[$(date '+%H:%M:%S.%3N')] INCREASE +${INCREASE_AMOUNT} | Creature: ${creature_id} | HTTP: ${http_code}"
    if [[ "$http_code" != "200" && "$http_code" != "202" ]]; then
        echo "  Body: ${body}"
    fi
}

fire_recruit_creature() {
    local creature_id="$1"
    local total_gold=$((RECRUIT_QUANTITY * COST_PER_TROOP_GOLD))
    local url="${BASE_URL}/games/${GAME_ID}/dwellings/${DWELLING_ID}/creature-recruitments"

    local response
    response=$(curl -s -w "\n%{http_code}" -X PUT "${url}" \
        -H "Content-Type: application/json" \
        -H "X-Player-Id: ${PLAYER_ID}" \
        -d "{
            \"creatureId\": \"${creature_id}\",
            \"armyId\": \"${ARMY_ID}\",
            \"quantity\": ${RECRUIT_QUANTITY},
            \"expectedCost\": {
                \"gold\": ${total_gold}
            }
        }")

    local http_code
    http_code=$(echo "$response" | tail -n1)
    local body
    body=$(echo "$response" | sed '$d')

    echo "[$(date '+%H:%M:%S.%3N')] RECRUIT x${RECRUIT_QUANTITY} | Creature: ${creature_id} | Cost: ${total_gold}g | HTTP: ${http_code}"
    if [[ "$http_code" != "200" && "$http_code" != "202" ]]; then
        echo "  Body: ${body}"
    fi
}

# -----------------------------------------------------------------------------
# Main
# -----------------------------------------------------------------------------
echo "=============================================="
echo "Multi-Creature Dwelling Stress Script"
echo "=============================================="
echo "Server:      ${BASE_URL}"
echo "Game ID:     ${GAME_ID}"
echo "Player ID:   ${PLAYER_ID}"
echo "Army ID:     ${ARMY_ID}"
echo "Dwelling:    ${DWELLING_ID}"
echo "Creatures:   ${#CREATURES[@]} types"
echo "Increase by: ${INCREASE_AMOUNT} per creature"
echo "Recruit qty: ${RECRUIT_QUANTITY} per creature"
echo "Interval:    ${INTERVAL_MS}ms"
echo "Cycles:      ${CYCLE_COUNT:-infinite}"
echo "=============================================="
echo ""

# Step 1: Build the dwelling
echo "--- Building dwelling ---"
fire_build_dwelling
sleep_ms 500
echo ""

# Step 2: Cycle through creatures — increase then recruit
echo "--- Starting increase & recruit cycles ---"
echo "Press Ctrl+C to stop"
echo ""

cycle=0
while true; do
    cycle=$((cycle + 1))
    echo "=== Cycle ${cycle} ==="

    for creature in "${CREATURES[@]}"; do
        # Increase available creatures
        fire_increase_available "$creature"
        sleep_ms "$INTERVAL_MS"

        # Recruit creatures
        fire_recruit_creature "$creature"
        sleep_ms "$INTERVAL_MS"
    done

    echo ""

    if [[ $CYCLE_COUNT -gt 0 && $cycle -ge $CYCLE_COUNT ]]; then
        echo "Completed ${cycle} cycle(s) across ${#CREATURES[@]} creature types."
        break
    fi
done
