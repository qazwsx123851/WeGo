package com.wego.service;

import com.wego.dto.response.ActivityResponse;
import com.wego.dto.response.ExpenseResponse;
import com.wego.dto.response.ExpenseSplitResponse;
import com.wego.dto.response.PersonalExpenseItemResponse;
import com.wego.dto.response.PersonalExpenseSummaryResponse;
import com.wego.dto.response.PlaceResponse;
import com.wego.dto.response.SettlementResponse;
import com.wego.dto.response.TodoResponse;
import com.wego.dto.response.TripResponse;
import com.wego.entity.Role;
import com.wego.entity.SplitType;
import com.wego.entity.TodoStatus;
import com.wego.entity.TransportMode;
import com.wego.entity.TransportSource;
import com.wego.entity.TransportWarning;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Provides all hardcoded demo data for a Tokyo 4-day trip preview.
 * Used by the landing page / demo mode to show realistic trip data
 * without requiring a database or authenticated user.
 */
@Component
public class DemoDataProvider {

    // --- Fixed UUIDs ---
    private static final UUID TRIP_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID MEMBER_XIAOMING = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID MEMBER_MEIKA = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final UUID MEMBER_DAHUI = UUID.fromString("00000000-0000-0000-0000-000000000103");

    // --- Trip dates ---
    private static final LocalDate START_DATE = LocalDate.of(2026, 4, 10);
    private static final LocalDate END_DATE = LocalDate.of(2026, 4, 13);
    private static final LocalDate DAY1 = START_DATE;
    private static final LocalDate DAY2 = LocalDate.of(2026, 4, 11);
    private static final LocalDate DAY3 = LocalDate.of(2026, 4, 12);
    private static final LocalDate DAY4 = END_DATE;

    // --- Timestamps ---
    private static final Instant CREATED_AT = Instant.parse("2026-03-20T08:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-03-25T10:30:00Z");

    // --- Member names ---
    private static final String NAME_XIAOMING = "\u5c0f\u660e";
    private static final String NAME_MEIKA = "\u7f8e\u9999";
    private static final String NAME_DAHUI = "\u5927\u8f1d";

    // ========== Public API ==========

    public TripResponse getDemoTrip() {
        return TripResponse.builder()
                .id(TRIP_ID)
                .title("\u6771\u4eac\u56db\u5929\u4e09\u591c\u81ea\u7531\u884c")
                .description("\u63a2\u7d22\u6771\u4eac\u7d93\u5178\u666f\u9ede\uff0c\u54c1\u5690\u7f8e\u98df\uff0c\u9ad4\u9a57\u65e5\u672c\u6587\u5316")
                .startDate(START_DATE)
                .endDate(END_DATE)
                .durationDays(4)
                .baseCurrency("JPY")
                .coverImageUrl(null)
                .ownerId(MEMBER_XIAOMING)
                .memberCount(3)
                .currentUserRole(Role.OWNER)
                .members(buildMembers())
                .createdAt(CREATED_AT)
                .daysUntil(null)
                .build();
    }

    public List<LocalDate> getDemoDates() {
        return List.of(DAY1, DAY2, DAY3, DAY4);
    }

    public Map<LocalDate, List<ActivityResponse>> getDemoActivitiesByDate() {
        Map<LocalDate, List<ActivityResponse>> map = new LinkedHashMap<>();
        map.put(DAY1, buildDay1Activities());
        map.put(DAY2, buildDay2Activities());
        map.put(DAY3, buildDay3Activities());
        map.put(DAY4, buildDay4Activities());
        return map;
    }

    public List<ActivityResponse> getAllDemoActivities() {
        List<ActivityResponse> all = new ArrayList<>();
        all.addAll(buildDay1Activities());
        all.addAll(buildDay2Activities());
        all.addAll(buildDay3Activities());
        all.addAll(buildDay4Activities());
        return all;
    }

    public List<ExpenseResponse> getDemoExpenses() {
        return buildExpenses();
    }

    public Map<LocalDate, List<ExpenseResponse>> getDemoExpensesByDate() {
        List<ExpenseResponse> expenses = buildExpenses();
        Map<LocalDate, List<ExpenseResponse>> map = new LinkedHashMap<>();
        for (ExpenseResponse e : expenses) {
            map.computeIfAbsent(e.getExpenseDate(), k -> new ArrayList<>()).add(e);
        }
        return map;
    }

    public SettlementResponse getDemoSettlement() {
        return buildSettlement();
    }

    public List<PersonalExpenseItemResponse> getDemoPersonalExpenses() {
        return buildPersonalExpenses();
    }

    public PersonalExpenseSummaryResponse getDemoPersonalSummary() {
        return buildPersonalSummary();
    }

    public List<TodoResponse> getDemoTodos() {
        return buildTodos();
    }

