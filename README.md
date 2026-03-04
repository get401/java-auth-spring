# Get401 Auth Spring

The **Get401 Auth Spring** library is a seamless Spring Boot / Spring Web MVC integration for the [Get401](https://app.get401.com) identity platform. It builds upon the solid foundation of `get401-auth-core` by providing a native Spring `HandlerInterceptor` that enforces declarative authentication and authorization natively on your Spring controllers.

---

## 🚀 Features

- **Seamless Spring MVC Integration:** Validates JSON Web Tokens (JWT) natively within the Spring HTTP request lifecycle.
- **Annotation-Driven Security:** Enforce Authentication (`@AuthGet401`), Role-Based Access Control (`@VerifyRoles`), and Scopes (`@VerifyScope`) directly on your controllers or handler methods.
- **Automatic Request Enrichment:** Injects `jwtClaims` and `jwtSubject` into the `HttpServletRequest` attributes upon successful authentication, allowing easy access inside your controllers.
- **Dynamic Ed25519 Key Fetching:** Inherits the core `JwtPublicKeyProvider` for fast, cached, and secure fetching of your application public key directly from the Get401 platform.
- **Cookie-Based Flow:** Automatically extracts tokens natively expected from the `aact` secure cookie.

## 📦 Requirements

- **Java:** Version 21 or higher.
- **Spring Boot:** Tested with 3.2.x+ (requires `spring-boot-starter-web`).

## 🛠️ Installation

Add the dependency to your project. *(If you are working with a snapshot or local build, ensure you have your local repository configured: `mavenLocal()` in Gradle).*

### Gradle

```groovy
dependencies {
    implementation 'com.get401:get401-auth-spring:0.0.1-SNAPSHOT'
}
```

### Maven

```xml
<dependency>
    <groupId>com.get401</groupId>
    <artifactId>get401-auth-spring</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

---

## ⚙️ Setup & Configuration

To integrate the library, you must provide a `JwtPublicKeyProvider` bean configured with your application's details, and then register the `JwtAuthenticationInterceptor` with Spring MVC.

### 1. Register Beans and Interceptor

Create a configuration class that implements `WebMvcConfigurer` to register the interceptor within your application context:

```java
import com.get401.auth.core.JwtPublicKeyProvider;
import com.get401.auth.spring.JwtAuthenticationInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class Get401SecurityConfig implements WebMvcConfigurer {

    // 1. Declare your public key provider bean
    @Bean
    public JwtPublicKeyProvider jwtPublicKeyProvider() {
        String appId = "your-get401-app-id";
        String origin = "https://yourdomain.com"; // Expected origin your tokens are tied to
        
        // Optional 3rd parameter: Get401 base URL. Null defaults to "https://app.get401.com"
        return new JwtPublicKeyProvider(appId, origin, null);
    }

    // Spring will automatically inject the JwtPublicKeyProvider above into the interceptor
    private final JwtAuthenticationInterceptor jwtAuthenticationInterceptor;

    public Get401SecurityConfig(JwtAuthenticationInterceptor jwtAuthenticationInterceptor) {
        this.jwtAuthenticationInterceptor = jwtAuthenticationInterceptor;
    }

    // 2. Register the interceptor against all endpoints
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthenticationInterceptor)
                .addPathPatterns("/**"); // Applies globally, but annotations dictate actual enforcement
    }
}
```

---

## 💻 Usage

Once the interceptor is registered, apply Get401 annotations to your `@RestController` classes or specific endpoint methods.

### 1. Securing Controllers with Annotations

```java
import com.get401.auth.core.annotation.AuthGet401;
import com.get401.auth.core.annotation.VerifyRoles;
import com.get401.auth.core.annotation.VerifyScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AuthGet401 // Base requirement: request must have a valid JWT cookie
public class SecureApiController {

    @GetMapping("/api/public-but-logged-in")
    public String openEndpoint() {
        return "You have a valid JWT token!";
    }

    @VerifyRoles({"admin", "editor"})
    @GetMapping("/api/admin/dashboard")
    public String adminDashboard() {
        // Only accessible if your JWT contains the "admin" or "editor" role
        return "Welcome to the admin dashboard.";
    }

    @VerifyScope({"write:billing"})
    @GetMapping("/api/billing/update")
    public String updateBilling() {
        // Only accessible if the scope string contains precisely "write:billing"
        return "Billing updated successfully.";
    }
}
```

### 2. Accessing Token Claims in Controllers

The `JwtAuthenticationInterceptor` extracts the JWT claims and subject and injects them into the HTTP Request attributes. You can extract these directly in your Spring controllers using `@RequestAttribute`:

```java
import io.jsonwebtoken.Claims;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AuthGet401
public class UserProfileController {

    @GetMapping("/api/me")
    public String getMyProfile(
            @RequestAttribute("jwtSubject") String userId,
            @RequestAttribute("jwtClaims") Claims claims) {
        
        String email = claims.get("email", String.class);
        return String.format("Hello %s, your User ID is %s", email, userId);
    }
}
```

---

## 🛡️ How It Works Internal Flow

1. **Pre-handling Request:** The `JwtAuthenticationInterceptor` intercepts all registered routes.
2. **Annotation Check:** Verification only executes if the targeted class or method has `@AuthGet401`, `@VerifyRoles`, or `@VerifyScope`. If missing, the request passes through cleanly.
3. **Cookie Extraction:** The interceptor looks for an `aact` cookie. If not provided, yields `401 Unauthorized`.
4. **Signature Verification:** Safely validates the token's Ed25519 cryptographic signature using the dynamically refreshed key.
5. **Role & Scope Evaluations:** Runs assertions on constraints specified in `@VerifyRoles` and `@VerifyScope`. 
6. **Request Augmentation:** Success injects standard token claims into Request Attributes: `jwtClaims` and `jwtSubject`.

## ⚖️ License

All rights reserved by Get401.
