# WeGo TDD 測試開發指南

## 1. TDD 概述

### 1.1 什麼是 TDD

Test-Driven Development (測試驅動開發) 是一種軟體開發方法，遵循「紅-綠-重構」循環：

```
    ┌─────────────┐
    │   1. RED    │  ← 撰寫失敗的測試
    │  寫測試     │
    └──────┬──────┘
           │
           ▼
    ┌─────────────┐
    │  2. GREEN   │  ← 撰寫最小程式碼使測試通過
    │  寫程式碼   │
    └──────┬──────┘
           │
           ▼
    ┌─────────────┐
    │ 3. REFACTOR │  ← 重構程式碼，保持測試通過
    │  重構       │
    └──────┬──────┘
           │
           └───────────→ 回到步驟 1
```

### 1.2 TDD 的好處

| 優點 | 說明 |
|------|------|
| 更好的設計 | 先思考如何使用 API，產生更好的介面設計 |
| 即時回饋 | 每次修改都能立即驗證是否破壞既有功能 |
| 文件化 | 測試即文件，展示程式碼的預期行為 |
| 信心重構 | 有完整測試覆蓋，重構時更有信心 |
| 減少 Debug | 問題在早期就被發現 |

### 1.3 WeGo 專案的測試策略

```
                    ┌─────────────────┐
                    │    E2E Tests    │  ← 少量，驗證關鍵流程
                    │   (Selenium)    │
                    └────────┬────────┘
                             │
              ┌──────────────┴──────────────┐
              │      Integration Tests      │  ← 中量，驗證模組整合
              │   (Spring Boot Test)        │
              └──────────────┬──────────────┘
                             │
    ┌────────────────────────┴────────────────────────┐
    │                   Unit Tests                    │  ← 大量，驗證單一類別
    │               (JUnit 5 + Mockito)               │
    └─────────────────────────────────────────────────┘
```

**測試比例建議**: Unit : Integration : E2E = 70% : 20% : 10%

---

## 2. 測試環境設定

### 2.1 Maven 依賴

```xml
<!-- pom.xml -->
<dependencies>
    <!-- Spring Boot Test Starter -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- JUnit 5 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.10.1</version>
        <scope>test</scope>
    </dependency>

    <!-- Mockito -->
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <version>5.8.0</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-junit-jupiter</artifactId>
        <version>5.8.0</version>
        <scope>test</scope>
    </dependency>

    <!-- AssertJ - 流暢斷言 -->
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <version>3.24.2</version>
        <scope>test</scope>
    </dependency>

    <!-- H2 Database - 測試用記憶體資料庫 -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Testcontainers - 整合測試用 -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <!-- Surefire Plugin - Unit Tests -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.2.3</version>
            <configuration>
                <includes>
                    <include>**/*Test.java</include>
                </includes>
                <excludes>
                    <exclude>**/*IntegrationTest.java</exclude>
                    <exclude>**/*E2ETest.java</exclude>
                </excludes>
            </configuration>
        </plugin>

        <!-- Failsafe Plugin - Integration Tests -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-failsafe-plugin</artifactId>
            <version>3.2.3</version>
            <configuration>
                <includes>
                    <include>**/*IntegrationTest.java</include>
                </includes>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>integration-test</goal>
                        <goal>verify</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>

        <!-- JaCoCo - 測試覆蓋率 -->
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.11</version>
            <executions>
                <execution>
                    <goals>
                        <goal>prepare-agent</goal>
                    </goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>test</phase>
                    <goals>
                        <goal>report</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### 2.2 測試設定檔

```yaml
# src/test/resources/application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: test-client-id
            client-secret: test-client-secret

# 停用外部 API
wego:
  google-maps:
    enabled: false
  weather:
    enabled: false
  exchange-rate:
    enabled: false