    public String getDemoTripContextForChat() {
        return buildChatContext();
    }

    // ========== Members ==========

    private List<TripResponse.MemberSummary> buildMembers() {
        return List.of(
                TripResponse.MemberSummary.builder()
                        .userId(MEMBER_XIAOMING).nickname(NAME_XIAOMING)
                        .avatarUrl(null).role(Role.OWNER).isGhost(false).build(),
                TripResponse.MemberSummary.builder()
                        .userId(MEMBER_MEIKA).nickname(NAME_MEIKA)
                        .avatarUrl(null).role(Role.EDITOR).isGhost(false).build(),
                TripResponse.MemberSummary.builder()
                        .userId(MEMBER_DAHUI).nickname(NAME_DAHUI)
                        .avatarUrl(null).role(Role.EDITOR).isGhost(false).build()
        );
    }

    // ========== Activities ==========

    private List<ActivityResponse> buildDay1Activities() {
        int day = 1;
        return List.of(
                activity(day, 0, "\u6210\u7530\u6a5f\u5834\u62b5\u9054", "transit_station",
                        "\u5343\u8449\u7e23\u6210\u7530\u5e02", 35.7647, 140.3864,
                        LocalTime.of(9, 0), LocalTime.of(10, 30), 90,
                        TransportMode.TRANSIT, 60, 65000),
                activity(day, 1, "\u6dfa\u8349\u5bfa\u30fb\u96f7\u9580", null,
                        "\u6771\u4eac\u90fd\u53f0\u6771\u5340\u6dfa\u8349 2-3-1", 35.7148, 139.7967,
                        LocalTime.of(11, 30), LocalTime.of(13, 0), 90,
                        TransportMode.WALKING, 5, 400),
                activity(day, 2, "\u5927\u9ed1\u5bb6\u5929\u5a66\u7f85", "restaurant",
                        "\u6771\u4eac\u90fd\u53f0\u6771\u5340\u6dfa\u8349 1-38-10", 35.7128, 139.7946,
                        LocalTime.of(13, 0), LocalTime.of(14, 0), 60,
                        TransportMode.WALKING, 3, 200),
                activity(day, 3, "\u4e0a\u91ce\u516c\u5712", null,
                        "\u6771\u4eac\u90fd\u53f0\u6771\u5340\u4e0a\u91ce\u516c\u5712", 35.7146, 139.7732,
                        LocalTime.of(15, 0), LocalTime.of(17, 0), 120,
                        TransportMode.WALKING, 15, 1200),
                activity(day, 4, "\u963f\u7f8e\u6a6b\u4e01", null,
                        "\u6771\u4eac\u90fd\u53f0\u6771\u5340\u4e0a\u91ce 4\u4e01\u76ee", 35.7107, 139.7747,
                        LocalTime.of(18, 0), LocalTime.of(19, 0), 60,
                        TransportMode.WALKING, 5, 400),
                activity(day, 5, "\u4e00\u862d\u62c9\u9eb5\u4e0a\u91ce\u5e97", "restaurant",
                        "\u6771\u4eac\u90fd\u53f0\u6771\u5340\u4e0a\u91ce 7-1-1", 35.7133, 139.7751,
                        LocalTime.of(19, 30), LocalTime.of(20, 30), 60,
                        null, null, null)
        );
    }

    private List<ActivityResponse> buildDay2Activities() {
        int day = 2;
        return List.of(
                activity(day, 0, "\u660e\u6cbb\u795e\u5bae", null,
                        "\u6771\u4eac\u90fd\u6e0b\u8c37\u5340\u4ee3\u3005\u6728\u795e\u5712\u753a 1-1", 35.6764, 139.6993,
                        LocalTime.of(9, 30), LocalTime.of(11, 0), 90,
                        TransportMode.TRANSIT, 25, 12000),
                activity(day, 1, "\u7af9\u4e0b\u901a", null,
                        "\u6771\u4eac\u90fd\u6e0b\u8c37\u5340\u795e\u5bae\u524d 1\u4e01\u76ee", 35.6702, 139.7026,
                        LocalTime.of(11, 0), LocalTime.of(12, 30), 90,
                        TransportMode.WALKING, 8, 600),
                activity(day, 2, "Bills \u8868\u53c3\u9053", "restaurant",
                        "\u6771\u4eac\u90fd\u6e0b\u8c37\u5340\u795e\u5bae\u524d 4-30-3", 35.6654, 139.7084,
                        LocalTime.of(12, 30), LocalTime.of(13, 30), 60,
                        TransportMode.WALKING, 5, 400),
                activity(day, 3, "\u6e0b\u8c37 Sky \u5c55\u671b\u53f0", null,
                        "\u6771\u4eac\u90fd\u6e0b\u8c37\u5340\u6e0b\u8c37 2-24-12", 35.6580, 139.7016,
                        LocalTime.of(14, 0), LocalTime.of(15, 30), 90,
                        TransportMode.TRANSIT, 10, 2000),
                activity(day, 4, "\u6e0b\u8c37\u5341\u5b57\u8def\u53e3", null,
                        "\u6771\u4eac\u90fd\u6e0b\u8c37\u5340\u9053\u7384\u5742 2\u4e01\u76ee", 35.6595, 139.7004,
                        LocalTime.of(16, 0), LocalTime.of(16, 30), 30,
                        TransportMode.WALKING, 2, 150),
                activity(day, 5, "\u65b0\u5bbf\u6b4c\u821e\u4f0e\u753a", null,
                        "\u6771\u4eac\u90fd\u65b0\u5bbf\u5340\u6b4c\u821e\u4f0e\u753a 1\u4e01\u76ee", 35.6938, 139.7034,
                        LocalTime.of(18, 30), LocalTime.of(19, 30), 60,
                        TransportMode.TRANSIT, 15, 5000),
                activity(day, 6, "\u601d\u51fa\u6a6b\u4e01", "restaurant",
                        "\u6771\u4eac\u90fd\u65b0\u5bbf\u5340\u897f\u65b0\u5bbf 1\u4e01\u76ee", 35.6930, 139.6996,
                        LocalTime.of(20, 0), LocalTime.of(21, 0), 60,
                        null, null, null)
        );
    }

