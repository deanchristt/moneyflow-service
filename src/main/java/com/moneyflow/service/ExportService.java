package com.moneyflow.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.moneyflow.model.entity.Transaction;
import com.moneyflow.model.enums.TransactionType;
import com.moneyflow.repository.TransactionRepository;
import com.moneyflow.security.SecurityUtils;
import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final TransactionRepository transactionRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Transactional(readOnly = true)
    public byte[] exportTransactionsToCSV(LocalDate startDate, LocalDate endDate) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Transaction> transactions = transactionRepository
                .findByUserIdAndTransactionDateBetweenAndIsActiveTrue(userId, startDate, endDate);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {

            // Header
            String[] header = {
                    "Date", "Type", "Category", "Account", "Amount", "Description", "Note", "Reference"
            };
            csvWriter.writeNext(header);

            // Data rows
            for (Transaction transaction : transactions) {
                String[] row = {
                        transaction.getTransactionDate().format(DATE_FORMATTER),
                        transaction.getType().name(),
                        transaction.getCategory().getName(),
                        transaction.getAccount().getName(),
                        transaction.getAmount().toString(),
                        transaction.getDescription() != null ? transaction.getDescription() : "",
                        transaction.getNote() != null ? transaction.getNote() : "",
                        transaction.getReferenceNumber() != null ? transaction.getReferenceNumber() : ""
                };
                csvWriter.writeNext(row);
            }

            csvWriter.flush();
            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to export transactions to CSV", e);
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportTransactionsToPDF(LocalDate startDate, LocalDate endDate) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Transaction> transactions = transactionRepository
                .findByUserIdAndTransactionDateBetweenAndIsActiveTrue(userId, startDate, endDate);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, outputStream);

            document.open();

            // Title
            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("Transaction Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            // Date range
            Font subtitleFont = new Font(Font.HELVETICA, 12, Font.NORMAL);
            Paragraph dateRange = new Paragraph(
                    String.format("Period: %s to %s", startDate.format(DATE_FORMATTER), endDate.format(DATE_FORMATTER)),
                    subtitleFont
            );
            dateRange.setAlignment(Element.ALIGN_CENTER);
            dateRange.setSpacingAfter(20);
            document.add(dateRange);

            // Summary
            BigDecimal totalIncome = transactions.stream()
                    .filter(t -> t.getType() == TransactionType.INCOME)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalExpense = transactions.stream()
                    .filter(t -> t.getType() == TransactionType.EXPENSE)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal netFlow = totalIncome.subtract(totalExpense);

            Font summaryFont = new Font(Font.HELVETICA, 10, Font.BOLD);
            Paragraph summary = new Paragraph();
            summary.add(new Chunk(String.format("Total Income: $%s  |  ", totalIncome), summaryFont));
            summary.add(new Chunk(String.format("Total Expense: $%s  |  ", totalExpense), summaryFont));
            summary.add(new Chunk(String.format("Net Flow: $%s", netFlow), summaryFont));
            summary.setAlignment(Element.ALIGN_CENTER);
            summary.setSpacingAfter(20);
            document.add(summary);

            // Table
            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2, 1.5f, 2, 2, 1.5f, 3, 2});

            // Table header
            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
            String[] headers = {"Date", "Type", "Category", "Account", "Amount", "Description", "Note"};

            for (String headerText : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(headerText, headerFont));
                cell.setBackgroundColor(new Color(66, 139, 202));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(8);
                table.addCell(cell);
            }

            // Table data
            Font dataFont = new Font(Font.HELVETICA, 9, Font.NORMAL);
            Font incomeFont = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(39, 174, 96));
            Font expenseFont = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(231, 76, 60));

            for (Transaction transaction : transactions) {
                table.addCell(createCell(transaction.getTransactionDate().format(DATE_FORMATTER), dataFont));

                // Type with color
                Font typeFont = transaction.getType() == TransactionType.INCOME ? incomeFont : expenseFont;
                table.addCell(createCell(transaction.getType().name(), typeFont));

                table.addCell(createCell(transaction.getCategory().getName(), dataFont));
                table.addCell(createCell(transaction.getAccount().getName(), dataFont));

                // Amount with color
                String amountStr = transaction.getType() == TransactionType.INCOME
                        ? "+" + transaction.getAmount().toString()
                        : "-" + transaction.getAmount().toString();
                table.addCell(createCell(amountStr, typeFont));

                table.addCell(createCell(
                        transaction.getDescription() != null ? transaction.getDescription() : "", dataFont));
                table.addCell(createCell(
                        transaction.getNote() != null ? transaction.getNote() : "", dataFont));
            }

            document.add(table);

            // Footer
            Paragraph footer = new Paragraph(
                    String.format("\nTotal Transactions: %d", transactions.size()),
                    subtitleFont
            );
            footer.setAlignment(Element.ALIGN_RIGHT);
            document.add(footer);

            document.close();
            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to export transactions to PDF", e);
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportMonthlyReportToPDF(Integer month, Integer year) {
        Long userId = SecurityUtils.getCurrentUserId();
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        List<Transaction> transactions = transactionRepository
                .findByUserIdAndTransactionDateBetweenAndIsActiveTrue(userId, startDate, endDate);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, outputStream);

            document.open();

            // Title
            Font titleFont = new Font(Font.HELVETICA, 20, Font.BOLD);
            Paragraph title = new Paragraph("Monthly Financial Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            // Month/Year
            Font subtitleFont = new Font(Font.HELVETICA, 14, Font.NORMAL);
            Paragraph monthYear = new Paragraph(
                    String.format("%s %d", startDate.getMonth().toString(), year),
                    subtitleFont
            );
            monthYear.setAlignment(Element.ALIGN_CENTER);
            monthYear.setSpacingAfter(30);
            document.add(monthYear);

            // Calculate summaries
            BigDecimal totalIncome = transactions.stream()
                    .filter(t -> t.getType() == TransactionType.INCOME)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalExpense = transactions.stream()
                    .filter(t -> t.getType() == TransactionType.EXPENSE)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal netFlow = totalIncome.subtract(totalExpense);

            // Summary table
            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(60);
            summaryTable.setHorizontalAlignment(Element.ALIGN_CENTER);

            Font labelFont = new Font(Font.HELVETICA, 12, Font.BOLD);
            Font valueFont = new Font(Font.HELVETICA, 12, Font.NORMAL);

            addSummaryRow(summaryTable, "Total Income:", "$" + totalIncome, labelFont,
                    new Font(Font.HELVETICA, 12, Font.BOLD, new Color(39, 174, 96)));
            addSummaryRow(summaryTable, "Total Expense:", "$" + totalExpense, labelFont,
                    new Font(Font.HELVETICA, 12, Font.BOLD, new Color(231, 76, 60)));
            addSummaryRow(summaryTable, "Net Flow:", "$" + netFlow, labelFont,
                    new Font(Font.HELVETICA, 12, Font.BOLD,
                            netFlow.compareTo(BigDecimal.ZERO) >= 0
                                    ? new Color(39, 174, 96) : new Color(231, 76, 60)));
            addSummaryRow(summaryTable, "Total Transactions:", String.valueOf(transactions.size()),
                    labelFont, valueFont);

            document.add(summaryTable);

            // Category breakdown
            document.add(new Paragraph("\n"));
            Font sectionFont = new Font(Font.HELVETICA, 14, Font.BOLD);
            Paragraph expenseSection = new Paragraph("Expense by Category", sectionFont);
            expenseSection.setSpacingBefore(20);
            expenseSection.setSpacingAfter(10);
            document.add(expenseSection);

            // Expense category table
            PdfPTable categoryTable = new PdfPTable(3);
            categoryTable.setWidthPercentage(80);
            categoryTable.setWidths(new float[]{3, 2, 1});

            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
            addCategoryHeader(categoryTable, headerFont);

            // Group by category
            transactions.stream()
                    .filter(t -> t.getType() == TransactionType.EXPENSE)
                    .collect(java.util.stream.Collectors.groupingBy(
                            t -> t.getCategory().getName(),
                            java.util.stream.Collectors.toList()))
                    .forEach((categoryName, categoryTransactions) -> {
                        BigDecimal categoryTotal = categoryTransactions.stream()
                                .map(Transaction::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        int count = categoryTransactions.size();
                        addCategoryRow(categoryTable, categoryName, categoryTotal, count);
                    });

            document.add(categoryTable);

            document.close();
            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to export monthly report to PDF", e);
        }
    }

    private PdfPCell createCell(String content, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setPadding(5);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private void addSummaryRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(8);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(8);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    private void addCategoryHeader(PdfPTable table, Font font) {
        String[] headers = {"Category", "Amount", "Count"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setBackgroundColor(new Color(66, 139, 202));
            cell.setPadding(8);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
    }

    private void addCategoryRow(PdfPTable table, String category, BigDecimal amount, int count) {
        Font dataFont = new Font(Font.HELVETICA, 9, Font.NORMAL);

        table.addCell(createCell(category, dataFont));
        table.addCell(createCell("$" + amount.toString(), dataFont));
        table.addCell(createCell(String.valueOf(count), dataFont));
    }
}
