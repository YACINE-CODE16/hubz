# HUBZ - Project Context for Claude Code

## 🎯 Project Overview
**Hubz** is a productivity app for managing multiple organizations, teams, tasks, goals, and personal habits.

---

## 🛠️ Tech Stack

### Backend
- **Java 21** (LTS)
- **Spring Boot 3.2+**
- **Spring Security** + **JWT** (jjwt-api 0.12.x)
- **Spring Data JPA** + **Hibernate**
- **PostgreSQL 16** (prod) / **H2** (dev)
- **Lombok** + **MapStruct**
- **Jakarta Validation**
- **JUnit 5** + **Mockito** + **AssertJ** (testing)

### Frontend
- **React 18** + **TypeScript**
- **Vite** (build tool)
- **TailwindCSS** (styling)
- **React Router v6** (navigation)
- **Axios** or **TanStack Query** (API calls)
- **Zustand** (state management)
- **Lucide React** (icons)
- **React Hook Form** (forms)
- **Vitest** + **React Testing Library** (testing)

### DevOps
- **Docker** + **Docker Compose**
- **Nginx** (reverse proxy)
- **GitHub Actions** (CI/CD)

---

## 🏗️ Architecture: Clean Architecture (4 Layers)

```
┌─────────────────────────────────────────┐
│           PRESENTATION                  │
│         (REST Controllers)              │
├─────────────────────────────────────────┤
│           APPLICATION                   │
│    (Services, DTOs, Use Cases, Ports)   │
├─────────────────────────────────────────┤
│             DOMAIN                      │
│   (Entities, Enums, Business Logic)     │
├─────────────────────────────────────────┤
│          INFRASTRUCTURE                 │
│    (JPA, Security, External APIs)       │
└─────────────────────────────────────────┘
```

### Rules (STRICT - MUST BE FOLLOWED)
- **Domain has NO dependencies** (pure Java, no Spring annotations, no JPA)
- **Application depends on Domain only** (no infrastructure imports)
- **Infrastructure implements Application ports** (adapters pattern)
- **Presentation uses Application services only** (no direct repository access)
- **NEVER expose JPA Entities** - ALWAYS use DTOs for API responses
- **NEVER import infrastructure packages in application layer**
- **NEVER import application/infrastructure packages in domain layer**
- **ALWAYS use port interfaces** for external dependencies (database, email, etc.)

### What NOT to Do (Anti-Patterns)
```java
// ❌ WRONG: Exposing JPA Entity in controller
@GetMapping("/{id}")
public UserEntity getUser(@PathVariable UUID id) {
    return userRepository.findById(id);
}

// ✅ CORRECT: Using DTO
@GetMapping("/{id}")
public UserResponse getUser(@PathVariable UUID id) {
    return userService.getUserById(id);
}

// ❌ WRONG: Using JPA annotations in domain
@Entity  // Never in domain layer!
public class User {
    @Id
    private UUID id;
}

// ✅ CORRECT: Pure domain model
public class User {
    private UUID id;
    // Pure business logic only
}

// ❌ WRONG: Repository in controller
@RestController
public class UserController {
    private final UserRepository repository;  // NO!
}

// ✅ CORRECT: Service in controller
@RestController
public class UserController {
    private final UserService service;  // YES!
}

// ❌ WRONG: Infrastructure dependency in application
public class UserService {
    private final UserJpaRepository repository;  // NO!
}

// ✅ CORRECT: Port interface in application
public class UserService {
    private final UserRepositoryPort repository;  // YES!
}
```

---

## 📁 Backend Structure

