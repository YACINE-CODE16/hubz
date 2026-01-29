package com.hubz.application.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@hubz.com}")
    private String fromEmail;

    @Value("${app.frontend-url:http://localhost:5175}")
    private String frontendUrl;

    public void sendInvitationEmail(String toEmail, String organizationName, String token, String role) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Invitation à rejoindre " + organizationName + " sur Hubz");

            String invitationUrl = frontendUrl + "/join/" + token;
            String htmlContent = buildInvitationEmailTemplate(organizationName, invitationUrl, role);

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Invitation email sent to {} for organization {}", toEmail, organizationName);
        } catch (MessagingException e) {
            log.error("Failed to send invitation email to {}", toEmail, e);
            throw new RuntimeException("Failed to send invitation email", e);
        }
    }

    private String buildInvitationEmailTemplate(String organizationName, String invitationUrl, String role) {
        String roleLabel = getRoleLabel(role);

        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                            line-height: 1.6;
                            color: #333;
                            max-width: 600px;
                            margin: 0 auto;
                            padding: 20px;
                            background-color: #f5f5f5;
                        }
                        .container {
                            background-color: #ffffff;
                            border-radius: 12px;
                            padding: 40px;
                            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
                        }
                        .header {
                            text-align: center;
                            margin-bottom: 30px;
                        }
                        .logo {
                            font-size: 32px;
                            font-weight: bold;
                            color: #3B82F6;
                            margin-bottom: 10px;
                        }
                        h1 {
                            color: #1a1a1a;
                            font-size: 24px;
                            margin-bottom: 20px;
                        }
                        p {
                            color: #666;
                            margin-bottom: 15px;
                        }
                        .organization-name {
                            color: #3B82F6;
                            font-weight: 600;
                        }
                        .role-badge {
                            display: inline-block;
                            padding: 6px 12px;
                            background-color: #E0F2FE;
                            color: #0369A1;
                            border-radius: 6px;
                            font-size: 14px;
                            font-weight: 500;
                            margin: 10px 0;
                        }
                        .button {
                            display: inline-block;
                            padding: 14px 32px;
                            background-color: #3B82F6;
                            color: #ffffff;
                            text-decoration: none;
                            border-radius: 8px;
                            margin: 20px 0;
                            font-weight: 600;
                            transition: background-color 0.3s;
                        }
                        .button:hover {
                            background-color: #2563EB;
                        }
                        .footer {
                            text-align: center;
                            margin-top: 30px;
                            padding-top: 20px;
                            border-top: 1px solid #e5e5e5;
                            color: #999;
                            font-size: 12px;
                        }
                        .expiry-notice {
                            background-color: #FEF3C7;
                            border-left: 4px solid #F59E0B;
                            padding: 12px;
                            margin: 20px 0;
                            border-radius: 4px;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <div class="logo">Hubz</div>
                        </div>

                        <h1>Vous avez été invité à rejoindre une organisation</h1>

                        <p>Bonjour,</p>

                        <p>Vous avez été invité à rejoindre l'organisation <span class="organization-name">%s</span> sur Hubz.</p>

                        <p>Votre rôle sera : <span class="role-badge">%s</span></p>

                        <div style="text-align: center;">
                            <a href="%s" class="button">Accepter l'invitation</a>
                        </div>

                        <div class="expiry-notice">
                            <strong>⚠️ Note importante :</strong> Cette invitation expire dans 7 jours.
                        </div>

                        <p>Si vous n'avez pas de compte Hubz, vous devrez en créer un avant d'accepter l'invitation.</p>

                        <p>Si vous n'attendiez pas cette invitation, vous pouvez simplement ignorer cet email.</p>

                        <div class="footer">
                            <p>Cet email a été envoyé par Hubz</p>
                            <p>Si le bouton ne fonctionne pas, copiez ce lien dans votre navigateur :<br>
                            <a href="%s" style="color: #3B82F6; word-break: break-all;">%s</a></p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(organizationName, roleLabel, invitationUrl, invitationUrl, invitationUrl);
    }

    private String getRoleLabel(String role) {
        return switch (role) {
            case "OWNER" -> "Propriétaire";
            case "ADMIN" -> "Administrateur";
            case "MEMBER" -> "Membre";
            case "VIEWER" -> "Lecteur";
            default -> role;
        };
    }
}
