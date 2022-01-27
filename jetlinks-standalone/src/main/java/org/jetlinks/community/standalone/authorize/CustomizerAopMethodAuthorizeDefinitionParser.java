package org.jetlinks.community.standalone.authorize;

import lombok.EqualsAndHashCode;
import org.hswebframework.web.aop.MethodInterceptorContext;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.basic.aop.AopMethodAuthorizeDefinitionCustomizerParser;
import org.hswebframework.web.authorization.basic.define.EmptyAuthorizeDefinition;
import org.hswebframework.web.authorization.define.AuthorizeDefinition;
import org.hswebframework.web.utils.AnnotationUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wusy
 * Company: 福建亿鑫海信息科技有限公司
 * Createtime : 2022/1/18 下午4:50
 * Description :
 * 注意：本内容仅限于福建亿鑫海信息科技有限公司内部传阅，禁止外泄以及用于其他的商业目的
 */
@Configuration
@ConditionalOnProperty(value = "hsweb.authorize.customizer.method-authorize", matchIfMissing = true)
public class CustomizerAopMethodAuthorizeDefinitionParser implements AopMethodAuthorizeDefinitionCustomizerParser {

    /**
     * 是否允许匿名访问,这是一个危险的配置,慎重开启,生产环境严禁开启
     */
    @Value("${hsweb.authorize.customizer.allow-anonymous:false}")
    private boolean anonymous;

    private final Map<CacheKey, AuthorizeDefinition> cache = new ConcurrentHashMap<>();

    private static final Set<String> excludeMethodName = new HashSet<>(Arrays.asList("toString", "clone", "hashCode", "getClass"));

    @Override
    public AuthorizeDefinition parse(Class<?> target, Method method, MethodInterceptorContext context) {
        if (excludeMethodName.contains(method.getName())) {
            return null;
        }
        //是否允许匿名访问
        if (anonymous) {
            return EmptyAuthorizeDefinition.instance;
        }
        CacheKey key = buildCacheKey(target, method);

        AuthorizeDefinition definition = cache.get(key);
        if (definition instanceof EmptyAuthorizeDefinition) {
            return null;
        }
        if (null != definition) {
            return definition;
        }

        Authorize annotation = AnnotationUtils.findAnnotation(target, method, Authorize.class);

        if (annotation != null && annotation.ignore()) {
            cache.put(key, EmptyAuthorizeDefinition.instance);
            return null;
        }
        synchronized (cache) {
            return cache.computeIfAbsent(key, (__) -> CustomizerAuthorizeDefinition.from(target, method));
        }
    }


    public CacheKey buildCacheKey(Class<?> target, Method method) {
        return new CacheKey(ClassUtils.getUserClass(target), method);
    }

    @EqualsAndHashCode
    static class CacheKey {
        private final Class<?> type;
        private final Method method;

        public CacheKey(Class<?> type, Method method) {
            this.type = type;
            this.method = method;
        }
    }
}