    private List<ActivityResponse> buildDay3Activities() {
        int day = 3;
        return List.of(
                activity(day, 0, "\u7bc9\u5730\u5834\u5916\u5e02\u5834", null,
                        "\u6771\u4eac\u90fd\u4e2d\u592e\u5340\u7bc9\u5730 4\u4e01\u76ee", 35.6654, 139.7707,
                        LocalTime.of(8, 0), LocalTime.of(10, 0), 120,
                        TransportMode.TRANSIT, 30, 10000),
                activity(day, 1, "teamLab Borderless", null,
                        "\u6771\u4eac\u90fd\u6c5f\u6771\u5340\u9752\u6d77 1-3-8", 35.6262, 139.7839,
                        LocalTime.of(10, 30), LocalTime.of(13, 0), 150,
                        TransportMode.TRANSIT, 20, 6000),
                activity(day, 2, "\u53f0\u5834 Diver City", "restaurant",
                        "\u6771\u4eac\u90fd\u6c5f\u6771\u5340\u9752\u6d77 1-1-10", 35.6252, 139.7754,
                        LocalTime.of(13, 0), LocalTime.of(14, 0), 60,
                        TransportMode.WALKING, 8, 600),
                activity(day, 3, "\u7368\u89d2\u7378\u92fc\u5f48", null,
                        "\u6771\u4eac\u90fd\u6c5f\u6771\u5340\u9752\u6d77 1-1-10", 35.6249, 139.7756,
                        LocalTime.of(15, 0), LocalTime.of(15, 30), 30,
                        TransportMode.WALKING, 1, 50),
                activity(day, 4, "\u6771\u4eac\u9435\u5854", null,
                        "\u6771\u4eac\u90fd\u6e2f\u5340\u829d\u516c\u5712 4-2-8", 35.6586, 139.7454,
                        LocalTime.of(17, 0), LocalTime.of(18, 30), 90,
                        TransportMode.TRANSIT, 25, 8000),
                activity(day, 5, "\u516d\u672c\u6728\u665a\u9910", "restaurant",
                        "\u6771\u4eac\u90fd\u6e2f\u5340\u516d\u672c\u6728 6\u4e01\u76ee", 35.6603, 139.7292,
                        LocalTime.of(19, 0), LocalTime.of(20, 30), 90,
                        null, null, null)
        );
    }

