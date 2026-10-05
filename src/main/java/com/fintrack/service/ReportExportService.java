package com.fintrack.service;

import com.fintrack.dto.CategoryReportResponse;
import com.fintrack.dto.MonthlyReportResponse;
import com.fintrack.dto.ReportSummaryResponse;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReportExportService {

    private static final float MARGIN = 50;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();

    private final ReportService reportService;

    public ReportExportService(
            ReportService reportService
    ) {
        this.reportService = reportService;
    }

    public byte[] generateCsv(
            Long userId,
            LocalDate from,
            LocalDate to
    ) {

        validateDateRange(from, to);

        ReportSummaryResponse summary =
                reportService.getSummary(
                        userId,
                        from,
                        to
                );

        List<CategoryReportResponse> categories =
                reportService.getCategoryReport(
                        userId,
                        from,
                        to
                );

        List<MonthlyReportResponse> monthly =
                reportService.getMonthlyReport(
                        userId,
                        from,
                        to
                );

        StringBuilder csv =
                new StringBuilder();

        csv.append("FinTrack Financial Report\n");
        csv.append("From,")
                .append(from)
                .append("\n");

        csv.append("To,")
                .append(to)
                .append("\n");

        csv.append("\n");

        csv.append("SUMMARY\n");
        csv.append("Metric,Amount\n");

        csv.append("Income,")
                .append(summary.getTotalIncome())
                .append("\n");

        csv.append("Expenses,")
                .append(summary.getTotalExpenses())
                .append("\n");

        csv.append("Net Savings,")
                .append(summary.getNetSavings())
                .append("\n");

        csv.append("Savings Rate,")
                .append(summary.getSavingsRate())
                .append("%\n");

        csv.append("\n");

        csv.append("SPENDING BY CATEGORY\n");
        csv.append("Category,Amount,Percentage\n");

        for (CategoryReportResponse category : categories) {

            csv.append(
                    csvEscape(category.getCategoryName())
            );

            csv.append(",")
                    .append(category.getAmount());

            csv.append(",")
                    .append(category.getPercentage())
                    .append("%\n");
        }

        csv.append("\n");

        csv.append("MONTHLY OVERVIEW\n");
        csv.append("Month,Income,Expenses,Savings\n");

        for (MonthlyReportResponse item : monthly) {

            csv.append(item.getMonth())
                    .append(",")
                    .append(item.getIncome())
                    .append(",")
                    .append(item.getExpenses())
                    .append(",")
                    .append(item.getSavings())
                    .append("\n");
        }

        return csv
                .toString()
                .getBytes(StandardCharsets.UTF_8);
    }

    public byte[] generatePdf(
            Long userId,
            LocalDate from,
            LocalDate to
    ) {

        validateDateRange(from, to);

        ReportSummaryResponse summary =
                reportService.getSummary(
                        userId,
                        from,
                        to
                );

        List<CategoryReportResponse> categories =
                reportService.getCategoryReport(
                        userId,
                        from,
                        to
                );

        List<MonthlyReportResponse> monthly =
                reportService.getMonthlyReport(
                        userId,
                        from,
                        to
                );

        try (
                PDDocument document =
                        new PDDocument();

                ByteArrayOutputStream output =
                        new ByteArrayOutputStream()
        ) {

            PdfPageWriter writer =
                    new PdfPageWriter(document);

            writer.title(
                    "FinTrack Financial Report"
            );

            writer.text(
                    "Period: "
                            + from
                            + " to "
                            + to,
                    10,
                    false
            );

            writer.space(15);

            writer.heading("Summary");

            writer.text(
                    "Income: Rs. "
                            + summary.getTotalIncome(),
                    10,
                    false
            );

            writer.text(
                    "Expenses: Rs. "
                            + summary.getTotalExpenses(),
                    10,
                    false
            );

            writer.text(
                    "Net Savings: Rs. "
                            + summary.getNetSavings(),
                    10,
                    false
            );

            writer.text(
                    "Savings Rate: "
                            + summary.getSavingsRate()
                            + "%",
                    10,
                    false
            );

            writer.space(15);

            writer.heading(
                    "Spending by Category"
            );

            if (categories.isEmpty()) {

                writer.text(
                        "No expenses found.",
                        10,
                        false
                );

            } else {

                for (CategoryReportResponse category :
                        categories) {

                    writer.text(
                            category.getCategoryName()
                                    + " - Rs. "
                                    + category.getAmount()
                                    + " ("
                                    + category.getPercentage()
                                    + "%)",
                            9,
                            false
                    );
                }
            }

            writer.space(15);

            writer.heading(
                    "Monthly Overview"
            );

            if (monthly.isEmpty()) {

                writer.text(
                        "No monthly transaction data found.",
                        10,
                        false
                );

            } else {

                for (MonthlyReportResponse item :
                        monthly) {

                    writer.text(
                            item.getMonth()
                                    + " - Income: Rs. "
                                    + item.getIncome()
                                    + ", Expenses: Rs. "
                                    + item.getExpenses()
                                    + ", Savings: Rs. "
                                    + item.getSavings(),
                            9,
                            false
                    );
                }
            }

            writer.close();

            document.save(output);

            return output.toByteArray();

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Failed to generate PDF report",
                    exception
            );
        }
    }

    private String csvEscape(
            String value
    ) {

        if (value == null) {
            return "";
        }

        if (
                value.contains(",")
                        || value.contains("\"")
                        || value.contains("\n")
                        || value.contains("\r")
        ) {

            return "\""
                    + value.replace(
                    "\"",
                    "\"\""
            )
                    + "\"";
        }

        return value;
    }

    private void validateDateRange(
            LocalDate from,
            LocalDate to
    ) {

        if (from == null || to == null) {
            throw new IllegalArgumentException(
                    "From and to dates are required"
            );
        }

        if (from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "From date cannot be after to date"
            );
        }
    }

    private static class PdfPageWriter {

        private final PDDocument document;

        private PDPage page;

        private PDPageContentStream content;

        private float y;

        PdfPageWriter(
                PDDocument document
        ) throws IOException {

            this.document = document;

            createPage();
        }

        private void createPage()
                throws IOException {

            page =
                    new PDPage(
                            PDRectangle.A4
                    );

            document.addPage(page);

            content =
                    new PDPageContentStream(
                            document,
                            page
                    );

            y =
                    PAGE_HEIGHT - MARGIN;
        }

        private void newPage()
                throws IOException {

            content.close();

            createPage();
        }

        void title(
                String text
        ) throws IOException {

            ensureSpace(35);

            write(
                    text,
                    18,
                    true
            );

            y -= 18;
        }

        void heading(
                String text
        ) throws IOException {

            ensureSpace(32);

            write(
                    text,
                    14,
                    true
            );

            y -= 16;
        }

        void text(
                String text,
                float fontSize,
                boolean bold
        ) throws IOException {

            ensureSpace(
                    fontSize + 12
            );

            write(
                    sanitizePdfText(text),
                    fontSize,
                    bold
            );

            y -= 14;
        }

        void space(
                float amount
        ) {

            y -= amount;
        }

        void close()
                throws IOException {

            content.close();
        }

        private void ensureSpace(
                float required
        ) throws IOException {

            if (y - required < MARGIN) {
                newPage();
            }
        }

        private void write(
                String text,
                float fontSize,
                boolean bold
        ) throws IOException {

            PDType1Font font =
                    bold
                            ? new PDType1Font(
                            Standard14Fonts.FontName.HELVETICA_BOLD
                    )
                            : new PDType1Font(
                            Standard14Fonts.FontName.HELVETICA
                    );

            content.beginText();

            content.setFont(
                    font,
                    fontSize
            );

            content.newLineAtOffset(
                    MARGIN,
                    y
            );

            content.showText(
                    sanitizePdfText(text)
            );

            content.endText();
        }

        private String sanitizePdfText(
                String text
        ) {

            if (text == null) {
                return "";
            }

            return text.replace(
                    "₹",
                    "Rs. "
            );
        }
    }
}