```
hubz-backend/
├── src/main/java/com/hubz/
│   ├── domain/
│   │   ├── model/           # Pure domain entities
│   │   ├── enums/           # TaskStatus, MemberRole, GoalType
│   │   └── exception/       # Business exceptions
│   │
│   ├── application/
│   │   ├── port/
│   │   │   ├── in/          # Use case interfaces
│   │   │   └── out/         # Repository interfaces
│   │   ├── service/         # Use case implementations
│   │   └── dto/
│   │       ├── request/     # Input DTOs
│   │       └── response/    # Output DTOs
│   │
│   ├── infrastructure/
│   │   ├── persistence/
│   │   │   ├── entity/      # JPA entities (@Entity)
│   │   │   ├── repository/  # JpaRepository interfaces
│   │   │   ├── adapter/     # Implements port/out interfaces
│   │   │   └── mapper/      # Entity <-> Domain mappers
│   │   ├── security/        # JWT, SecurityConfig
│   │   └── config/          # CORS, App configs
│   │
│   ├── presentation/
│   │   ├── controller/      # REST controllers
│   │   └── advice/          # Exception handlers
│   │
│   └── HubzApplication.java
```

---

## 📁 Frontend Structure

```
hubz-frontend/
├── src/
│   ├── components/
│   │   ├── ui/              # Button, Card, Input, Modal...
│   │   ├── layout/          # Sidebar, Header, Layout...
│   │   └── features/        # Feature-specific components
│   │
│   ├── pages/
│   │   ├── auth/            # Login, Register
│   │   ├── hub/             # Home/Hub page
│   │   ├── personal/        # Personal space pages
│   │   └── organization/    # Organization pages
│   │
│   ├── hooks/               # Custom React hooks
│   ├── services/            # API service functions
│   ├── stores/              # Zustand stores
│   ├── types/               # TypeScript interfaces
│   ├── utils/               # Helper functions
│   ├── lib/                 # Third-party configs
│   │
│   ├── App.tsx
│   ├── main.tsx
│   └── index.css
```

---

## 🗃️ Domain Models

### User
```java
UUID id
String email (unique)
String password (hashed)
String firstName
String lastName
String description
LocalDateTime createdAt
LocalDateTime updatedAt
```

### Organization
```java
UUID id
String name
String description
String icon
String color
String readme
UUID ownerId
LocalDateTime createdAt
```

### OrganizationMember
```java
UUID id
UUID organizationId
UUID userId
MemberRole role (OWNER, ADMIN, MEMBER, VIEWER)
LocalDateTime joinedAt
```

### OrganizationInvitation
```java
UUID id
UUID organizationId
String email
MemberRole role
String token (UUID)
UUID createdBy
LocalDateTime createdAt
LocalDateTime expiresAt (7 days)
Boolean used
UUID acceptedBy (nullable)
LocalDateTime acceptedAt (nullable)
```

### Team
```java
UUID id
String name
UUID organizationId
LocalDateTime createdAt
```

### Task
```java
UUID id
String title
String description
TaskStatus status (TODO, IN_PROGRESS, DONE)
TaskPriority priority (LOW, MEDIUM, HIGH, URGENT)
LocalDate dueDate
UUID organizationId
UUID teamId (nullable)
UUID assigneeId
UUID createdById
LocalDateTime createdAt
LocalDateTime updatedAt
```

### Goal
```java
UUID id
String title
GoalType type (SHORT, MEDIUM, LONG)
Integer targetValue
Integer currentValue
LocalDate deadline
UUID organizationId (nullable = personal)
UUID userId
LocalDateTime createdAt
```

### Event
```java
UUID id
String title
String description
LocalDateTime startTime
LocalDateTime endTime
String objective
UUID organizationId (nullable)
UUID userId
LocalDateTime createdAt
```

### Habit
```java
UUID id
String name
String icon
HabitFrequency frequency (DAILY, WEEKLY)
UUID userId
LocalDateTime createdAt
```

### HabitLog
```java
UUID id
UUID habitId
LocalDate date
Boolean completed
```

### Note
```java
UUID id
String title
String content
String category
UUID organizationId
UUID createdById
LocalDateTime createdAt
LocalDateTime updatedAt
```

---

## 🔌 API Endpoints

### Auth
```
POST /api/auth/register    # Create account
POST /api/auth/login       # Get JWT token
POST /api/auth/refresh     # Refresh token
GET  /api/auth/me          # Current user info
```