    private List<ActivityResponse> buildDay4Activities() {
        int day = 4;
        return List.of(
                activity(day, 0, "\u79cb\u8449\u539f\u96fb\u5668\u8857", null,
                        "\u6771\u4eac\u90fd\u5343\u4ee3\u7530\u5340\u5916\u795e\u7530 1\u4e01\u76ee", 35.6984, 139.7710,
                        LocalTime.of(9, 0), LocalTime.of(11, 0), 120,
                        TransportMode.TRANSIT, 20, 8000),
                activity(day, 1, "\u795e\u7530\u795e\u793e", null,
                        "\u6771\u4eac\u90fd\u5343\u4ee3\u7530\u5340\u5916\u795e\u7530 2-16-2", 35.7019, 139.7681,
                        LocalTime.of(11, 30), LocalTime.of(12, 30), 60,
                        TransportMode.WALKING, 8, 600),
                activity(day, 2, "\u79cb\u8449\u539f\u5348\u9910", "restaurant",
                        "\u6771\u4eac\u90fd\u5343\u4ee3\u7530\u5340\u795e\u7530\u4f50\u4e45\u9593\u753a", 35.6989, 139.7710,
                        LocalTime.of(12, 30), LocalTime.of(13, 30), 60,
                        TransportMode.WALKING, 5, 350),
                activity(day, 3, "\u6771\u4eac\u8eca\u7ad9\u4e00\u756a\u8857", null,
                        "\u6771\u4eac\u90fd\u5343\u4ee3\u7530\u5340\u4e38\u306e\u5185 1-9-1", 35.6812, 139.7671,
                        LocalTime.of(14, 0), LocalTime.of(15, 30), 90,
                        TransportMode.TRANSIT, 10, 3000),
                activity(day, 4, "\u4e38\u4e4b\u5167\u901b\u8857", null,
                        "\u6771\u4eac\u90fd\u5343\u4ee3\u7530\u5340\u4e38\u306e\u5185 2\u4e01\u76ee", 35.6819, 139.7643,
                        LocalTime.of(16, 0), LocalTime.of(18, 0), 120,
                        TransportMode.WALKING, 3, 200),
                activity(day, 5, "\u8fd4\u56de\u6a5f\u5834", "transit_station",
                        "\u5343\u8449\u7e23\u6210\u7530\u5e02", 35.7647, 140.3864,
                        LocalTime.of(18, 0), null, null,
                        TransportMode.TRANSIT, 75, 65000)
        );
    }

    private ActivityResponse activity(int day, int sortOrder, String name, String category,
                                      String address, double lat, double lng,
                                      LocalTime startTime, LocalTime endTime, Integer durationMinutes,
                                      TransportMode transportMode, Integer transportDuration, Integer transportDistance) {
        UUID activityId = UUID.fromString(String.format("00000000-0000-0000-0000-0000%02d%02d0000", day, sortOrder));
        UUID placeId = UUID.fromString(String.format("00000000-0000-0000-0000-0000%02d%02d0001", day, sortOrder));

        return ActivityResponse.builder()
                .id(activityId)
                .tripId(TRIP_ID)
                .place(PlaceResponse.builder()
                        .id(placeId)
                        .name(name)
                        .address(address)
                        .latitude(lat)
                        .longitude(lng)
                        .category(category)
                        .rating(category != null && category.equals("restaurant") ? 4.2 : null)
                        .priceLevel(category != null && category.equals("restaurant") ? 2 : null)
                        .googlePlaceId(null)
                        .photoReference(null)
                        .build())
                .day(day)
                .sortOrder(sortOrder)
                .startTime(startTime)
                .endTime(endTime)
                .durationMinutes(durationMinutes)
                .note(null)
                .transportMode(transportMode)
                .transportDurationMinutes(transportDuration)
                .transportDistanceMeters(transportDistance)
                .transportSource(transportMode != null ? TransportSource.HAVERSINE : null)
                .transportWarning(transportMode != null ? TransportWarning.NONE : null)
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .build();
    }

    // ========== Expenses ==========

    private List<ExpenseResponse> buildExpenses() {
        return List.of(
                expense(1, "\u6210\u7530Express\u8eca\u7968", bd("9600"), MEMBER_XIAOMING, NAME_XIAOMING,
                        SplitType.EQUAL, "TRANSPORT", DAY1, equalSplits(bd("3200"))),
                expense(2, "\u6dfa\u8349\u5bfa\u5468\u908a\u5348\u9910", bd("4500"), MEMBER_MEIKA, NAME_MEIKA,
                        SplitType.EQUAL, "FOOD", DAY1, equalSplits(bd("1500"))),
                expense(3, "\u4e0a\u91ce\u96f6\u98df", bd("2800"), MEMBER_DAHUI, NAME_DAHUI,
                        SplitType.EQUAL, "FOOD", DAY1, equalSplits(bd("933"))),
                expense(4, "\u6e0b\u8c37Sky\u9580\u7968", bd("6000"), MEMBER_XIAOMING, NAME_XIAOMING,
                        SplitType.EQUAL, "ENTERTAINMENT", DAY2, equalSplits(bd("2000"))),
                expense(5, "Bills\u9b06\u9905\u65e9\u5348\u9910", bd("7200"), MEMBER_MEIKA, NAME_MEIKA,
                        SplitType.EQUAL, "FOOD", DAY2, equalSplits(bd("2400"))),
                expense(6, "\u65b0\u5bbf\u5c45\u9152\u5c4b", bd("12000"), MEMBER_DAHUI, NAME_DAHUI,
                        SplitType.CUSTOM, "FOOD", DAY2, customSplits(bd("5000"), bd("4000"), bd("3000"))),
                expense(7, "teamLab\u9580\u7968", bd("9900"), MEMBER_XIAOMING, NAME_XIAOMING,
                        SplitType.EQUAL, "ENTERTAINMENT", DAY3, equalSplits(bd("3300"))),
                expense(8, "\u53f0\u5834\u5348\u9910", bd("4200"), MEMBER_MEIKA, NAME_MEIKA,
                        SplitType.EQUAL, "FOOD", DAY3, equalSplits(bd("1400"))),
                expense(9, "\u6771\u4eac\u9435\u5854\u9580\u7968", bd("3600"), MEMBER_DAHUI, NAME_DAHUI,
                        SplitType.EQUAL, "ENTERTAINMENT", DAY3, equalSplits(bd("1200"))),
                expense(10, "\u98ef\u5e97\u4f4f\u5bbf3\u665a", bd("90000"), MEMBER_XIAOMING, NAME_XIAOMING,
                        SplitType.EQUAL, "ACCOMMODATION", DAY1, equalSplits(bd("30000"))),
                expense(11, "\u79cb\u8449\u539f\u8cfc\u7269", bd("15000"), MEMBER_MEIKA, NAME_MEIKA,
                        SplitType.CUSTOM, "SHOPPING", DAY4, customSplits(bd("8000"), bd("5000"), bd("2000"))),
                expense(12, "\u6771\u4eac\u8eca\u7ad9\u4f34\u624b\u79ae", bd("8400"), MEMBER_DAHUI, NAME_DAHUI,
                        SplitType.EQUAL, "FOOD", DAY4, equalSplits(bd("2800")))
        );
    }

