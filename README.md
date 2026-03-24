# Get401 Auth Spring

**Get401 Auth Spring** is the official Spring Boot integration for the [Get401](https://app.get401.com) identity platform. Add the dependency, set two properties, and your application gains annotation-driven JWT authentication, role and scope enforcement, and a fully injectable user management client.

---

## Features

- **Zero-boilerplate setup:** Auto-configures everything from `application.yml` — no manual bean wiring required.
- **Annotation-driven security:** Enforce authentication (`@AuthGet401`), roles (`@VerifyRoles`), and scopes (`@VerifyScope`) directly on controllers or individual handler methods.
- **Request enrichment:** Injects `jwtClaims` and `jwtSubject` as request attributes on every authenticated request.
- **User management client:** Auto-configured `Get401Client` bean for server-to-server user operations — list, retrieve, and disable users via the Get401 backend API.
- **Dynamic Ed25519 key fetching:** Cached public key provisioning directly from the Get401 platform, refreshed automatically on expiry.

## Requirements

- **Java:** 21 or higher
- **Spring Boot:** 3.2.x+ with `spring-boot-starter-web`

## Installation

### Gradle

```groovy
dependencies {
    implementation 'com.get401:auth-spring:0.0.1'
}
```

### Maven

```xml
<dependency>
    <groupId>com.get401</groupId>
    <artifactId>auth-spring</artifactId>
    <version>0.0.1</version>
</dependency>
```

---

## Configuration

All features are driven by properties. Add the relevant sections to your `application.yml`:

```yaml
get401:
  # Required for JWT authentication and annotation enforcement
  auth:
    app-id: your-get401-app-id
    origin: https://yourdomain.com
    # base-url: https://app.get401.com  # optional, defaults to the Get401 platform

  # Required only if you need the user management client (Get401Client)
  client:
    api-key: sk_live_your_api_key
    # base-url: https://app.get401.com  # optional
```

That's all. The interceptor and client beans are registered automatically.

---

## JWT Authentication

### Securing Controllers

Apply annotations to your `@RestController` classes or individual methods. The interceptor only activates on handlers that carry at least one Get401 annotation — unannotated endpoints are never touched.

```java
@RestController
@AuthGet401  // All methods in this controller require a valid JWT
public class SecureApiController {

    @GetMapping("/api/profile")
    public String profile() {
        return "Authenticated.";
    }

    @VerifyRoles({"admin", "editor"})
    @GetMapping("/api/admin/dashboard")
    public String adminDashboard() {
        // Requires at least one of the listed roles in the JWT
        return "Welcome, admin.";
    }

    @VerifyScope({"write:billing"})
    @GetMapping("/api/billing/update")
    public String updateBilling() {
        // Requires all listed scopes to be present in the JWT
        return "Billing updated.";
    }
}
```

Annotations can be placed on the class (applies to all methods) or on individual methods (overrides nothing — both levels are checked). `@VerifyRoles` and `@VerifyScope` imply `@AuthGet401`; you do not need to stack all three.

### Accessing Token Data in Controllers

On a successful authentication, `jwtClaims` and `jwtSubject` are injected as request attributes. Access them with `@RequestAttribute`:

```java
@RestController
@AuthGet401
public class MeController {

    @GetMapping("/api/me")
    public String me(
            @RequestAttribute("jwtSubject") String userId,
            @RequestAttribute("jwtClaims") Claims claims) {

        String email = claims.get("email", String.class);
        return String.format("Hello %s — ID: %s", email, userId);
    }
}
```

### How the Interceptor Works

1. Every incoming request passes through `JwtAuthenticationInterceptor`.
2. If the target handler has no Get401 annotation, the request continues immediately.
3. The `aact` cookie is extracted. Missing cookie → `401 Unauthorized`.
4. The JWT signature is verified against the cached Ed25519 public key fetched from Get401. Invalid token → `401 Unauthorized`.
5. Role and scope claims are checked against `@VerifyRoles` / `@VerifyScope` constraints. Insufficient access → `403 Forbidden`.
6. On success, `jwtClaims` (`Claims`) and `jwtSubject` (`String`) are set as request attributes.

---

## User Management Client

When `get401.client.api-key` is set, a `Get401Client` bean is auto-configured and available for injection anywhere in your application. It is intended for trusted server-to-server use only.

```java
@Service
public class UserAdminService {

    private final Get401Client get401Client;

    public UserAdminService(Get401Client get401Client) {
        this.get401Client = get401Client;
    }

    public User getUser(String userId) {
        return get401Client.getUserById(userId);
    }

    public List<User> getAllUsers() {
        List<User> all = new ArrayList<>();
        UsersPage page = get401Client.listUsers();
        while (page != null) {
            all.addAll(page.getItems());
            page = page.getNext() != null ? get401Client.listUsers(page.getNext()) : null;
        }
        return all;
    }

    public void disableUser(String userId) {
        get401Client.disableUser(userId);
    }
}
```

All client methods throw `Get401ApiException` (a `RuntimeException`) on non-2xx responses, exposing the HTTP status code and an error code string:

```java
try {
    get401Client.getUserById("usr_unknown");
} catch (Get401ApiException e) {
    // e.getStatus()    — HTTP status code (e.g. 404)
    // e.getErrorCode() — platform error string (e.g. "not_found")
}
```

---

## Advanced: Overriding Auto-Configured Beans

Both beans support `@ConditionalOnMissingBean`. Declare your own bean to take full control:

```java
@Bean
public JwtPublicKeyProvider jwtPublicKeyProvider() {
    return new JwtPublicKeyProvider("your-app-id", "https://yourdomain.com", null);
}

@Bean
public Get401Client get401Client() {
    return new Get401Client("sk_live_your_key", "https://staging.get401.com");
}
```

---

## License

Apache 2.0