### Organizations
```
GET    /api/organizations
POST   /api/organizations
GET    /api/organizations/{id}
PUT    /api/organizations/{id}
DELETE /api/organizations/{id}
POST   /api/organizations/{id}/members
DELETE /api/organizations/{id}/members/{userId}
GET    /api/organizations/{id}/members

# Invitations
POST   /api/organizations/{orgId}/invitations     # Create invitation
GET    /api/organizations/{orgId}/invitations     # List invitations
GET    /api/invitations/{token}/info              # Get invitation details (public)
POST   /api/invitations/{token}/accept            # Accept invitation
DELETE /api/invitations/{invitationId}            # Delete invitation
```

### Teams
```
GET    /api/organizations/{orgId}/teams
POST   /api/organizations/{orgId}/teams
PUT    /api/teams/{id}
DELETE /api/teams/{id}
POST   /api/teams/{id}/members
DELETE /api/teams/{id}/members/{userId}
```

### Tasks
```
GET    /api/organizations/{orgId}/tasks
POST   /api/organizations/{orgId}/tasks
GET    /api/tasks/{id}
PUT    /api/tasks/{id}
PATCH  /api/tasks/{id}/status
DELETE /api/tasks/{id}
GET    /api/users/me/tasks    # All tasks across orgs
```

### Goals
```
GET    /api/organizations/{orgId}/goals
POST   /api/organizations/{orgId}/goals
GET    /api/users/me/goals    # Personal goals
POST   /api/users/me/goals
PUT    /api/goals/{id}
PATCH  /api/goals/{id}/progress
DELETE /api/goals/{id}
```

### Events
```
GET    /api/organizations/{orgId}/events
POST   /api/organizations/{orgId}/events
GET    /api/users/me/events   # Personal events
POST   /api/users/me/events
PUT    /api/events/{id}
DELETE /api/events/{id}
```

### Habits (Personal only)
```
GET    /api/users/me/habits
POST   /api/users/me/habits
PUT    /api/habits/{id}
DELETE /api/habits/{id}
POST   /api/habits/{id}/log
GET    /api/habits/{id}/logs
```

### Notes
```
GET    /api/organizations/{orgId}/notes
POST   /api/organizations/{orgId}/notes
PUT    /api/notes/{id}
DELETE /api/notes/{id}
```

---

## 🎨 UI/UX Specifications

### Colors (Tailwind)
```
Dark Mode:
- Background: #0A0A0F (dark-base)
- Cards: #12121A (dark-card)
- Hover: #1A1A24 (dark-hover)

Light Mode:
- Background: #F8FAFC (light-base)
- Cards: #FFFFFF (light-card)
- Hover: #F1F5F9 (light-hover)

Accent: #3B82F6 (blue)
Success: #22C55E
Warning: #F59E0B
Error: #EF4444
Info: #8B5CF6
```

### Typography
- Font: Inter
- H1: 32px Bold
- H2: 24px Semibold
- H3: 18px Semibold
- Body: 16px Regular
- Small: 14px Medium

### Components
- Border radius: 12px (rounded-xl)
- Cards: Glassmorphism (backdrop-blur, transparent bg)
- Icons: Lucide React (outline style)
- Animations: Subtle (200-300ms)

---

## 🔐 Security

### JWT Configuration
- Access token: 24h expiration
- Stored in localStorage (or httpOnly cookie)
- Header: `Authorization: Bearer <token>`

### Password
- Hashed with BCrypt
- Minimum 8 characters

### CORS
- Allow frontend origin only
- Allow credentials

---

## 📝 Coding Conventions

### Java
- Use Lombok (@Data, @Builder, @NoArgsConstructor, @AllArgsConstructor)
- Use UUID for all IDs
- Use Optional for nullable returns
- Validate inputs with @Valid and Jakarta annotations
- Use ResponseEntity for controller responses

### TypeScript/React
- Functional components only
- Use TypeScript interfaces for all props and data
- Use custom hooks for reusable logic
- Tailwind for all styling
- No inline styles

### Naming
- Java: camelCase for variables/methods, PascalCase for classes
- TypeScript: camelCase for variables/functions, PascalCase for components/types
- API endpoints: kebab-case for multi-word resources
- Database: snake_case for columns

