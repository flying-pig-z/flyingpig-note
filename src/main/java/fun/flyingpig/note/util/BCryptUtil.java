package fun.flyingpig.note.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * BCrypt密码加密工具类
 */
public class BCryptUtil {
    
    /**
     * 默认的哈希强度
     */
    private static final int DEFAULT_STRENGTH = 12;
    
    /**
     * 对密码进行哈希
     * @param plainPassword 明文密码
     * @return 哈希后的密码
     */
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(DEFAULT_STRENGTH));
    }
    
    /**
     * 验证密码是否匹配
     * @param plainPassword 明文密码
     * @param hashedPassword 哈希后的密码
     * @return 如果密码匹配返回true，否则返回false
     */
    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}