    private ExpenseResponse expense(int seq, String description, BigDecimal amount,
                                    UUID paidBy, String paidByName,
                                    SplitType splitType, String category,
                                    LocalDate date, List<ExpenseSplitResponse> splits) {
        UUID expenseId = UUID.fromString(String.format("00000000-0000-0000-0000-000000e%05d", seq));
        return ExpenseResponse.builder()
                .id(expenseId)
                .tripId(TRIP_ID)
                .description(description)
                .amount(amount)
                .currency("JPY")
                .exchangeRate(BigDecimal.ONE)
                .paidBy(paidBy)
                .paidByName(paidByName)
                .paidByAvatarUrl(null)
                .paidByIsGhost(false)
                .splitType(splitType)
                .splits(splits)
                .category(category)
                .expenseDate(date)
                .activityId(null)
                .receiptUrl(null)
                .note(null)
                .createdBy(paidBy)
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .build();
    }

    private List<ExpenseSplitResponse> equalSplits(BigDecimal perPerson) {
        return List.of(
                split(1, MEMBER_XIAOMING, NAME_XIAOMING, perPerson),
                split(2, MEMBER_MEIKA, NAME_MEIKA, perPerson),
                split(3, MEMBER_DAHUI, NAME_DAHUI, perPerson)
        );
    }

    private List<ExpenseSplitResponse> customSplits(BigDecimal xiaoming, BigDecimal meika, BigDecimal dahui) {
        return List.of(
                split(1, MEMBER_XIAOMING, NAME_XIAOMING, xiaoming),
                split(2, MEMBER_MEIKA, NAME_MEIKA, meika),
                split(3, MEMBER_DAHUI, NAME_DAHUI, dahui)
        );
    }

    private ExpenseSplitResponse split(int seq, UUID userId, String nickname, BigDecimal amount) {
        return ExpenseSplitResponse.builder()
                .id(UUID.fromString(String.format("00000000-0000-0000-0000-0000000%05d", seq)))
                .userId(userId)
                .userNickname(nickname)
                .userAvatarUrl(null)
                .isGhost(false)
                .amount(amount)
                .isSettled(false)
                .settledAt(null)
                .build();
    }

    // ========== Settlement ==========

