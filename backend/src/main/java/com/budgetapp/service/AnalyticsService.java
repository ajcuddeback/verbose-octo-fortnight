package com.budgetapp.service;

import com.budgetapp.dto.response.CategoryBreakdownResponse;
import com.budgetapp.dto.response.PredictionResponse;
import com.budgetapp.dto.response.SpendingDnaResponse;
import com.budgetapp.dto.response.SpendingTrendResponse;
import com.budgetapp.exception.ResourceNotFoundException;
import com.budgetapp.model.Category;
import com.budgetapp.model.Transaction;
import com.budgetapp.model.TransactionType;
import com.budgetapp.model.User;
import com.budgetapp.repository.TransactionRepository;
import com.budgetapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public SpendingTrendResponse getSpendingTrends(Long userId, int months) {
        User user = getUserById(userId);
        List<SpendingTrendResponse.MonthlyDataPoint> dataPoints = new ArrayList<>();

        YearMonth currentMonth = YearMonth.now();

        for (int i = months - 1; i >= 0; i--) {
            YearMonth targetMonth = currentMonth.minusMonths(i);
            LocalDate startDate = targetMonth.atDay(1);
            LocalDate endDate = targetMonth.atEndOfMonth();

            List<Transaction> monthTransactions = transactionRepository
                    .findByUserAndTransactionDateBetween(user, startDate, endDate);

            BigDecimal totalIncome = monthTransactions.stream()
                    .filter(t -> t.getType() == TransactionType.INCOME)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalExpenses = monthTransactions.stream()
                    .filter(t -> t.getType() == TransactionType.EXPENSE)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal netSavings = totalIncome.subtract(totalExpenses);

            dataPoints.add(SpendingTrendResponse.MonthlyDataPoint.builder()
                    .month(targetMonth.getMonthValue())
                    .year(targetMonth.getYear())
                    .totalIncome(totalIncome)
                    .totalExpenses(totalExpenses)
                    .netSavings(netSavings)
                    .build());
        }

        return SpendingTrendResponse.builder()
                .dataPoints(dataPoints)
                .build();
    }

    public CategoryBreakdownResponse getCategoryBreakdown(Long userId, int month, int year) {
        User user = getUserById(userId);
        YearMonth targetMonth = YearMonth.of(year, month);
        LocalDate startDate = targetMonth.atDay(1);
        LocalDate endDate = targetMonth.atEndOfMonth();

        List<Transaction> transactions = transactionRepository
                .findByUserAndTransactionDateBetween(user, startDate, endDate);

        // Filter expenses only and group by category
        Map<Category, BigDecimal> categoryTotals = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(
                        t -> t.getCategory() != null ? t.getCategory() : createUncategorized(),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        BigDecimal totalAmount = categoryTotals.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CategoryBreakdownResponse.CategoryData> categories = categoryTotals.entrySet().stream()
                .map(entry -> {
                    double percentage = totalAmount.compareTo(BigDecimal.ZERO) > 0
                            ? entry.getValue()
                                    .divide(totalAmount, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100))
                                    .doubleValue()
                            : 0.0;
                    return CategoryBreakdownResponse.CategoryData.builder()
                            .categoryId(entry.getKey().getId())
                            .categoryName(entry.getKey().getName())
                            .colorHex(entry.getKey().getColorHex())
                            .totalAmount(entry.getValue())
                            .percentage(percentage)
                            .build();
                })
                .sorted(Comparator.comparing(CategoryBreakdownResponse.CategoryData::getTotalAmount).reversed())
                .collect(Collectors.toList());

        return CategoryBreakdownResponse.builder()
                .categories(categories)
                .totalAmount(totalAmount)
                .build();
    }

    public PredictionResponse getPredictions(Long userId) {
        User user = getUserById(userId);
        int basedOnMonths = 6;
        YearMonth currentMonth = YearMonth.now();

        List<BigDecimal> expenseData = new ArrayList<>();
        List<BigDecimal> incomeData = new ArrayList<>();

        for (int i = basedOnMonths; i >= 1; i--) {
            YearMonth targetMonth = currentMonth.minusMonths(i);
            LocalDate startDate = targetMonth.atDay(1);
            LocalDate endDate = targetMonth.atEndOfMonth();

            List<Transaction> monthTransactions = transactionRepository
                    .findByUserAndTransactionDateBetween(user, startDate, endDate);

            BigDecimal monthExpenses = monthTransactions.stream()
                    .filter(t -> t.getType() == TransactionType.EXPENSE)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal monthIncome = monthTransactions.stream()
                    .filter(t -> t.getType() == TransactionType.INCOME)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            expenseData.add(monthExpenses);
            incomeData.add(monthIncome);
        }

        // Linear regression prediction
        BigDecimal predictedExpenses = linearRegression(expenseData);
        BigDecimal predictedIncome = linearRegression(incomeData);

        // Calculate confidence based on data variance
        double confidence = calculateConfidence(expenseData);

        // Build monthly breakdown for next 3 months
        List<PredictionResponse.MonthlyPrediction> monthlyBreakdown = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            YearMonth futureMonth = currentMonth.plusMonths(i);
            monthlyBreakdown.add(PredictionResponse.MonthlyPrediction.builder()
                    .month(futureMonth.getMonthValue())
                    .year(futureMonth.getYear())
                    .predictedExpenses(predictedExpenses)
                    .predictedIncome(predictedIncome)
                    .build());
        }

        return PredictionResponse.builder()
                .predictedExpenses(predictedExpenses)
                .predictedIncome(predictedIncome)
                .confidence(confidence)
                .basedOnMonths(basedOnMonths)
                .monthlyBreakdown(monthlyBreakdown)
                .build();
    }

    /**
     * Linear regression: slope = (sum(x*y) - n*mean_x*mean_y) / (sum(x^2) - n*mean_x^2)
     * Predicts next value (month n+1)
     */
    private BigDecimal linearRegression(List<BigDecimal> data) {
        int n = data.size();
        if (n == 0) {
            return BigDecimal.ZERO;
        }
        if (n == 1) {
            return data.get(0);
        }

        // x values: 1, 2, 3, ... n
        double meanX = (n + 1.0) / 2.0;
        double meanY = data.stream()
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0.0);

        double sumXY = 0.0;
        double sumX2 = 0.0;

        for (int i = 0; i < n; i++) {
            double x = i + 1;
            double y = data.get(i).doubleValue();
            sumXY += x * y;
            sumX2 += x * x;
        }

        double denominator = sumX2 - n * meanX * meanX;
        if (Math.abs(denominator) < 1e-10) {
            return data.get(n - 1); // Return last value if no trend
        }

        double slope = (sumXY - n * meanX * meanY) / denominator;
        double intercept = meanY - slope * meanX;

        // Predict for month n+1
        double prediction = slope * (n + 1) + intercept;
        return BigDecimal.valueOf(Math.max(0, prediction)).setScale(2, RoundingMode.HALF_UP);
    }

    private double calculateConfidence(List<BigDecimal> data) {
        if (data.isEmpty()) return 0.0;

        double mean = data.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0.0);
        if (mean == 0.0) return 0.5;

        double variance = data.stream()
                .mapToDouble(d -> Math.pow(d.doubleValue() - mean, 2))
                .average()
                .orElse(0.0);

        double coefficientOfVariation = Math.sqrt(variance) / mean;
        // Lower variation = higher confidence
        double confidence = Math.max(0.1, Math.min(0.95, 1.0 - coefficientOfVariation));
        return Math.round(confidence * 100.0) / 100.0;
    }

    public SpendingDnaResponse getSpendingDna(Long userId) {
        User user = getUserById(userId);
        YearMonth currentMonth = YearMonth.now();

        // Look at last 3 months of spending
        LocalDate startDate = currentMonth.minusMonths(3).atDay(1);
        LocalDate endDate = LocalDate.now();

        List<Transaction> transactions = transactionRepository
                .findByUserAndTransactionDateBetween(user, startDate, endDate);

        // Group expenses by category
        Map<String, BigDecimal> categoryTotals = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .filter(t -> t.getCategory() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getCategory().getName(),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        BigDecimal totalExpenses = categoryTotals.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Build top categories list
        List<SpendingDnaResponse.TopCategory> topCategories = categoryTotals.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    double percentage = totalExpenses.compareTo(BigDecimal.ZERO) > 0
                            ? entry.getValue()
                                    .divide(totalExpenses, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100))
                                    .doubleValue()
                            : 0.0;
                    return SpendingDnaResponse.TopCategory.builder()
                            .categoryName(entry.getKey())
                            .totalAmount(entry.getValue())
                            .percentage(percentage)
                            .build();
                })
                .collect(Collectors.toList());

        // Determine personality type based on top category
        String topCategoryName = topCategories.isEmpty() ? "" :
                topCategories.get(0).getCategoryName().toLowerCase();

        String personalityType;
        String personalityDescription;
        List<String> insights = new ArrayList<>();

        if (topCategoryName.contains("food") || topCategoryName.contains("dining")
                || topCategoryName.contains("restaurant")) {
            personalityType = "Food Enthusiast";
            personalityDescription = "You love exploring culinary experiences! Food and dining are central to your lifestyle.";
            insights.add("Consider meal prepping to reduce dining out expenses.");
            insights.add("Look for restaurant loyalty programs and happy hour deals.");
            insights.add("Set a monthly dining budget to enjoy food while staying on track.");
        } else if (topCategoryName.contains("travel") || topCategoryName.contains("vacation")
                || topCategoryName.contains("transport")) {
            personalityType = "Experience Seeker";
            personalityDescription = "Adventure is your priority! You invest heavily in experiences and travel.";
            insights.add("Consider a dedicated travel savings account.");
            insights.add("Book flights and accommodations in advance for better deals.");
            insights.add("Look into travel rewards credit cards to offset travel costs.");
        } else if (topCategoryName.contains("housing") || topCategoryName.contains("rent")
                || topCategoryName.contains("utilities") || topCategoryName.contains("home")) {
            personalityType = "Home Nester";
            personalityDescription = "Your home is your sanctuary. You prioritize comfort and creating a perfect living space.";
            insights.add("Look for energy-efficient upgrades to reduce utility bills.");
            insights.add("Consider refinancing if you have a mortgage.");
            insights.add("Explore home improvement DIY to save on renovation costs.");
        } else if (topCategoryName.contains("shopping") || topCategoryName.contains("clothing")
                || topCategoryName.contains("retail")) {
            personalityType = "Lifestyle Curator";
            personalityDescription = "You have a keen eye for style and quality. Shopping is both a hobby and a self-expression tool.";
            insights.add("Try a 30-day no-shopping challenge to reset spending habits.");
            insights.add("Implement a 24-hour rule before making non-essential purchases.");
            insights.add("Consider selling items you no longer use to offset shopping costs.");
        } else if (isEssentialSpender(categoryTotals)) {
            personalityType = "Essential Minimalist";
            personalityDescription = "You're practical and focused. Your spending is concentrated on necessities and essentials.";
            insights.add("You're doing great keeping expenses focused on essentials!");
            insights.add("Consider allocating savings towards investments for long-term growth.");
            insights.add("Look for opportunities to automate savings from your disciplined approach.");
        } else {
            personalityType = "Balanced Spender";
            personalityDescription = "You maintain a healthy balance across different spending categories, showing financial maturity.";
            insights.add("Your balanced approach to spending is commendable!");
            insights.add("Consider diversifying your investment portfolio.");
            insights.add("Keep tracking your spending to maintain this healthy balance.");
        }

        // Add universal insights
        if (totalExpenses.compareTo(BigDecimal.ZERO) > 0) {
            insights.add(String.format("Your total spending over the last 3 months was $%.2f.",
                    totalExpenses.doubleValue()));
        }

        return SpendingDnaResponse.builder()
                .personalityType(personalityType)
                .personalityDescription(personalityDescription)
                .topCategories(topCategories)
                .insights(insights)
                .build();
    }

    private boolean isEssentialSpender(Map<String, BigDecimal> categoryTotals) {
        Set<String> essentialKeywords = Set.of("grocery", "groceries", "healthcare", "medical",
                "insurance", "utilities", "transport", "fuel", "gas");
        return categoryTotals.keySet().stream()
                .filter(name -> essentialKeywords.stream()
                        .anyMatch(keyword -> name.toLowerCase().contains(keyword)))
                .count() >= 2;
    }

    private Category createUncategorized() {
        Category uncategorized = new Category();
        uncategorized.setName("Uncategorized");
        uncategorized.setColorHex("#9E9E9E");
        return uncategorized;
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
