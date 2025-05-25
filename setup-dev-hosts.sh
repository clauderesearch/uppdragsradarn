#!/bin/bash

# Script to set up local development environment hosts for single domain
# Run with: sudo ./setup-dev-hosts.sh

echo "Setting up development hosts file entries..."

# Check if running with sudo
if [ "$EUID" -ne 0 ]; then 
    echo "Please run with sudo: sudo ./setup-dev-hosts.sh"
    exit 1
fi

# Backup current hosts file
cp /etc/hosts /etc/hosts.backup.$(date +%Y%m%d_%H%M%S)

# Check if entries already exist
if grep -q "dev.uppdragsradarn.se" /etc/hosts; then
    echo "Development hosts entries already exist. Skipping..."
else
    echo "Adding development hosts entries..."
    cat >> /etc/hosts << EOF

# Uppdragsradarn local development (single domain)
127.0.0.1    dev.uppdragsradarn.se
127.0.0.1    www.dev.uppdragsradarn.se
EOF
    echo "Hosts file updated successfully!"
fi

# Install and configure mkcert for local HTTPS
echo ""
echo "Setting up mkcert for local HTTPS development..."

# Detect OS and install mkcert if not present
if ! command -v mkcert &> /dev/null; then
    echo "mkcert not found. Installing..."
    
    # Detect OS and use appropriate package manager
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        if command -v brew &> /dev/null; then
            brew install mkcert
        else
            echo "Error: Homebrew not found. Please install Homebrew first."
            echo "Visit: https://brew.sh"
            exit 1
        fi
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        # Linux
        if command -v apt-get &> /dev/null; then
            # Debian/Ubuntu
            apt-get update
            apt-get install -y libnss3-tools wget
            MKCERT_VERSION="v1.4.4"
            MKCERT_URL="https://github.com/FiloSottile/mkcert/releases/download/${MKCERT_VERSION}/mkcert-${MKCERT_VERSION}-linux-amd64"
            wget -O /usr/local/bin/mkcert "$MKCERT_URL"
            chmod +x /usr/local/bin/mkcert
        elif command -v yum &> /dev/null; then
            # RHEL/CentOS
            yum install -y nss-tools wget
            MKCERT_VERSION="v1.4.4"
            MKCERT_URL="https://github.com/FiloSottile/mkcert/releases/download/${MKCERT_VERSION}/mkcert-${MKCERT_VERSION}-linux-amd64"
            wget -O /usr/local/bin/mkcert "$MKCERT_URL"
            chmod +x /usr/local/bin/mkcert
        else
            echo "Error: Unsupported Linux distribution"
            exit 1
        fi
    else
        echo "Error: Unsupported operating system"
        exit 1
    fi
else
    echo "mkcert already installed."
fi

# Create certs directory if it doesn't exist
CERTS_DIR="./backend/certs"
mkdir -p "$CERTS_DIR"

# Install mkcert root CA (if not already installed)
echo "Installing mkcert CA..."
mkcert -install

# Generate certificates for single domain
echo "Generating certificates for dev domain..."
cd "$CERTS_DIR" || exit
mkcert dev.uppdragsradarn.se www.dev.uppdragsradarn.se localhost

# Rename certificates for clarity
mv "dev.uppdragsradarn.se+2.pem" "dev.uppdragsradarn.se.crt"
mv "dev.uppdragsradarn.se+2-key.pem" "dev.uppdragsradarn.se.key"

cd - > /dev/null || exit

echo "HTTPS certificates generated successfully!"
echo ""
echo "Certificates are located in: $CERTS_DIR"
echo "- Certificate: $CERTS_DIR/dev.uppdragsradarn.se.crt"
echo "- Private Key: $CERTS_DIR/dev.uppdragsradarn.se.key"

echo ""
echo "Development environment setup complete!"
echo ""
echo "To start the development environment:"
echo "1. Start backend services: cd backend && docker-compose --env-file .env.dev up"
echo "2. Start backend app: cd backend && ./mvnw spring-boot:run -Dspring.profiles.active=dev"  
echo "3. Start frontend: cd frontend && npm run dev"
echo "4. Start admin: cd admin && npm run dev"
echo ""
echo "Access the applications at:"
echo "- Frontend: https://dev.uppdragsradarn.se"
echo "- Admin: https://dev.uppdragsradarn.se/admin"
echo "- API: https://dev.uppdragsradarn.se/api"
echo ""
echo "Note: All applications now use the same domain with path-based routing!"