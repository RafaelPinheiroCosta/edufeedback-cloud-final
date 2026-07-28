package br.com.edufeedback.email;

public interface EmailSender {
  SendResult sendHtml(String to, String subject, String html);

  record SendResult(String operationId, String status) {}
}
