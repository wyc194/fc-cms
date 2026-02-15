package club.freecity.cms.util;

import club.freecity.cms.exception.BusinessException;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.regex.Pattern;

public class PasswordUtils {

    // 至少8位，包含字母和数字
    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final String PASSWORD_PATTERN = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{8,}$";
    private static final Pattern PATTERN = Pattern.compile(PASSWORD_PATTERN);

    /**
     * 校验密码强度
     * @param password 密码
     * @throws BusinessException 如果校验不通过
     */
    public static void validate(String password) {
        if (!StringUtils.hasText(password)) {
            throw new BusinessException("密码不能为空");
        }
        if (password.length() < PASSWORD_MIN_LENGTH) {
            throw new BusinessException("密码长度至少为8位");
        }
        if (!PATTERN.matcher(password).matches()) {
            throw new BusinessException("密码必须包含字母和数字");
        }
    }

    /**
     * 生成符合策略的随机密码
     * @return 随机密码
     */
    public static String generateRandomPassword() {
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String allChars = letters + digits + "@$!%*#?&";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        
        // 确保包含至少一个字母和一个数字
        sb.append(letters.charAt(random.nextInt(letters.length())));
        sb.append(digits.charAt(random.nextInt(digits.length())));
        
        // 填充剩余位数到最小长度
        for (int i = 0; i < PASSWORD_MIN_LENGTH - 2; i++) {
            sb.append(allChars.charAt(random.nextInt(allChars.length())));
        }
        
        // 打乱顺序
        char[] passwordArray = sb.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }
        
        return new String(passwordArray);
    }
}