---

## 🧪 Testing Requirements (MANDATORY)

### Testing Philosophy
- **ALL new features MUST have tests**
- **Tests are NOT optional** - they are part of the definition of "done"
- **Test coverage minimum: 70%** for all layers
- **Critical business logic: 90%+ coverage**
- **Write tests BEFORE or DURING implementation**, not after
- **Tests must be maintainable** - follow same clean architecture principles

---

### Backend Testing Stack

#### Frameworks & Libraries
```xml
<!-- JUnit 5 (Jupiter) -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>

<!-- Mockito (mocking) -->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>

<!-- Spring Boot Test -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- AssertJ (fluent assertions) -->
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <scope>test</scope>
</dependency>
```

---

### Testing by Layer

#### 1. Domain Layer Tests
**What to test:**
- Business logic and rules
- Domain model validations
- Domain exceptions
- Pure methods with no dependencies

**Characteristics:**
- NO Spring context needed
- NO mocks needed (pure Java)
- Fast execution (milliseconds)
- 100% coverage target

**Example:**
```java
class UserTest {

    @Test
    void shouldCreateValidUser() {
        // Given
        UUID id = UUID.randomUUID();
        String email = "test@example.com";

        // When
        User user = User.builder()
            .id(id)
            .email(email)
            .firstName("John")
            .lastName("Doe")
            .build();

        // Then
        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getFullName()).isEqualTo("John Doe");
    }

    @Test
    void shouldThrowExceptionWhenEmailInvalid() {
        // Given & When & Then
        assertThatThrownBy(() -> User.builder()
            .email("invalid-email")
            .build())
            .isInstanceOf(InvalidEmailException.class)
            .hasMessage("Email format is invalid");
    }
}
```

---

#### 2. Application Layer Tests (Services)
**What to test:**
- Use case logic
- Service orchestration
- Business rules enforcement
- Error handling
- Authorization checks

**Characteristics:**
- Mock repository ports (use Mockito)
- NO Spring context (unit tests)
- Test behavior, not implementation
- Fast execution

**Example:**
```java
class OrganizationServiceTest {

    @Mock
    private OrganizationRepositoryPort organizationRepository;

    @Mock
    private OrganizationMemberRepositoryPort memberRepository;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private OrganizationService organizationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldCreateOrganization() {
        // Given
        UUID userId = UUID.randomUUID();
        CreateOrganizationRequest request = CreateOrganizationRequest.builder()
            .name("Test Org")
            .description("Test Description")
            .build();

        Organization org = Organization.builder()
            .id(UUID.randomUUID())
            .name(request.getName())
            .description(request.getDescription())
            .ownerId(userId)
            .build();

        when(organizationRepository.save(any(Organization.class)))
            .thenReturn(org);

        // When
        OrganizationResponse response = organizationService
            .createOrganization(request, userId);

        // Then
        assertThat(response.getName()).isEqualTo("Test Org");
        assertThat(response.getDescription()).isEqualTo("Test Description");

        verify(organizationRepository).save(any(Organization.class));
        verify(memberRepository).save(any(OrganizationMember.class));
    }

    @Test
    void shouldThrowExceptionWhenUnauthorized() {
        // Given
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        doThrow(new UnauthorizedException("Not authorized"))
            .when(authorizationService)
            .checkOrganizationAdminAccess(orgId, userId);

        // When & Then
        assertThatThrownBy(() ->
            organizationService.deleteOrganization(orgId, userId))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessage("Not authorized");
    }
}
```

---

#### 3. Infrastructure Layer Tests (Adapters, Repositories)
**What to test:**
- JPA repository queries
- Entity-Domain mapping
- Database constraints
- Adapter implementations

**Characteristics:**
- Use `@DataJpaTest` for repository tests
- Use in-memory H2 database
- Test actual database interactions
- Slower than unit tests

