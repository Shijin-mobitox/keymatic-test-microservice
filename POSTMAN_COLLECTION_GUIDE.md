# 📬 KyMatic Keycloak APIs - Postman Collection Guide

This guide explains how to use the comprehensive Postman collection for all Keycloak APIs integrated in the KyMatic project.

## 📁 Files Created

- **`KyMatic-Keycloak-APIs.postman_collection.json`** - Main collection with all API requests
- **`KyMatic-Keycloak.postman_environment.json`** - Environment variables for cloud Keycloak
- **`POSTMAN_COLLECTION_GUIDE.md`** - This setup guide

## 🚀 Quick Setup

### 1. Import into Postman

1. **Open Postman**
2. **Import Collection**: 
   - Click "Import" → Select `KyMatic-Keycloak-APIs.postman_collection.json`
3. **Import Environment**: 
   - Click "Import" → Select `KyMatic-Keycloak.postman_environment.json`
4. **Select Environment**: 
   - Choose "KyMatic - Cloud Keycloak" from the environment dropdown

### 2. First-Time Setup

**Run these requests in order:**

1. **🔐 Authentication & Tokens → Get Admin Token (Master Realm)**
   - This gets admin access for Keycloak management operations
   - Token is automatically saved to environment variables

2. **🔐 Authentication & Tokens → Get User Token (Kymatic Realm)**
   - Gets user token with tenant_id claims
   - Shows how JWT tokens work in your application

## 📂 Collection Structure

### 🔐 Authentication & Tokens
- **Get Admin Token** - For admin operations
- **Get User Token** - For user operations (shows tenant_id claims)
- **Refresh Token** - Token renewal
- **Get UserInfo** - User information endpoint
- **Get JWKS** - Public keys for JWT validation

### 👥 User Management
- **List Users** - Browse all users
- **Search Users by Email** - Find specific users
- **Create User** - Add new users
- **Set User Password** - Password management
- **Delete User** - User cleanup

### 🏢 Organization Management (Keycloak 26+)
- **List Organizations** - View all tenants
- **Search by Alias** - Find specific tenant
- **Create Organization** - **Core tenant creation API**
- **Get Members** - View organization membership
- **Add User to Organization** - **Most challenging API in project**
- **Get/Assign Roles** - Role management
- **Delete Organization** - Cleanup operations

### 👥 Group Management (Alternative)
- **List/Search Groups** - Alternative to Organizations
- **Create Group** - More stable than Organizations
- **Assign User to Group** - Reliable user assignment
- **Delete Group** - Cleanup

### 🎛️ Client & Realm Management
- **Get Realm Info** - Realm configuration
- **List Clients** - All OAuth clients
- **Get React Client** - Frontend client config

### 🚀 Application Services
- **Tenant Service** - Main business logic (Port 8083)
- **API Service** - User context API (Port 8084)
- **Gateway Service** - Request routing (Port 8081)
- **Auth Service** - Authentication (Port 8082)

### 🧪 Testing & Debugging
- **Complete Tenant Creation Flow** - End-to-end test
- **Debug Organization Assignment** - Troubleshooting
- **Decode JWT Token** - Token inspection utility

## 🔧 Configuration

### Environment Variables

The collection uses these key variables:

| Variable | Value | Purpose |
|----------|-------|---------|
| `keycloak_base_url` | Your cloud Keycloak URL | Base URL for all requests |
| `realm_name` | `kymatic` | Target realm |
| `admin_username` | `admin-gG7X0T1x` | Admin credentials |
| `admin_password` | `blEzm8bnafcGnv50` | Admin password |
| `react_client_id` | `kymatic-react-client` | Frontend client |
| `test_username` | `testuser` | Test user credentials |
| `test_password` | `testpassword` | Test user password |

### Automatic Variables

These are set automatically by requests:

- `admin_token` - Admin access token
- `user_token` - User access token  
- `organization_id` - Created organization ID
- `created_user_id` - Created user ID
- `group_id` - Created group ID

## 📝 Usage Workflows

### 🏗️ Create Complete Tenant

1. **Get Admin Token**
2. **🏢 Create Organization** 
3. **👥 Create User**
4. **👥 Set User Password**
5. **🏢 Add User to Organization**
6. **✅ Verify with Get Members**

### 🔍 Debug User Assignment Issues

1. **Get Admin Token**
2. **🏢 List Organizations** - Find your org
3. **🏢 Get Organization Members** - Check assignments
4. **👥 Search Users by Email** - Verify user exists
5. **🧪 Debug Organization Assignment** - Detailed check

### 🧪 Test Authentication Flow

1. **Get User Token** - Check JWT claims in console
2. **🧪 Decode JWT Token** - See all token contents
3. **🚀 API Service - Get Current User** - Test backend integration
4. **Get UserInfo** - Validate with Keycloak

### 📊 Monitor System Health

1. **🚀 Tenant Service - Health Check**
2. **🚀 API Service - Health Check**  
3. **🚀 Gateway Service - Health Check**
4. **🚀 Auth Service - Health Check**

## 🎯 Key API Endpoints

### Most Important for Development:

1. **`POST /realms/kymatic/protocol/openid-connect/token`** - Get tokens
2. **`POST /admin/realms/kymatic/organizations`** - Create tenants
3. **`POST /admin/realms/kymatic/organizations/{id}/members`** - Assign users
4. **`POST /api/tenants`** - Complete business logic

### Most Challenging:

1. **Organization Member Assignment** - Timing issues with Keycloak 26
2. **User Creation + Assignment** - Multi-step atomic operation
3. **JWT Token Validation** - Complex claim processing

## 🚨 Troubleshooting

### Common Issues:

1. **"Client not found"** 
   - Run: `🎛️ Get React Client` to verify client exists
   - Check `react_client_id` environment variable

2. **"User assignment failed"**
   - Run: `🧪 Debug Organization Assignment`
   - Check: `👥 Search Users by Email` - user exists?
   - Try: Groups as alternative to Organizations

3. **"Token expired"**
   - Run: `🔐 Get Admin Token` to refresh
   - Check token expiration with `🧪 Decode JWT Token`

4. **"Organization not found"**
   - Run: `🏢 List Organizations` to see all orgs
   - Check: `🏢 Search Organization by Alias`

### Debug Steps:

1. **Always start with Get Admin Token**
2. **Check service health endpoints first**
3. **Use Debug requests to understand state**
4. **Check Console tab for detailed JWT information**
5. **Verify environment variables are set correctly**

## 🔒 Security Notes

- **Admin credentials** are configured for your cloud instance
- **Tokens expire** - refresh as needed
- **Test users** are for development only
- **Environment variables** store sensitive data securely in Postman

## 📈 Advanced Usage

### Bulk Operations:
- Use **Collection Runner** to test multiple scenarios
- Set up **Pre-request Scripts** for data generation
- Use **Tests tab** for automated validation

### Integration Testing:
- Chain requests using environment variables
- Validate complete user journeys
- Test error scenarios and rollbacks

### Monitoring:
- Set up **Monitors** for health check endpoints
- Use **Newman** for CI/CD integration
- Export results for reporting

---

## 🎉 Ready to Use!

Your Postman collection is now configured with **85+ requests** covering every Keycloak API used in your project. Start with the **Get Admin Token** request and explore the organized folder structure.

**Happy API Testing! 🚀**
