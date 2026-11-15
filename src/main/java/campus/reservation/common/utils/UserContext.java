package campus.reservation.common.utils;

public class UserContext {
    private static final ThreadLocal<Long> USER_HOLDER = new ThreadLocal<>();

    public static void setUserId(Long userId){
        USER_HOLDER.set(userId);
    }
    public static Long getUserId(){
        return USER_HOLDER.get();
    }
    public static void remove(){
        USER_HOLDER.remove();// 清除当前线程的用户ID，防止内存泄漏
    }
}
