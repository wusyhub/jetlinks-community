package org.jetlinks.community.auth.listener;

import org.hswebframework.web.authorization.events.AuthorizationDecodeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.util.Base64;

/**
 * @author wusy
 * Company: 福建亿鑫海信息科技有限公司
 * Createtime : 2022/2/9 上午9:31
 * Description : 认证密码的时候进行base64解密
 * 注意：本内容仅限于福建亿鑫海信息科技有限公司内部传阅，禁止外泄以及用于其他的商业目的
 */
@Component
public class AuthorizationDecodeEventListener {

    @EventListener
    public void handleDemoEvent(AuthorizationDecodeEvent event) {
        event.setPassword(new String(Base64.getDecoder().decode(event.getPassword()), Charset.defaultCharset()));
    }
}
