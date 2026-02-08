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

    public void sendPasswordResetEmail(String toEmail, String firstName, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Réinitialisation de votre mot de passe Hubz");

            String resetUrl = frontendUrl + "/reset-password/" + token;
            String htmlContent = buildPasswordResetEmailTemplate(firstName, resetUrl);

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to {}", toEmail, e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    public void sendEmailVerificationEmail(String toEmail, String firstName, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Vérifiez votre adresse email - Hubz");

            String verificationUrl = frontendUrl + "/verify-email/" + token;
            String htmlContent = buildEmailVerificationTemplate(firstName, verificationUrl);

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email verification sent to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send email verification to {}", toEmail, e);
            throw new RuntimeException("Failed to send email verification", e);
        }
    }

    private String buildPasswordResetEmailTemplate(String firstName, String resetUrl) {
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

                        <h1>Réinitialisation de votre mot de passe</h1>

                        <p>Bonjour %s,</p>

                        <p>Nous avons reçu une demande de réinitialisation de votre mot de passe. Cliquez sur le bouton ci-dessous pour créer un nouveau mot de passe :</p>

                        <div style="text-align: center;">
                            <a href="%s" class="button">Réinitialiser mon mot de passe</a>
                        </div>

                        <div class="expiry-notice">
                            <strong>Important :</strong> Ce lien expire dans 1 heure.
                        </div>

                        <p>Si vous n'avez pas demandé cette réinitialisation, vous pouvez ignorer cet email. Votre mot de passe restera inchangé.</p>

                        <div class="footer">
                            <p>Cet email a été envoyé par Hubz</p>
                            <p>Si le bouton ne fonctionne pas, copiez ce lien dans votre navigateur :<br>
                            <a href="%s" style="color: #3B82F6; word-break: break-all;">%s</a></p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(firstName, resetUrl, resetUrl, resetUrl);
    }

    public void sendWelcomeEmail(String toEmail, String firstName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Bienvenue sur Hubz !");

            String loginUrl = frontendUrl + "/login";
            String htmlContent = buildWelcomeEmailTemplate(firstName, loginUrl);

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Welcome email sent to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send welcome email to {}", toEmail, e);
            // Don't throw exception for welcome email - it's not critical
        }
    }

    private String buildWelcomeEmailTemplate(String firstName, String loginUrl) {
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
                        .welcome-icon {
                            font-size: 64px;
                            margin-bottom: 20px;
                        }
                        h1 {
                            color: #1a1a1a;
                            font-size: 28px;
                            margin-bottom: 20px;
                        }
                        p {
                            color: #666;
                            margin-bottom: 15px;
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
                        }
                        .feature-list {
                            background-color: #F8FAFC;
                            border-radius: 8px;
                            padding: 20px;
                            margin: 20px 0;
                        }
                        .feature-item {
                            display: flex;
                            align-items: center;
                            margin-bottom: 12px;
                            color: #333;
                        }
                        .feature-item:last-child {
                            margin-bottom: 0;
                        }
                        .feature-icon {
                            font-size: 20px;
                            margin-right: 12px;
                        }
                        .footer {
                            text-align: center;
                            margin-top: 30px;
                            padding-top: 20px;
                            border-top: 1px solid #e5e5e5;
                            color: #999;
                            font-size: 12px;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <div class="logo">Hubz</div>
                            <div class="welcome-icon">&#127881;</div>
                        </div>

                        <h1>Bienvenue sur Hubz, %s !</h1>

                        <p>Nous sommes ravis de vous compter parmi nous ! Hubz est votre nouvel espace pour gerer vos organisations, equipes et projets de maniere efficace.</p>

                        <div class="feature-list">
                            <div class="feature-item">
                                <span class="feature-icon">&#128188;</span>
                                <span>Creez et gerez vos organisations</span>
                            </div>
                            <div class="feature-item">
                                <span class="feature-icon">&#128203;</span>
                                <span>Organisez vos taches avec un tableau Kanban</span>
                            </div>
                            <div class="feature-item">
                                <span class="feature-icon">&#127919;</span>
                                <span>Definissez et suivez vos objectifs</span>
                            </div>
                            <div class="feature-item">
                                <span class="feature-icon">&#128197;</span>
                                <span>Planifiez vos evenements dans le calendrier</span>
                            </div>
                            <div class="feature-item">
                                <span class="feature-icon">&#9989;</span>
                                <span>Suivez vos habitudes quotidiennes</span>
                            </div>
                        </div>

                        <p>Pret a commencer ? Connectez-vous pour explorer toutes les fonctionnalites :</p>

                        <div style="text-align: center;">
                            <a href="%s" class="button">Se connecter a Hubz</a>
                        </div>

                        <div class="footer">
                            <p>Cet email a ete envoye par Hubz</p>
                            <p>Si vous n'avez pas cree de compte sur Hubz, vous pouvez ignorer cet email.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(firstName, loginUrl);
    }

    private String buildEmailVerificationTemplate(String firstName, String verificationUrl) {
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
                        .button {
                            display: inline-block;
                            padding: 14px 32px;
                            background-color: #22C55E;
                            color: #ffffff;
                            text-decoration: none;
                            border-radius: 8px;
                            margin: 20px 0;
                            font-weight: 600;
                            transition: background-color 0.3s;
                        }
                        .button:hover {
                            background-color: #16A34A;
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
                        .welcome-icon {
                            font-size: 48px;
                            margin-bottom: 20px;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <div class="logo">Hubz</div>
                            <div class="welcome-icon">&#127881;</div>
                        </div>

                        <h1>Bienvenue sur Hubz !</h1>

                        <p>Bonjour %s,</p>

                        <p>Merci de vous être inscrit sur Hubz ! Pour activer votre compte et commencer à utiliser toutes nos fonctionnalités, veuillez vérifier votre adresse email en cliquant sur le bouton ci-dessous :</p>

                        <div style="text-align: center;">
                            <a href="%s" class="button">Vérifier mon email</a>
                        </div>

                        <div class="expiry-notice">
                            <strong>Important :</strong> Ce lien expire dans 24 heures.
                        </div>

                        <p>Si vous n'avez pas créé de compte sur Hubz, vous pouvez ignorer cet email.</p>

                        <div class="footer">
                            <p>Cet email a été envoyé par Hubz</p>
                            <p>Si le bouton ne fonctionne pas, copiez ce lien dans votre navigateur :<br>
                            <a href="%s" style="color: #3B82F6; word-break: break-all;">%s</a></p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(firstName, verificationUrl, verificationUrl, verificationUrl);
    }

    /**
     * Send weekly digest email to user.
     *
     * @param toEmail                 the recipient email
     * @param firstName               the user's first name
     * @param tasksCompletedThisWeek  tasks completed this week
     * @param tasksCompletedLastWeek  tasks completed last week
     * @param goalsInProgress         number of goals in progress
     * @param goalsCompleted          number of goals completed
     * @param habitsCompletionRate    habits completion rate (0-100)
     * @param upcomingEventsCount     number of events next week
     * @param topAchievement          top achievement of the week
     */
    public void sendWeeklyDigestEmail(
            String toEmail,
            String firstName,
            int tasksCompletedThisWeek,
            int tasksCompletedLastWeek,
            int goalsInProgress,
            int goalsCompleted,
            int habitsCompletionRate,
            int upcomingEventsCount,
            String topAchievement
    ) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Votre recap hebdomadaire Hubz");

            String dashboardUrl = frontendUrl + "/personal";
            String htmlContent = buildWeeklyDigestTemplate(
                    firstName,
                    tasksCompletedThisWeek,
                    tasksCompletedLastWeek,
                    goalsInProgress,
                    goalsCompleted,
                    habitsCompletionRate,
                    upcomingEventsCount,
                    topAchievement,
                    dashboardUrl
            );

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Weekly digest email sent to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send weekly digest email to {}", toEmail, e);
            // Don't throw exception for digest email - it's not critical
        }
    }

    private String buildWeeklyDigestTemplate(
            String firstName,
            int tasksCompletedThisWeek,
            int tasksCompletedLastWeek,
            int goalsInProgress,
            int goalsCompleted,
            int habitsCompletionRate,
            int upcomingEventsCount,
            String topAchievement,
            String dashboardUrl
    ) {
        String tasksTrend = buildTasksTrendHtml(tasksCompletedThisWeek, tasksCompletedLastWeek);
        String habitsBarWidth = String.valueOf(habitsCompletionRate);
        String habitsBarColor = habitsCompletionRate >= 80 ? "#22C55E" :
                habitsCompletionRate >= 50 ? "#F59E0B" : "#EF4444";

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
                        .week-icon {
                            font-size: 48px;
                            margin-bottom: 10px;
                        }
                        h1 {
                            color: #1a1a1a;
                            font-size: 24px;
                            margin-bottom: 10px;
                        }
                        .subtitle {
                            color: #666;
                            font-size: 14px;
                            margin-bottom: 30px;
                        }
                        .stats-grid {
                            display: grid;
                            grid-template-columns: repeat(2, 1fr);
                            gap: 16px;
                            margin: 20px 0;
                        }
                        .stat-card {
                            background-color: #F8FAFC;
                            border-radius: 8px;
                            padding: 16px;
                            text-align: center;
                        }
                        .stat-value {
                            font-size: 28px;
                            font-weight: bold;
                            color: #1a1a1a;
                        }
                        .stat-label {
                            font-size: 12px;
                            color: #666;
                            margin-top: 4px;
                        }
                        .stat-trend {
                            font-size: 12px;
                            margin-top: 4px;
                        }
                        .trend-up {
                            color: #22C55E;
                        }
                        .trend-down {
                            color: #EF4444;
                        }
                        .trend-same {
                            color: #666;
                        }
                        .section {
                            margin: 24px 0;
                        }
                        .section-title {
                            font-size: 16px;
                            font-weight: 600;
                            color: #1a1a1a;
                            margin-bottom: 12px;
                        }
                        .progress-bar-container {
                            background-color: #E5E7EB;
                            border-radius: 8px;
                            height: 12px;
                            overflow: hidden;
                        }
                        .progress-bar {
                            height: 100%%;
                            border-radius: 8px;
                        }
                        .achievement-box {
                            background: linear-gradient(135deg, #FEF3C7 0%%, #FDE68A 100%%);
                            border-radius: 8px;
                            padding: 16px;
                            margin: 20px 0;
                        }
                        .achievement-icon {
                            font-size: 24px;
                            margin-right: 8px;
                        }
                        .achievement-text {
                            font-weight: 500;
                            color: #92400E;
                        }
                        .events-preview {
                            background-color: #EFF6FF;
                            border-radius: 8px;
                            padding: 16px;
                            margin: 20px 0;
                        }
                        .events-icon {
                            font-size: 20px;
                            margin-right: 8px;
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
                        }
                        .footer {
                            text-align: center;
                            margin-top: 30px;
                            padding-top: 20px;
                            border-top: 1px solid #e5e5e5;
                            color: #999;
                            font-size: 12px;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <div class="logo">Hubz</div>
                            <div class="week-icon">&#128200;</div>
                            <h1>Recap de la semaine</h1>
                            <div class="subtitle">Bonjour %s, voici votre bilan hebdomadaire</div>
                        </div>

                        <div class="stats-grid">
                            <div class="stat-card">
                                <div class="stat-value">%d</div>
                                <div class="stat-label">Taches completees</div>
                                %s
                            </div>
                            <div class="stat-card">
                                <div class="stat-value">%d</div>
                                <div class="stat-label">Objectifs en cours</div>
                            </div>
                            <div class="stat-card">
                                <div class="stat-value">%d</div>
                                <div class="stat-label">Objectifs atteints</div>
                            </div>
                            <div class="stat-card">
                                <div class="stat-value">%d</div>
                                <div class="stat-label">Evenements a venir</div>
                            </div>
                        </div>

                        <div class="section">
                            <div class="section-title">Habitudes - Taux de completion</div>
                            <div class="progress-bar-container">
                                <div class="progress-bar" style="width: %s%%; background-color: %s;"></div>
                            </div>
                            <div style="text-align: right; font-size: 14px; color: #666; margin-top: 4px;">%d%%</div>
                        </div>

                        %s

                        <div class="events-preview">
                            <span class="events-icon">&#128197;</span>
                            <span style="color: #1E40AF;">%d evenement(s) prevu(s) pour la semaine prochaine</span>
                        </div>

                        <div style="text-align: center;">
                            <a href="%s" class="button">Voir mon tableau de bord</a>
                        </div>

                        <div class="footer">
                            <p>Cet email a ete envoye par Hubz</p>
                            <p>Vous recevez ce recapitulatif car les notifications digest sont activees dans vos preferences.</p>
                            <p>Pour vous desabonner, modifiez vos preferences dans les parametres de votre compte.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                firstName,
                tasksCompletedThisWeek,
                tasksTrend,
                goalsInProgress,
                goalsCompleted,
                upcomingEventsCount,
                habitsBarWidth,
                habitsBarColor,
                habitsCompletionRate,
                buildAchievementHtml(topAchievement),
                upcomingEventsCount,
                dashboardUrl
        );
    }

    private String buildTasksTrendHtml(int thisWeek, int lastWeek) {
        if (thisWeek > lastWeek) {
            int diff = thisWeek - lastWeek;
            return "<div class=\"stat-trend trend-up\">+%d vs semaine derniere</div>".formatted(diff);
        } else if (thisWeek < lastWeek) {
            int diff = lastWeek - thisWeek;
            return "<div class=\"stat-trend trend-down\">-%d vs semaine derniere</div>".formatted(diff);
        } else {
            return "<div class=\"stat-trend trend-same\">=  semaine derniere</div>";
        }
    }

    private String buildAchievementHtml(String achievement) {
        if (achievement == null || achievement.isBlank()) {
            return "";
        }
        return """
                <div class="achievement-box">
                    <span class="achievement-icon">&#127942;</span>
                    <span class="achievement-text">%s</span>
                </div>
                """.formatted(achievement);
    }

    /**
     * Send deadline reminder email to user.
     *
     * @param toEmail   the recipient email
     * @param firstName the user's first name
     * @param reminders the deadline reminder data
     */
    public void sendDeadlineReminderEmail(
            String toEmail,
            String firstName,
            DeadlineReminderService.DeadlineReminderData reminders
    ) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Rappel: " + reminders.totalCount() + " echeance(s) a venir - Hubz");

            String dashboardUrl = frontendUrl + "/personal";
            String htmlContent = buildDeadlineReminderTemplate(firstName, reminders, dashboardUrl);

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Deadline reminder email sent to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send deadline reminder email to {}", toEmail, e);
            // Don't throw exception for reminder email - it's not critical
        }
    }

    private String buildDeadlineReminderTemplate(
            String firstName,
            DeadlineReminderService.DeadlineReminderData reminders,
            String dashboardUrl
    ) {
        StringBuilder todayHtml = buildDeadlineItemsHtml(reminders.todayItems(), "#EF4444");
        StringBuilder thisWeekHtml = buildDeadlineItemsHtml(reminders.thisWeekItems(), "#F59E0B");
        StringBuilder nextWeekHtml = buildDeadlineItemsHtml(reminders.nextWeekItems(), "#3B82F6");

        String todaySection = reminders.todayItems().isEmpty() ? "" : """
                <div class="deadline-section">
                    <h3 class="section-title urgent">Aujourd'hui / Demain (%d)</h3>
                    %s
                </div>
                """.formatted(reminders.todayItems().size(), todayHtml);

        String thisWeekSection = reminders.thisWeekItems().isEmpty() ? "" : """
                <div class="deadline-section">
                    <h3 class="section-title warning">Cette semaine (%d)</h3>
                    %s
                </div>
                """.formatted(reminders.thisWeekItems().size(), thisWeekHtml);

        String nextWeekSection = reminders.nextWeekItems().isEmpty() ? "" : """
                <div class="deadline-section">
                    <h3 class="section-title info">Semaine prochaine (%d)</h3>
                    %s
                </div>
                """.formatted(reminders.nextWeekItems().size(), nextWeekHtml);

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
                        .reminder-icon {
                            font-size: 48px;
                            margin-bottom: 10px;
                        }
                        h1 {
                            color: #1a1a1a;
                            font-size: 24px;
                            margin-bottom: 10px;
                        }
                        .subtitle {
                            color: #666;
                            font-size: 14px;
                            margin-bottom: 30px;
                        }
                        .deadline-section {
                            margin: 24px 0;
                        }
                        .section-title {
                            font-size: 16px;
                            font-weight: 600;
                            padding: 8px 12px;
                            border-radius: 6px;
                            margin-bottom: 12px;
                        }
                        .section-title.urgent {
                            background-color: #FEE2E2;
                            color: #DC2626;
                        }
                        .section-title.warning {
                            background-color: #FEF3C7;
                            color: #D97706;
                        }
                        .section-title.info {
                            background-color: #DBEAFE;
                            color: #2563EB;
                        }
                        .deadline-item {
                            display: flex;
                            align-items: center;
                            justify-content: space-between;
                            padding: 12px 16px;
                            background-color: #F8FAFC;
                            border-radius: 8px;
                            margin-bottom: 8px;
                            border-left: 4px solid;
                        }
                        .deadline-item-content {
                            flex: 1;
                        }
                        .deadline-item-title {
                            font-weight: 500;
                            color: #1a1a1a;
                            margin-bottom: 2px;
                        }
                        .deadline-item-type {
                            font-size: 12px;
                            color: #666;
                        }
                        .deadline-item-date {
                            font-size: 14px;
                            font-weight: 500;
                            color: #666;
                            text-align: right;
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
                        }
                        .footer {
                            text-align: center;
                            margin-top: 30px;
                            padding-top: 20px;
                            border-top: 1px solid #e5e5e5;
                            color: #999;
                            font-size: 12px;
                        }
                        .summary-box {
                            background: linear-gradient(135deg, #EFF6FF 0%%, #DBEAFE 100%%);
                            border-radius: 8px;
                            padding: 16px;
                            text-align: center;
                            margin-bottom: 24px;
                        }
                        .summary-count {
                            font-size: 36px;
                            font-weight: bold;
                            color: #1E40AF;
                        }
                        .summary-label {
                            color: #3B82F6;
                            font-size: 14px;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <div class="logo">Hubz</div>
                            <div class="reminder-icon">&#128276;</div>
                            <h1>Rappel d'echeances</h1>
                            <div class="subtitle">Bonjour %s, voici vos prochaines echeances</div>
                        </div>

                        <div class="summary-box">
                            <div class="summary-count">%d</div>
                            <div class="summary-label">echeance(s) a venir</div>
                        </div>

                        %s
                        %s
                        %s

                        <div style="text-align: center;">
                            <a href="%s" class="button">Voir mon tableau de bord</a>
                        </div>

                        <div class="footer">
                            <p>Cet email a ete envoye par Hubz</p>
                            <p>Vous recevez ce rappel car les rappels d'echeance sont actives dans vos preferences.</p>
                            <p>Pour vous desabonner, modifiez vos preferences dans les parametres de votre compte.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                firstName,
                reminders.totalCount(),
                todaySection,
                thisWeekSection,
                nextWeekSection,
                dashboardUrl
        );
    }

    /**
     * Send notification email to user.
     *
     * @param toEmail          the recipient email
     * @param firstName        the user's first name
     * @param notificationType the type of notification
     * @param title            the notification title
     * @param message          the notification message
     * @param link             optional link to the relevant page
     */
    public void sendNotificationEmail(
            String toEmail,
            String firstName,
            String notificationType,
            String title,
            String message,
            String link
    ) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(title + " - Hubz");

            String actionUrl = link != null ? frontendUrl + link : frontendUrl;
            String htmlContent = buildNotificationEmailTemplate(firstName, notificationType, title, message, actionUrl);

            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("Notification email sent to {} for type {}", toEmail, notificationType);
        } catch (MessagingException e) {
            log.error("Failed to send notification email to {} for type {}", toEmail, notificationType, e);
            // Don't throw exception for notification email - it's not critical
        }
    }

    private String buildNotificationEmailTemplate(
            String firstName,
            String notificationType,
            String title,
            String message,
            String actionUrl
    ) {
        String iconEmoji = getNotificationIcon(notificationType);
        String accentColor = getNotificationColor(notificationType);

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
                        .notification-icon {
                            font-size: 48px;
                            margin-bottom: 10px;
                        }
                        h1 {
                            color: #1a1a1a;
                            font-size: 22px;
                            margin-bottom: 10px;
                        }
                        .notification-badge {
                            display: inline-block;
                            padding: 4px 12px;
                            border-radius: 16px;
                            font-size: 12px;
                            font-weight: 500;
                            margin-bottom: 20px;
                            background-color: %s;
                            color: white;
                        }
                        p {
                            color: #666;
                            margin-bottom: 15px;
                        }
                        .message-box {
                            background-color: #F8FAFC;
                            border-radius: 8px;
                            padding: 20px;
                            margin: 20px 0;
                            border-left: 4px solid %s;
                        }
                        .message-box p {
                            color: #333;
                            margin: 0;
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
                        }
                        .footer {
                            text-align: center;
                            margin-top: 30px;
                            padding-top: 20px;
                            border-top: 1px solid #e5e5e5;
                            color: #999;
                            font-size: 12px;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <div class="logo">Hubz</div>
                            <div class="notification-icon">%s</div>
                            <h1>%s</h1>
                        </div>

                        <p>Bonjour %s,</p>

                        <div class="message-box">
                            <p>%s</p>
                        </div>

                        <div style="text-align: center;">
                            <a href="%s" class="button">Voir sur Hubz</a>
                        </div>

                        <div class="footer">
                            <p>Cet email a ete envoye par Hubz</p>
                            <p>Pour gerer vos preferences de notification, rendez-vous dans les parametres de votre compte.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(accentColor, accentColor, iconEmoji, title, firstName, message, actionUrl);
    }

    private String getNotificationIcon(String notificationType) {
        return switch (notificationType) {
            case "TASK_ASSIGNED" -> "&#128203;"; // Clipboard
            case "TASK_COMPLETED" -> "&#9989;"; // Check mark
            case "TASK_DUE_SOON", "TASK_OVERDUE" -> "&#9200;"; // Alarm clock
            case "MENTION" -> "&#128172;"; // Speech bubble
            case "ORGANIZATION_INVITE" -> "&#128101;"; // People
            case "ORGANIZATION_ROLE_CHANGED" -> "&#128081;"; // Crown
            case "ORGANIZATION_MEMBER_JOINED" -> "&#128075;"; // Waving hand
            case "ORGANIZATION_MEMBER_LEFT" -> "&#128682;"; // Door
            case "GOAL_DEADLINE_APPROACHING", "GOAL_AT_RISK" -> "&#127919;"; // Target
            case "GOAL_COMPLETED" -> "&#127942;"; // Trophy
            case "EVENT_REMINDER", "EVENT_INVITATION" -> "&#128197;"; // Calendar
            case "EVENT_UPDATED" -> "&#128260;"; // Refresh
            case "EVENT_CANCELLED" -> "&#10060;"; // Cross mark
            default -> "&#128276;"; // Bell
        };
    }

    private String getNotificationColor(String notificationType) {
        return switch (notificationType) {
            case "TASK_ASSIGNED" -> "#3B82F6"; // Blue
            case "TASK_COMPLETED", "GOAL_COMPLETED" -> "#22C55E"; // Green
            case "TASK_DUE_SOON", "GOAL_DEADLINE_APPROACHING", "GOAL_AT_RISK" -> "#F59E0B"; // Yellow
            case "TASK_OVERDUE", "EVENT_CANCELLED" -> "#EF4444"; // Red
            case "ORGANIZATION_INVITE" -> "#8B5CF6"; // Purple
            case "ORGANIZATION_ROLE_CHANGED", "ORGANIZATION_MEMBER_JOINED", "ORGANIZATION_MEMBER_LEFT" -> "#6366F1"; // Indigo
            case "EVENT_REMINDER", "EVENT_UPDATED", "EVENT_INVITATION" -> "#06B6D4"; // Cyan
            case "MENTION" -> "#EC4899"; // Pink
            default -> "#6B7280"; // Gray
        };
    }

    private StringBuilder buildDeadlineItemsHtml(
            java.util.List<DeadlineReminderService.DeadlineItem> items,
            String borderColor
    ) {
        StringBuilder html = new StringBuilder();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (DeadlineReminderService.DeadlineItem item : items) {
            String formattedDate = item.dueDate().format(formatter);
            html.append("""
                    <div class="deadline-item" style="border-left-color: %s;">
                        <div class="deadline-item-content">
                            <div class="deadline-item-title">%s</div>
                            <div class="deadline-item-type">%s - %s</div>
                        </div>
                        <div class="deadline-item-date">%s</div>
                    </div>
                    """.formatted(borderColor, item.title(), item.type(), item.urgency(), formattedDate));
        }

        return html;
    }
}