```

### 2.3 測試目錄結構

```
src/test/java/com/wego/
├── unit/                           # 單元測試
│   ├── service/
│   │   ├── TripServiceTest.java
│   │   ├── ActivityServiceTest.java
│   │   ├── ExpenseServiceTest.java
│   │   └── SettlementServiceTest.java
│   ├── domain/
│   │   ├── DebtSimplifierTest.java
│   │   ├── RouteOptimizerTest.java
│   │   └── PermissionCheckerTest.java
│   └── mapper/
│       ├── TripMapperTest.java
│       └── ExpenseMapperTest.java
│
├── integration/                    # 整合測試
│   ├── repository/
│   │   ├── TripRepositoryIntegrationTest.java
│   │   └── ExpenseRepositoryIntegrationTest.java
│   ├── api/
│   │   ├── TripApiIntegrationTest.java
│   │   ├── ActivityApiIntegrationTest.java
│   │   └── ExpenseApiIntegrationTest.java
│   └── security/
│       └── OAuth2IntegrationTest.java
│
├── e2e/                            # 端對端測試
│   ├── TripE2ETest.java
│   └── ExpenseE2ETest.java
│
├── fixture/                        # 測試資料工廠
│   ├── UserFixture.java
│   ├── TripFixture.java
│   ├── ActivityFixture.java
│   └── ExpenseFixture.java
│
└── config/                         # 測試設定
    ├── TestConfig.java
    └── TestSecurityConfig.java
```

---

## 3. 單元測試 (Unit Tests)

### 3.1 命名規範

```java
// 格式: methodName_scenario_expectedBehavior
@Test
void createTrip_withValidInput_shouldReturnCreatedTrip() { }

@Test
void createTrip_withNullTitle_shouldThrowValidationException() { }