    private SettlementResponse buildSettlement() {
        // Total: 9600+4500+2800+6000+7200+12000+9900+4200+3600+90000+15000+8400 = 173200
        BigDecimal totalExpenses = bd("173200");

        // Per-person shares:
        // Xiaoming: 3200+1500+933+2000+2400+5000+3300+1400+1200+30000+8000+2800 = 61733
        // Meika:    3200+1500+933+2000+2400+4000+3300+1400+1200+30000+5000+2800 = 57733
        // Dahui:    3200+1500+934+2000+2400+3000+3300+1400+1200+30000+2000+2800 = 53734
        //
        // Paid:
        // Xiaoming paid: 9600+6000+9900+90000 = 115500
        // Meika paid:    4500+7200+4200+15000 = 30900
        // Dahui paid:    2800+12000+3600+8400 = 26800
        //
        // Balance (paid - share):
        // Xiaoming: 115500 - 61733 = +53767
        // Meika:    30900 - 57733 = -26833
        // Dahui:    26800 - 53734 = -26934

        Map<UUID, BigDecimal> balances = new LinkedHashMap<>();
        balances.put(MEMBER_XIAOMING, bd("53767"));
        balances.put(MEMBER_MEIKA, bd("-26833"));
        balances.put(MEMBER_DAHUI, bd("-26934"));

        List<SettlementResponse.SettlementItemResponse> settlements = List.of(
                SettlementResponse.SettlementItemResponse.builder()
                        .fromUserId(MEMBER_MEIKA).fromUserName(NAME_MEIKA)
                        .fromUserAvatarUrl(null).fromIsGhost(false)
                        .toUserId(MEMBER_XIAOMING).toUserName(NAME_XIAOMING)
                        .toUserAvatarUrl(null).toIsGhost(false)
                        .amount(bd("26833"))
                        .build(),
                SettlementResponse.SettlementItemResponse.builder()
                        .fromUserId(MEMBER_DAHUI).fromUserName(NAME_DAHUI)
                        .fromUserAvatarUrl(null).fromIsGhost(false)
                        .toUserId(MEMBER_XIAOMING).toUserName(NAME_XIAOMING)
                        .toUserAvatarUrl(null).toIsGhost(false)
                        .amount(bd("26934"))
                        .build()
        );

        Map<String, BigDecimal> currencyBreakdown = new LinkedHashMap<>();
        currencyBreakdown.put("JPY", totalExpenses);

        return SettlementResponse.builder()
                .settlements(settlements)
                .totalExpenses(totalExpenses)
                .baseCurrency("JPY")
                .expenseCount(12)
                .currencyBreakdown(currencyBreakdown)
                .conversionWarnings(List.of())
                .userBalances(balances)
                .build();
    }

    // ========== Personal Expenses ==========

    private List<PersonalExpenseItemResponse> buildPersonalExpenses() {
        // AUTO entries: user's share from team expenses (xiaoming's portion)
        List<PersonalExpenseItemResponse> items = new ArrayList<>();

        items.add(autoExpense("\u6210\u7530Express\u8eca\u7968", bd("3200"), "TRANSPORT", DAY1, NAME_XIAOMING, 1));
        items.add(autoExpense("\u6dfa\u8349\u5bfa\u5468\u908a\u5348\u9910", bd("1500"), "FOOD", DAY1, NAME_MEIKA, 2));
        items.add(autoExpense("\u98ef\u5e97\u4f4f\u5bbf3\u665a", bd("30000"), "ACCOMMODATION", DAY1, NAME_XIAOMING, 10));
        items.add(autoExpense("\u6e0b\u8c37Sky\u9580\u7968", bd("2000"), "ENTERTAINMENT", DAY2, NAME_XIAOMING, 4));
        items.add(autoExpense("Bills\u9b06\u9905\u65e9\u5348\u9910", bd("2400"), "FOOD", DAY2, NAME_MEIKA, 5));
        items.add(autoExpense("\u65b0\u5bbf\u5c45\u9152\u5c4b", bd("5000"), "FOOD", DAY2, NAME_DAHUI, 6));
        items.add(autoExpense("teamLab\u9580\u7968", bd("3300"), "ENTERTAINMENT", DAY3, NAME_XIAOMING, 7));

        // MANUAL entries: personal purchases
        items.add(manualExpense("\u661f\u5df4\u514b\u62ff\u9435", bd("500"), "FOOD", DAY1, 1));
        items.add(manualExpense("\u85e5\u5986\u5e97\u8cfc\u7269", bd("6800"), "SHOPPING", DAY2, 2));
        items.add(manualExpense("\u4fbf\u5229\u5546\u5e97\u98f2\u6599", bd("350"), "FOOD", DAY3, 3));

        return items;
    }

    private PersonalExpenseItemResponse autoExpense(String description, BigDecimal amount,
                                                    String category, LocalDate date,
                                                    String paidByName, int expenseSeq) {
        return PersonalExpenseItemResponse.builder()
                .source(PersonalExpenseItemResponse.Source.AUTO)
                .id(null)
                .description(description)
                .amount(amount)
                .originalAmount(amount)
                .originalCurrency("JPY")
                .exchangeRate(null)
                .category(category)
                .expenseDate(date)
                .paidByName(paidByName)
                .tripExpenseId(UUID.fromString(String.format("00000000-0000-0000-0000-000000e%05d", expenseSeq)))
                .build();
    }

    private PersonalExpenseItemResponse manualExpense(String description, BigDecimal amount,
                                                      String category, LocalDate date, int seq) {
        return PersonalExpenseItemResponse.builder()
                .source(PersonalExpenseItemResponse.Source.MANUAL)
                .id(UUID.fromString(String.format("00000000-0000-0000-0000-000000d%05d", seq)))
                .description(description)
                .amount(amount)
                .originalAmount(amount)
                .originalCurrency("JPY")
                .exchangeRate(null)
                .category(category)
                .expenseDate(date)
                .paidByName(null)
                .tripExpenseId(null)
                .build();
    }

