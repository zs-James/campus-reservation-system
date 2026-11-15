package campus.reservation.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        //创建一个MybatisPlusInterceptor对象
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        //添加分页拦截器
        PaginationInnerInterceptor pageInterceptor = new PaginationInnerInterceptor();

        //配置分页拦截器
        pageInterceptor.setDbType(DbType.MYSQL);
        pageInterceptor.setMaxLimit(1000L);

        //将分页拦截器添加到MybatisPlusInterceptor对象中
        interceptor.addInnerInterceptor(pageInterceptor);
        return interceptor;
    }
}