@Test
void calculateSettlement_withThreeMembers_shouldReturnSimplifiedDebts() { }
```

### 3.2 Service 層測試範例

```java
@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripMemberRepository memberRepository;

    @Mock
    private PermissionChecker permissionChecker;

    @InjectMocks
    private TripService tripService;

    private User owner;
    private CreateTripRequest request;

    @BeforeEach
    void setUp() {
        owner = UserFixture.createUser();
        request = new CreateTripRequest();
        request.setTitle("東京五日遊");
        request.setStartDate(LocalDate.of(2024, 3, 15));
        request.setEndDate(LocalDate.of(2024, 3, 19));
        request.setBaseCurrency("TWD");
    }

    @Nested
    @DisplayName("建立行程")
    class CreateTrip {

        @Test
        @DisplayName("正常情況：應建立行程並設定建立者為 Owner")
        void withValidInput_shouldCreateTripAndSetOwner() {
            // Given
            when(tripRepository.save(any(Trip.class)))
                .thenAnswer(invocation -> {
                    Trip trip = invocation.getArgument(0);
                    trip.setId(UUID.randomUUID());
                    return trip;
                });

            // When
            TripResponse response = tripService.createTrip(request, owner);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTitle()).isEqualTo("東京五日遊");

            // 驗證 Repository 被正確呼叫
            verify(tripRepository).save(argThat(trip ->
                trip.getTitle().equals("東京五日遊") &&
                trip.getOwnerId().equals(owner.getId())
            ));

            // 驗證成員關係被建立
            verify(memberRepository).save(argThat(member ->
                member.getRole() == Role.OWNER &&
                member.getUserId().equals(owner.getId())
            ));
        }

        @Test
        @DisplayName("結束日期早於開始日期：應拋出例外")
        void withEndDateBeforeStartDate_shouldThrowException() {
            // Given
            request.setEndDate(LocalDate.of(2024, 3, 10)); // 早於開始日期

            // When & Then
            assertThatThrownBy(() -> tripService.createTrip(request, owner))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("結束日期不可早於開始日期");

            verify(tripRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("更新行程")
    class UpdateTrip {

        @Test
        @DisplayName("Owner 更新：應成功更新")
        void asOwner_shouldUpdateSuccessfully() {
            // Given
            UUID tripId = UUID.randomUUID();
            Trip existingTrip = TripFixture.createTrip(tripId, owner.getId());

            when(tripRepository.findById(tripId)).thenReturn(Optional.of(existingTrip));
            when(permissionChecker.canEdit(tripId, owner.getId())).thenReturn(true);

            UpdateTripRequest updateRequest = new UpdateTripRequest();
            updateRequest.setTitle("大阪七日遊");

            // When
            TripResponse response = tripService.updateTrip(tripId, updateRequest, owner);

            // Then
            assertThat(response.getTitle()).isEqualTo("大阪七日遊");
            verify(tripRepository).save(any());
        }

        @Test
        @DisplayName("Viewer 更新：應拋出權限不足例外")
        void asViewer_shouldThrowForbiddenException() {
            // Given
            UUID tripId = UUID.randomUUID();
            User viewer = UserFixture.createUser();

            when(permissionChecker.canEdit(tripId, viewer.getId())).thenReturn(false);

            UpdateTripRequest updateRequest = new UpdateTripRequest();

            // When & Then
            assertThatThrownBy(() ->
                tripService.updateTrip(tripId, updateRequest, viewer))
                .isInstanceOf(ForbiddenException.class);

            verify(tripRepository, never()).save(any());
        }
    }
}
```

### 3.3 Domain 層測試範例

```java
class DebtSimplifierTest {

    private DebtSimplifier debtSimplifier;

    @BeforeEach
    void setUp() {
        debtSimplifier = new DebtSimplifier();
    }

    @Nested
    @DisplayName("簡化債務關係")
    class Simplify {

        @Test
        @DisplayName("三人均分：A 付 300，B 付 600 → A 付 B 100，C 付 B 200")
        void threeMembers_evenSplit_shouldSimplifyCorrectly() {
            // Given
            UUID userA = UUID.randomUUID();
            UUID userB = UUID.randomUUID();
            UUID userC = UUID.randomUUID();

            // A 付了 300，三人均分 (每人 100)
            // B 付了 600，三人均分 (每人 200)
            Map<UUID, BigDecimal> payments = Map.of(
                userA, new BigDecimal("300"),
                userB, new BigDecimal("600"),
                userC, BigDecimal.ZERO
            );

            List<ExpenseSplit> splits = List.of(
                // 300 的分攤
                new ExpenseSplit(userA, new BigDecimal("100")),
                new ExpenseSplit(userB, new BigDecimal("100")),
                new ExpenseSplit(userC, new BigDecimal("100")),
                // 600 的分攤
                new ExpenseSplit(userA, new BigDecimal("200")),
                new ExpenseSplit(userB, new BigDecimal("200")),
                new ExpenseSplit(userC, new BigDecimal("200"))
            );

            // When
            List<Settlement> settlements = debtSimplifier.simplify(splits, payments);

            // Then
            // 淨額計算：
            // A: 付 300，應付 300 → 淨額 0... 等等重新算
            // A: 付 300，應分攤 100+200=300 → 淨額 0
            // B: 付 600，應分攤 100+200=300 → 淨額 +300 (應收)
            // C: 付 0，應分攤 100+200=300 → 淨額 -300 (應付)

            assertThat(settlements).hasSize(1);
            assertThat(settlements.get(0))
                .satisfies(s -> {
                    assertThat(s.getFromUserId()).isEqualTo(userC);
                    assertThat(s.getToUserId()).isEqualTo(userB);
                    assertThat(s.getAmount()).isEqualByComparingTo("300");
                });
        }

        @Test
        @DisplayName("複雜情況：四人多筆支出，應最小化交易次數")
        void fourMembers_multipleExpenses_shouldMinimizeTransactions() {
            // Given
            UUID userA = UUID.randomUUID();
            UUID userB = UUID.randomUUID();
            UUID userC = UUID.randomUUID();
            UUID userD = UUID.randomUUID();

            // 設定複雜的支出情境...
            Map<UUID, BigDecimal> payments = Map.of(
                userA, new BigDecimal("1000"),
                userB, new BigDecimal("500"),
                userC, new BigDecimal("300"),
                userD, new BigDecimal("200")
            );

            // 總共 2000，四人均分，每人應付 500
            List<ExpenseSplit> splits = List.of(
                new ExpenseSplit(userA, new BigDecimal("500")),
                new ExpenseSplit(userB, new BigDecimal("500")),
                new ExpenseSplit(userC, new BigDecimal("500")),
                new ExpenseSplit(userD, new BigDecimal("500"))
            );

            // When
            List<Settlement> settlements = debtSimplifier.simplify(splits, payments);

            // Then
            // 淨額：A +500, B 0, C -200, D -300
            // 最佳解：C 付 A 200, D 付 A 300
            assertThat(settlements).hasSize(2);

            BigDecimal totalTransferred = settlements.stream()
                .map(Settlement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(totalTransferred).isEqualByComparingTo("500");
        }

        @Test
        @DisplayName("已結清：無債務關係")
        void allSettled_shouldReturnEmptyList() {
            // Given
            UUID userA = UUID.randomUUID();
            UUID userB = UUID.randomUUID();

            Map<UUID, BigDecimal> payments = Map.of(
                userA, new BigDecimal("100"),
                userB, new BigDecimal("100")
            );

            List<ExpenseSplit> splits = List.of(
                new ExpenseSplit(userA, new BigDecimal("100")),
                new ExpenseSplit(userB, new BigDecimal("100"))
            );

            // When
            List<Settlement> settlements = debtSimplifier.simplify(splits, payments);

            // Then
            assertThat(settlements).isEmpty();
        }
    }
}
```

### 3.4 路線優化測試範例

```java
class RouteOptimizerTest {

    private RouteOptimizer routeOptimizer;

    @BeforeEach
    void setUp() {
        routeOptimizer = new RouteOptimizer();
    }

    @Test
    @DisplayName("三個景點：應按最近鄰居排序")
    void threeActivities_shouldOptimizeByNearestNeighbor() {
        // Given
        // A(0,0) → B(10,0) → C(5,0)
        // 原始順序 A → B → C，總距離 = 10 + 5 = 15
        // 優化順序 A → C → B，總距離 = 5 + 5 = 10

        Activity activityA = createActivityWithPlace("A", 0, 0);
        Activity activityB = createActivityWithPlace("B", 10, 0);
        Activity activityC = createActivityWithPlace("C", 5, 0);

        List<Activity> original = List.of(activityA, activityB, activityC);

        // When
        List<Activity> optimized = routeOptimizer.optimize(original);

        // Then
        assertThat(optimized).hasSize(3);
        assertThat(optimized.get(0).getPlace().getName()).isEqualTo("A");
        assertThat(optimized.get(1).getPlace().getName()).isEqualTo("C");
        assertThat(optimized.get(2).getPlace().getName()).isEqualTo("B");
    }

    @Test
    @DisplayName("兩個以下景點：直接返回原列表")
    void twoOrLessActivities_shouldReturnOriginal() {
        // Given
        Activity activityA = createActivityWithPlace("A", 0, 0);
        Activity activityB = createActivityWithPlace("B", 10, 0);

        List<Activity> original = List.of(activityA, activityB);

        // When
        List<Activity> optimized = routeOptimizer.optimize(original);

        // Then
        assertThat(optimized).isEqualTo(original);
    }

    @Test
    @DisplayName("空列表：應返回空列表")
    void emptyList_shouldReturnEmpty() {
        // Given
        List<Activity> original = List.of();

        // When
        List<Activity> optimized = routeOptimizer.optimize(original);

        // Then
        assertThat(optimized).isEmpty();
    }

    private Activity createActivityWithPlace(String name, double lat, double lng) {
        Place place = new Place();
        place.setName(name);
        place.setLat(BigDecimal.valueOf(lat));
        place.setLng(BigDecimal.valueOf(lng));

        Activity activity = new Activity();
        activity.setPlace(place);
        return activity;
    }
}
```

---

## 4. 整合測試 (Integration Tests)

### 4.1 Repository 整合測試

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class TripRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(UserFixture.createUser());
    }

    @Test
    @DisplayName("依用戶 ID 查詢行程列表")
    void findByMemberId_shouldReturnUserTrips() {
        // Given
        Trip trip1 = TripFixture.createTrip(owner.getId());
        trip1.setTitle("東京行");
        tripRepository.save(trip1);

        Trip trip2 = TripFixture.createTrip(owner.getId());
        trip2.setTitle("大阪行");
        tripRepository.save(trip2);

        entityManager.flush();
        entityManager.clear();

        // When
        List<Trip> trips = tripRepository.findByMemberId(owner.getId());

        // Then
        assertThat(trips).hasSize(2);
        assertThat(trips).extracting(Trip::getTitle)
            .containsExactlyInAnyOrder("東京行", "大阪行");
    }

    @Test
    @DisplayName("依日期範圍查詢")
    void findByDateRange_shouldReturnMatchingTrips() {
        // Given
        Trip pastTrip = TripFixture.createTrip(owner.getId());
        pastTrip.setStartDate(LocalDate.of(2024, 1, 1));
        pastTrip.setEndDate(LocalDate.of(2024, 1, 5));
        tripRepository.save(pastTrip);

        Trip currentTrip = TripFixture.createTrip(owner.getId());
        currentTrip.setStartDate(LocalDate.of(2024, 3, 1));
        currentTrip.setEndDate(LocalDate.of(2024, 3, 10));
        tripRepository.save(currentTrip);

        entityManager.flush();

        // When
        List<Trip> trips = tripRepository.findTripsInDateRange(
            LocalDate.of(2024, 2, 1),
            LocalDate.of(2024, 4, 1)
        );

        // Then
        assertThat(trips).hasSize(1);
        assertThat(trips.get(0).getStartDate())
            .isEqualTo(LocalDate.of(2024, 3, 1));
    }
}
```

### 4.2 API 整合測試

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TripApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private String authToken;

    @BeforeEach
    void setUp() {
        // 清理資料
        tripRepository.deleteAll();
        userRepository.deleteAll();

        // 建立測試用戶並取得 token
        testUser = userRepository.save(UserFixture.createUser());
        authToken = generateTestToken(testUser);
    }

    @Nested
    @DisplayName("POST /api/trips")
    class CreateTrip {

        @Test
        @DisplayName("有效請求：應返回 201 Created")
        void withValidRequest_shouldReturn201() throws Exception {
            // Given
            CreateTripRequest request = new CreateTripRequest();
            request.setTitle("東京五日遊");
            request.setStartDate(LocalDate.of(2024, 3, 15));
            request.setEndDate(LocalDate.of(2024, 3, 19));

            // When & Then
            mockMvc.perform(post("/api/trips")
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("東京五日遊"))
                .andExpect(jsonPath("$.data.id").exists());

            // 驗證資料庫
            assertThat(tripRepository.findAll()).hasSize(1);
        }

        @Test
        @DisplayName("缺少標題：應返回 400 Bad Request")
        void withMissingTitle_shouldReturn400() throws Exception {
            // Given
            CreateTripRequest request = new CreateTripRequest();
            request.setStartDate(LocalDate.of(2024, 3, 15));
            request.setEndDate(LocalDate.of(2024, 3, 19));
            // title 為空

            // When & Then
            mockMvc.perform(post("/api/trips")
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("行程名稱不可為空"));
        }

        @Test
        @DisplayName("未登入：應返回 401 Unauthorized")
        void withoutAuth_shouldReturn401() throws Exception {
            // Given
            CreateTripRequest request = new CreateTripRequest();
            request.setTitle("東京五日遊");
            request.setStartDate(LocalDate.of(2024, 3, 15));
            request.setEndDate(LocalDate.of(2024, 3, 19));

            // When & Then
            mockMvc.perform(post("/api/trips")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/trips/{tripId}")
    class GetTrip {

        @Test
        @DisplayName("行程成員查詢：應返回行程詳情")
        void asMember_shouldReturnTripDetail() throws Exception {
            // Given
            Trip trip = TripFixture.createTrip(testUser.getId());
            trip = tripRepository.save(trip);

            // When & Then
            mockMvc.perform(get("/api/trips/{tripId}", trip.getId())
                    .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(trip.getId().toString()));
        }

        @Test
        @DisplayName("非成員查詢：應返回 403 Forbidden")
        void asNonMember_shouldReturn403() throws Exception {
            // Given
            User otherUser = userRepository.save(UserFixture.createUser());
            Trip trip = TripFixture.createTrip(otherUser.getId());
            trip = tripRepository.save(trip);

            // When & Then
            mockMvc.perform(get("/api/trips/{tripId}", trip.getId())
                    .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("不存在的行程：應返回 404 Not Found")
        void withNonExistentTrip_shouldReturn404() throws Exception {
            // Given
            UUID nonExistentId = UUID.randomUUID();

            // When & Then
            mockMvc.perform(get("/api/trips/{tripId}", nonExistentId)
                    .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("TRIP_NOT_FOUND"));
        }
    }
}
```

---

## 5. 測試資料工廠 (Test Fixtures)

### 5.1 User Fixture

```java
public class UserFixture {

    private static final AtomicLong counter = new AtomicLong();

    public static User createUser() {
        long id = counter.incrementAndGet();
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user" + id + "@test.com");
        user.setNickname("TestUser" + id);
        user.setProvider("google");
        user.setProviderId("google-" + id);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    public static User createUser(String email, String nickname) {
        User user = createUser();
        user.setEmail(email);
        user.setNickname(nickname);
        return user;
    }
}
```

### 5.2 Trip Fixture

```java
public class TripFixture {

    public static Trip createTrip(UUID ownerId) {
        Trip trip = new Trip();
        trip.setId(UUID.randomUUID());
        trip.setTitle("測試行程");
        trip.setStartDate(LocalDate.now().plusDays(7));
        trip.setEndDate(LocalDate.now().plusDays(10));
        trip.setBaseCurrency("TWD");
        trip.setOwnerId(ownerId);
        trip.setCreatedAt(LocalDateTime.now());
        trip.setUpdatedAt(LocalDateTime.now());
        return trip;
    }

    public static Trip createTrip(UUID tripId, UUID ownerId) {
        Trip trip = createTrip(ownerId);
        trip.setId(tripId);
        return trip;
    }

    public static Trip createTripWithDates(UUID ownerId,
                                            LocalDate startDate,
                                            LocalDate endDate) {
        Trip trip = createTrip(ownerId);
        trip.setStartDate(startDate);
        trip.setEndDate(endDate);
        return trip;
    }
}
```

### 5.3 Expense Fixture

```java
public class ExpenseFixture {

    public static Expense createExpense(UUID tripId, UUID payerId) {
        Expense expense = new Expense();
        expense.setId(UUID.randomUUID());
        expense.setTripId(tripId);
        expense.setTitle("測試支出");
        expense.setAmount(new BigDecimal("1000"));
        expense.setCurrency("TWD");
        expense.setExchangeRate(BigDecimal.ONE);
        expense.setPayerId(payerId);
        expense.setCategory("餐飲");
        expense.setCreatedAt(LocalDateTime.now());
        return expense;
    }

    public static Expense createExpense(UUID tripId,
                                         UUID payerId,
                                         BigDecimal amount,
                                         String currency) {
        Expense expense = createExpense(tripId, payerId);
        expense.setAmount(amount);
        expense.setCurrency(currency);
        return expense;
    }

    public static List<ExpenseSplit> createEqualSplits(UUID expenseId,
                                                        List<UUID> userIds,
                                                        BigDecimal totalAmount) {
        BigDecimal splitAmount = totalAmount.divide(
            BigDecimal.valueOf(userIds.size()),
            2,
            RoundingMode.HALF_UP
        );

        return userIds.stream()
            .map(userId -> {
                ExpenseSplit split = new ExpenseSplit();
                split.setId(UUID.randomUUID());
                split.setExpenseId(expenseId);
                split.setUserId(userId);
                split.setAmount(splitAmount);
                split.setSettled(false);
                return split;
            })
            .collect(Collectors.toList());
    }
}
```

---

## 6. 測試覆蓋率目標

### 6.1 覆蓋率要求

| 層級 | 行覆蓋率目標 | 分支覆蓋率目標 |
|------|-------------|---------------|
| Service | ≥ 80% | ≥ 70% |
| Domain | ≥ 90% | ≥ 85% |
| Controller | ≥ 70% | ≥ 60% |
| Repository | ≥ 60% | - |
| **整體** | **≥ 80%** | **≥ 70%** |

### 6.2 排除覆蓋率計算的項目

```xml
<!-- JaCoCo 排除設定 -->
<configuration>
    <excludes>
        <exclude>**/WegoApplication.class</exclude>
        <exclude>**/config/**</exclude>
        <exclude>**/entity/**</exclude>
        <exclude>**/dto/**</exclude>
        <exclude>**/exception/**</exclude>
    </excludes>
</configuration>
```

### 6.3 執行測試與產生報告

```bash
# 執行單元測試
./mvnw test

# 執行整合測試
./mvnw verify

# 產生覆蓋率報告
./mvnw jacoco:report

# 報告位置
open target/site/jacoco/index.html
```

---

## 7. TDD 工作流程

### 7.1 開發一個新功能的步驟

以「新增景點」功能為例：

#### Step 1: 寫測試 (RED)

```java
@Test
@DisplayName("新增景點到行程")
void createActivity_shouldAddActivityToTrip() {
    // Given
    UUID tripId = UUID.randomUUID();
    CreateActivityRequest request = new CreateActivityRequest();
    request.setPlaceName("淺草寺");
    request.setDay(1);
    // ...

    // When
    ActivityResponse response = activityService.createActivity(tripId, request, user);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.getPlaceName()).isEqualTo("淺草寺");
    // ...
}
```

執行測試 → **失敗** (因為 `createActivity` 方法還不存在)

#### Step 2: 寫程式碼 (GREEN)

```java
public ActivityResponse createActivity(UUID tripId,
                                        CreateActivityRequest request,
                                        User user) {
    // 最小實作讓測試通過
    Activity activity = new Activity();
    activity.setId(UUID.randomUUID());
    // ...
    return mapper.toResponse(activityRepository.save(activity));
}
```

執行測試 → **通過**

#### Step 3: 重構 (REFACTOR)

```java
public ActivityResponse createActivity(UUID tripId,
                                        CreateActivityRequest request,
                                        User user) {
    // 1. 權限檢查
    if (!permissionChecker.canEdit(tripId, user.getId())) {
        throw new ForbiddenException("無權限新增景點");
    }

    // 2. 找或建立地點
    Place place = findOrCreatePlace(request);

    // 3. 建立活動
    Activity activity = Activity.builder()
        .tripId(tripId)
        .place(place)
        .day(request.getDay())
        .sortOrder(getNextSortOrder(tripId, request.getDay()))
        .build();

    // 4. 儲存並回傳
    return mapper.toResponse(activityRepository.save(activity));
}
```

執行測試 → **仍然通過**

### 7.2 TDD Checklist

開發新功能時，確認以下項目：

- [ ] 先寫測試，測試失敗
- [ ] 寫最小程式碼讓測試通過
- [ ] 重構程式碼
- [ ] 所有測試仍然通過
- [ ] 測試覆蓋正常情況與邊界情況
- [ ] 測試命名清楚描述預期行為

---

## 8. 持續整合 (CI) 設定

### GitHub Actions 工作流程

```yaml
# .github/workflows/test.yml
name: Test

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  test:
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_DB: testdb
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      - name: Run Unit Tests
        run: ./mvnw test

      - name: Run Integration Tests
        run: ./mvnw verify -DskipUnitTests
        env:
          DATABASE_URL: jdbc:postgresql://localhost:5432/testdb
          DATABASE_USERNAME: test
          DATABASE_PASSWORD: test

      - name: Generate Coverage Report
        run: ./mvnw jacoco:report

      - name: Upload Coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          files: target/site/jacoco/jacoco.xml

      - name: Check Coverage Threshold
        run: |
          COVERAGE=$(grep -oP 'Total.*?(\d+)%' target/site/jacoco/index.html | grep -oP '\d+' | head -1)
          if [ "$COVERAGE" -lt 80 ]; then
            echo "Coverage is below 80%: $COVERAGE%"
            exit 1
          fi
```

---

## 9. 常見問題與最佳實踐

### 9.1 測試應該獨立

```java
// ❌ Bad - 測試之間有依賴
@Test
@Order(1)
void createTrip() { ... }

@Test
@Order(2)
void updateCreatedTrip() { ... }  // 依賴第一個測試建立的資料

// ✅ Good - 每個測試獨立
@Test
void updateTrip_shouldUpdateSuccessfully() {
    // 在測試中建立所需資料
    Trip trip = tripRepository.save(TripFixture.createTrip(owner.getId()));

    // 執行測試
    tripService.updateTrip(trip.getId(), updateRequest, owner);
}
```

### 9.2 避免過度 Mock

```java
// ❌ Bad - Mock 了太多東西，測試失去意義
@Test
void calculateSettlement() {
    when(expenseRepository.findByTripId(any())).thenReturn(expenses);
    when(debtSimplifier.simplify(any(), any())).thenReturn(settlements);  // 連核心邏輯都 Mock
    // ...
}

// ✅ Good - 只 Mock 外部依賴，測試真實邏輯
@Test
void calculateSettlement() {
    when(expenseRepository.findByTripId(tripId)).thenReturn(expenses);

    // 使用真實的 DebtSimplifier
    List<Settlement> result = settlementService.calculateSettlement(tripId);

    assertThat(result).hasSize(2);
}
```

### 9.3 測試邊界條件

```java
@Nested
@DisplayName("邊界條件測試")
class EdgeCases {

    @Test
    @DisplayName("空列表")
    void emptyList() { ... }

    @Test
    @DisplayName("單一元素")
    void singleElement() { ... }

    @Test
    @DisplayName("最大值")
    void maxValue() { ... }

    @Test
    @DisplayName("null 輸入")
    void nullInput() { ... }
}
```

### 9.4 使用有意義的測試資料

```java
// ❌ Bad - 無意義的測試資料
@Test
void test1() {
    Expense expense = new Expense();
    expense.setAmount(new BigDecimal("123.45"));
    // ...
}

// ✅ Good - 有意義的測試資料
@Test
void calculateSettlement_withDinnerExpense_shouldSplitEvenly() {
    Expense dinnerExpense = ExpenseFixture.createExpense(tripId, payerId);
    dinnerExpense.setTitle("團體晚餐");
    dinnerExpense.setAmount(new BigDecimal("3000"));  // 3000 元晚餐
    dinnerExpense.setCategory("餐飲");
    // ...
}
```
