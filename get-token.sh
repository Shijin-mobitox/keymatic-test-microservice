#!/bin/bash
# Keycloak Token Generator Script (Bash version)
# This script generates JWT access tokens from Keycloak for use with tenant-service

USERNAME=${1:-user1}
PASSWORD=${2:-password}
CLIENT_ID=${3:-tenant-service}
CLIENT_SECRET=${4:-tenant-secret}
REALM=${5:-kymatic}
KEYCLOAK_URL=${6:-http://localhost:8085}

echo "========================================="
echo "Keycloak Token Generator"
echo "========================================="
echo ""

TOKEN_URL="$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token"

echo "Requesting token from: $TOKEN_URL"
echo "Username: $USERNAME"
echo "Client: $CLIENT_ID"
echo ""

RESPONSE=$(curl -s -X POST "$TOKEN_URL" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=$CLIENT_ID" \
  -d "client_secret=$CLIENT_SECRET" \
  -d "username=$USERNAME" \
  -d "password=$PASSWORD")

# Check if we got an error
if echo "$RESPONSE" | grep -q "error"; then
    echo "✗ Error generating token:"
    echo "$RESPONSE" | jq '.' 2>/dev/null || echo "$RESPONSE"
    exit 1
fi

# Extract access token
ACCESS_TOKEN=$(echo "$RESPONSE" | jq -r '.access_token' 2>/dev/null)

if [ -z "$ACCESS_TOKEN" ] || [ "$ACCESS_TOKEN" = "null" ]; then
    echo "✗ Failed to extract access token from response:"
    echo "$RESPONSE"
    exit 1
fi

echo "✓ Token generated successfully!"
echo ""
echo "Access Token:"
echo "$ACCESS_TOKEN"
echo ""

# Extract other fields
TOKEN_TYPE=$(echo "$RESPONSE" | jq -r '.token_type' 2>/dev/null)
EXPIRES_IN=$(echo "$RESPONSE" | jq -r '.expires_in' 2>/dev/null)
REFRESH_TOKEN=$(echo "$RESPONSE" | jq -r '.refresh_token' 2>/dev/null)

echo "Token Type: $TOKEN_TYPE"
echo "Expires In: $EXPIRES_IN seconds"
echo "Refresh Token: $REFRESH_TOKEN"
echo ""

# Decode JWT to show claims (without verification)
if command -v jq &> /dev/null && command -v base64 &> /dev/null; then
    PAYLOAD=$(echo "$ACCESS_TOKEN" | cut -d. -f2)
    # Add padding if needed
    case $((${#PAYLOAD} % 4)) in
        2) PAYLOAD="${PAYLOAD}==" ;;
        3) PAYLOAD="${PAYLOAD}=" ;;
    esac
    
    CLAIMS=$(echo "$PAYLOAD" | base64 -d 2>/dev/null | jq '.' 2>/dev/null)
    if [ -n "$CLAIMS" ]; then
        echo "Token Claims:"
        echo "$CLAIMS" | jq -r '
            "  - Subject (sub): " + .sub,
            "  - Username (preferred_username): " + .preferred_username,
            "  - Email: " + .email,
            "  - Tenant ID: " + .tenant_id,
            "  - Expires: " + (.exp | todateiso8601)
        ' 2>/dev/null || echo "$CLAIMS"
        echo ""
    fi
fi

# Save token to file
echo "$ACCESS_TOKEN" > token.txt
echo "Token saved to: token.txt"
echo ""
echo "To use this token, run:"
echo "  export TOKEN=\$(cat token.txt)"
echo "  curl -H \"Authorization: Bearer \$TOKEN\" http://localhost:8083/api/me"
echo ""