**Example:**
```java
@DataJpaTest
class OrganizationJpaRepositoryTest {

    @Autowired
    private OrganizationJpaRepository repository;

    @Test
    void shouldSaveAndFindOrganization() {
        // Given
        OrganizationEntity entity = OrganizationEntity.builder()
            .id(UUID.randomUUID())
            .name("Test Org")
            .description("Test")
            .ownerId(UUID.randomUUID())
            .createdAt(LocalDateTime.now())
            .build();

        // When
        OrganizationEntity saved = repository.save(entity);
        Optional<OrganizationEntity> found = repository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Org");
    }

    @Test
    void shouldFindByOwnerId() {
        // Given
        UUID ownerId = UUID.randomUUID();
        OrganizationEntity org1 = createOrganization("Org 1", ownerId);
        OrganizationEntity org2 = createOrganization("Org 2", ownerId);
        repository.saveAll(List.of(org1, org2));

        // When
        List<OrganizationEntity> organizations =
            repository.findByOwnerId(ownerId);

        // Then
        assertThat(organizations).hasSize(2);
        assertThat(organizations)
            .extracting(OrganizationEntity::getName)
            .containsExactlyInAnyOrder("Org 1", "Org 2");
    }
}
```

---

#### 4. Presentation Layer Tests (Controllers)
**What to test:**
- HTTP endpoints
- Request/Response mapping
- Status codes
- Authentication/Authorization
- Input validation

**Characteristics:**
- Use `@WebMvcTest` for controller tests
- Mock service layer with `@MockBean`
- Use `MockMvc` to simulate HTTP requests
- Test HTTP layer only, not business logic

**Example:**
```java
@WebMvcTest(OrganizationController.class)
class OrganizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrganizationService organizationService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void shouldCreateOrganization() throws Exception {
        // Given
        CreateOrganizationRequest request = CreateOrganizationRequest.builder()
            .name("Test Org")
            .description("Test")
            .build();

        OrganizationResponse response = OrganizationResponse.builder()
            .id(UUID.randomUUID())
            .name("Test Org")
            .description("Test")
            .build();

        when(organizationService.createOrganization(any(), any()))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "Test Org",
                        "description": "Test"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Test Org"))
            .andExpect(jsonPath("$.description").value("Test"));
    }

    @Test
    void shouldReturn400WhenNameMissing() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "description": "Test"
                    }
                    """))
            .andExpect(status().isBadRequest());
    }
}
```

---

#### 5. Integration Tests
**What to test:**
- Full request-to-database flow
- Multiple layers working together
- Real authentication/authorization
- Transaction management

**Characteristics:**
- Use `@SpringBootTest`
- Use `TestRestTemplate` or `MockMvc`
- Slower execution
- Test realistic scenarios

**Example:**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrganizationIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OrganizationJpaRepository organizationRepository;

    private String authToken;

    @BeforeEach
    void setUp() {
        // Create user and get JWT token
        authToken = authenticateUser("test@example.com", "password");
    }

    @Test
    void shouldCreateAndRetrieveOrganization() {
        // Given
        CreateOrganizationRequest request = CreateOrganizationRequest.builder()
            .name("Integration Test Org")
            .description("Full flow test")
            .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        HttpEntity<CreateOrganizationRequest> entity =
            new HttpEntity<>(request, headers);

        // When - Create
        ResponseEntity<OrganizationResponse> createResponse =
            restTemplate.exchange(
                "/api/organizations",
                HttpMethod.POST,
                entity,
                OrganizationResponse.class
            );

        // Then - Verify creation
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody().getName())
            .isEqualTo("Integration Test Org");

        UUID orgId = createResponse.getBody().getId();

        // When - Retrieve
        ResponseEntity<OrganizationResponse> getResponse =
            restTemplate.exchange(
                "/api/organizations/" + orgId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                OrganizationResponse.class
            );

        // Then - Verify retrieval
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().getName())
            .isEqualTo("Integration Test Org");

        // Verify database
        Optional<OrganizationEntity> dbOrg =
            organizationRepository.findById(orgId);
        assertThat(dbOrg).isPresent();
    }
}
```

---

### Frontend Testing Stack

#### Frameworks & Libraries
```json
{
  "devDependencies": {
    "vitest": "^1.0.0",
    "@testing-library/react": "^14.0.0",
    "@testing-library/user-event": "^14.0.0",
    "@testing-library/jest-dom": "^6.0.0",
    "msw": "^2.0.0"
  }
}
```

---

### Frontend Testing Requirements

#### 1. Component Tests
**What to test:**
- Component rendering
- User interactions
- Conditional rendering
- Props handling
- Event handlers

**Example:**
```typescript
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { Button } from './Button';

