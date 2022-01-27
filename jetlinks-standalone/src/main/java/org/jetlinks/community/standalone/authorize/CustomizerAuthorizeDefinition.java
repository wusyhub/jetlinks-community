package org.jetlinks.community.standalone.authorize;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hswebframework.web.authorization.define.AopAuthorizeDefinition;
import org.hswebframework.web.authorization.define.DimensionsDefinition;
import org.hswebframework.web.authorization.define.Phased;
import org.hswebframework.web.authorization.define.ResourcesDefinition;

import java.lang.reflect.Method;

/**
 * @author wusy
 * Company: 福建亿鑫海信息科技有限公司
 * Createtime : 2022/1/18 下午4:48
 * Description :
 * 注意：本内容仅限于福建亿鑫海信息科技有限公司内部传阅，禁止外泄以及用于其他的商业目的
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CustomizerAuthorizeDefinition implements AopAuthorizeDefinition {

    @JsonIgnore
    private Class<?> targetClass;

    @JsonIgnore
    private Method targetMethod;

    private ResourcesDefinition resources = new ResourcesDefinition();

    private DimensionsDefinition dimensions = new DimensionsDefinition();

    private String message = "error.access_denied";

    private Phased phased;

    public static AopAuthorizeDefinition from(Class<?> targetClass, Method method) {
        return new CustomizerAopAuthorizeDefinitionParser(targetClass, method).parse();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
