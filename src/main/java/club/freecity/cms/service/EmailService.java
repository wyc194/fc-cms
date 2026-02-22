package club.freecity.cms.service;

public interface EmailService {
    /**
     * 发送简单文本邮件
     * @param to 收件人
     * @param subject 主题
     * @param content 内容
     */
    void sendSimpleMail(String to, String subject, String content);

    /**
     * 发送 HTML 邮件
     * @param to 收件人
     * @param subject 主题
     * @param content HTML 内容
     */
    void sendHtmlMail(String to, String subject, String content);
}
