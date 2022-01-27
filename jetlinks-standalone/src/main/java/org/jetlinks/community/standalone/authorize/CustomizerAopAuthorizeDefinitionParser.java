package org.jetlinks.community.standalone.authorize;

import org.hswebframework.web.authorization.annotation.*;
import org.hswebframework.web.authorization.basic.define.EmptyAuthorizeDefinition;
import org.hswebframework.web.authorization.define.AopAuthorizeDefinition;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.CollectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author wusy
 * Company: 福建亿鑫海信息科技有限公司
 * Createtime : 2022/1/25 上午10:36
 * Description :
 * 注意：本内容仅限于福建亿鑫海信息科技有限公司内部传阅，禁止外泄以及用于其他的商业目的
 */
public class CustomizerAopAuthorizeDefinitionParser {

    private static final Set<Class<? extends Annotation>> types = new HashSet<>(Arrays.asList(
        Authorize.class,
        DataAccess.class,
        Dimension.class,
        Resource.class,
        ResourceAction.class,
        DataAccessType.class
    ));

    private final Set<Annotation> methodAnnotation;

    private final Set<Annotation> classAnnotation;

    private final Map<Class<? extends Annotation>, List<Annotation>> classAnnotationGroup;

    private final Map<Class<? extends Annotation>, List<Annotation>> methodAnnotationGroup;

    private final CustomizerAuthorizeDefinition definition;

    public CustomizerAopAuthorizeDefinitionParser(Class<?> targetClass, Method method) {
        definition = new CustomizerAuthorizeDefinition();
        definition.setTargetClass(targetClass);
        definition.setTargetMethod(method);

        methodAnnotation = AnnotatedElementUtils.findAllMergedAnnotations(method, types);

        classAnnotation = AnnotatedElementUtils.findAllMergedAnnotations(targetClass, types);

        classAnnotationGroup = classAnnotation.stream().collect(Collectors.groupingBy(Annotation::annotationType));

        methodAnnotationGroup = methodAnnotation.stream().collect(Collectors.groupingBy(Annotation::annotationType));
    }

    public AopAuthorizeDefinition parse() {
        //没有任何注解
        if (CollectionUtils.isEmpty(classAnnotation) && CollectionUtils.isEmpty(methodAnnotation)) {
            return EmptyAuthorizeDefinition.instance;
        }
        return definition;
    }
}
