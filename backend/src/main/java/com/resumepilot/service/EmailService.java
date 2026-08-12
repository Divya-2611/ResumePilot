package com.resumepilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Sends branded HTML transactional emails.
 * Two pluggable backends:
 *   MAIL_MODE=api (default) - Brevo HTTP API on port 443 (never blocked), or
 *   MAIL_MODE=smtp          - JavaMail SMTP relay (Brevo SMTP relay).
 * Sending is asynchronous so auth flows are not slowed by mail latency.
 */
@Slf4j
@Service
@EnableAsync
@RequiredArgsConstructor
public class EmailService {

    private static final String BRAND = "ResumePilot";
    private static final String BRAND_BLUE = "#B45309";
    private static final String BRAND_LIGHT = "#FEF3C7";
    private static final String BREVO_ENDPOINT = "https://api.brevo.com/v3/smtp/email";

    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;

    @Value("${MAIL_MODE:api}")
    private String mailMode;

    @Value("${BREVO_API_KEY:}")
    private String brevoApiKey;

    @Value("${MAIL_FROM:${spring.mail.username:}}")
    private String fromAddress;

    /** Sends an OTP email with the code shown large and highlighted. */
    @Async
    public void sendOtpEmail(String to, String subject, String otp, int expiresMinutes) {
        String otpSpaced = String.join("&nbsp;", otp.split(""));
        String body = """
                <div style="text-align:center; margin: 24px 0;">
                  <p style="font-size:14px; color:#64748B; margin:0 0 6px 0;">Your verification code is</p>
                  <div style="display:inline-block; background:%s; border:2px solid %s; border-radius:12px;
                              padding:14px 32px; letter-spacing:10px;">
                    <span style="font-size:42px; font-weight:800; color:%s;">%s</span>
                  </div>
                  <p style="font-size:13px; color:#94A3B8; margin:14px 0 0 0;">This code expires in <b>%d minutes</b>.</p>
                </div>
                """.formatted(BRAND_LIGHT, BRAND_BLUE, BRAND_BLUE, otpSpaced, expiresMinutes);
        sendEmail(to, subject, layout(body,
                "Enter this code on the " + BRAND + " page to verify your account."));
    }

    /** Welcome email after successful email verification - username shown big. */
    @Async
    public void sendWelcomeEmail(String to, String firstName) {
        String body = """
                <div style="text-align:center; margin: 24px 0;">
                  <p style="font-size:15px; color:#64748B; margin:0 0 4px 0;">Welcome to</p>
                  <p style="font-size:14px; color:#94A3B8; margin:0 0 14px 0;">ResumePilot</p>
                  <span style="display:inline-block; font-size:32px; font-weight:800; color:%s;">Hi %s 👋</span>
                  <p style="font-size:14px; color:#334155; line-height:1.7; margin:16px 24px 0 24px;">
                    Your account is verified. Upload your resume, paste a job description,
                    and let AI craft an ATS-optimized version for you.
                  </p>
                </div>
                """.formatted(BRAND_BLUE, firstName);
        sendEmail(to, "Welcome to ResumePilot 🎉", layout(body,
                "Happy resume optimizing - your next opportunity is one optimization away."));
    }

    @Async
    public void sendPasswordResetConfirmation(String to) {
        String body = """
                <div style="text-align:center; margin: 24px 0;">
                  <span style="display:inline-block; font-size:22px; font-weight:700; color:#334155;">
                    Password changed successfully
                  </span>
                  <p style="font-size:14px; color:#64748B; line-height:1.7; margin:14px 24px 0 24px;">
                    Your <b>ResumePilot</b> password was changed.
                    If this was not you, please contact support immediately.
                  </p>
                </div>
                """;
        sendEmail(to, "Password changed", layout(body,
                "Security notice for your " + BRAND + " account."));
    }

    /**
     * Sends the branded HTML email (with a plain-text fallback for old clients).
     */
    private void sendEmail(String to, String subject, String htmlBody) {
        int attempt = 0;
        while (attempt < 2) {
            attempt++;
            try {
                if ("api".equalsIgnoreCase(mailMode)) {
                    sendBrevoApi(to, subject, htmlBody);
                } else {
                    sendSmtp(to, subject, htmlBody);
                }
                log.info("Email sent to {}", to);
                return;
            } catch (Exception e) {
                log.warn("Email send to {} failed (attempt {}/2): {}", to, attempt, e.getMessage());
                if (attempt < 2) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
    }

    private void sendSmtp(String to, String subject, String htmlBody) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromAddress);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(stripHtml(htmlBody), htmlBody);
        mailSender.send(message);
    }

    /** Sends via Brevo's transactional HTTP API (port 443) - no SMTP needed. */
    private void sendBrevoApi(String to, String subject, String htmlBody) throws Exception {
        if (brevoApiKey.isBlank()) {
            throw new IllegalStateException("BREVO_API_KEY is not set");
        }
        Map<String, Object> payload = Map.of(
                "sender", Map.of("name", BRAND, "email", fromAddress),
                "to", List.of(Map.of("email", to)),
                "subject", subject,
                "htmlContent", htmlBody,
                "textContent", stripHtml(htmlBody));
        HttpRequest request = HttpRequest.newBuilder(URI.create(BREVO_ENDPOINT))
                .header("api-key", brevoApiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            throw new RuntimeException("Brevo API returned " + response.statusCode() + ": " + response.body());
        }
    }

    /** Branded layout: header bar, content card, footer. */
    private String layout(String bodyHtml, String subtitle) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0; padding:0; background-color:#F1F5F9; font-family:Arial, Helvetica, sans-serif;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#F1F5F9;">
                    <tr>
                      <td align="center" style="padding:32px 16px;">
                        <table role="presentation" width="600" cellpadding="0" cellspacing="0"
                               style="max-width:600px; width:100%%; background-color:#FFFFFF; border-radius:16px;
                                      overflow:hidden; box-shadow:0 4px 16px rgba(15,23,42,0.08);">
                          <tr>
                            <td style="background-color:%s; padding:22px 28px;">
                              <span style="font-size:18px; font-weight:800; color:#FFFFFF;">🚀 %s</span>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:28px 32px;">
                              %s
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:20px 32px; border-top:1px solid #E2E8F0; background-color:#F8FAFC;">
                              <p style="font-size:12px; color:#94A3B8; margin:0; text-align:center;">
                                %s · If you did not request this email, please ignore it.
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(BRAND_BLUE, BRAND, bodyHtml, subtitle);
    }

    /** Crude HTML-to-text for the plain-text alternative part. */
    private String stripHtml(String html) {
        return html.replaceAll("<[^>]+>", " ").replaceAll("&nbsp;", " ")
                .replaceAll("\\s+", " ").trim();
    }
}
