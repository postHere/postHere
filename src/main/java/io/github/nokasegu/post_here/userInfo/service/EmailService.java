package io.github.nokasegu.post_here.userInfo.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;

    /**
     * 인증 코드를 포함한 이메일을 발송합니다.
     *
     * @param toEmail 수신자 이메일 주소
     * @param code    인증 코드
     */
    public void sendVerificationEmail(String toEmail, String code) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            mimeMessageHelper.setTo(toEmail);
            mimeMessageHelper.setSubject("[PostHere] 회원가입 이메일 인증 코드입니다.");

            // ✅ 이메일 본문 HTML 형식으로 구성
            StringBuilder emailBody = new StringBuilder();
            emailBody.append("<div style='font-family: Arial, sans-serif; text-align: center; color: #333;'>");
            emailBody.append("<h1 style='color: #4A90E2;'>PostHere 이메일 인증</h1>");
            emailBody.append("<p>PostHere에 가입해주셔서 감사합니다. 아래 인증 코드를 입력하여 가입을 완료해주세요.</p>");
            emailBody.append("<div style='background-color: #f2f2f2; padding: 20px; border-radius: 10px; display: inline-block; margin: 20px 0;'>");
            emailBody.append("<h2 style='color: #E74C3C; font-size: 24px; letter-spacing: 4px; margin: 0;'>");
            emailBody.append(code);
            emailBody.append("</h2>");
            emailBody.append("</div>");
            emailBody.append("<p style='font-size: 12px; color: #888;'>이 코드는 3분 동안 유효합니다.</p>");
            emailBody.append("</div>");

            mimeMessageHelper.setText(emailBody.toString(), true); // true: HTML 형식으로 발송

            javaMailSender.send(mimeMessage);
        } catch (MessagingException e) {
            // 이메일 발송 실패 시 로깅 또는 예외 처리
            throw new RuntimeException("이메일 발송에 실패했습니다.", e);
        }
    }
}