    private PersonalExpenseSummaryResponse buildPersonalSummary() {
        // Total: 3200+1500+30000+2000+2400+5000+3300+500+6800+350 = 55050
        BigDecimal total = bd("55050");
        BigDecimal budget = bd("80000");
        BigDecimal dailyAvg = total.divide(bd("4"), 0, RoundingMode.HALF_UP);

        Map<String, BigDecimal> categoryBreakdown = new LinkedHashMap<>();
        categoryBreakdown.put("FOOD", bd("10050"));       // 1500+2400+5000+500+350
        categoryBreakdown.put("TRANSPORT", bd("3200"));
        categoryBreakdown.put("ACCOMMODATION", bd("30000"));
        categoryBreakdown.put("ENTERTAINMENT", bd("5300")); // 2000+3300
        categoryBreakdown.put("SHOPPING", bd("6800"));

        Map<LocalDate, BigDecimal> dailyAmounts = new LinkedHashMap<>();
        dailyAmounts.put(DAY1, bd("35200"));  // 3200+1500+30000+500
        dailyAmounts.put(DAY2, bd("16200"));  // 2000+2400+5000+6800
        dailyAmounts.put(DAY3, bd("3650"));   // 3300+350
        dailyAmounts.put(DAY4, BigDecimal.ZERO);

        return PersonalExpenseSummaryResponse.builder()
                .totalAmount(total)
                .dailyAverage(dailyAvg)
                .categoryBreakdown(categoryBreakdown)
                .dailyAmounts(dailyAmounts)
                .budget(budget)
                .budgetStatus(PersonalExpenseSummaryResponse.BudgetStatus.YELLOW)
                .budgetOverage(null)
                .build();
    }

    // ========== Todos ==========

    private List<TodoResponse> buildTodos() {
        Instant completedAt = Instant.parse("2026-03-28T12:00:00Z");
        return List.of(
                TodoResponse.builder()
                        .id(UUID.fromString("00000000-0000-0000-0000-000000b00001"))
                        .tripId(TRIP_ID)
                        .title("\u8b77\u7167\u6709\u6548\u671f\u78ba\u8a8d")
                        .description("\u78ba\u8a8d\u8b77\u7167\u6709\u6548\u671f\u81f3\u5c11\u5230 2026 \u5e74 10 \u6708")
                        .assigneeId(MEMBER_XIAOMING).assigneeName(NAME_XIAOMING).assigneeAvatarUrl(null)
                        .dueDate(null)
                        .status(TodoStatus.COMPLETED)
                        .createdBy(MEMBER_XIAOMING).createdByName(NAME_XIAOMING)
                        .createdAt(CREATED_AT).updatedAt(completedAt).completedAt(completedAt)
                        .isOverdue(false)
                        .build(),
                TodoResponse.builder()
                        .id(UUID.fromString("00000000-0000-0000-0000-000000b00002"))
                        .tripId(TRIP_ID)
                        .title("\u9810\u8a02\u9910\u5ef3")
                        .description("\u9810\u8a02 Bills \u8868\u53c3\u9053\u548c\u516d\u672c\u6728\u665a\u9910\u9910\u5ef3")
                        .assigneeId(MEMBER_MEIKA).assigneeName(NAME_MEIKA).assigneeAvatarUrl(null)
                        .dueDate(null)
                        .status(TodoStatus.COMPLETED)
                        .createdBy(MEMBER_XIAOMING).createdByName(NAME_XIAOMING)
                        .createdAt(CREATED_AT).updatedAt(completedAt).completedAt(completedAt)
                        .isOverdue(false)
                        .build(),
                TodoResponse.builder()
                        .id(UUID.fromString("00000000-0000-0000-0000-000000b00003"))
                        .tripId(TRIP_ID)
                        .title("\u5141\u63db\u65e5\u5e63")
                        .description("\u81f3\u5c11\u6e96\u5099 5 \u842c\u65e5\u5e63\u73fe\u91d1")
                        .assigneeId(MEMBER_DAHUI).assigneeName(NAME_DAHUI).assigneeAvatarUrl(null)
                        .dueDate(null)
                        .status(TodoStatus.IN_PROGRESS)
                        .createdBy(MEMBER_XIAOMING).createdByName(NAME_XIAOMING)
                        .createdAt(CREATED_AT).updatedAt(UPDATED_AT).completedAt(null)
                        .isOverdue(false)
                        .build(),
                TodoResponse.builder()
                        .id(UUID.fromString("00000000-0000-0000-0000-000000b00004"))
                        .tripId(TRIP_ID)
                        .title("\u8cfc\u8cb7\u65c5\u904a\u4fdd\u96aa")
                        .description("\u6bd4\u8f03\u5404\u5bb6\u65c5\u904a\u5e73\u5b89\u4fdd\u96aa\u65b9\u6848")
                        .assigneeId(MEMBER_XIAOMING).assigneeName(NAME_XIAOMING).assigneeAvatarUrl(null)
                        .dueDate(LocalDate.of(2026, 4, 8))
                        .status(TodoStatus.PENDING)
                        .createdBy(MEMBER_XIAOMING).createdByName(NAME_XIAOMING)
                        .createdAt(CREATED_AT).updatedAt(UPDATED_AT).completedAt(null)
                        .isOverdue(false)
                        .build(),
                TodoResponse.builder()
                        .id(UUID.fromString("00000000-0000-0000-0000-000000b00005"))
                        .tripId(TRIP_ID)
                        .title("\u6574\u7406\u884c\u674e\u6e05\u55ae")
                        .description("\u5217\u51fa\u5fc5\u5e36\u7269\u54c1\u6e05\u55ae\u4e26\u5206\u4eab\u7d66\u5927\u5bb6")
                        .assigneeId(MEMBER_MEIKA).assigneeName(NAME_MEIKA).assigneeAvatarUrl(null)
                        .dueDate(LocalDate.of(2026, 4, 9))
                        .status(TodoStatus.PENDING)
                        .createdBy(MEMBER_MEIKA).createdByName(NAME_MEIKA)
                        .createdAt(CREATED_AT).updatedAt(UPDATED_AT).completedAt(null)
                        .isOverdue(false)
                        .build(),
                TodoResponse.builder()
                        .id(UUID.fromString("00000000-0000-0000-0000-000000b00006"))
                        .tripId(TRIP_ID)
                        .title("\u4e0b\u8f09\u96e2\u7dda\u5730\u5716")
                        .description("\u4e0b\u8f09 Google Maps \u6771\u4eac\u5340\u57df\u96e2\u7dda\u5730\u5716")
                        .assigneeId(null).assigneeName(null).assigneeAvatarUrl(null)
                        .dueDate(LocalDate.of(2026, 4, 9))
                        .status(TodoStatus.PENDING)
                        .createdBy(MEMBER_DAHUI).createdByName(NAME_DAHUI)
                        .createdAt(CREATED_AT).updatedAt(UPDATED_AT).completedAt(null)
                        .isOverdue(false)
                        .build()
        );
    }

