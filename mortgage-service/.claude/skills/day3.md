# Day 3 Goals — Spring Security & JWT

## Topics to Cover
- SecurityFilterChain setup
- JWT token generation & validation
- UserDetailsService implementation
- Role-based access (ADMIN / CUSTOMER)
- @PreAuthorize method-level security
- CSRF & CORS config
- 401 vs 403 handling
- Password encoding (BCrypt)
- Stateless session management

## Endpoints to Secure
- POST /api/v1/auth/register
- POST /api/v1/auth/login   ← returns JWT token
- GET  /api/v1/customers    ← ADMIN only
- GET  /api/v1/loans/customer/{id} ← CUSTOMER (own data only)