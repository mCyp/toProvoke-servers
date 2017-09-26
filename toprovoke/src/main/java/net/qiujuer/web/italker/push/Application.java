package net.qiujuer.web.italker.push;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import net.qiujuer.web.italker.push.provider.AuthRequestFilter;
import net.qiujuer.web.italker.push.provider.GsonProvider;
import net.qiujuer.web.italker.push.service.AccountService;
import org.glassfish.jersey.server.ResourceConfig;

import java.util.logging.Logger;

/**
 * Created by Administrator on 2017/7/19.
 */
public class Application extends ResourceConfig{
    public Application(){
        //注册逻辑处理的包名
        packages(AccountService.class.getPackage().getName());
        // 注册全局拦截器
        register(AuthRequestFilter.class);
        //注册一个json转换器
        //register(JacksonJsonProvider.class);
        //替换解析器为Gson
        register(GsonProvider.class);
        //注册日志打印输出
        register(Logger.class);
    }
}