describe('Button', () => {
  it('should render with text', () => {
    render(<Button>Click me</Button>);
    expect(screen.getByText('Click me')).toBeInTheDocument();
  });

  it('should call onClick when clicked', () => {
    const handleClick = vi.fn();
    render(<Button onClick={handleClick}>Click</Button>);

    fireEvent.click(screen.getByText('Click'));
    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it('should be disabled when disabled prop is true', () => {
    render(<Button disabled>Click</Button>);
    expect(screen.getByText('Click')).toBeDisabled();
  });
});
```

#### 2. Hook Tests
**What to test:**
- Custom hooks logic
- State management
- Side effects
- Error handling

**Example:**
```typescript
import { renderHook, act } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { useOrganization } from './useOrganization';

describe('useOrganization', () => {
  it('should fetch organization data', async () => {
    const { result } = renderHook(() => useOrganization('org-id'));

    await act(async () => {
      await result.current.fetchOrganization();
    });

    expect(result.current.organization).toBeDefined();
    expect(result.current.loading).toBe(false);
  });
});
```

#### 3. Integration Tests (API Mocking)
**What to test:**
- Full user flows
- API interactions
- Navigation
- State updates

**Example:**
```typescript
import { render, screen, waitFor } from '@testing-library/react';
import { setupServer } from 'msw/node';
import { rest } from 'msw';
import { OrganizationPage } from './OrganizationPage';

const server = setupServer(
  rest.get('/api/organizations/:id', (req, res, ctx) => {
    return res(ctx.json({
      id: req.params.id,
      name: 'Test Org',
      description: 'Test Description'
    }));
  })
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('OrganizationPage', () => {
  it('should display organization data', async () => {
    render(<OrganizationPage />);

    await waitFor(() => {
      expect(screen.getByText('Test Org')).toBeInTheDocument();
    });
  });
});
```

---

### Test Coverage Requirements

| Layer | Minimum Coverage | Target Coverage |
|-------|-----------------|-----------------|
| Domain Models | 90% | 100% |
| Application Services | 80% | 90% |
| Infrastructure Adapters | 70% | 80% |
| Presentation Controllers | 70% | 80% |
| Frontend Components | 70% | 80% |
| **Overall Project** | **70%** | **85%** |

---

### Testing Commands

#### Backend
```bash
# Run all tests
./mvnw test

# Run tests with coverage
./mvnw test jacoco:report

# Run specific test class
./mvnw test -Dtest=OrganizationServiceTest

# Run tests in specific package
./mvnw test -Dtest=com.hubz.application.service.*
```

#### Frontend
```bash
# Run all tests
npm run test

# Run tests in watch mode
npm run test:watch

# Run tests with coverage
npm run test:coverage

# Run specific test file
npm run test OrganizationPage.test.tsx
```

---

### Test Organization

#### Backend Test Structure
```
src/test/java/com/hubz/
├── domain/
│   └── model/
│       ├── UserTest.java
│       ├── OrganizationTest.java
│       └── TaskTest.java
├── application/
│   └── service/
│       ├── UserServiceTest.java
│       ├── OrganizationServiceTest.java
│       └── TaskServiceTest.java
├── infrastructure/
│   ├── persistence/
│   │   ├── adapter/
│   │   │   └── OrganizationRepositoryAdapterTest.java
│   │   └── repository/
│   │       └── OrganizationJpaRepositoryTest.java
│   └── mapper/
│       └── OrganizationMapperTest.java
├── presentation/
│   └── controller/
│       ├── OrganizationControllerTest.java
│       └── TaskControllerTest.java
└── integration/
    ├── OrganizationIntegrationTest.java
    └── AuthenticationIntegrationTest.java
```

#### Frontend Test Structure
```
src/
├── components/
│   ├── ui/
│   │   ├── Button.tsx
│   │   ├── Button.test.tsx
│   │   ├── Card.tsx
│   │   └── Card.test.tsx
│   └── features/
│       ├── OrganizationCard.tsx
│       └── OrganizationCard.test.tsx
├── hooks/
│   ├── useOrganization.ts
│   └── useOrganization.test.ts
├── services/
│   ├── organization.service.ts
│   └── organization.service.test.ts
└── pages/
    ├── OrganizationPage.tsx
    └── OrganizationPage.test.tsx
```

---

### Testing Best Practices

#### General Rules
1. **Test behavior, not implementation** - Don't test private methods
2. **One assertion per test** - Keep tests focused and simple
3. **Arrange-Act-Assert (AAA) pattern** - Structure all tests consistently
4. **Descriptive test names** - `shouldCreateOrganizationWhenValidRequest`
5. **No logic in tests** - Tests should be straightforward
6. **Independent tests** - Each test should run in isolation
7. **Fast tests** - Unit tests should run in milliseconds
8. **Deterministic tests** - No random data, no time dependencies

#### What NOT to Test
- Framework code (Spring, React)
- Third-party libraries
- Getters/Setters (unless they have logic)
- Private methods directly
- Configuration files

#### When to Write Tests
1. **Before implementation** (TDD approach) - Write test first
2. **During implementation** - Write test alongside code
3. **NEVER after feature is "done"** - Tests are part of "done"

#### Coverage vs Quality
- **70% coverage ≠ good tests** - Quality matters more than quantity
- **Focus on critical paths** - Auth, payments, data loss scenarios
- **Edge cases matter** - Test null, empty, boundary values
- **Happy path + error cases** - Both must be tested

---

## 🚀 Development Commands

### Backend
```bash
cd hubz-backend
./mvnw spring-boot:run           # Run dev server
./mvnw clean package             # Build JAR
./mvnw test                      # Run tests
```

### Frontend
```bash
cd hubz-frontend
npm run dev                      # Run dev server (port 5173)
npm run build                    # Build for production
npm run preview                  # Preview production build
```

### Docker
```bash
docker-compose up -d             # Start all services
docker-compose down              # Stop all services
docker-compose logs -f           # View logs
```

---

## ✅ Current Progress

- [ ] Backend Setup (Spring Boot init)
- [ ] Auth Module (Register, Login, JWT)
- [ ] Frontend Setup (Vite + React + Tailwind)
- [ ] Auth Pages (Login, Register)
- [ ] Organizations CRUD
- [ ] Teams CRUD
- [ ] Tasks (Kanban)
- [ ] Goals
- [ ] Calendar/Events
- [ ] Habits
- [ ] Personal Space
- [ ] Docker Setup
- [ ] Deployment

---

## 🎯 Development Principles (CRITICAL)

### ALWAYS Remember:
1. **Clean Architecture is NON-NEGOTIABLE** - Follow the 4-layer architecture strictly
2. **Tests are MANDATORY** - Every feature must have tests (minimum 70% coverage)
3. **DTOs ALWAYS** - Never expose JPA entities in controllers
4. **Port/Adapter Pattern** - Infrastructure implements application ports
5. **Domain Purity** - Domain layer has ZERO external dependencies

### Before Every Feature:
1. ✅ Design the domain model (pure Java)
2. ✅ Define DTOs (request/response)
3. ✅ Create repository port interface (application layer)
4. ✅ Implement service with business logic
5. ✅ Write tests for service layer
6. ✅ Create JPA entity and repository adapter (infrastructure)
7. ✅ Write repository tests
8. ✅ Implement controller (presentation)
9. ✅ Write controller tests
10. ✅ Run all tests and verify coverage

### Current Development Status
See [FEATURES.md](FEATURES.md) for detailed feature status and roadmap.