    // ========== Chat Context ==========

    private String buildChatContext() {
        return """
                行程名稱: 東京四天三夜自由行
                日期: 2026-04-10 至 2026-04-13 (4天)
                成員: 小明(Owner), 美香(Editor), 大輝(Editor)
                幣別: JPY

                === Day 1 (4/10) - 淺草・上野 ===
                09:00 成田機場抵達 (90分鐘)
                11:30 淺草寺・雷門 (90分鐘)
                13:00 大黒家天婦羅 (60分鐘)
                15:00 上野公園 (120分鐘)
                18:00 阿美橫丁 (60分鐘)
                19:30 一蘭拉麵上野店 (60分鐘)

                === Day 2 (4/11) - 澀谷・原宿・新宿 ===
                09:30 明治神宮 (90分鐘)
                11:00 竹下通 (90分鐘)
                12:30 Bills 表參道 (60分鐘)
                14:00 澀谷 Sky 展望台 (90分鐘)
                16:00 澀谷十字路口 (30分鐘)
                18:30 新宿歌舞伎町 (60分鐘)
                20:00 思出橫丁 (60分鐘)

                === Day 3 (4/12) - 築地・台場・東京鐵塔 ===
                08:00 築地場外市場 (120分鐘)
                10:30 teamLab Borderless (150分鐘)
                13:00 台場 Diver City (60分鐘)
                15:00 獨角獸鋼彈 (30分鐘)
                17:00 東京鐵塔 (90分鐘)
                19:00 六本木晚餐 (90分鐘)

                === Day 4 (4/13) - 秋葉原・東京車站 ===
                09:00 秋葉原電器街 (120分鐘)
                11:30 神田神社 (60分鐘)
                12:30 秋葉原午餐 (60分鐘)
                14:00 東京車站一番街 (90分鐘)
                16:00 丸之內逛街 (120分鐘)
                18:00 返回機場

                === 費用摘要 ===
                總支出: ¥173,200 (12筆)
                住宿: ¥90,000 | 餐飲: ¥39,100 | 交通: ¥9,600 | 娛樂: ¥19,500 | 購物: ¥15,000

                === 待辦事項 ===
                [已完成] 護照有效期確認 (小明)
                [已完成] 預訂餐廳 (美香)
                [進行中] 兌換日幣 (大輝)
                [待辦] 購買旅遊保險 (小明, 截止 4/8)
                [待辦] 整理行李清單 (美香, 截止 4/9)
                [待辦] 下載離線地圖 (未指派, 截止 4/9)
                """;
    }

    // ========== Utility ==========

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
