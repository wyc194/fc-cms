package club.freecity.cms.service;

public interface VerificationService {
    /**
     * 发送验证码到指定邮箱
     * @param email 邮箱
     * @param ip 请求 IP (用于限流)
     */
    void sendVerificationCode(String email, String ip);

    /**
     * 校验验证码
     * @param email 邮箱
     * @param code 验证码
     * @return 是否通过
     */
    boolean verifyCode(String email, String code);
}
